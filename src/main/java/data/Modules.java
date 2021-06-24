package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ast.RequesterFanIn;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
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
public class Modules implements Map<String, Module> {
    HashMap<String, Module> modules = new HashMap<>();

    public void analyzeAllModules(Commits commits) {
        identifyChangesOnModule(commits);
        giveNumberToModules();
        analyzeDevelopmentHistoryOnModule(commits);
        completeDevelopmentHistoryOnModule();
        identifyCommitsHead();
        identifyCommitsRoot();
    }

    private void giveNumberToModules() {
        int index = 0;
        for (Entry<String, Module> entry:modules.entrySet()) {
            entry.getValue().num=index;
            index+=1;
            entry.getValue().numOfModulesAll = modules.size();
        }
    }

    public void identifyChangesOnModule(Commits commits) {
        for (Commit commit : ProgressBar.wrap(commits.values(), "identifyChangeOnModules")) {
            for (ChangesOnModule changesOnModule : commit.idParent2Modifications.values()) {
                for (ChangeOnModule changeOnModule : changesOnModule.values()) {
                    if (!changeOnModule.pathOld.equals("/dev/null")) {
                        putChangeOnModule(changeOnModule, changeOnModule.pathOld);
                    }
                    if (!changeOnModule.pathNew.equals("/dev/null")) {
                        putChangeOnModule(changeOnModule, changeOnModule.pathNew);
                    }
                }
            }
        }
    }

    public void putChangeOnModule(ChangeOnModule changeOnModule, String pathModule) {
        if (!modules.containsKey(pathModule)) {
            Module module = new Module(pathModule);
            modules.put(pathModule, module);
        }
        modules.get(pathModule).changesOnModule.put(changeOnModule.idCommitParent, changeOnModule.idCommit, changeOnModule.pathOld, changeOnModule.pathNew, changeOnModule);
    }

    public void analyzeDevelopmentHistoryOnModule(Commits commits) {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "identifyCommitsParent")) {
            Module moduleTarget = modules.get(pathModule);
            Queue<ChangeOnModule> modificationsTarget = new ArrayDeque<>(moduleTarget.getChangesOnModule().values());

            ChangeOnModule changeOnModuleTarget;
            while (0 < modificationsTarget.size()) {
                changeOnModuleTarget = modificationsTarget.poll();
                moduleTarget.changesOnModule.put(changeOnModuleTarget.idCommitParent, changeOnModuleTarget.idCommit, changeOnModuleTarget.pathOld, changeOnModuleTarget.pathNew, changeOnModuleTarget);
                if (changeOnModuleTarget.type.equals("ADD")) {//親が存在しない。
                } else {//親が存在する。
                    if (!changeOnModuleTarget.parentsModification.isEmpty()) {//親特定済み
                        ChangesOnModule changesOnModule = new ChangesOnModule();
                        changeOnModuleTarget.loadAncestors(changesOnModule);
                        for (Entry<MultiKey<? extends String>, ChangeOnModule> m : changesOnModule.entrySet()) {
                            moduleTarget.changesOnModule.put(m.getKey(), m.getValue());
                        }
                        continue;
                    }
                    Set<String> idsCommitTarget = new HashSet<>();
                    Module moduleBefore = modules.get(changeOnModuleTarget.pathNewParent);
                    if (moduleBefore != null)
                        idsCommitTarget.addAll(moduleBefore.changesOnModule.values().stream().map(a -> a.idCommit).collect(Collectors.toList()));

                    Commit commitNow = commits.get(changeOnModuleTarget.idCommitParent);
                    while (true) {
                        if (idsCommitTarget.contains(commitNow.id)) {
                            boolean isOK = false;
                            for (ChangesOnModule changesOnModule : commitNow.idParent2Modifications.values()) {
                                for (ChangeOnModule changeOnModule : changesOnModule.values()) {
                                    if (Objects.equals(changeOnModuleTarget.pathNewParent, changeOnModule.pathNew)) {
                                        changeOnModuleTarget.parentsModification.put(changeOnModule.idCommitParent, changeOnModule.idCommit, changeOnModule.pathOld, changeOnModule.pathNew, changeOnModule);
                                        changeOnModuleTarget.parents.add(changeOnModule.idCommit);
                                        changeOnModule.childrenModification.put(changeOnModuleTarget.idCommitParent, changeOnModuleTarget.idCommit, changeOnModuleTarget.pathOld, changeOnModuleTarget.pathNew, changeOnModuleTarget);
                                        changeOnModule.children.add(changeOnModuleTarget.idCommit);
                                        modificationsTarget.add(changeOnModule);
                                        isOK = true;
                                    }
                                }
                            }
                            if (isOK) break;
                        }
                        commitNow = commits.get(commitNow.idParentMaster);
                        if (commitNow == null) break;
                    }
                }
            }
        }
    }

    public void completeDevelopmentHistoryOnModule() {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "completeCommitHistory")) {
            Module moduleTarget = modules.get(pathModule);
            Queue<ChangeOnModule> modificationsTarget = new ArrayDeque<>(moduleTarget.getChangesOnModule().values().stream().filter(a -> Objects.equals(a.type, "RENAME") | Objects.equals(a.type, "COPY")).collect(Collectors.toList()));
            ChangeOnModule changeOnModuleTarget;
            while (0 < modificationsTarget.size()) {//親
                changeOnModuleTarget = modificationsTarget.poll();
                for (ChangeOnModule changeOnModule : changeOnModuleTarget.parentsModification.values()) {
                    moduleTarget.changesOnModule.put(changeOnModule.idCommitParent, changeOnModule.idCommit, changeOnModule.pathOld, changeOnModule.pathNew, changeOnModule);
                    if (!moduleTarget.changesOnModule.containsValue(changeOnModule) & !modificationsTarget.contains(changeOnModule)) {
                        modificationsTarget.add(changeOnModule);
                    }
                }
            }
            modificationsTarget = new ArrayDeque<>(moduleTarget.getChangesOnModule().values().stream().filter(a -> Objects.equals(a.type, "RENAME") | Objects.equals(a.type, "COPY")).collect(Collectors.toList()));
            while (0 < modificationsTarget.size()) {//子
                changeOnModuleTarget = modificationsTarget.poll();
                for (ChangeOnModule changeOnModule : changeOnModuleTarget.childrenModification.values()) {
                    moduleTarget.changesOnModule.put(changeOnModule.idCommitParent, changeOnModule.idCommit, changeOnModule.pathOld, changeOnModule.pathNew, changeOnModule);
                    if (!moduleTarget.changesOnModule.containsValue(changeOnModule) & !modificationsTarget.contains(changeOnModule)) {
                        modificationsTarget.add(changeOnModule);
                    }
                }
            }
        }
    }

    public void identifyCommitsHead() {
        for (String path : modules.keySet()) {
            Module module = modules.get(path);
            for (ChangeOnModule changeOnModule : module.changesOnModule.values()) {
                if (changeOnModule.children.size() == 0) {
                    module.commitsHead.add(changeOnModule.idCommit);
                }
            }
        }
    }

    public void identifyCommitsRoot() {
        for (String path : modules.keySet()) {
            Module module = modules.get(path);
            for (ChangeOnModule changeOnModule : module.changesOnModule.values()) {
                if (changeOnModule.parents.size() == 0) {
                    module.commitsRoot.add(changeOnModule.idCommit);
                }
            }
        }
    }

    public void identifyTargetModules(Modules modulesAll, Repository repositoryMethod, String commitTarget) throws IOException, GitAPIException {
        List<String> pathSources = new ArrayList<>();
        RevCommit revCommit = repositoryMethod.parseCommit(repositoryMethod.resolve(commitTarget));
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
        for (String pathSource : ProgressBar.wrap(pathSources, "identifyTargetModules")) {
            //Module module = new Module(pathSource);
            Module moduleTarget = modulesAll.get(pathSource).clone();
            if (!pathSource.contains("test")) modules.put(pathSource, moduleTarget);
            //if(!pathSource.contains("test") & 0<commitsInInterval.size()) modules.put(pathSource, moduleTarget);
        }
    }

    public void calculateAST(Repository repositoryMethod, String revisionMethodTarget) throws IOException, GitAPIException {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "calculateAST")) {
            Module module = modules.get(pathModule);
            module.loadSrcFromRepository(repositoryMethod, revisionMethodTarget);
            module.calcCompilationUnit();
            module.calcAST();
        }
    }

    public void calculateCommitGraph(Commits commitsAll, Modules modulesAll, String revisionMethod_referHistoryFrom, String revisionMethod_target, Bugs bugsAll) throws IOException {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "calculateCommitGraph")) {
            Module module = modules.get(pathModule);
            module.calcCommitsInInterval(commitsAll, revisionMethod_referHistoryFrom, revisionMethod_target);
            module.calcModificationsInInterval(commitsAll, revisionMethod_referHistoryFrom, revisionMethod_target);
            module.calcCommitGraph(commitsAll, modulesAll, revisionMethod_target, bugsAll);
        }
    }

    public void calculateCodeMetrics(Repository repositoryFile, String revisionFileTarget, Repository repositoryMethod, String revisionMethodTarget) throws IOException, GitAPIException {
        checkoutRepository(repositoryFile, revisionFileTarget);
        calculateFanIn(repositoryFile.getDirectory().getParentFile().getAbsolutePath());
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "calcCodeMetrics")) {
            Module module = modules.get(pathModule);
            module.loadSrcFromRepository(repositoryMethod, revisionMethodTarget);
            module.calcCompilationUnit();
            module.calcFanOut();
            module.calcParameters();
            module.calcLocalVar();
            module.calcCommentRatio();
            module.calcCountPath();
            module.calcComplexity();
            module.calcExecStmt();
            module.calcMaxNesting();
        }
    }

    public void calculateFanIn(String pathRepositoryFile) {
        System.out.println("calculating FanIn...");
        final String[] sourcePathDirs = {};
        final String[] libraries = findFiles(pathRepositoryFile, ".jar").toArray(new String[0]);
        final String[] sources = findFiles(pathRepositoryFile, ".java").toArray(new String[0]);

        ASTParser parser = ASTParser.newParser(AST.JLS14);
        final Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_14);
        parser.setCompilerOptions(options);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        parser.setEnvironment(libraries, sourcePathDirs, null, true);

        String[] keys = new String[]{""};
        RequesterFanIn requesterFanIn = new RequesterFanIn(modules);
        parser.createASTs(sources, null, keys, requesterFanIn, new NullProgressMonitor());
        //int countCalledMethod = 0;
        for (String idMethodCalled : ProgressBar.wrap(requesterFanIn.methodsCalled, "processMethodCalled")) {
            if (idMethodCalled == null) continue;
            //countCalledMethod++;
            boolean flag = false;
            for (String pathMethod : modules.keySet()) {
                String idMethod = modules.get(pathMethod).id;
                if (Objects.equals(idMethod, idMethodCalled)) {
                    modules.get(pathMethod).fanIn++;
                    //flag=true;
                    break;
                }
            }
            //if(!flag)System.out.println(idMethodCalled);
        }
        System.out.println("FanIn caluculated");
    }

    public void calculateProcessMetrics(Commits commitsAll, String revisionMethod_referHistoryFrom, String revisionMethod_target) {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "calcProcessMetrics")) {
            Module module = modules.get(pathModule);
            module.calcCommitsInInterval(commitsAll, revisionMethod_referHistoryFrom, revisionMethod_target);
            module.calcModificationsInInterval(commitsAll, revisionMethod_referHistoryFrom, revisionMethod_target);
            module.calcModuleHistories();
            module.calcAuthors();
            module.calcStmtAdded();
            module.calcMaxStmtAdded();
            module.calcAvgStmtAdded();
            module.calcStmtDeleted();
            module.calcMaxStmtDeleted();
            module.calcAvgStmtDeleted();
            module.calcChurn();
            module.calcMaxChurn();
            module.calcAvgChurn();
            module.calcDecl();
            module.calcCond();
            module.calcElseAdded();
            module.calcElseDeleted();
        }
    }

    public void calculateIsBuggy(Commits commitsAll, String revisionMethod_target, String revisionMethod_referBugReportsUntil, Bugs bugsAll) {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "calculateIsBuggy")) {
            Module module = modules.get(pathModule);
            module.calcIsBuggy(commitsAll, revisionMethod_target, revisionMethod_referBugReportsUntil, bugsAll);
        }
    }

    public void calculateHasBeenBuggy(Commits commitsAll, String revisionMethod_target, Bugs bugsAll) {
        for (String pathModule : ProgressBar.wrap(modules.keySet(), "calculateHasBeenBuggy")) {
            Module module = modules.get(pathModule);
            module.calcHasBeenBuggy(commitsAll, revisionMethod_target, bugsAll);
        }
    }

    public void saveAsJson(String pathModules) {
        int count = 0;
        for (Entry<String, Module> entry : ProgressBar.wrap(modules.entrySet(), "saveModules")) {
            String path = pathModules + "/" + entry.getKey() + ".json";
            File file = new File(path);
            path = file.getAbsolutePath();
            if (254 < path.length()) {
                path = pathModules + "/" + Integer.toString(count) + ".json";
                file = new File(path);
            }
            File dir = new File(file.getParent());
            dir.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(writer, entry.getValue());
                count++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAsCSV(String pathDataset) {
        File dir = new File(pathDataset);
        File dirParent = new File(dir.getParent());
        dirParent.mkdirs();
        try {
            FileOutputStream fos = new FileOutputStream(pathDataset);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(osw);
            CsvConfiguration config = new CsvConfiguration();
            config.setFieldDelimiter(',');
            config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
            Serializer serializer = CsvIOFactory.createFactory(config, Module.class).createSerializer();

            serializer.open(writer);
            for (String key : modules.keySet()) {
                Module module = modules.get(key);
                serializer.write(module);
            }
            serializer.close(true);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadModulesFromFile(String pathModules) {
        List<String> paths = findFiles(pathModules, ".json");
        for (String path : ProgressBar.wrap(paths, "loadModulesFromFile")) {
            try {
                String strFile = readFile(path);
                ObjectMapper mapper = new ObjectMapper();
                SimpleModule simpleModule = new SimpleModule();
                simpleModule.addKeyDeserializer(MultiKey.class, new DeserializerModification());
                mapper.registerModule(simpleModule);
                Module module = mapper.readValue(strFile, new TypeReference<Module>() {
                });
                modules.put(module.path, module);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
