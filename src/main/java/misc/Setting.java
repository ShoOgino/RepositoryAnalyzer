package misc;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import util.FileUtil;

import java.io.IOException;

public class Setting {
    final public String pathProject;
    final public String pathRepositoryMethod;
    final public String pathRepositoryFile;
    final public String pathModules;
    final public String pathCommits;
    final public String pathBugs;
    final public String pathTasks;

    public Setting(String[] args) {
        ArgBean bean = new ArgBean();
        CmdLineParser parser = new CmdLineParser(bean);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("usage:");
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);
        }
        pathProject =  bean.pathProject;
        pathRepositoryMethod = pathProject+"/repositoryMethod";
        pathRepositoryFile = pathProject + "/repositoryFile";
        pathModules         = pathProject+"/modules";
        pathCommits = pathProject+"/commits";
        pathBugs  = pathProject+"/bugs.json";
        pathTasks = pathProject+ "/tasks.csv";
    }
}