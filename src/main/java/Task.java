import data.Bugs;
import data.Commits;
import data.Modules;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class Task implements Runnable{
    private int numTask;
    private String name;
    private String granuality;
    private String product;
    private String pathProject;
    private String pathRepositoryFile;
    private Repository repositoryFile;
    private String[] commitsTargetFile;
    private String pathRepositoryMethod;
    private Repository repositoryMethod;
    private String[] commitsTargetMethod;
    private String pathDataset;
    private Commits commitsAll;
    private Modules modulesAll;
    private Bugs bugsAll;


    public Task(int numTask, String name, String granuality, String product, String pathProject, String pathRepositoryFile, String[] commitsTargetFile, String pathRepositoryMethod, String[] commitsTargetMethod, Commits commitsAll, Modules modulesAll, Bugs bugsAll) throws IOException {
        System.out.println("task "+ numTask +" started");
        this.numTask = numTask;
        this.name = name;
        this.granuality = granuality;
        this.product = product;
        this.pathProject = pathProject;
        this.pathRepositoryFile = pathRepositoryFile;
        this.commitsTargetFile = commitsTargetFile;
        this.pathRepositoryMethod = pathRepositoryMethod;
        this.commitsTargetMethod = commitsTargetMethod;
        this.commitsAll = commitsAll;
        this.modulesAll = modulesAll;
        this.bugsAll = bugsAll;
        this.pathDataset = pathProject+"/datasets/"+name+".csv";
    }


    @Override
    public void run() {
        if(Objects.equals(product, "metrics")){
            try {
                File fileRepositoryFileOriginal = new File(pathRepositoryFile);
                String pathRepositoryFileCopy = pathProject+"/repositoryFile_" + numTask;
                File fileRepositoryFileCopy = new File(pathRepositoryFileCopy);
                FileUtils.copyDirectory(fileRepositoryFileOriginal, fileRepositoryFileCopy);
                pathRepositoryFile = pathRepositoryFileCopy;
                repositoryFile = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryFileCopy + "/.git")).build();
                repositoryMethod = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryMethod + "/.git")).build();


                //タスク実行
                Modules modulesTarget = new Modules();
                modulesTarget.identifyTargetModules(commitsAll, modulesAll, repositoryMethod, commitsTargetMethod);
                modulesTarget.calcCodeMetrics(repositoryFile, commitsTargetFile, repositoryMethod, commitsTargetMethod);
                modulesTarget.calcProcessMetrics(modulesAll, commitsAll, bugsAll, commitsTargetMethod);
                modulesTarget.saveMetricsAsRecords(pathDataset);

                File fileRepositoryFile = new File(pathRepositoryFile);
                FileUtils.deleteDirectory(fileRepositoryFile);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }else if(Objects.equals(product, "AST")){
            //jsonにASTとノード数とisBuggyの値をデシリアライズ。
            //
        }
    }
}
