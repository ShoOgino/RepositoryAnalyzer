package misc;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Setting {
    @Option(name = "--pathProject", required = true)
    public  String pathProject;
    @Option(name = "--loadHistoryFromFile")
    public boolean loadHistoryFromFile = false;
    @Option(name = "--multiProcess")
    public boolean multiProcess = false;

    public final String pathRepositoryMethod;
    public final String pathRepositoryFile;
    public final String pathModules;
    public final String pathCommits;
    public final String pathBugs;
    public final String pathTasks;

    public Setting(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("usage:");
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);
        }
        pathRepositoryMethod = this.pathProject +"/repositoryMethod";
        pathRepositoryFile   = this.pathProject +"/repositoryFile";
        pathModules          = this.pathProject +"/modules";
        pathCommits          = this.pathProject +"/commits";
        pathBugs             = this.pathProject +"/bugs.json";
        pathTasks            = this.pathProject +"/tasks.csv";
    }
}