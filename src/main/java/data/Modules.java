package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ast.RequestorFanIn;
import com.fasterxml.jackson.databind.module.SimpleModule;
import misc.DeserializerModification;
import misc.DoubleConverter;
import me.tongfei.progressbar.ProgressBar;
import net.sf.jsefa.Serializer;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;
import org.apache.commons.collections4.keyvalue.MultiKey;
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
import static util.FileUtil.readFile;
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
                    case "COPY":
                        addCommit(modification, modification.pathOld);
                        addCommit(modification, modification.pathNew);
                        break;
                    case "MODIFY":
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
                //早期リターン。既に辿っているなら、辿らない。
                if(modificationsComplete.stream().map(s->s.idCommit).collect(Collectors.toList()).contains(modificationTarget.idCommit))
                    continue;
                //早期リターン。対象コミットがaddかつmergeでないなら、親コミットはない。それ以上辿らない。
                if(modificationTarget.type.equals("ADD")&!modificationTarget.isMerge){
                    modificationsComplete.add(modificationTarget);
                    continue;
                }
                //対象コミットから、複数ある親コミットをそれぞれたどる。
                for(String idCommitParent : commits.get(modificationTarget.idCommit).parents) {
                    commitNow = commits.get(idCommitParent);
                    while(true) {
                        Modification finalModificationTarget = modificationTarget;
                        List<Modification> modifications =
                                commitNow
                                        .modifications
                                        .values()
                                        .stream()
                                        .filter(a->
                                                                !finalModificationTarget.type.equals("ADD")&finalModificationTarget.pathOld.equals(a.pathNew) |
                                                                        (
                                                                                finalModificationTarget.pathOld.equals(a.pathOld)
                                                                                        &finalModificationTarget.pathNew.equals(a.pathNew)
                                                                        )
                                        )
                                        .collect(Collectors.toList());
                        if (!modifications.isEmpty()) {
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
    public void identifyCommitsChild(){
        for(Module module: ProgressBar.wrap(modules.values(), "identifyCommitsChild")){
            Set<Modification> modificationsComplete = new HashSet<>();
            Queue<Modification> modificationsTarget = new ArrayDeque<>(module.modifications.values());

            while(0<modificationsTarget.size()) {
                Modification modificationTarget = modificationsTarget.poll();
                if(modificationsComplete.contains(modificationTarget)){
                    continue;
                }
                String pathModuleNew;
                if (modificationTarget.type.equals("DELETE")) {
                    pathModuleNew = modificationTarget.pathOld;
                }else{
                    pathModuleNew = modificationTarget.pathNew;
                }
                Module moduleNew = modules.get(pathModuleNew);
                for(Modification modificationNew: moduleNew.modifications.values()){
                    if(modificationNew.parents.contains(modificationTarget.idCommit)){
                        modificationTarget.children.add(modificationNew.idCommit);
                        modificationsTarget.add(modificationNew);
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
            String prefix = pathRepository.replace("\\", "/")+"/";
            int index = pathSource.indexOf(prefix);
            String pathModule = pathSource.substring(index+prefix.length());
            Module module = new Module(pathModule);
            modules.put(pathModule, modulesAll.get(pathModule));
        }
    }

    //対象モジュール全部について、コードメトリクスを算出する。
    public void calcCodeMetrics(String pathRepositoryFile, String[] commitEdgesFile, String pathRepositoryMethod, String[] commitEdgesMethod) throws IOException, GitAPIException {
        checkoutRepository(pathRepositoryFile, commitEdgesFile[1]);
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
            //module.calcLOC(pathRepositoryMethod);
        }
    }

    //FanInは個々のモジュールで独立に計算できない。仕方なく別口で計算する。
    private void calcFanIn(String pathRepositoryFile, String[] commitEdgesFile) throws GitAPIException, IOException {
        final String[] sourcePathDirs = {};
        final String[] libraries      = findFiles(pathRepositoryFile, ".jar", "test").toArray(new String[0]);
        //todo やっつけ仕事。これらのファイルのCompilationUnitオブジェクトを作ろうとするとNullポインタエラーが出るので、スキップする。
        final String[] sources        = findFiles(pathRepositoryFile, ".java", "test")
                .stream()
                .filter(item->
                        !item.contains("/lttng/org.eclipse.linuxtools.tmf.ui/src/org/eclipse/linuxtools/internal/tmf/ui/parsers/custom/CustomTxtTraceDefinition.java")
                        &!item.contains("/lttng/org.eclipse.linuxtools.gdbtrace.core/src/org/eclipse/linuxtools/internal/gdbtrace/core/trace/GdbTrace.java")
                        &!item.contains("/lttng/org.eclipse.linuxtools.lttng2.control.ui/src/org/eclipse/linuxtools/internal/lttng2/control/ui/views/property/TraceProbeEventPropertySource.java")
                        &!item.contains("/lttng/org.eclipse.linuxtools.lttng2.control.ui/src/org/eclipse/linuxtools/internal/lttng2/control/ui/views/property/TraceSessionPropertySource.java")
                        &!item.contains("/lttng/org.eclipse.linuxtools.tmf.core/src/org/eclipse/linuxtools/tmf/core/parsers/custom/CustomTxtTraceDefinition.java")
                )
                .collect(Collectors.toList())
                .toArray(new String[1]);

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
    public void calcProcessMetrics(Modules modulesAll, Commits commitsAll, Bugs bugsAll, String[] commitEdges) {
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcProcessMetrics")){
            Module module = modules.get(pathModule);
            module.calcModuleHistories(commitsAll, commitEdges);
            module.calcAuthors(commitsAll, commitEdges);
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
            //module.calcAddLOC(commitsAll, commitEdges);
            //module.calcDelLOC(commitsAll, commitEdges);
            //module.calcDevMinor(commitsAll, commitEdges);
            //module.calcDevMajor(commitsAll, commitEdges);
            //module.calcOwnership(commitsAll, commitEdges);
            //module.calcFixChgNum(commitsAll, bugsAll, commitEdges);
            //module.calcPastBugNum(commitsAll, bugsAll, commitEdges);
            //module.calcBugIntroNum(modulesAll, commitsAll, bugsAll, commitEdges);
            //module.calcLogCoupNum(modulesAll, commitsAll, bugsAll, commitEdges);
            //module.calcPeriod(commitsAll, commitEdges);
            //module.calcAvgInterval(commitsAll, commitEdges);
            //module.calcMaxInterval(commitsAll, commitEdges);
            //module.calcMinInterval(commitsAll, commitEdges);
            module.calcIsBuggy(commitsAll, bugsAll, commitEdges);
        }
    }
    //算出されたレコードをファイルに保存する。
    public void saveMetricsAsRecords(String pathDataset) {
        File dir = new File(pathDataset);
        File dirParent = new File(dir.getParent());
        dirParent.mkdirs();
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

    public void loadModulesFromFile(String pathModules) {
        List<String> paths = findFiles(pathModules, "json");
        for(String path : ProgressBar.wrap(paths, "loadModulesFromFile")){
            try {
                String strFile = readFile(path);
                ObjectMapper mapper = new ObjectMapper();
                SimpleModule simpleModule = new SimpleModule();
                simpleModule.addKeyDeserializer(MultiKey.class, new DeserializerModification());
                mapper.registerModule(simpleModule);
                Module module = mapper.readValue(strFile, new TypeReference<Module>() {});
                modules.put(module.path, module );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
