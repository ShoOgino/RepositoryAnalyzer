package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class NodeAST4Experiment{
    public Integer num = null;
    public String source = null;
    public String nameType = null;
    public Integer numType = null;
    @JsonIgnore
    public NodeAST4Experiment parent = null;
    public List<NodeAST4Experiment> children = new ArrayList<>();
}
