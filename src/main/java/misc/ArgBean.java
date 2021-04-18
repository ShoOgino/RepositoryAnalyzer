package misc;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class ArgBean {
    @Option(name = "--pathProject", metaVar = "pathProject", required = true)
    public  String pathProject = new String() ;
    @Option(name = "-c", aliases = "--idCommitHead", metaVar = "idCommitHead", required = true)
    public  String idCommitHead = "";
    @Option(name = "--commitEdgesMethod", handler = StringArrayOptionHandler.class, metaVar = "commitEdgesMethod")
    public  String[] commitEdgesMethod = new String[2];
    @Option(name = "--commitEdgesFile", handler = StringArrayOptionHandler.class, metaVar = "commitEdgesFile")
    public  String[] commitEdgesFile = new String[2] ;
}
