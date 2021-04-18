package ast;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VisitorLocalVar  extends ASTVisitor{
	public int NOVariables=0;

	public boolean visit(VariableDeclarationStatement node) {
		NOVariables++;
    	return super.visit(node);
	}
}
