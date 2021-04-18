package data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ast.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import misc.DoubleConverter;
import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import static util.FileUtil.readFile;

@CsvDataType()
@Data
public class Module {
	public String id="";
	@CsvField(pos = 1)
	public String path;
	public Modifications modifications;
	public ArrayList<String> commitsHead;
	public ArrayList<String> commitsRoot;

	@CsvField(pos = 2)
	int isBuggy=0;
	@CsvField(pos = 3)
	int fanIn=0;
	@CsvField(pos = 4)
	int fanOut=0;
	@CsvField(pos = 5)
	int parameters=0;
	@CsvField(pos = 6)
	int localVar=0;
	@CsvField(pos = 7, converterType = DoubleConverter.class)
	double commentRatio=0;
	@CsvField(pos = 8)
	long countPath=0;
	@CsvField(pos = 9)
	int complexity=0;
	@CsvField(pos = 10)
	int execStmt=0;
	@CsvField(pos = 11)
	int maxNesting=0;

	//process metrics
	@CsvField(pos = 12)
	int moduleHistories=0;
	@CsvField(pos = 13)
	int authors    = 0;
	@CsvField(pos = 14)
	int stmtAdded=0;
	@CsvField(pos = 15)
	int maxStmtAdded=0;
	@CsvField(pos = 16, converterType = DoubleConverter.class)
	double avgStmtAdded=0;
	@CsvField(pos = 17)
	int stmtDeleted=0;
	@CsvField(pos = 18)
	int maxStmtDeleted=0;
	@CsvField(pos = 19, converterType = DoubleConverter.class)
	double avgStmtDeleted=0;
	@CsvField(pos = 20)
	int churn=0;
	@CsvField(pos = 21)
	int maxChurn=0;
	@CsvField(pos = 22, converterType = DoubleConverter.class)
	double avgChurn=0;
	@CsvField(pos = 23)
	int decl=0;
	@CsvField(pos = 24)
	int cond=0;
	@CsvField(pos = 25)
	int elseAdded=0;
	@CsvField(pos = 26)
	int elseDeleted=0;
	public Module() {
		this.path=new String();
		this.modifications = new Modifications();
		this.commitsHead = new ArrayList<>();
		this.commitsRoot = new ArrayList<>();
	}

	public Module(String path) {
		this.path=path;
		this.modifications = new Modifications();
		this.commitsHead = new ArrayList<>();
		this.commitsRoot = new ArrayList<>();
	}

	public void calcMaxNesting(String pathRepository) {
		CompilationUnit unit =getCompilationUnit(pathRepository);
		VisitorMaxNesting visitorMaxNesting = new VisitorMaxNesting();
		unit.accept(visitorMaxNesting);
		maxNesting = visitorMaxNesting.maxNesting;
	}

	public void calcExecStmt(String pathRepository) {
		CompilationUnit unit =getCompilationUnit(pathRepository);
		VisitorExecStmt visitorExecStmt = new VisitorExecStmt();
		unit.accept(visitorExecStmt);
		execStmt = visitorExecStmt.execStmt;
	}

	public  void calcComplexity(String pathRepository) {
		CompilationUnit unit =getCompilationUnit(pathRepository);
		VisitorComplexity visitorComplexity = new VisitorComplexity();
		unit.accept(visitorComplexity);
		complexity = visitorComplexity.complexity;
	}

	public void calcCountPath(String pathRepository) {
		CompilationUnit unit =getCompilationUnit(pathRepository);
		VisitorCountPath visitorCountPath = new VisitorCountPath();
		unit.accept(visitorCountPath);
		long countPath=1;
		for(int branch: visitorCountPath.branches) {
			countPath*=branch;
		}
		this.countPath = countPath;
	}

	public void calcCommentRatio(String pathRepository) {
		String regex  = "\n|\r\n";
		String sourceMethod = readFile(pathRepository +"/"+path);
		String[] linesMethod = sourceMethod.split(regex, 0);

		int countLineCode=0;
		int countLineComment=0;
		boolean inComment=false;
		for(String line:linesMethod) {
			countLineCode++;
			if(line.matches(".*\\*/\\S+")) {
				inComment=false;
			}else if(line.matches(".*\\*/\\s*")) {
				inComment=false;
				countLineComment++;
			}else if(inComment) {
				countLineComment++;
			}else if(line.matches("\\S+/\\*.*")){
				inComment=true;
			}else if(line.matches("\\s*/\\*.*")){
				countLineComment++;
				inComment=true;
			}else if(line.matches("\\S+//.*")) {
			}else if(line.matches("\\s*//.*")) {
				countLineComment++;
			}
		}
		commentRatio = (float) countLineComment/ (float)countLineCode;
	}

	public  void calcLocalVar(String pathRepository) {
		CompilationUnit unit =getCompilationUnit(pathRepository);
		VisitorLocalVar visitorLocalVar = new VisitorLocalVar();
		unit.accept(visitorLocalVar);
		localVar = visitorLocalVar.NOVariables;
	}

	public  void calcParameters(String pathRepository) {
		CompilationUnit unit =getCompilationUnit(pathRepository);
		data.VisitorMethodDeclaration visitorMethodDeclaration = new data.VisitorMethodDeclaration();
		unit.accept(visitorMethodDeclaration);
		parameters = visitorMethodDeclaration.parameters;
	}

	public  void calcFanOut(String pathRepository) {
		CompilationUnit unit =getCompilationUnit(pathRepository);
		VisitorFanout visitor = new VisitorFanout();
		unit.accept(visitor);
		this.fanOut = visitor.fanout;
	}

	public  void calcCond(Commits commitsAll, String[] intervalCommit) {
		int cond=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				EntityType et = change.getChangedEntity().getType();
				if(change.getChangeType()== ChangeType.CONDITION_EXPRESSION_CHANGE)cond++;
			}
		}
		this.cond = cond;
	}

	public  void calcDecl(Commits commitsAll, String[] intervalCommit) {
		int decl=0;
		List<ChangeType> ctdecl= Arrays.asList(
				ChangeType.METHOD_RENAMING,
				ChangeType.PARAMETER_DELETE,
				ChangeType.PARAMETER_INSERT,
				ChangeType.PARAMETER_ORDERING_CHANGE,
				ChangeType.PARAMETER_RENAMING,
				ChangeType.PARAMETER_TYPE_CHANGE,
				ChangeType.RETURN_TYPE_INSERT,
				ChangeType.RETURN_TYPE_DELETE,
				ChangeType.RETURN_TYPE_CHANGE,
				ChangeType.PARAMETER_TYPE_CHANGE
		);
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				EntityType et = change.getChangedEntity().getType();
				if(ctdecl.contains(change.getChangeType()))decl++;
			}
		}
		this.decl = decl;
	}

	public  void calcAvgChurn(Commits commitsAll, String[] intervalCommit) {
		calcChurn(commitsAll, intervalCommit);
		calcModuleHistories(commitsAll, intervalCommit);
		this.avgChurn= churn/(float)moduleHistories;
	}

	public  void calcMaxChurn(Commits commitsAll, String[] intervalCommit) {
		int maxChurn=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			int churnTemp=0;
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)churnTemp++;
				else if(change.getChangeType()==ChangeType.STATEMENT_DELETE)churnTemp--;
			}
			if(maxChurn<churnTemp)maxChurn=churnTemp;
		}
		this.maxChurn = maxChurn;
	}

	public  void calcChurn(Commits commitsAll, String[] intervalCommit) {
		int churn=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)churn++;
				else if(change.getChangeType()==ChangeType.STATEMENT_DELETE)churn--;
			}
		}
		this.churn = churn;
	}

	public  void calcAvgStmtDeleted(Commits commitsAll, String[] intervalCommit) {
		int avgStmtDeleted=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_DELETE)avgStmtDeleted++;
			}
		}
		calcModuleHistories(commitsAll, intervalCommit);
		this.avgStmtDeleted = avgStmtDeleted/(double)moduleHistories;
	}

	public  void calcMaxStmtDeleted(Commits commitsAll, String[] intervalCommit) {
		int maxStmtDeleted=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			int stmtDeletedOnCommit=0;
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_DELETE)stmtDeletedOnCommit++;
			}
			if(maxStmtDeleted<stmtDeletedOnCommit) {
				maxStmtDeleted=stmtDeletedOnCommit;
			}
		}
		this.maxStmtDeleted=  maxStmtDeleted;
	}

	public  void calcStmtDeleted(Commits commitsAll, String[] intervalCommit) {
		int stmtDeleted=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_DELETE)stmtDeleted++;
			}
		}
		this.stmtDeleted = stmtDeleted;
	}

	public  void calcAvgStmtAdded(Commits commitsAll, String[] intervalCommit) {
		int avgStmtAdded=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)avgStmtAdded++;
			}
		}
		calcModuleHistories(commitsAll, intervalCommit);
		this.avgStmtAdded = avgStmtAdded/(double)moduleHistories;
	}

	public  void calcMaxStmtAdded(Commits commitsAll, String[] intervalCommit) {
		int maxStmtAdded=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll,intervalCommit);
		for(Modification modification: modifications) {
			int stmtAddedTemp=0;
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)stmtAddedTemp++;
			}
			if(maxStmtAdded<stmtAddedTemp) {
				maxStmtAdded=stmtAddedTemp;
			}
		}
		this.maxStmtAdded =  maxStmtAdded;
	}

	public  void calcStmtAdded(Commits commitsAll, String[] intervalCommit) {
		int stmtAdded=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)stmtAdded++;
			}
		}
		this.stmtAdded = stmtAdded;
	}

	public  void calcElseDeleted(Commits commitsAll, String[] intervalCommit) {
		int elseDeleted=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				EntityType et = change.getChangedEntity().getType();
				if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_DELETE & et.toString().equals("ELSE_STATEMENT"))elseDeleted++;
			}
		}
		this.elseDeleted = elseDeleted;
	}

	public  void calcElseAdded(Commits commitsAll, String[] intervalCommit) {
		int elseAdded=0;
		List<Modification> modifications = calcModificationsInInterval(commitsAll, intervalCommit);
		for(Modification modification: modifications) {
			List<SourceCodeChange> changes = identifyChanges(modification);
			for(SourceCodeChange change : changes) {
				EntityType et = change.getChangedEntity().getType();
				if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_INSERT & et.toString().equals("ELSE_STATEMENT") )elseAdded++;
			}
		}
		this.elseAdded = elseAdded;
	}

	public  void calcDevTotal(Commits commitsAll, String[] intervalCommit) {
		Set<String> setAuthors = new HashSet<>();
		List<Commit> commits =calcCommitsInInterval(commitsAll,intervalCommit);
		commits.stream().forEach(item->setAuthors.add(item.author));
		int authors=setAuthors.size();
		this.authors = authors;
	}

	public  void calcModuleHistories(Commits commitsAll, String[] intervalCommit) {
		List<Commit> commits =calcCommitsInInterval(commitsAll, intervalCommit);
		int moduleHistories=commits.size();
		this.moduleHistories = moduleHistories;
	}

	public  List<Commit> calcCommitsInInterval(Commits commitsAll, String[] intervalCommit){
		List<Commit> commits = new ArrayList<Commit>();

		int dateBegin = commitsAll.get(intervalCommit[0]).date;
		int dateEnd   = commitsAll.get(intervalCommit[1]).date;
		for(Modification modification: modifications.values()) {
			Commit commit = commitsAll.get(modification.idCommit);
			if(dateBegin<=commit.date & commit.date<=dateEnd & !commit.isMerge) {
				commits.add(commit);
			}
		}

		return commits;
	}

	public  List<Modification> calcModificationsInInterval(Commits commitsAll, String[] intervalCommit){
		List<Modification> modificationsResult = new ArrayList<>();

		int dateBegin = commitsAll.get(intervalCommit[0]).date;
		int dateEnd   = commitsAll.get(intervalCommit[1]).date;
		for(Modification modification: modifications.values()) {
			Commit commit = commitsAll.get(modification.idCommit);
			if(dateBegin<=commit.date & commit.date<=dateEnd & !commit.isMerge) {
				modificationsResult.add(modification);
			}
		}

		return modificationsResult;
	}

	public  void calcIsBuggy(Commits commitsAll,Bugs bugsAll,  String[] intervalCommit) {
		HashMap<String, String[]> fix2induces = bugsAll.get(path);
		if(fix2induces==null)return;
		for(String idCommitFix: fix2induces.keySet()) {
			for(String idCommitInduce: fix2induces.get(idCommitFix)) {
				Commit commitFix = commitsAll.get(idCommitFix);
				Commit commitInduce = commitsAll.get(idCommitInduce);
				Commit commitTimePoint = commitsAll.get(intervalCommit[1]);
				if(commitInduce.date<commitTimePoint.date & commitTimePoint.date<commitFix.date)isBuggy=1;
			}
		}
		return;
	}

	public CompilationUnit getCompilationUnit(String pathRepository) {
		String sourceMethod = readFile( pathRepository+"/" + path);
		String sourceClass = "public class Dummy{"+sourceMethod+"}";
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(sourceClass.toCharArray());
		CompilationUnit unit =(CompilationUnit) parser.createAST(new NullProgressMonitor());
		return unit;
	}

	public  List<SourceCodeChange> identifyChanges(Modification modification){
		String sourcePrev =  null;
		String sourceCurrent =null;
		String strPre = null;
		String strPost = null;
		if(modification.sourceOld==null) {
			String regex  = "\\n|\\r\\n";
			String tmp=modification.sourceNew;
			String[] lines = tmp.split(regex, 0);

			boolean inComment=false;
			int count=0;
			for(String line : lines) {
				if(line.matches(".*/\\*.*")) {
					inComment=true;
					count++;
				}else if(line.matches(".*\\*/.*")) {
					inComment=false;
					count++;
				}else if(inComment) {
					count++;
				}else if(line.matches(".*//.*")) {
					count++;
				}else{
					break;
				}
			}
			tmp="";
			for(int i=count;i<lines.length;i++){
				tmp=tmp+lines[i]+"\n";
			}

			Pattern patternPre = Pattern.compile("[\\s\\S.]*?(?=\\{)");
			Matcher matcherPre = patternPre.matcher(tmp);
			if(matcherPre.find()) {
				strPre=matcherPre.group();
			}
			Pattern patternPost = Pattern.compile("(?<=\\{)[\\s\\S.]*");
			Matcher matcherPost = patternPost.matcher(tmp);
			if(matcherPost.find()) {
				strPost=matcherPost.group();
			}
			sourcePrev= "public class Test{"+strPre+
					"{"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"}"+
					"}";
			sourceCurrent ="public class Test{"+strPre+
					"{"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					"dummy();\n"+
					strPost+
					"}";
		}else {
			sourcePrev= "public class Dummy{"+modification.sourceOld+"}";
			sourceCurrent ="public class Dummy{"+modification.sourceNew+"}";
		}

		FileDistiller distiller = ChangeDistiller.createFileDistiller();
		try {
			distiller.extractClassifiedSourceCodeChanges(sourcePrev, sourceCurrent);
		} catch(Exception e) {
			System.err.println("Warning: error while change distilling. " + e.getMessage());
		}
		return distiller.getSourceCodeChanges();
	}
}