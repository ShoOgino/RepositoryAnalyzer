package data;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ast.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import misc.DoubleConverter;
import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

@JsonIgnoreProperties(ignoreUnknown=true)
@CsvDataType()
@Data
public class Module implements Cloneable{
	public String id="";
	@CsvField(pos = 1)
	public String path=null;
	@JsonIgnore
	public String source=null;
	public CompilationUnit compilationUnit =null;
	public ChangesOnModule changesOnModule =null;
	public ArrayList<String> commitsHead=null;
	public ArrayList<String> commitsRoot=null;
	@CsvField(pos = 2)
	@JsonIgnore
	int hasBeenBuggy=0;
	@CsvField(pos = 3)
	@JsonIgnore
	int isBuggy=0;
	@CsvField(pos = 4)
	@JsonIgnore
	int fanIn=0;
	@CsvField(pos = 5)
	@JsonIgnore
	int fanOut=0;
	@CsvField(pos = 6)
	@JsonIgnore
	int parameters=0;
	@CsvField(pos = 7)
	@JsonIgnore
	int localVar=0;
	@CsvField(pos = 8, converterType = DoubleConverter.class)
	@JsonIgnore
	double commentRatio=0;
	@CsvField(pos = 9)
	@JsonIgnore
	long countPath=0;
	@CsvField(pos = 10)
	@JsonIgnore
	int complexity=0;
	@CsvField(pos = 11)
	@JsonIgnore
	int execStmt=0;
	@CsvField(pos = 12)
	@JsonIgnore
	int maxNesting=0;

	//process metrics
	@CsvField(pos = 13)
	@JsonIgnore
	int moduleHistories=0;
	@CsvField(pos = 14)
	@JsonIgnore
	int authors = 0;
	@CsvField(pos = 15)
	@JsonIgnore
	int stmtAdded=0;
	@CsvField(pos = 16)
	@JsonIgnore
	int maxStmtAdded=0;
	@CsvField(pos = 17, converterType = DoubleConverter.class)
	@JsonIgnore
	double avgStmtAdded=0;
	@CsvField(pos = 18)
	@JsonIgnore
	int stmtDeleted=0;
	@CsvField(pos = 19)
	@JsonIgnore
	int maxStmtDeleted=0;
	@CsvField(pos = 20, converterType = DoubleConverter.class)
	@JsonIgnore
	double avgStmtDeleted=0;
	@CsvField(pos = 21)
	@JsonIgnore
	int churn=0;
	@CsvField(pos = 22)
	@JsonIgnore
	int maxChurn=0;
	@CsvField(pos = 23, converterType = DoubleConverter.class)
	@JsonIgnore
	double avgChurn=0;
	@CsvField(pos = 24)
	@JsonIgnore
	int decl=0;
	@CsvField(pos = 25)
	@JsonIgnore
	int cond=0;
	@CsvField(pos = 26)
	@JsonIgnore
	int elseAdded=0;
	@CsvField(pos = 27)
	@JsonIgnore
	int elseDeleted=0;


	//@CsvField(pos = 27)
	@JsonIgnore
	int LOC = 0;
	//@CsvField(pos = 28)
	@JsonIgnore
	int addLOC = 0;
	//@CsvField(pos = 29)
	@JsonIgnore
	int delLOC = 0;
	//@CsvField(pos = 30)
	@JsonIgnore
	int devMinor    = 0;
	//@CsvField(pos = 31)
	@JsonIgnore
	int devMajor    = 0;
	//@CsvField(pos = 32)
	@JsonIgnore
	double ownership   = 0;
	//@CsvField(pos = 33)
	@JsonIgnore
	int fixChgNum = 0;
	//@CsvField(pos = 34)
	@JsonIgnore
	int pastBugNum  = 0;
	//@CsvField(pos = 35)
	@JsonIgnore
	int bugIntroNum = 0;
	//@CsvField(pos = 36)
	@JsonIgnore
	int logCoupNum  = 0;
	//@CsvField(pos = 37)
	@JsonIgnore
	int period      = 0;
	//@CsvField(pos = 38)
	@JsonIgnore
	double avgInterval = 0;
	//@CsvField(pos = 39)
	@JsonIgnore
	int maxInterval = 0;
	//@CsvField(pos = 40)
	@JsonIgnore
	int minInterval = 0;

	public Module clone() {
		Module module = null;
		try {
			module = (Module) super.clone();
			module.id=this.id;
			module.path=this.path;
			module.source=this.source;
			module.changesOnModule =this.changesOnModule;
			module.commitsHead=this.commitsHead;
			module.commitsRoot=this.commitsRoot;
		}catch (Exception e){
			module = null;
		}
		return module;
	}

	public Module() {
		this.path=new String();
		this.changesOnModule = new ChangesOnModule();
		this.commitsHead = new ArrayList<>();
		this.commitsRoot = new ArrayList<>();
	}

	public Module(String path) {
		this.path=path;
		this.changesOnModule = new ChangesOnModule();
		this.commitsHead = new ArrayList<>();
		this.commitsRoot = new ArrayList<>();
	}

	public void calcMaxNesting() {
		VisitorMaxNesting visitorMaxNesting = new VisitorMaxNesting();
		compilationUnit.accept(visitorMaxNesting);
		maxNesting = visitorMaxNesting.maxNesting;
	}

	public void calcExecStmt() {
		VisitorExecStmt visitorExecStmt = new VisitorExecStmt();
		compilationUnit.accept(visitorExecStmt);
		execStmt = visitorExecStmt.execStmt;
	}

	public  void calcComplexity() {
		VisitorComplexity visitorComplexity = new VisitorComplexity();
		compilationUnit.accept(visitorComplexity);
		complexity = visitorComplexity.complexity;
	}

	public void calcCountPath() {
		VisitorCountPath visitorCountPath = new VisitorCountPath();
		compilationUnit.accept(visitorCountPath);
		long countPath=1;
		for(int branch: visitorCountPath.branches) {
			countPath*=branch;
		}
		this.countPath = countPath;
	}

	public void calcCommentRatio() {
		String regex  = "\n|\r\n";
		String[] linesMethod = this.source.split(regex, 0);

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

	public  void calcLocalVar() {
		VisitorLocalVar visitorLocalVar = new VisitorLocalVar();
		compilationUnit.accept(visitorLocalVar);
		localVar = visitorLocalVar.NOVariables;
	}

	public  void calcParameters() {
		data.VisitorMethodDeclaration visitorMethodDeclaration = new data.VisitorMethodDeclaration();
		compilationUnit.accept(visitorMethodDeclaration);
		parameters = visitorMethodDeclaration.parameters;
	}

	public  void calcFanOut() {
		VisitorFanout visitor = new VisitorFanout();
		compilationUnit.accept(visitor);
		this.fanOut = visitor.fanout;
	}

	public  void calcCond(Commits commitsAll, String[] intervalCommit) {
		int cond=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
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
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
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
		if(moduleHistories==0) this.avgChurn = 0;
		else this.avgChurn= churn/(float)moduleHistories;
	}

	public  void calcMaxChurn(Commits commitsAll, String[] intervalCommit) {
		int maxChurn=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			int churnTemp=0;
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
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
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)churn++;
				else if(change.getChangeType()==ChangeType.STATEMENT_DELETE)churn--;
			}
		}
		this.churn = churn;
	}

	public  void calcAvgStmtDeleted(Commits commitsAll, String[] intervalCommit) {
		int avgStmtDeleted=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_DELETE)avgStmtDeleted++;
			}
		}
		calcModuleHistories(commitsAll, intervalCommit);
		if(moduleHistories==0) this.avgStmtDeleted = 0;
		else this.avgStmtDeleted = avgStmtDeleted/(double)moduleHistories;
	}

	public  void calcMaxStmtDeleted(Commits commitsAll, String[] intervalCommit) {
		int maxStmtDeleted=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			int stmtDeletedOnCommit=0;
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
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
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_DELETE)stmtDeleted++;
			}
		}
		this.stmtDeleted = stmtDeleted;
	}

	public  void calcAvgStmtAdded(Commits commitsAll, String[] intervalCommit) {
		int avgStmtAdded=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)avgStmtAdded++;
			}
		}
		calcModuleHistories(commitsAll, intervalCommit);
		if(moduleHistories==0) this.avgStmtAdded = 0;
		else this.avgStmtAdded = avgStmtAdded/(double)moduleHistories;
	}

	public  void calcMaxStmtAdded(Commits commitsAll, String[] intervalCommit) {
		int maxStmtAdded=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll,intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			int stmtAddedTemp=0;
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
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
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
			for(SourceCodeChange change : changes) {
				if(change.getChangeType()==ChangeType.STATEMENT_INSERT)stmtAdded++;
			}
		}
		this.stmtAdded = stmtAdded;
	}

	public  void calcElseDeleted(Commits commitsAll, String[] intervalCommit) {
		int elseDeleted=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
			for(SourceCodeChange change : changes) {
				EntityType et = change.getChangedEntity().getType();
				if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_DELETE & et.toString().equals("ELSE_STATEMENT"))elseDeleted++;
			}
		}
		this.elseDeleted = elseDeleted;
	}

	public  void calcElseAdded(Commits commitsAll, String[] intervalCommit) {
		int elseAdded=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		for(ChangeOnModule changeOnModule : changeOnModules) {
			List<SourceCodeChange> changes = identifyChanges(changeOnModule);
			for(SourceCodeChange change : changes) {
				EntityType et = change.getChangedEntity().getType();
				if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_INSERT & et.toString().equals("ELSE_STATEMENT") )elseAdded++;
			}
		}
		this.elseAdded = elseAdded;
	}

	public  void calcAuthors(Commits commitsAll, String[] intervalCommit) {
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

	public  void calcLOC(String pathRepository) {
		//String sourceCode = readSourceCode(pathRepository);
		//this.LOC = (int)sourceCode.count();
	}

	public  void calcAddLOC(Commits commitsAll, String[] intervalCommit) {
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		int addLOC=0;
		for(ChangeOnModule changeOnModule : changeOnModules){
			addLOC += changeOnModule.calcNOAddedLines();
		}
		this.addLOC = addLOC;
	}

	public  void calcDelLOC(Commits commitsAll, String[] intervalCommit) {
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		int delLOC=0;
		for(ChangeOnModule changeOnModule : changeOnModules){
			delLOC += changeOnModule.calcNODeletedLines();
		}
		this.delLOC = delLOC;
	}

	public  void calcDevMinor(Commits commitsAll, String[] intervalCommit) {
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		Set<String> setAuthors = new HashSet<>();
		changeOnModules.stream().forEach(item->setAuthors.add(item.author));

		int devMinor = 0;
		for(String author: setAuthors){
			int count = changeOnModules.stream().filter(item->item.author.equals(author)).collect(Collectors.toList()).size();
			if(count/(float) changeOnModules.size()<0.2){
				devMinor++;
			}
		}
		this.devMinor = devMinor;
	}

	public  void calcDevMajor(Commits commitsAll, String[] intervalCommit) {
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		Set<String> setAuthors = new HashSet<>();
		changeOnModules.stream().forEach(item->setAuthors.add(item.author));

		int devMajor = 0;
		for(String author: setAuthors){
			int count = changeOnModules.stream().filter(item->item.author.equals(author)).collect(Collectors.toList()).size();
			if(0.2<count/(float) changeOnModules.size()){
				devMajor++;
			}
		}
		this.devMajor = devMajor;
	}

	public void calcOwnership(Commits commitsAll, String[] intervalCommit) {
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		Set<String> setAuthors = new HashSet<>();
		changeOnModules.stream().forEach(item->setAuthors.add(item.author));

		for(String author: setAuthors){
			int count = changeOnModules.stream().filter(item->item.author.equals(author)).collect(Collectors.toList()).size();
			double ownership = count/(float) changeOnModules.size();
			if(this.ownership<ownership){
				this.ownership=ownership;
			}
		}
	}

	public  void calcFixChgNum(Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<BugAtomic> bugAtomics = bugsAll.identifyAtomicBugs(this.path);
		for(BugAtomic bugAtomic: bugAtomics){
			int dateBegin = commitsAll.get(intervalCommit[0]).date;
			int dateCommitFix = commitsAll.get(bugAtomic.idCommitFix).date;
			int dateEnd = commitsAll.get(intervalCommit[1]).date;
			if(dateBegin<dateCommitFix & dateCommitFix<dateEnd){
				this.fixChgNum++;
			}
		}
	}

	public  void calcPastBugNum(Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<Bug> bugs = bugsAll.identifyBug(this.path);
		for(Bug bug: bugs){
			for(BugAtomic bugAtomic: bug.bugAtomics){
				int dateBegin = commitsAll.get(intervalCommit[0]).date;
				int dateCommitFix = commitsAll.get(bugAtomic.idCommitFix).date;
				int dateEnd = commitsAll.get(intervalCommit[1]).date;
				if(dateBegin<dateCommitFix & dateCommitFix<dateEnd){
					this.pastBugNum++;
					break;
				}
			}
		}
	}
/*
	public void calcBugIntroNum(Modules modulesAll, Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<Commit> commitsInInterval = calcCommitsInInterval(commitsAll, intervalCommit);
		for(Commit commit : commitsInInterval){
			if(isCommitInducingBugToOtherModule(commit, modulesAll, bugsAll)){
				this.bugIntroNum++;
			}
		}
	}
 */
/*
	public boolean isCommitInducingBugToOtherModule(Commit commit, Modules modulesAll, Bugs bugsAll){
		for(Modification modification: commit.modifications.values()){
			Module module = modification.type.equals("DELETE")? modulesAll.get(modification.pathOld):modulesAll.get(modification.pathNew);
			Set<String> paths = module.modifications.values().stream().map(a->a.pathNew).collect(Collectors.toSet());
			for(String path: paths){
				List<Bug> bugs = bugsAll.identifyBug(path);
				for(Bug bug: bugs){
					for(BugAtomic bugAtomic: bug.bugAtomics){
						for(String idCommitInduce: bugAtomic.idsCommitInduce){
							if(idCommitInduce.equals(commit.id)){
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
*/
	/*
	public  void calcLogCoupNum(Modules modulesAll, Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<Commit> commitsInInterval = calcCommitsInInterval(commitsAll, intervalCommit);
		for(Commit commit : commitsInInterval){
			if(isCommitChangeModuleHasBeenBuggy(commit, commitsAll, modulesAll, bugsAll)){
				this.logCoupNum++;
			}
		}
	}
	 */
/*
	private boolean isCommitChangeModuleHasBeenBuggy(Commit commit, Commits commitsAll, Modules modulesAll, Bugs bugsAll) {
		for(Modification modification: commit.modifications.values()){
			Module module = modification.type.equals("DELETE")? modulesAll.get(modification.pathOld):modulesAll.get(modification.pathNew);
			Set<String> paths = module.modifications.values().stream().map(a->a.pathNew).collect(Collectors.toSet());
			for(String path: paths){
				List<Bug> bugs = bugsAll.identifyBug(path);
				for(Bug bug: bugs){
					for(BugAtomic bugAtomic: bug.bugAtomics){
						for(String idCommitInduce: bugAtomic.idsCommitInduce){
							if(commitsAll.get(idCommitInduce).date<commit.date){
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
*/
	public  void calcPeriod(Commits commitsAll, String[] intervalCommit) {
		int periodFrom = Integer.MAX_VALUE;
		int periodTo   = commitsAll.get(intervalCommit[1]).date;
		for(ChangeOnModule changeOnModule : changesOnModule.values()){
			if(changeOnModule.date<periodFrom){
				periodFrom = changeOnModule.date;
			}
		}
		if(periodFrom<commitsAll.get(intervalCommit[0]).date){
			periodFrom = commitsAll.get(intervalCommit[0]).date;
		}
		this.period = (periodTo-periodFrom)/(60*60*24);
	}

	public  void calcAvgInterval(Commits commitsAll, String[] intervalCommit) {
		calcPeriod(commitsAll ,intervalCommit);
		calcModuleHistories(commitsAll ,intervalCommit);
		this.avgInterval = this.period/this.moduleHistories;
	}

	public  void calcMaxInterval(Commits commitsAll, String[] intervalCommit) {
		int maxInterval=0;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		changeOnModules = changeOnModules.stream().sorted(Comparator.comparingInt(a -> a.date)).collect(Collectors.toList());
		if(changeOnModules.size()<2){
			this.maxInterval=0;
			return;
		}
		for(int i = 0; i< changeOnModules.size()-1; i++){
			int interval= changeOnModules.get(i+1).date- changeOnModules.get(i).date;
			if(maxInterval<interval){
				maxInterval=interval;
			}
		}
		this.maxInterval=maxInterval/(60*60*24*7);
	}

	public  void calcMinInterval(Commits commitsAll, String[] intervalCommit) {
		int minInterval=Integer.MAX_VALUE;
		List<ChangeOnModule> changeOnModules = calcModificationsInInterval(commitsAll, intervalCommit);
		changeOnModules = changeOnModules.stream().sorted(Comparator.comparingInt(a -> a.date)).collect(Collectors.toList());
		if(changeOnModules.size()<2){
			this.minInterval=0;
			return;
		}
		for(int i = 0; i< changeOnModules.size()-1; i++){
			int interval= changeOnModules.get(i+1).date- changeOnModules.get(i).date;
			if(interval < minInterval){
				minInterval=interval;
			}
		}
		this.minInterval=minInterval/(60*60*24*7);
	}

	public  List<Commit> calcCommitsInInterval(Commits commitsAll, String[] intervalCommit){
		List<Commit> commits = new ArrayList<Commit>();

		Commit commitBeginning = commitsAll.get(intervalCommit[0]);
		int dateBegin = commitBeginning.date;
		Commit commitEnd = commitsAll.get(intervalCommit[1]);
		int dateEnd   = commitEnd.date;
		for(ChangeOnModule changeOnModule : changesOnModule.values()) {
			Commit commit = commitsAll.get(changeOnModule.idCommit);
			if(dateBegin<=commit.date & commit.date<=dateEnd & !commit.isMerge) {
				commits.add(commit);
			}
		}
		return commits;
	}

	public  List<ChangeOnModule> calcModificationsInInterval(Commits commitsAll, String[] intervalCommit){
		List<ChangeOnModule> modificationsResult = new ArrayList<>();

		int dateBegin = commitsAll.get(intervalCommit[0]).date;
		int dateEnd   = commitsAll.get(intervalCommit[1]).date;
		for(ChangeOnModule changeOnModule : changesOnModule.values()) {
			Commit commit = commitsAll.get(changeOnModule.idCommit);
			if(dateBegin<=commit.date & commit.date<=dateEnd & !commit.isMerge) {
				modificationsResult.add(changeOnModule);
			}
		}
		return  modificationsResult;
	}

	public void calcIsBuggy(Commits commitsAll, Bugs bugsAll,  String[] intervalCommit) {
		for(String oneOfPath: calcPaths()) {
			List<BugAtomic> bugAtomics = bugsAll.identifyAtomicBugs(oneOfPath);
			for (BugAtomic bugAtomic : bugAtomics) {
				Commit commitFix = commitsAll.get(bugAtomic.idCommitFix);
				Commit commitTimePoint = commitsAll.get(intervalCommit[1]);
				Commit commitLastBugFix = commitsAll.get(intervalCommit[2]);
				for (String idCommit : bugAtomic.idsCommitInduce) {
					Commit commitInduce = commitsAll.get(idCommit);
					if (commitInduce.date < commitTimePoint.date & commitTimePoint.date < commitFix.date & commitFix.date < commitLastBugFix.date)
						isBuggy = 1;
				}
			}
		}
	}

	public Set<String> calcPaths(){
		Set<String> paths = new HashSet<>();
		for(ChangeOnModule changeOnModule : this.changesOnModule.values()){
			if(!Objects.equals(changeOnModule.type, "DELETE")) paths.add(changeOnModule.pathNew);
		}
		return paths;
	}

	public void calcHasBeenBuggy(Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<BugAtomic> bugAtomics = bugsAll.identifyAtomicBugs(path);
		if(bugAtomics==null)return;
		for(BugAtomic bugAtomic: bugAtomics) {
			Commit commitFix = commitsAll.get(bugAtomic.idCommitFix);
			Commit commitTimePoint = commitsAll.get(intervalCommit[1]);
			if (commitFix.date < commitTimePoint.date){
					this.hasBeenBuggy = 1;
			}
		}
	}

	public void calcCompilationUnit() {
		String sourceClass = "public class Dummy{"+this.source+"}";
		ASTParser parser = ASTParser.newParser(AST.JLS14);
		parser.setSource(sourceClass.toCharArray());
		this.compilationUnit =(CompilationUnit) parser.createAST(new NullProgressMonitor());
	}

	public void loadSrcFromRepository(Repository repositoryMethod, String idCommit) throws IOException {
		RevCommit revCommit = repositoryMethod.parseCommit(repositoryMethod.resolve(idCommit));
		RevTree tree = revCommit.getTree();
		try (TreeWalk treeWalk = new TreeWalk(repositoryMethod)) {
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathSuffixFilter.create(path));
			while (treeWalk.next()) {
				ObjectLoader loader = repositoryMethod.open(treeWalk.getObjectId(0));
				this.source = new String(loader.getBytes());
			}
		}
	}

	public  List<SourceCodeChange> identifyChanges(ChangeOnModule changeOnModule){
		String sourcePrev =  null;
		String sourceCurrent =null;
		String strPre = null;
		String strPost = null;
		if(changeOnModule.sourceOld.equals("")) {
			String regex  = "\\n|\\r\\n";
			String tmp= changeOnModule.sourceNew;
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
			sourcePrev= "public class Dummy{"+ changeOnModule.sourceOld+"}";
			sourceCurrent ="public class Dummy{"+ changeOnModule.sourceNew+"}";
		}

		FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);
		try {
			distiller.extractClassifiedSourceCodeChanges(sourcePrev, sourceCurrent);
		} catch(Exception e) {
		}
		return distiller.getSourceCodeChanges();
	}
}