import data.*;
import misc.ArgBean;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

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
		String pathRepositoryFile = pathProject+"/repositoryFile";
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
			}else {
			}
			//そのリポジトリに過去に存在したモジュール・コミット・バグをまとめる。
			commitsAll.loadCommitsFromRepository(pathRepositoryMethod, idCommitHead, pathCommits);
			commitsAll.loadCommitsFromFile(pathCommits);
			modulesAll.analyzeModules(commitsAll);
			bugsAll.loadBugsFromFile(pathBugs);

			//コミット・モジュールデータをファイルからロード
			//modulesAll.loadModulesFromFile(pathModules);
			//commitsAll.loadCommitsFromFile(pathCommits);
			//bugsAll.loadBugsFromFile(pathBugs);
			if(bean.saveHistoryToFile) {
				modulesAll.saveToFile(pathModules);
				commitsAll.saveToFile(pathCommits);
			}
			if(bean.calcMetrics){
				//個々のモジュールについてメトリクスを計測
				Modules modulesTarget = new Modules();
				modulesTarget.identifyTargetModules(modulesAll, pathRepositoryMethod, commitEdgesMethod);
				modulesTarget.calcCodeMetrics(pathRepositoryFile, commitEdgesFile, pathRepositoryMethod, commitEdgesMethod);
				modulesTarget.calcProcessMetrics(modulesAll, commitsAll, bugsAll, commitEdgesMethod);
				modulesTarget.saveMetricsAsRecords(pathDataset);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}