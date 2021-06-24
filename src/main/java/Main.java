import data.*;
import misc.Setting;
import util.FileUtil;

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
		executeTasks(project, setting);
	}

	private static void executeTasks(Project project, Setting setting) throws IOException {
		List<Task> tasks = parseTasks(project, setting);

		ExecutorService pool;
		if(setting.multiProcess) {
			Runtime r = Runtime.getRuntime();
			int numOfCPUs = r.availableProcessors();
			pool = Executors.newFixedThreadPool(numOfCPUs-1);
		}else {
			pool = Executors.newFixedThreadPool(1);
		}
		for(Task task: tasks) pool.submit(task);
		pool.shutdown();
	}

	private static List<Task> parseTasks(Project project, Setting setting) throws IOException {
		List<Task> tasks = new ArrayList<>();
		String[] tasksStr = FileUtil.readFile(setting.pathTasks).split("\\n");
		for (int numTask=0; numTask<tasksStr.length; numTask++) {
			String[] pattern = tasksStr[numTask].replace("\r", "").split(",");
			String nameTask = Arrays.copyOfRange(pattern, 0, 1)[0];
			String granularity = Arrays.copyOfRange(pattern, 1, 2)[0];
			String product = Arrays.copyOfRange(pattern, 2, 3)[0];
			String revisionFile_target   = Arrays.copyOfRange(pattern, 4, 5)[0];
			String revisionMethod_referHistoryFrom = Arrays.copyOfRange(pattern, 6, 7)[0];
			String revisionMethod_target = Arrays.copyOfRange(pattern, 7, 8)[0];
			String revisionMethod_referBugReportUntil = Arrays.copyOfRange(pattern, 8, 9)[0];
			Task task = new Task(
					numTask,
					nameTask,
					granularity,
					product,
					setting.pathProject,
					setting.pathRepositoryFile,
					revisionFile_target,
					setting.pathRepositoryMethod,
					revisionMethod_referHistoryFrom,
					revisionMethod_target,
					revisionMethod_referBugReportUntil,
					project.commitsAll,
					project.modulesAll,
					project.bugsAll
			);
			task.run();
			tasks.add(task);
		}
		return tasks;
	}
}