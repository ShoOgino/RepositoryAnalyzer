package misc;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class ArgBean {
    @Option(name = "--pathProject", metaVar = "pathProject", required = true)
    public  String pathProject = new String() ;
    @Option(name = "--pathCommitIDs", metaVar = "pathCommitIDs")
    public  String pathCommitIDs = new String();
    @Option(name = "--headCommitID", metaVar = "idCommitHead")
    public  String headCommitID = new String();
    @Option(name="--skipAnalyzation")
    public boolean isSkipAnalyzation;
    @Option(name="--calcMetrics")
    public boolean calcMetrics;
}
