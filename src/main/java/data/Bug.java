package data;

import java.util.ArrayList;
import java.util.List;

public class Bug {
    String id;
    List<BugAtomic> bugAtomics;

    public Bug(String id, List<BugAtomic> bugAtomics){
        this.id = id;
        this.bugAtomics = bugAtomics;
    }

    public Bug(){
        this.id=new String();
        this.bugAtomics = new ArrayList<>();
    }
}
