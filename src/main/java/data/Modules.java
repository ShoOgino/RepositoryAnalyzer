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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

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
        identifyCommitParent(commits);
        completeCommitHistory();
        checkParent();
        checkChildren();
        identifyCommitsHead();
        identifyCommitsRoot();
    }

    private void check() {
        for(Module module: ProgressBar.wrap(modules.values(),"check")) {
            boolean hasAddOrRenameOrCopy = false;
            for(Modification modification: module.modifications.values()) {
                if(Objects.equals(modification.type, "ADD")
                | Objects.equals(modification.type, "RENAME")
                | Objects.equals(modification.type, "COPY")){
                    hasAddOrRenameOrCopy=true;
                }
            }
            if (hasAddOrRenameOrCopy) continue;
            else System.out.println(module.path);
        }
    }

    public void checkParent(){
        int countAll = 0;
        int countYabai =0;
        int count = 0;
        for(Module module: ProgressBar.wrap(modules.values(), "testIdentifyParents")){
            for(Modification modification: module.modifications.values()){
                if(modification.type.equals("ADD"))continue;;
                countAll++;
                if(modification.parents.size()==0){
                    System.out.println(module.path);
                    System.out.println(modification.idCommit);
                    if(Objects.equals(modification.type, "RENAME") | Objects.equals(modification.type, "COPY")) countYabai++;
                    count++;
                    continue;
                }
                boolean isParentOk = true;
                for(Modification modificationParent: modification.parentsModification.values()){
                    if(!Objects.equals(modification.sourceOld, modificationParent.sourceNew)){
                        isParentOk=false;
                        break;
                    }
                }
                if(isParentOk)continue;
                count++;
                if(Objects.equals(modification.type, "RENAME") | Objects.equals(modification.type, "COPY")) countYabai++;
                System.out.println(module.path);
                System.out.println(modification.idCommit);
                System.out.println(modification.sourceOld);
                for(Modification modificationBefore: modification.parentsModification.values()) {
                    System.out.println(modificationBefore.idCommit);
                    System.out.println(modificationBefore.sourceNew);
                }
            }
        }
        System.out.println(countAll);
        System.out.println(countYabai);
        System.out.println(count);
    }


    public void checkChildren(){
        int countAll = 0;
        int countYabai =0;
        int count = 0;
        for(Module module: ProgressBar.wrap(modules.values(), "testIdentifyChildlen")){
            for(Modification modification: module.modifications.values()){
                if(modification.type.equals("DELETE"))continue;;
                countAll++;
                /*
                if(modification.children.size()==0){
                    System.out.println(module.path);
                    System.out.println(modification.idCommit);
                    if(Objects.equals(modification.type, "RENAME") | Objects.equals(modification.type, "COPY")) countYabai++;
                    count++;
                    continue;
                }
                 */
                boolean isChildOK = true;
                for(Modification child: modification.childrenModification.values()){
                    if(!Objects.equals(modification.sourceNew, child.sourceOld)){
                        isChildOK=false;
                        break;
                    }
                }
                if(isChildOK)continue;
                count++;
                if(Objects.equals(modification.type, "RENAME") | Objects.equals(modification.type, "COPY")) countYabai++;
                System.out.println(module.path);
                System.out.println(modification.idCommit);
                System.out.println(modification.sourceNew);
                for(Modification modificationAfter: modification.childrenModification.values()) {
                    System.out.println(modificationAfter.idCommit);
                    System.out.println(modificationAfter.sourceOld);
                }
            }
        }
        System.out.println(countAll);
        System.out.println(countYabai);
        System.out.println(count);
    }


    public void identifyModificationsOnModule(Commits commits){
        for(Commit commit: ProgressBar.wrap(commits.values(), "identifyModificationsOnModule")) {
            for (Modifications modifications : commit.idParent2Modifications.values()) {
                for (Modification modification : modifications.values()) {
                    if(!modification.pathOld.equals("/dev/null")) {
                        addCommit(modification, modification.pathOld);
                    }
                    if(!modification.pathNew.equals("/dev/null")) {
                        addCommit(modification, modification.pathNew);
                    }
                }
            }
        }
    }


    public void addCommit(Modification modification, String pathModule) {
        if (!modules.containsKey(pathModule)) {
            Module module = new Module(pathModule);
            modules.put(pathModule, module);
        }
        modules.get(pathModule).modifications.put(modification.idCommitParent, modification.idCommit, modification.pathOld, modification.pathNew, modification);
    }


    //個々のモジュールについて、そのモジュールに対するコミットについて、親子関係を解析する。親の方向。
    public void identifyCommitParent(Commits commits){
        for(String pathModule: ProgressBar.wrap(modules.keySet(),"identifyCommitsParent")) {
            Module moduleTarget = modules.get(pathModule);
            Queue<Modification> modificationsTarget = new ArrayDeque<>(moduleTarget.getModifications().values());

            Modification modificationTarget;
            while(0 < modificationsTarget.size()) {
                modificationTarget = modificationsTarget.poll();
                moduleTarget.modifications.put(modificationTarget.idCommitParent, modificationTarget.idCommit, modificationTarget.pathOld, modificationTarget.pathNew, modificationTarget);
                if (modificationTarget.type.equals("ADD")) {//親が存在しない。
                } else {//親が存在する。
                    if(!modificationTarget.parentsModification.isEmpty()){//親特定済み
                        Modifications modifications = new Modifications();
                        modificationTarget.loadAncestors(modifications);
                        for (Entry<MultiKey<? extends String>, Modification> m : modifications.entrySet()) {
                            moduleTarget.modifications.put(m.getKey(), m.getValue());
                        }
                        continue;
                    }
                    Set<String> idsCommitTarget = new HashSet<>();
                    Module moduleBefore = modules.get(modificationTarget.pathNewParent);
                    if (moduleBefore != null) idsCommitTarget.addAll(moduleBefore.modifications.values().stream().map(a -> a.idCommit).collect(Collectors.toList()));

                    Commit commitNow = commits.get(modificationTarget.idCommitParent);
                    while (true) {
                        if (idsCommitTarget.contains(commitNow.id)) {
                            boolean isOK=false;
                            for (Modifications modifications : commitNow.idParent2Modifications.values()) {
                                for (Modification modification : modifications.values()) {
                                    if (Objects.equals(modificationTarget.pathNewParent, modification.pathNew)) {
                                        modificationTarget.parentsModification.put(modification.idCommitParent, modification.idCommit, modification.pathOld, modification.pathNew, modification);
                                        modificationTarget.parents.add(modification.idCommit);
                                        modification.childrenModification.put(modificationTarget.idCommitParent, modificationTarget.idCommit, modificationTarget.pathOld, modificationTarget.pathNew, modificationTarget);
                                        modification.children.add(modificationTarget.idCommit);
                                        modificationsTarget.add(modification);
                                        isOK = true;
                                    }
                                }
                            }
                            if(isOK)break;
                        }
                        commitNow = commits.get(commitNow.idParentMaster);
                        if(commitNow==null)break;
                    }
                }
            }
        }
    }

    //個々のモジュールについて、そのモジュールに対するコミットについて、親子関係を解析する。子の方向。
    //リネームを貫通して追跡する。
    public void completeCommitHistory() {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "completeCommitHistory")) {
            Module moduleTarget = modules.get(pathModule);
            Queue<Modification> modificationsTarget = new ArrayDeque<>(moduleTarget.getModifications().values().stream().filter(a->Objects.equals(a.type, "RENAME")|Objects.equals(a.type, "COPY")).collect(Collectors.toList()));
            Modification modificationTarget;
            while (0 < modificationsTarget.size()) {//親
                modificationTarget = modificationsTarget.poll();
                for(Modification modification: modificationTarget.parentsModification.values()) {
                    moduleTarget.modifications.put(modification.idCommitParent, modification.idCommit, modification.pathOld, modification.pathNew, modification);
                    modificationsTarget.add(modification);
                }
            }
            modificationsTarget = new ArrayDeque<>(moduleTarget.getModifications().values().stream().filter(a->Objects.equals(a.type, "RENAME")|Objects.equals(a.type, "COPY")).collect(Collectors.toList()));
            while (0 < modificationsTarget.size()) {//子
                modificationTarget = modificationsTarget.poll();
                for(Modification modification: modificationTarget.childrenModification.values()) {
                    moduleTarget.modifications.put(modification.idCommitParent, modification.idCommit, modification.pathOld, modification.pathNew, modification);
                    modificationsTarget.add(modification);
                }
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
                if (modification.idCommitParent.equals("")) {
                    module.commitsRoot.add(modification.idCommit);
                }
            }
        }
    }

    public void saveToFile(String pathModules) {
        int count=0;
        for(Entry<String, Module> entry : ProgressBar.wrap(modules.entrySet(), "saveModules")) {
            String path = pathModules+"/"+entry.getKey()+".json";
            File file =  new File(path);
            path = file.getAbsolutePath();
            if(255<path.length()){
                path = pathModules+"/"+Integer.toString(count)+".json";
                file = new File(path);
            }
            File dir = new File(file.getParent());
            dir.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(writer, entry.getValue());
                count++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void identifyTargetModules(Modules modulesAll, Repository repositoryMethod, String[] commitEdges) throws IOException, GitAPIException, GitAPIException {
        List<String> pathSources = new ArrayList<>();
        RevCommit revCommit = repositoryMethod.parseCommit(repositoryMethod.resolve(commitEdges[1]));
        RevTree tree = revCommit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repositoryMethod)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathSuffixFilter.create(".mjava"));
            while (treeWalk.next()) {
                //System.out.println(treeWalk.getPathString());
                pathSources.add(treeWalk.getPathString());
            }
        }
        for(String pathSource: ProgressBar.wrap(pathSources, "identifyTargetModules")) {
            modules.put(pathSource, modulesAll.get(pathSource));
        }
    }

    //対象モジュール全部について、コードメトリクスを算出する。
    public void calcCodeMetrics(Repository repositoryFile, String[] commitEdgesFile,Repository repositoryMethod, String[] commitEdgesMethod) throws IOException, GitAPIException {
        checkoutRepository(repositoryFile, commitEdgesFile[1]);
        System.out.println("calculating FanIn...");
        calcFanIn(repositoryFile.getDirectory().getParentFile().getAbsolutePath(), commitEdgesFile);
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcCodeMetrics")){
            Module module = modules.get(pathModule);
            module.loadSrcFromRepository(repositoryMethod, commitEdgesMethod[1]);
            module.calcFanOut();
            module.calcParameters();
            module.calcLocalVar();
            module.calcCommentRatio();
            module.calcCountPath();
            module.calcComplexity();
            module.calcExecStmt();
            module.calcMaxNesting();
            //module.calcLOC(pathRepositoryMethod);
        }
    }

    //FanInは個々のモジュールで独立に計算できない。仕方なく別口で計算する。
    public void calcFanIn(String pathRepositoryFile, String[] commitEdgesFile) throws GitAPIException, IOException {
        final String[] sourcePathDirs = {};
        final String[] libraries      = findFiles(pathRepositoryFile, ".jar", "test").toArray(new String[0]);
        final String[] sources        = findFiles(pathRepositoryFile, ".java", "test").toArray(new String[0]);

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        final Map<String,String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
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
                if(Objects.equals(idMethod, idMethodCalled)) {
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
            module.calcHasBeenBuggy(commitsAll, bugsAll, commitEdges);
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
        List<String> paths = findFiles(pathModules, ".json");
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
