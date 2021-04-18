package ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

public class VisitorMaxNesting extends ASTVisitor {
	public int maxNesting=0;

	public boolean visit(IfStatement node) {
		ArrayList<Statement> statements= getStatementsIf(node);
		for(Statement statement:statements) {
			VisitorMaxNesting visitor=new VisitorMaxNesting();
			statement.accept(visitor);
			if(maxNesting<visitor.maxNesting+1) {
				maxNesting=visitor.maxNesting+1;
			}
		}
		return false;
	}


	public boolean visit(SwitchStatement node) {
		List<Statement> statements= node.statements();
		for(int i=0; i<statements.size();i++) {
			if(statements.get(i).getNodeType()==ASTNode.SWITCH_CASE) {
				for(int j=i; j<statements.size();j++) {
					if(statements.get(j).getNodeType()==ASTNode.BREAK_STATEMENT)break;
					VisitorMaxNesting visitor=new VisitorMaxNesting();
					statements.get(j).accept(visitor);
					if(maxNesting<visitor.maxNesting+1) {
						maxNesting=visitor.maxNesting+1;
					}
				}
			}
		}
		return false;
	}
	public boolean visit(ForStatement node) {
		Statement statement=node.getBody();
		VisitorMaxNesting visitor=new VisitorMaxNesting();
		statement.accept(visitor);
		if(maxNesting<visitor.maxNesting+1) {
			maxNesting=visitor.maxNesting+1;
		}
	    return false;
	}

	public boolean visit(EnhancedForStatement node) {
		Statement statement=node.getBody();
		VisitorMaxNesting visitor=new VisitorMaxNesting();
		statement.accept(visitor);
		if(maxNesting<visitor.maxNesting+1) {
			maxNesting=visitor.maxNesting+1;
		}
	    return false;
	}

	public boolean visit(WhileStatement node) {
		Statement statement=node.getBody();
		VisitorMaxNesting visitor=new VisitorMaxNesting();
		statement.accept(visitor);
		if(maxNesting<visitor.maxNesting+1) {
			maxNesting=visitor.maxNesting+1;
		}
		return false;
	}

	public boolean visit(TryStatement node) {
		Statement statement=node.getBody();
		VisitorMaxNesting visitor = new VisitorMaxNesting();
		statement.accept(visitor);
		if(maxNesting<visitor.maxNesting+1) {
			maxNesting=visitor.maxNesting+1;
		}
		List<CatchClause> catchClauses = node.catchClauses();
		for(CatchClause catchClause: catchClauses) {
			visitor=new VisitorMaxNesting();
			catchClause.accept(visitor);
			if(maxNesting<visitor.maxNesting+1) {
				maxNesting=visitor.maxNesting+1;
			}
		}
		if(node.getFinally()!=null) {
		    statement=node.getFinally();
		    visitor=new VisitorMaxNesting();
	    	statement.accept(visitor);
	    	if(maxNesting<visitor.maxNesting+1) {
	    		maxNesting=visitor.maxNesting+1;
	    	}
		}
		return false;
	}
	//purpose: skip anonymous class.
	public boolean visit(AnonymousClassDeclaration classDeclarationStatement) {
		return false;
	}

	public ArrayList<Statement> getStatementsIf(IfStatement node){
		ArrayList<Statement> statements= new ArrayList<Statement>();
		statements.add(node.getThenStatement());

		Statement last=node.getElseStatement();
		while(last!=null) {
			statements.add(last);
			if(last.getNodeType()!=ASTNode.IF_STATEMENT)break;
			IfStatement tmp=(IfStatement)last;
			last=tmp.getElseStatement();
		}
		return statements;
	}
}
