import data.Bugs;
import data.Commits;
import data.Modules;
import misc.ArgBean;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static util.RepositoryUtil.checkoutRepository;

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
		String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+"_"+commitEdgesMethod[2].substring(0,8)+".csv";
		String pathModules = pathProject+"/modules";
		String pathCommits = pathProject+"/commits";
		String pathBugs = pathProject+"/bugs.json";
		Commits commitsAll = new Commits();
		Modules modulesAll = new Modules();
		Bugs bugsAll = new Bugs();


		try {
			if(bean.isSkipAnalyzation){
				//コミット・モジュールデータをファイルからロード
				commitsAll.loadCommitsFromFile(pathCommits);
				modulesAll.loadModulesFromFile(pathModules);
			}else {
				//そのリポジトリに過去に存在したモジュール・コミット・バグをまとめる。
				commitsAll.loadCommitsFromRepository(pathRepositoryMethod, idCommitHead);
				modulesAll.analyzeModules(commitsAll);
			}
			if(bean.isSave) {
				commitsAll.save(pathCommits);
				modulesAll.save(pathModules);
			}

			bugsAll.loadBugs(pathBugs);

			//個々のモジュールについてメトリクスを計測
			Modules modulesTarget = new Modules();
			modulesTarget.identifyTargetModules(modulesAll, pathRepositoryMethod, commitEdgesMethod);
			modulesTarget.calcCodeMetrics(pathRepositoryFile, commitEdgesFile, pathRepositoryMethod, commitEdgesMethod);
			modulesTarget.calcProcessMetrics(modulesAll, commitsAll, bugsAll, commitEdgesMethod);
			modulesTarget.saveMetricsAsRecords(pathDataset);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}