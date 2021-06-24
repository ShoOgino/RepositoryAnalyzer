import data.Bugs;
import data.Commits;
import data.Modules;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

public class Task implements Runnable{
    private final int numTask;
    private final String name;
    private final String granularity;
    private final String product;
    private final String pathProject;
    private final String pathRepositoryFileOriginal;
    private final String pathRepositoryFileCopy;
    private Repository repositoryFile;
    private final String revisionFile_target;
    private final String pathRepositoryMethod;
    private final Repository repositoryMethod;
    private final String revisionMethod_referHistoryFrom;
    private final String revisionMethod_target;
    private final String revisionMethod_referBugReportsUntil;
    private final String pathOutput;
    private final Commits commitsAll;
    private final Modules modulesAll;
    private final Bugs bugsAll;

    public Task(int numTask,
                String name,
                String granularity,
                String product,
                String pathProject,
                String pathRepositoryFile,
                String revisionFile,
                String pathRepositoryMethod,
                String revisionMethod_referHistoryFrom,
                String revisionMethod_referHistoryUntil,
                String revisionMethod_referBugReportsUntil,
                Commits commitsAll,
                Modules modulesAll,
                Bugs bugsAll
    ) throws IOException {
        System.out.println("task "+ numTask +" started");
        this.numTask = numTask;
        this.name = name;
        this.granularity = granularity;
        this.product = product;
        this.pathProject = pathProject;
        this.pathRepositoryFileOriginal = pathRepositoryFile;
        this.pathRepositoryFileCopy = pathRepositoryFile+numTask;
        this.revisionFile_target = revisionFile;
        this.pathRepositoryMethod = pathRepositoryMethod;
        this.repositoryMethod = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryMethod + "/.git")).build();
        this.revisionMethod_referHistoryFrom = revisionMethod_referHistoryFrom;
        this.revisionMethod_target = revisionMethod_referHistoryUntil;
        this.revisionMethod_referBugReportsUntil = revisionMethod_referBugReportsUntil;
        this.pathOutput = pathProject+"/output/"+this.name;
        this.commitsAll = commitsAll;
        this.modulesAll = modulesAll;
        this.bugsAll = bugsAll;
    }

    @Override
    public void run() {
        try {
            // copy repositoryFile to process tasks in parallel. (we have to checkout a revision in repositoryFile and keep it while processing a task.)
            File fileRepositoryFileOriginal = new File(pathRepositoryFileOriginal);
            File fileRepositoryFileCopy = new File(pathRepositoryFileCopy);
            FileUtils.copyDirectory(fileRepositoryFileOriginal, fileRepositoryFileCopy);
            repositoryFile = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryFileCopy + "/.git")).build();

            // identify target modules
            Modules modulesTarget = new Modules();
            modulesTarget.identifyTargetModules(modulesAll, repositoryMethod, revisionMethod_target);

            // calculate products on the target modules
            if (product.contains("tokens")){
            }
            if (product.contains("AST")) {
                modulesTarget.calculateAST(repositoryMethod, revisionMethod_target);
            }
            if (product.contains("commitGraph")){
                modulesTarget.calculateCommitGraph(commitsAll, modulesAll, revisionMethod_referHistoryFrom, revisionMethod_target, bugsAll);
            }
            if (product.contains("metrics")) {
                modulesTarget.calculateCodeMetrics(repositoryFile, revisionFile_target, repositoryMethod, revisionMethod_target);
                modulesTarget.calculateProcessMetrics(commitsAll, revisionMethod_referHistoryFrom, revisionMethod_target);
            }
            if(product.contains("isBuggy")){
                modulesTarget.calculateIsBuggy(commitsAll, revisionMethod_target, revisionMethod_referBugReportsUntil, bugsAll);
            }
            if(product.contains("hasBeenBuggy")){
                modulesTarget.calculateHasBeenBuggy(commitsAll, revisionMethod_target, bugsAll);
            }

            // save data
            if (
                    product.contains("AST")
                            | product.contains("commitGraph")
                            | product.contains("tokens")
                            | product.contains("hasBeenBuggy")
                            | product.contains("isBuggy")
            ) {
                modulesTarget.saveAsJson(pathOutput);
            }
            if (product.contains("metrics")) {
                modulesTarget.saveAsCSV(pathOutput + ".csv");
            }

            // delete copied repositoryFile.
            repositoryFile.close();
            FileUtils.deleteDirectory(fileRepositoryFileCopy);
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }
}
