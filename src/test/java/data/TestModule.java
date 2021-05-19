package data;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestModule {
    public static String   pathProject   = "C:\\Users\\ShoOgino\\data\\workspace\\MLTool\\datasets\\egit";
    public static String   idCommitHead = "b459d7381ea57e435bd9b71eb37a4cb4160e252b";
    public static String[] commitEdgesMethod = {"2c1b0f4ad24fb082e5eb355e912519c21a5e3f41", "1241472396d11fe0e7b31c6faf82d04d39f965a6", "2774041935d41453e5080f0e3cbeef136a05597d"};
    public static String[] commitEdgesFile =  {"dfbdc456d8645fc0c310b5e15cf8d25d8ff7f84b","0cc8d32aff8ce91f71d2cdac8f3e362aff747ae7"};

    public static String pathRepositoryMethod = pathProject+"/repositoryMethod";
    public static Repository repositoryMethod;
    public static String pathRepositoryFile = pathProject+"/repositoryFile";
    public static Repository repositoryFile;

    static {
        try {
            repositoryFile = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryFile + "/.git")).build();
            repositoryMethod =  new FileRepositoryBuilder().setGitDir(new File(pathRepositoryMethod + "/.git")).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+".csv";
    public static String pathModules = pathProject+"/modules.json";
    public static String pathCommits = pathProject+"/commits";
    public static String pathBugs = pathProject+"/bugs.json";

    public static Commits commitsAll = new Commits();
    public static Modules modulesAll = new Modules();
    public static Bugs bugsAll = new Bugs();

    public TestModule() throws IOException {
    }

    @BeforeAll
    static public void setUp() throws GitAPIException, IOException {
        util.RepositoryUtil.checkoutRepository(repositoryFile, commitEdgesFile[1]);
        util.RepositoryUtil.checkoutRepository(repositoryMethod, commitEdgesMethod[1]);
        commitsAll.loadCommitsFromRepository(repositoryMethod, idCommitHead, pathCommits);
        modulesAll.analyzeModules(commitsAll);
        bugsAll.loadBugsFromFile(pathBugs);
    }

    @Test
    public void testCalcFanOut1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/command/FetchConfiguredRemoteCommand#execute(ExecutionEvent).mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(10, module.getFanOut());
    }

    @Test
    public void testCalcFanOut2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitMessageComponentStateManager#persistState(Repository,CommitMessageComponentState).mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(12, module.getFanOut());
    }

    @Test
    public void testCalcFanOut3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/SynchronizeWithActionHandler#execute(ExecutionEvent).mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(15, module.getFanOut());
    }
    @Test
    public void testCalcFanOut4(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/CachedCheckboxTreeViewer#updateCheckState(Object,boolean).mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(11, module.getFanOut());
    }
    @Test
    public void testCalcFanOut5(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/BranchHierarchyNode#getPathList().mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(12, module.getFanOut());
    }

    @Test
    public void testCalcLocalVar1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/push/SimpleConfigurePushDialog#createDialogArea(Composite).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(35, module.getLocalVar());
    }
    @Test
    public void testCalcLocalVar2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#decorateImage(Image,Object).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(12, module.getLocalVar());
    }
    @Test
    public void testCalcLocalVar3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/history/GitHistoryPage#buildFilterPaths(IResource[],File[],Repository).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(10, module.getLocalVar());
    }
    @Test
    public void testCalcLocalVar4(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CompareTreeView#reactOnOpen(OpenEvent).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(17, module.getLocalVar());
    }
    @Test
    public void testCalcLocalVar5(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/commands/shared/AbstractSharedCommandHandler#getRepository(ExecutionEvent).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(10, module.getLocalVar());
    }


    @Test
    public void testCalcCommentRatio1(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/ContainerTreeIterator#ContainerTreeIterator(Repository,IWorkspaceRoot).mjava");
        module.calcCommentRatio(pathRepositoryMethod);
        assertEquals(String.format("%.5f", 0.684210538864135), String.format("%.5f", module.getCommentRatio()));
    }
    @Test
    public void testCalcCommentRatio2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/decorators/DecoratableResourceHelper#createThreeWayTreeWalk(RepositoryMapping,ArrayList[String]).mjava");
        module.calcCommentRatio(pathRepositoryMethod);
        assertEquals(String.format("%.5f",0.161290317773818), String.format("%.5f",module.getCommentRatio()));
    }

    @Test
    public void testCalcCommentRatio3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#getSimpleText(RepositoryTreeNode).mjava");
        module.calcCommentRatio(pathRepositoryMethod);
        assertEquals(String.format("%.5f",0.0933333333), String.format("%.5f",module.getCommentRatio()));
    }
    @Test
    public void testCalcCommentRatio4(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/op/ResetOperation#execute(IProgressMonitor).mjava");
        module.calcCommentRatio(pathRepositoryMethod);
        assertEquals(String.format("%.5f",0.19047619), String.format("%.5f",module.getCommentRatio()));
    }

    @Test
    public void testCalcCountPath1(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/ContainerTreeIterator#isEntryIgnoredByTeamProvider(IResource).mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(4, module.getCountPath());
    }

    @Test
    public void testCalcCountPath2(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/synchronize/GitSyncInfo#calculateKindImpl(Repository,TreeWalk,int,int).mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(16, module.getCountPath());
    }

    @Test
    public void testCalcCountPath3(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/synchronize/dto/GitSynchronizeData#updateRevs().mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(8, module.getCountPath());
    }
    @Test
    public void testCalcCountPath4(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#decorateImage(Image,Object).mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(109, module.getCountPath());
    }
    @Test
    public void testCalcCountPath5(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/command/ShowInHistoryCommand#execute(ExecutionEvent).mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(51, module.getCountPath());
    }
    @Test
    public void testCalcComplexity1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitDialog#getFileStatus(String,IndexDiff).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(14, module.getComplexity());
    }
    @Test
    public void testCalcComplexity2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/history/FileDiff#compute(TreeWalk,RevCommit).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(13, module.getComplexity());
    }
    @Test
    public void testCalcComplexity3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/command/RepositoriesViewCommandHandler#enableWorkingDirCommand(Object).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(13, module.getComplexity());
    }
    @Test
    public void testCalcComplexity4(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/RepositoryActionHandler#convertSelection(IEvaluationContext,Object).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(11, module.getComplexity());
    }
    @Test
    public void testCalcComplexity5(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/decorators/DecoratableResourceAdapter.RecursiveStateFilter#shouldRecurse(TreeWalk).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(9, module.getComplexity());
    }
    @Test
    public void testCalcExecStmt1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/commit/CommitUI#getSelectedFiles().mjava");
        module.calcExecStmt(pathRepositoryMethod);
        assertEquals(10, module.getExecStmt());
    }
    @Test
    public void testCalcExecStmt2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/CompareUtils#getAdapter(Object,Class,boolean).mjava");
        module.calcExecStmt(pathRepositoryMethod);
        assertEquals(11, module.getExecStmt());
    }
    @Test
    public void testCalcExecStmt3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/CompareWithIndexActionHandler#execute(ExecutionEvent).mjava");
        module.calcExecStmt(pathRepositoryMethod);
        assertEquals(13, module.getExecStmt());
    }
    @Test
    public void testCalcMaxNesting1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#decorateImage(Image,Object).mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(8, module.getMaxNesting());
    }
    @Test
    public void testCalcMaxNesting2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/clone/GitSelectWizardPage#createControl(Composite).mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(7, module.getMaxNesting());
    }
    @Test
    public void testCalcMaxNesting3(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/project/RepositoryMapping#getGitDirAbsolutePath().mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(1, module.getMaxNesting());
    }
    @Test
    public void testCalcMaxNesting4(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/sharing/SharingWizard#performFinish().mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(3, module.getMaxNesting());
    }
    @Test
    public void testCalcMaxNesting5(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/clone/GitCreateGeneralProjectPage#checkPage().mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(3, module.getMaxNesting());
    }

    @Test
    public void testCalcModuleHistories1(){
        String pathModule="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitDialog#okPressed().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(10, module.getModuleHistories());
    }
    @Test
    public void testCalcModuleHistories2(){
        String pathModule ="org.eclipse.egit.core/src/org/eclipse/egit/core/project/RepositoryMapping#getGitDirAbsolutePath().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(3, module.getModuleHistories());
    }
    @Test
    public void testCalcModuleHistories3(){
        String pathModule = "org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/MergeActionHandler#execute(ExecutionEvent).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(8, module.getModuleHistories());
    }
    @Test
    public void testCalcModuleHistories4(){
        String pathModule = "org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/history/GitHistoryPage#initActions().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(11, module.getModuleHistories());
    }
    @Test
    public void testCalcModuleHistories5(){
        String pathModule = "org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/RepositoryTreeNode#hashCode().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(10, module.getModuleHistories());
    }


    @Test
    public void testCalcAuthors1(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitDialog#okPressed().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcAuthors(commitsAll, commitEdgesMethod);
        assertEquals(6, module.getAuthors());
    }
    @Test
    public void testCalcAuthors2(){
        String pathModule ="org.eclipse.egit.core/src/org/eclipse/egit/core/project/RepositoryMapping#getGitDirAbsolutePath().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcAuthors(commitsAll, commitEdgesMethod);
        assertEquals(3, module.getAuthors());
    }
    @Test
    public void testCalcAuthors3(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/MergeActionHandler#execute(ExecutionEvent).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcAuthors(commitsAll, commitEdgesMethod);
        assertEquals(4, module.getAuthors());
    }
    @Test
    public void testCalcAuthors4(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/history/GitHistoryPage#initActions().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcAuthors(commitsAll, commitEdgesMethod);
        assertEquals(4, module.getAuthors());
    }
    @Test
    public void testCalcAuthors5(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/RepositoryTreeNode#hashCode().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcAuthors(commitsAll, commitEdgesMethod);
        assertEquals(3, module.getAuthors());
    }
    /*
    @Test
    public void testCalcLOC1(){
        String pathModule ="";
        Module module = modulesAll.get(pathModule);
        module.calcLOC(pathRepositoryMethod);
        assertEquals(, module.getLOC());
    }
    @Test
    public void testCalcLOC2(){
        String pathModule ="";
        Module module = modulesAll.get(pathModule);
        module.calcLOC(pathRepositoryMethod);
        assertEquals(, module.getLOC());
    }
    @Test
    public void testCalcLOC3(){
        String pathModule ="";
        Module module = modulesAll.get(pathModule);
        module.calcLOC(pathRepositoryMethod);
        assertEquals(, module.getLOC());
    }

    @Test
    public void testCalcFixChgNum1(){
        String pathModule="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CompareTreeView#reactOnOpen(OpenEvent).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcFixChgNum(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(6 ,module.fixChgNum);
    }

    @Test
    public void testCalcMinInterval1(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/RepositoryTreeNode#hashCode().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcMinInterval(commitsAll, commitEdgesMethod);
        assertEquals(1, module.getMinInterval());
    }

    @Test
    public void testCalcIsBuggyTrue1(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#getImage(Object).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(1, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyTrue2(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/history/command/AbstractHistoryCommandHandler#getSelection(GitHistoryPage).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(1, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyTrue3(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/decorators/GitDocument#GitDocument(IResource).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(1, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyTrue4(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/staging/StagingView#doReload(Repository,IProgressMonitor,String).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(1, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyTrue5(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/clone/SourceBranchPage#createControl(Composite).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(1, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyFalse1(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/CachedCheckboxTreeViewer#setAllChecked(boolean).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(0, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyFalse2(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/ShowBlameAction#ShowBlameAction().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(0, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyFalse3(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoryRemotePropertySource#getPropertyValue(Object).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(0, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyFalse4(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CompareTreeView.PathNode#toString().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(0, module.getIsBuggy());
    }

    @Test
    public void testCalcIsBuggyFalse5(){
        String pathModule ="org.eclipse.egit.core/src/org/eclipse/egit/core/securestorage/EGitSecureStore#getCredentials(URIish).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcIsBuggy(commitsAll, bugsAll, commitEdgesMethod);
        assertEquals(0, module.getIsBuggy());
    }
    
     */
}