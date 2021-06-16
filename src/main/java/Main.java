import data.*;
import misc.Setting;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.apache.commons.io.FileUtils;
import util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	public static void main(String[] args) throws IOException {
		Setting setting = new Setting(args);
		Project project = new Project(setting);
		executeTasksInParallel(createTasks(project, setting));
	}

	private static List<Task> createTasks(Project project, Setting setting) throws IOException {
		List<Task> tasks = new ArrayList<>();
		String[] tasksStr = FileUtil.readFile(setting.pathTasks).split("\\n");
		for (int numTask=0; numTask<tasksStr.length; numTask++) {
			String[] pattern = tasksStr[numTask].replace("\r", "").split(",");
		    String nameTask = Arrays.copyOfRange(pattern, 0, 1)[0];
		    String granuarity = Arrays.copyOfRange(pattern, 1, 2)[0];
		    String product = Arrays.copyOfRange(pattern, 2, 3)[0];
		    String[] commitsTargetFile   = Arrays.copyOfRange(pattern, 3, 4);
		    String[] commitsTargetMethod = Arrays.copyOfRange(pattern, 4, 7);
		    Task task = new Task(
		    		numTask,
		    		nameTask,
		    		granuarity,
		    		product,
		    		setting.pathProject,
		    		setting.pathRepositoryFile,
		    		commitsTargetFile,
		    		setting.pathRepositoryMethod,
		    		commitsTargetMethod,
		    		project.commitsAll,
		    		project.modulesAll,
		    		project.bugsAll
		    );
		    tasks.add(task);
		}
		return tasks;
	}

	private static void executeTasksInParallel(List<Task> tasks) throws IOException {
		Runtime r = Runtime.getRuntime();
		int numOfCPUs = r.availableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(numOfCPUs-2);
		for(Task task: tasks) pool.submit(task);
		pool.shutdown();
	}
}