package ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

public class VisitorCountPath  extends ASTVisitor{
    public ArrayList<Integer> branches=new ArrayList<Integer>();

	public boolean visit(IfStatement node) {
		int pathIf=0;
		ArrayList<Statement> statements= getStatementsIf(node);
		for(Statement statement:statements) {
			if(statement==null) {
				pathIf++;
				continue;
			}
			int pathStatement=1;
			VisitorCountPath visitor=new VisitorCountPath();
			statement.accept(visitor);
			for(Integer branch: visitor.branches) {
				pathStatement=pathStatement*branch;
			}
			pathIf+=pathStatement;
		}
		if(pathIf<2)pathIf=2;
		branches.add(pathIf);
		return false;
	}
	public ArrayList<Statement> getStatementsIf(IfStatement node){
		ArrayList<Statement> statements= new ArrayList<Statement>();
		statements.add(node.getThenStatement());
		Statement last=node.getElseStatement();
		if(last==null) {
			statements.add(null);
		}
		while(last!=null) {
			if(last.getNodeType()==ASTNode.IF_STATEMENT) {
				IfStatement tmp=(IfStatement)last;
				statements.add(tmp.getThenStatement());
				last=tmp.getElseStatement();
				if(last==null) {
					statements.add(null);
				}
			}else{
				statements.add(last);
				break;
			}
		}
		return statements;
	}

	public boolean visit(SwitchStatement node) {
		int pathSwitch=0;
		List<Statement> statements = node.statements();
		for(int i=0; i<statements.size();i++) {
			if(statements.get(i).getNodeType()==ASTNode.SWITCH_CASE) {
				int pathSwitchCase=1;
				for(int j=i; j<statements.size();j++) {
					if(statements.get(j).getNodeType()==ASTNode.BREAK_STATEMENT)break;
					VisitorCountPath visitor=new VisitorCountPath();
					statements.get(j).accept(visitor);
					for(Integer branch: visitor.branches) {
						pathSwitchCase=pathSwitchCase*branch;
					}
				}
				pathSwitch+=pathSwitchCase;
			}
		}
		if(pathSwitch==0)pathSwitch=1;
		branches.add(pathSwitch);
		return false;
	}


	//purpose: skip anonymous class.
	public boolean visit(AnonymousClassDeclaration classDeclarationStatement) {
		return false;
	}

}