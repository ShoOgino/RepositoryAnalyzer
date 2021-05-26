import data.*;
import misc.ArgBean;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class Main {
	// リポジトリ・バグデータを分析し、各種データをファイルにまとめ直す。(AnalyzeRepository)
	public static void main(String[] args) throws CmdLineException, IOException, GitAPIException {
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

		String pathProject = bean.pathProject;
		String pathRepositoryMethod = pathProject+"/repositoryMethod";
		Repository repositoryMethod = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryMethod + "/.git")).build();
		String pathRepositoryFile = pathProject+"/repositoryFile";
		Repository repositoryFile = new FileRepositoryBuilder().setGitDir(new File(pathRepositoryFile + "/.git")).build();
		String pathModules = pathProject+"/modules";
		String pathCommits = pathProject+"/commits";
		String pathBugs = pathProject+"/bugs.json";
		Commits commitsAll = new Commits();
		Modules modulesAll = new Modules();
		Bugs bugsAll = new Bugs();

		String pathCommitIDs = bean.pathCommitIDs;
		String[] commitIDsPatterns = FileUtil.readFile(pathCommitIDs).split("\\n");
		String idCommitHead = commitIDsPatterns[commitIDsPatterns.length-1].split(",")[5];

		if(bean.loadHistoryFromFile){
			commitsAll.loadCommitsFromFile(pathCommits);
			modulesAll.loadModulesFromFile(pathModules);
		}else {
			commitsAll.loadCommitsFromRepository(repositoryMethod, idCommitHead, pathCommits);
			commitsAll.loadCommitsFromFile(pathCommits);
			modulesAll.analyzeModules(commitsAll);
			if(bean.saveHistoryToFile) {
				modulesAll.saveToFile(pathModules);
			}
		}
		bugsAll.loadBugsFromFile(pathBugs);

		for(String line: commitIDsPatterns){
			String[] commitIDsPattern = line.replace("\r", "").split(",");
			try {
				String[] commitEdgesFile   = Arrays.copyOfRange(commitIDsPattern, 1, 3);
    			String[] commitEdgesMethod = Arrays.copyOfRange(commitIDsPattern, 3, 6);
    			String pathDataset = pathProject + "/datasets/" + commitIDsPattern[0] + ".csv";
				//String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+"_"+commitEdgesMethod[2].substring(0,8)+".csv";
				//if(bean.calcMetrics){
				//個々のモジュールについてメトリクスを計測
					Modules modulesTarget = new Modules();
					modulesTarget.identifyTargetModules(modulesAll, repositoryMethod, commitEdgesMethod);
					modulesTarget.calcCodeMetrics(repositoryFile, commitEdgesFile, repositoryMethod, commitEdgesMethod);
					modulesTarget.calcProcessMetrics(modulesAll, commitsAll, bugsAll, commitEdgesMethod);
					modulesTarget.saveMetricsAsRecords(pathDataset);
				//}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}