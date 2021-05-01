package data;

import java.util.List;
import java.util.Map;

public class BugAtomic {
    String path;
    String idCommitFix;
    List<String> idsCommitInduce;

    public BugAtomic(String path, String idCommitFix, List<String> idsCommitInduce){
        this.path = path;
        this.idCommitFix = idCommitFix;
        this.idsCommitInduce = idsCommitInduce;
    }
}
