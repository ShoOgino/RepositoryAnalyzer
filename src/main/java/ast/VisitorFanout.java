package ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class VisitorFanout extends ASTVisitor{
	public int fanout=0;

	public boolean visit(MethodInvocation node) {
		fanout++;
    	return super.visit(node);
	}

}
