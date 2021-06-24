package data;

import java.util.*;

public class NodeCommit4Experiment {
    public Integer num = null;
    public String idCommit = null;
    public String idCommitParent = null;
    public boolean isMerge;
    public boolean isFixingBug;
    public Integer interval = 10000;//days
    public Person author = null;
    public Integer[] semantics = new Integer[396];//396
    public Set<Integer> coupling = new HashSet<>();
    public Integer[] churn = new Integer[3];
    public List<Integer> parents = new ArrayList<>();

    public NodeCommit4Experiment(){
        Arrays.fill(semantics, 0);
        Arrays.fill(churn, 0);
    }
}
