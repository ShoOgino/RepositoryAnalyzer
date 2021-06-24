package ast;

import data.NodeAST4Experiment;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class Visitor4CalculatingAST extends ASTVisitor {
    List<NodeAST4Experiment> nodeASTs4Experiment = new ArrayList<>();
    public void preVisit(ASTNode node) {
        NodeAST4Experiment nodeAST4Experiment = new NodeAST4Experiment();
        nodeAST4Experiment.num = node.getNodeType();
        String nameClass = node.getClass().toString();
        if(0<nameClass.split(".").length) {
            nodeAST4Experiment.nameType = nameClass.split(".")[nameClass.split(".").length];
        }else{
            nodeAST4Experiment.nameType = nameClass;
        }

        ASTNode parent = node.getParent();
        if(parent!=null)System.out.println(parent.toString());
    }
}
