package data;

import misc.Setting;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

public class Project {
	public Repository repositoryFile;
	public Repository repositoryMethod;
    public Commits commitsAll  = new Commits();
	public Modules modulesAll = new Modules();
	public Bugs bugsAll = new Bugs();
	public People authorsAll = new People();

	public Project(Setting setting){
		try {
			repositoryFile = new FileRepositoryBuilder().setGitDir(new File(setting.pathRepositoryFile + "/.git")).build();
			repositoryMethod = new FileRepositoryBuilder().setGitDir(new File(setting.pathRepositoryMethod + "/.git")).build();
		    commitsAll.loadCommitsFromRepository(repositoryMethod, setting.pathCommits);
		    commitsAll.loadCommitsFromFile(setting.pathCommits);
		    authorsAll.analyze(commitsAll);
		    authorsAll.giveNumberToPerson();
		    commitsAll.completeAuthor(authorsAll);
		    modulesAll.analyzeAllModules(commitsAll);
		    modulesAll.saveAsJson(setting.pathModules);
		    bugsAll.loadBugsFromFile(setting.pathBugs);
		}catch (IOException exception){
			exception.printStackTrace();
		}
	}
}