package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import ast.RequestorFanIn;
import misc.DoubleConverter;
import me.tongfei.progressbar.ProgressBar;
import net.sf.jsefa.Serializer;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.config.CsvConfiguration;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static util.FileUtil.findFiles;
import static util.RepositoryUtil.checkoutRepository;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Modules implements Map<String, Module>{
    HashMap<String, Module> modules = new HashMap<>();

    public void analyzeModules(Commits commits){
        identifyModificationsOnModule(commits);
        identifyCommitsParent(commits);
        identifyCommitsChild();
        identifyCommitsHead();
        identifyCommitsRoot();
    }


    public void identifyModificationsOnModule(Commits commits){
        for(Commit commit: ProgressBar.wrap(commits.values(), "identifyModificationsOnModule")){
            for(Modification modification: commit.modifications.values()){
                switch (modification.type) {
                    case "ADD":
                        addCommit(modification, modification.pathNew);
                        break;
                    case "RENAME":
                        addCommit(modification, modification.pathOld);
                        addCommit(modification, modification.pathNew);
                        break;
                    case "COPY":
                        addCommit(modification, modification.pathOld);
                        addCommit(modification, modification.pathNew);
                        break;
                    case "MODIFY":
                        addCommit(modification, modification.pathOld);
                        break;
                    case "DELETE":
                        addCommit(modification, modification.pathOld);
                        break;
                }
            }
        }
    }


    public void addCommit(Modification modification, String pathModule) {
        if (!modules.containsKey(pathModule)) {
            Module module = new Module(pathModule);
            modules.put(pathModule, module);
        }
        modules.get(pathModule).modifications.put(modification.idCommit, modification.pathOld, modification.pathNew, modification);
    }


    //個々のモジュールについて、そのモジュールに対するコミットについて、親子関係を解析する。親の方向。
    //リネームを貫通して追跡する。
    public void identifyCommitsParent(Commits commits){
        for(String pathModule: ProgressBar.wrap(modules.keySet(),"identifyCommitsParent")) {
            ArrayList<Modification> modificationsComplete = new ArrayList<>();
            Queue<Modification> modificationsTarget = new ArrayDeque<>(modules.get(pathModule).modifications.values());

            Modification modificationTarget;
            Commit commitNow;
            while(0 < modificationsTarget.size()) {
                modificationTarget = modificationsTarget.poll();
                //既に辿っているなら、辿らない。
                if(modificationsComplete.stream().map(s->s.idCommit).collect(Collectors.toList()).contains(modificationTarget.idCommit))continue;
                //対象コミットがaddかつmergeでないなら、親コミットはない。それ以上辿らない。
                if(modificationTarget.pathOld.equals("/dev/null")&!commits.get(modificationTarget.idCommit).isMerge){
                        modificationsComplete.add(modificationTarget);
                        continue;
                }
                //対象コミットから、複数ある親コミットをそれぞれたどる。
                for(String idCommitParent : commits.get(modificationTarget.idCommit).parents) {
                    commitNow = commits.get(idCommitParent);
                    while(true) {
                        Modification finalModificationTarget = modificationTarget;
                        List<Modification> modifications = commitNow.modifications.values().stream().filter(a-> finalModificationTarget.pathOld.equals(a.pathNew)).collect(Collectors.toList());
                        if(0<modifications.size()
                                &&(!(modificationTarget.type.equals("ADD")&commits.get(modificationTarget.idCommit).isMerge)&modificationTarget.pathOld.equals(modifications.get(0).pathNew))
                                |((modificationTarget.type.equals("ADD")&commits.get(modificationTarget.idCommit).isMerge)&modificationTarget.pathNew.equals(modifications.get(0).pathNew))
                        ) {
                            modificationTarget.parents.add(commitNow.id);
                            Modification modification = modifications.get(0);
                            modificationsTarget.add(modification);
                            break;
                        }
                        if(0 == commitNow.parents.size()) break;
                        else commitNow = commits.get(commitNow.parents.get(0));
                    }
                }
                modificationsComplete.add(modificationTarget);
            }
            modules.get(pathModule).modifications.clear();
            for(Modification tmp:modificationsComplete) {
                modules.get(pathModule).modifications.put(tmp.idCommit, tmp.pathOld, tmp.pathNew, tmp);
            }
        }
    }

    //個々のモジュールについて、そのモジュールに対するコミットについて、親子関係を解析する。子の方向。
    //リネームを貫通して追跡する。
    //todo: gitは過去の方向にはよく追跡するが、未来の方向にはうまく追跡できない。（未来方向にはmergeコミットに当たるものがないため。）これを考慮しなければいけない。すべての子コミットの方向へ探索しなければいけない。
    public void identifyCommitsChild(){
        for(Module module: ProgressBar.wrap(modules.values(), "identifyCommitsChild"){
            ArrayList<Modification> modificationsComplete = new ArrayList<>();
            Queue<Modification> modificationsTarget = new ArrayDeque<>(module.modifications.values());
            //それぞれのmodificationについて、自分をparentsに持つmodificationを探す。自分がrenameかcopyの場合、rename後、commit後のモジュールの中を探す。見つかったらそいつをchildsにaddしてやる。
            while(0<modificationsTarget.size()) {
                Modification modificationTarget = modificationsTarget.poll();
                String pathModule = modificationTarget.pathNew;
                if (modificationTarget.type.equals("DELETE")) {
                    if (modificationTarget.isMerge) continue;
                    else pathModule = modificationTarget.pathOld;
                }
                Module moduleNew = modules.get(pathModule);
                for(Modification modificationNewModule: moduleNew.modifications.values()){
                    if(modificationNewModule.parents.contains(modificationTarget.idCommit)){
                        modificationTarget.children.add(modificationNewModule.idCommit);
                        modificationsTarget.add(modificationNewModule);
                    }
                }
                modificationsComplete.add(modificationTarget);
            }

            module.modifications.clear();
            for(Modification modificationTemp: modificationsComplete) {
                module.modifications.put(modificationTemp.idCommit, modificationTemp.pathOld, modificationTemp.pathNew, modificationTemp);
            }
        }
    }

    public void identifyCommitsHead() {
        for(String path: modules.keySet()) {
            Module module = modules.get(path);
            //childrenの要素がないidCommitが、そのモジュールに対するヘッドコミット。
            for(Modification modification: module.modifications.values()) {
                if(0== modification.children.size()) {
                    module.commitsHead.add(modification.idCommit);
                }
            }
        }
    }

    public void identifyCommitsRoot() {
        for(String path: modules.keySet()) {
            Module module = modules.get(path);
            //parentsの要素がないidCommitが、そのモジュールにとってのルートコミット。
            for (Modification modification : module.modifications.values()) {
                if (0 == modification.parents.size()) {
                    module.commitsRoot.add(modification.idCommit);
                }
            }
        }
    }

    public void save(String pathModules) {
        for(Map.Entry<String, Module> entry : ProgressBar.wrap(modules.entrySet(), "saveModules")) {
            File file =  new File(pathModules+"/"+entry.getKey()+".json");
            File dir = new File(file.getParent());
            dir.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(writer, entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void identifyTargetModules(Modules modulesAll, String pathRepository, String[] commitEdges) throws IOException, GitAPIException, GitAPIException {
        checkoutRepository(pathRepository, commitEdges[1]);
        List<String> pathSources = findFiles(pathRepository, ".mjava", "test");
        for(String pathSource: ProgressBar.wrap(pathSources, "identifyTargetModules")) {
            pathSource=pathSource.replace("\\", "/");
            String prefix = pathRepository.replace("\\", "/")+"/";
            int index = pathSource.indexOf(prefix);
            String pathModule = pathSource.substring(index+prefix.length());
            Module module = new Module(pathModule);
            modules.put(pathModule, modulesAll.get(pathModule));
        }
    }

    //対象モジュール全部について、コードメトリクスを算出する。
    public void calcCodeMetrics(String pathRepositoryFile, String[] commitEdgesFile, String pathRepositoryMethod, String[] commitEdgesMethod) throws IOException, GitAPIException {
        checkoutRepository(pathRepositoryMethod, commitEdgesMethod[1]);
        System.out.println("calculating FanIn...");
        calcFanIn(pathRepositoryFile, commitEdgesFile);
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcCodeMetrics")){
            Module module = modules.get(pathModule);
            module.calcFanOut(pathRepositoryMethod);
            module.calcParameters(pathRepositoryMethod);
            module.calcLocalVar(pathRepositoryMethod);
            module.calcCommentRatio(pathRepositoryMethod);
            module.calcCountPath(pathRepositoryMethod);
            module.calcComplexity(pathRepositoryMethod);
            module.calcExecStmt(pathRepositoryMethod);
            module.calcMaxNesting(pathRepositoryMethod);
        }
    }

    //FanInは個々のモジュールで独立に計算できない。仕方なく別口で計算する。
    private void calcFanIn(String pathRepositoryFile, String[] commitEdgesFile) throws GitAPIException, IOException {
        checkoutRepository(pathRepositoryFile, commitEdgesFile[1]);
        final String[] sourcePathDirs = {};
        final String[] libraries      = findFiles(pathRepositoryFile, ".jar", "test").toArray(new String[0]);
        final String[] sources        = findFiles(pathRepositoryFile, ".java", "test").toArray(new String[0]);

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        final Map<String,String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
        parser.setCompilerOptions(options);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        parser.setEnvironment(libraries, sourcePathDirs, null, true);

        String[] keys = new String[] {""};
        RequestorFanIn requestorFanIn = new RequestorFanIn(modules);
        parser.createASTs(sources, null, keys, requestorFanIn, new NullProgressMonitor());
        for(String idMethodCalled: requestorFanIn.methodsCalled) {
            for(String pathMethod: modules.keySet()) {
                String idMethod = modules.get(pathMethod).id;
                if(idMethod.equals(idMethodCalled)) {
                    modules.get(pathMethod).fanIn++;
                }
            }
        }
    }

    //対象モジュール全部について、プロセスメトリクスを算出する。
    public void calcProcessMetrics(Commits commitsAll, Bugs bugsAll, String[] commitEdges) {
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcProcessMetrics")){
            Module module = modules.get(pathModule);
            module.calcModuleHistories(commitsAll, commitEdges);
            module.calcDevTotal(commitsAll, commitEdges);
            module.calcStmtAdded(commitsAll, commitEdges);
            module.calcMaxStmtAdded(commitsAll, commitEdges);
            module.calcAvgStmtAdded(commitsAll, commitEdges);
            module.calcStmtDeleted(commitsAll, commitEdges);
            module.calcMaxStmtDeleted(commitsAll, commitEdges);
            module.calcAvgStmtDeleted(commitsAll, commitEdges);
            module.calcChurn(commitsAll, commitEdges);
            module.calcMaxChurn(commitsAll, commitEdges);
            module.calcAvgChurn(commitsAll, commitEdges);
            module.calcDecl(commitsAll, commitEdges);
            module.calcCond(commitsAll, commitEdges);
            module.calcElseAdded(commitsAll, commitEdges);
            module.calcElseDeleted(commitsAll, commitEdges);
            module.calcIsBuggy(commitsAll, bugsAll, commitEdges);
        }
    }
    //算出されたレコードをファイルに保存する。
    public void saveMetricsAsRecords(String pathDataset) {
        try {
            FileOutputStream fos= new FileOutputStream(pathDataset);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(osw);
            CsvConfiguration config = new CsvConfiguration();
            config.setFieldDelimiter(',');
            config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
            Serializer serializer = CsvIOFactory.createFactory(config, Module.class).createSerializer();

            serializer.open(writer);
            for(String key: modules.keySet()) {
                Module module=modules.get(key);
                serializer.write(module);
            }
            serializer.close(true);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int size() {
        return modules.size();
    }

    @Override
    public boolean isEmpty() {
        return modules.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return modules.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return modules.containsValue(value);
    }

    @Override
    public Module get(Object key) {
        return modules.get(key);
    }

    @Override
    public Module put(String key, Module value) {
        return modules.put(key, value);
    }

    @Override
    public Module remove(Object key) {
        return modules.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Module> m) {
        modules.putAll(m);
    }

    @Override
    public void clear() {
        modules.clear();
    }

    @Override
    public Set<String> keySet() {
        return modules.keySet();
    }

    @Override
    public Collection<Module> values() {
        return modules.values();
    }

    @Override
    public Set<Entry<String, Module>> entrySet() {
        return modules.entrySet();
    }
}
