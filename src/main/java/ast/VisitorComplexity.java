package ast;

import org.eclipse.jdt.core.dom.*;

public class VisitorComplexity extends ASTVisitor {
	public int complexity=1;

	public boolean visit(IfStatement node) {
	    complexity++;
	    if(node.getElseStatement()!=null){
		    if(node.getElseStatement().getNodeType() != Statement.IF_STATEMENT){
		    	complexity++;
			}
		}
	    return true;
    }
	public boolean visit(ForStatement node) {
		complexity++;
		return true;
	}
	public boolean visit(EnhancedForStatement node) {
		complexity++;
		return true;
	}
	public boolean visit(WhileStatement node) {
		complexity++;
		return true;
	}
	public boolean visit(SwitchStatement node) {
		complexity++;
		return true;
	}

}
