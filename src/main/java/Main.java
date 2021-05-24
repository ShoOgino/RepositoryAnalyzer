import data.*;
import misc.ArgBean;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;

public class Main {
	// リポジトリ・バグデータを分析し、各種データをファイルにまとめ直す。(AnalyzeRepository)
	public static void main(String[] args) throws CmdLineException, IOException {
		ArgBean bean = new ArgBean();
		CmdLineParser parser = new CmdLineParser(bean);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("usage:");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			return;
		}

		final String pathProject = bean.pathProject;
		final String idCommitHead = bean.idCommitHead;
		final String[] commitEdgesMethod = bean.commitEdgesMethod;
		final String[] commitEdgesFile = bean.commitEdgesFile;
		String pathRepositoryMethod = pathProject+"/repositoryMethod";
		Repository repositoryMethod = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryMethod + "/.git")).build();
		String pathRepositoryFile = pathProject+"/repositoryFile";
		Repository repositoryFile = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryFile + "/.git")).build();
		String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+".csv";
		//String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+"_"+commitEdgesMethod[2].substring(0,8)+".csv";
		String pathModules = pathProject+"/modules";
		String pathCommits = pathProject+"/commits";
		String pathBugs = pathProject+"/bugs.json";
		Commits commitsAll = new Commits();
		Modules modulesAll = new Modules();
		Bugs bugsAll = new Bugs();

		try {
			if(bean.loadHistoryFromFile){
				commitsAll.loadCommitsFromFile(pathCommits);
				modulesAll.loadModulesFromFile(pathModules);
			}else {
				commitsAll.loadCommitsFromRepository(repositoryMethod, idCommitHead, pathCommits);
				commitsAll.loadCommitsFromFile(pathCommits);
				modulesAll.analyzeModules(commitsAll);
			}
			bugsAll.loadBugsFromFile(pathBugs);
			if(bean.calcMetrics){
				//個々のモジュールについてメトリクスを計測
				Modules modulesTarget = new Modules();
				modulesTarget.identifyTargetModules(modulesAll, repositoryMethod, commitEdgesMethod);
				modulesTarget.calcCodeMetrics(repositoryFile, commitEdgesFile, repositoryMethod, commitEdgesMethod);
				modulesTarget.calcProcessMetrics(modulesAll, commitsAll, bugsAll, commitEdgesMethod);
				modulesTarget.saveMetricsAsRecords(pathDataset);
			}
			if(bean.saveHistoryToFile) {
				modulesAll.saveToFile(pathModules);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}