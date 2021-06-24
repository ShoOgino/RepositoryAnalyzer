package misc;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class ArgBean {
    @Option(name = "--pathProject", metaVar = "pathProject", required = true)
    public  String pathProject = new String() ;
    @Option(name = "--loadHistoryFromFile")
    public boolean loadHistoryFromFile;
    @Option(name = "--multiProcess")
    public boolean multiProcess;
}
