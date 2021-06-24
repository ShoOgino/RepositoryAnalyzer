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
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import lombok.Data;
import misc.DoubleConverter;
import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.w3c.dom.Node;

@JsonIgnoreProperties(ignoreUnknown = true)
@CsvDataType()
@Data
public class Module implements Cloneable {
    public Integer numOfModulesAll = null;
    public Integer num = null;
    @JsonIgnore
    public String id = "";
    @CsvField(pos = 1)
    public String path = null;
    public String source = null;
    public List<String> tokens = null;
    @JsonIgnore
    public CompilationUnit compilationUnit = null;
    public NodeAST4Experiment ast = null;
    public List<NodeCommit4Experiment> commitGraph = null;
    public ChangesOnModule changesOnModule = null;
    @JsonIgnore
    public List<Commit> commitsInInterval = null;
    public List<ChangeOnModule> changesOnModuleInInterval = null;
    public ArrayList<String> commitsHead = null;
    public ArrayList<String> commitsRoot = null;
    int hasBeenBuggy = 0;
    @CsvField(pos = 2)
    int isBuggy = 0;
    //code metrics
    @CsvField(pos = 3)
    int fanIn = 0;
    @CsvField(pos = 4)
    int fanOut = 0;
    @CsvField(pos = 5)
    int parameters = 0;
    @CsvField(pos = 6)
    int localVar = 0;
    @CsvField(pos = 7, converterType = DoubleConverter.class)
    double commentRatio = 0;
    @CsvField(pos = 8)
    long countPath = 0;
    @CsvField(pos = 9)
    int complexity = 0;
    @CsvField(pos = 10)
    int execStmt = 0;
    @CsvField(pos = 11)
    int maxNesting = 0;
    //process metrics
    @CsvField(pos = 12)
    int moduleHistories = 0;
    @CsvField(pos = 13)
    int authors = 0;
    @CsvField(pos = 14)
    int stmtAdded = 0;
    @CsvField(pos = 15)
    int maxStmtAdded = 0;
    @CsvField(pos = 16, converterType = DoubleConverter.class)
    double avgStmtAdded = 0;
    @CsvField(pos = 17)
    int stmtDeleted = 0;
    @CsvField(pos = 18)
    int maxStmtDeleted = 0;
    @CsvField(pos = 19, converterType = DoubleConverter.class)
    double avgStmtDeleted = 0;
    @CsvField(pos = 20)
    int churn = 0;
    @CsvField(pos = 21)
    int maxChurn = 0;
    @CsvField(pos = 22, converterType = DoubleConverter.class)
    double avgChurn = 0;
    @CsvField(pos = 23)
    int decl = 0;
    @CsvField(pos = 24)
    int cond = 0;
    @CsvField(pos = 25)
    int elseAdded = 0;
    @CsvField(pos = 26)
    int elseDeleted = 0;

    //@JsonIgnore @CsvField(pos = 27) int LOC = 0;
    //@JsonIgnore @CsvField(pos = 28)	int addLOC = 0;
    //@JsonIgnore @CsvField(pos = 29)	int delLOC = 0;
    //@JsonIgnore @CsvField(pos = 30)	int devMinor = 0;
    //@JsonIgnore @CsvField(pos = 31)	int devMajor = 0;
    //@JsonIgnore @CsvField(pos = 32)	double ownership = 0;
    //@JsonIgnore @CsvField(pos = 33)	int fixChgNum = 0;
    //@JsonIgnore @CsvField(pos = 34)	int pastBugNum = 0;
    //@JsonIgnore @CsvField(pos = 35)	int bugIntroNum = 0;
    //@JsonIgnore @CsvField(pos = 36)	int logCoupNum = 0;
    //@JsonIgnore @CsvField(pos = 37)	int period = 0;
    //@JsonIgnore @CsvField(pos = 38)	double avgInterval = 0;
    //@JsonIgnore @CsvField(pos = 39)	int maxInterval = 0;
    //@JsonIgnore @CsvField(pos = 40)	int minInterval = 0;
    public Module clone() {
        Module module = null;
        try {
            module = (Module) super.clone();
            module.id = this.id;
            module.path = this.path;
            module.source = this.source;
            module.changesOnModule = this.changesOnModule;
            module.commitsHead = this.commitsHead;
            module.commitsRoot = this.commitsRoot;
        } catch (Exception e) {
            module = null;
        }
        return module;
    }

    public Module() {
        this.path = new String();
        this.changesOnModule = new ChangesOnModule();
        this.commitsHead = new ArrayList<>();
        this.commitsRoot = new ArrayList<>();
    }

    public Module(String path) {
        this.path = path;
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

    public void calcComplexity() {
        VisitorComplexity visitorComplexity = new VisitorComplexity();
        compilationUnit.accept(visitorComplexity);
        complexity = visitorComplexity.complexity;
    }

    public void calcCountPath() {
        VisitorCountPath visitorCountPath = new VisitorCountPath();
        compilationUnit.accept(visitorCountPath);
        long countPath = 1;
        for (int branch : visitorCountPath.branches) {
            countPath *= branch;
        }
        this.countPath = countPath;
    }

    public void calcCommentRatio() {
        String regex = "\n|\r\n";
        String[] linesMethod = this.source.split(regex, 0);

        int countLineCode = 0;
        int countLineComment = 0;
        boolean inComment = false;
        for (String line : linesMethod) {
            countLineCode++;
            if (line.matches(".*\\*/\\S+")) {
                inComment = false;
            } else if (line.matches(".*\\*/\\s*")) {
                inComment = false;
                countLineComment++;
            } else if (inComment) {
                countLineComment++;
            } else if (line.matches("\\S+/\\*.*")) {
                inComment = true;
            } else if (line.matches("\\s*/\\*.*")) {
                countLineComment++;
                inComment = true;
            } else if (line.matches("\\S+//.*")) {
            } else if (line.matches("\\s*//.*")) {
                countLineComment++;
            }
        }
        commentRatio = (float) countLineComment / (float) countLineCode;
    }

    public void calcLocalVar() {
        VisitorLocalVar visitorLocalVar = new VisitorLocalVar();
        compilationUnit.accept(visitorLocalVar);
        localVar = visitorLocalVar.NOVariables;
    }

    public void calcParameters() {
        data.VisitorMethodDeclaration visitorMethodDeclaration = new data.VisitorMethodDeclaration();
        compilationUnit.accept(visitorMethodDeclaration);
        parameters = visitorMethodDeclaration.parameters;
    }

    public void calcFanOut() {
        VisitorFanout visitor = new VisitorFanout();
        compilationUnit.accept(visitor);
        this.fanOut = visitor.fanout;
    }

    public void calcCond() {
        int cond = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                EntityType et = change.getChangedEntity().getType();
                if (change.getChangeType() == ChangeType.CONDITION_EXPRESSION_CHANGE) cond++;
            }
        }
        this.cond = cond;
    }

    public void calcDecl() {
        int decl = 0;
        List<ChangeType> ctdecl = Arrays.asList(
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
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                EntityType et = change.getChangedEntity().getType();
                if (ctdecl.contains(change.getChangeType())) decl++;
            }
        }
        this.decl = decl;
    }

    public void calcAvgChurn() {
        calcChurn();
        calcModuleHistories();
        if (moduleHistories == 0) this.avgChurn = 0;
        else this.avgChurn = churn / (float) moduleHistories;
    }

    public void calcMaxChurn() {
        int maxChurn = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            int churnTemp = 0;
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_INSERT) churnTemp++;
                else if (change.getChangeType() == ChangeType.STATEMENT_DELETE) churnTemp--;
            }
            if (maxChurn < churnTemp) maxChurn = churnTemp;
        }
        this.maxChurn = maxChurn;
    }

    public void calcChurn() {
        int churn = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_INSERT) churn++;
                else if (change.getChangeType() == ChangeType.STATEMENT_DELETE) churn--;
            }
        }
        this.churn = churn;
    }

    public void calcAvgStmtDeleted() {
        int avgStmtDeleted = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_DELETE) avgStmtDeleted++;
            }
        }
        calcModuleHistories();
        if (moduleHistories == 0) this.avgStmtDeleted = 0;
        else this.avgStmtDeleted = avgStmtDeleted / (double) moduleHistories;
    }

    public void calcMaxStmtDeleted() {
        int maxStmtDeleted = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            int stmtDeletedOnCommit = 0;
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_DELETE) stmtDeletedOnCommit++;
            }
            if (maxStmtDeleted < stmtDeletedOnCommit) {
                maxStmtDeleted = stmtDeletedOnCommit;
            }
        }
        this.maxStmtDeleted = maxStmtDeleted;
    }

    public void calcStmtDeleted() {
        int stmtDeleted = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_DELETE) stmtDeleted++;
            }
        }
        this.stmtDeleted = stmtDeleted;
    }

    public void calcAvgStmtAdded() {
        int avgStmtAdded = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_INSERT) avgStmtAdded++;
            }
        }
        calcModuleHistories();
        if (moduleHistories == 0) this.avgStmtAdded = 0;
        else this.avgStmtAdded = avgStmtAdded / (double) moduleHistories;
    }

    public void calcMaxStmtAdded() {
        int maxStmtAdded = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            int stmtAddedTemp = 0;
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_INSERT) stmtAddedTemp++;
            }
            if (maxStmtAdded < stmtAddedTemp) {
                maxStmtAdded = stmtAddedTemp;
            }
        }
        this.maxStmtAdded = maxStmtAdded;
    }

    public void calcStmtAdded() {
        int stmtAdded = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                if (change.getChangeType() == ChangeType.STATEMENT_INSERT) stmtAdded++;
            }
        }
        this.stmtAdded = stmtAdded;
    }

    public void calcElseDeleted() {
        int elseDeleted = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                EntityType et = change.getChangedEntity().getType();
                if (change.getChangeType() == ChangeType.ALTERNATIVE_PART_DELETE & et.toString().equals("ELSE_STATEMENT"))
                    elseDeleted++;
            }
        }
        this.elseDeleted = elseDeleted;
    }

    public void calcElseAdded() {
        int elseAdded = 0;
        for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
            List<SourceCodeChange> changes = identifyChanges(changeOnModule);
            for (SourceCodeChange change : changes) {
                EntityType et = change.getChangedEntity().getType();
                if (change.getChangeType() == ChangeType.ALTERNATIVE_PART_INSERT & et.toString().equals("ELSE_STATEMENT"))
                    elseAdded++;
            }
        }
        this.elseAdded = elseAdded;
    }

    public void calcAuthors() {
        Set<String> setAuthors = new HashSet<>();
        commitsInInterval.stream().forEach(item -> setAuthors.add(item.author.name));
        int authors = setAuthors.size();
        this.authors = authors;
    }

    public void calcModuleHistories() {
        int moduleHistories = commitsInInterval.size();
        this.moduleHistories = moduleHistories;
    }
/*
	public void calcLOC(String pathRepository) {
		//String sourceCode = readSourceCode(pathRepository);
		//this.LOC = (int)sourceCode.count();
	}

	public void calcAddLOC(Commits commitsAll, String[] intervalCommit) {
		int addLOC = 0;
		for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
			addLOC += changeOnModule.calcNOAddedLines();
		}
		this.addLOC = addLOC;
	}

	public void calcDelLOC(Commits commitsAll, String[] intervalCommit) {
		int delLOC = 0;
		for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
			delLOC += changeOnModule.calcNODeletedLines();
		}
		this.delLOC = delLOC;
	}

	public void calcDevMinor() {
		Set<String> setAuthors = new HashSet<>();
		changesOnModuleInInterval.stream().forEach(item -> setAuthors.add(item.author));

		int devMinor = 0;
		for (String author : setAuthors) {
			int count = changesOnModuleInInterval.stream().filter(item -> item.author.equals(author)).collect(Collectors.toList()).size();
			if (count / (float) changesOnModuleInInterval.size() < 0.2) {
				devMinor++;
			}
		}
		this.devMinor = devMinor;
	}

	public void calcDevMajor() {
		Set<String> setAuthors = new HashSet<>();
		changesOnModuleInInterval.stream().forEach(item -> setAuthors.add(item.author));

		int devMajor = 0;
		for (String author : setAuthors) {
			int count = changesOnModuleInInterval.stream().filter(item -> item.author.equals(author)).collect(Collectors.toList()).size();
			if (0.2 < count / (float) changesOnModuleInInterval.size()) {
				devMajor++;
			}
		}
		this.devMajor = devMajor;
	}

	public void calcOwnership() {
		Set<String> setAuthors = new HashSet<>();
		changesOnModuleInInterval.stream().forEach(item -> setAuthors.add(item.author));

		for (String author : setAuthors) {
			int count = changesOnModuleInInterval.stream().filter(item -> item.author.equals(author)).collect(Collectors.toList()).size();
			double ownership = count / (float) changesOnModuleInInterval.size();
			if (this.ownership < ownership) {
				this.ownership = ownership;
			}
		}
	}

	public void calcFixChgNum(Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<BugAtomic> bugAtomics = bugsAll.identifyAtomicBugs(this.path);
		for (BugAtomic bugAtomic : bugAtomics) {
			int dateBegin = commitsAll.get(intervalCommit[0]).date;
			int dateCommitFix = commitsAll.get(bugAtomic.idCommitFix).date;
			int dateEnd = commitsAll.get(intervalCommit[1]).date;
			if (dateBegin < dateCommitFix & dateCommitFix < dateEnd) {
				this.fixChgNum++;
			}
		}
	}

	public void calcPastBugNum(Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<Bug> bugs = bugsAll.identifyBug(this.path);
		for (Bug bug : bugs) {
			for (BugAtomic bugAtomic : bug.bugAtomics) {
				int dateBegin = commitsAll.get(intervalCommit[0]).date;
				int dateCommitFix = commitsAll.get(bugAtomic.idCommitFix).date;
				int dateEnd = commitsAll.get(intervalCommit[1]).date;
				if (dateBegin < dateCommitFix & dateCommitFix < dateEnd) {
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

	public  void calcLogCoupNum(Modules modulesAll, Commits commitsAll, Bugs bugsAll, String[] intervalCommit) {
		List<Commit> commitsInInterval = calcCommitsInInterval(commitsAll, intervalCommit);
		for(Commit commit : commitsInInterval){
			if(isCommitChangeModuleHasBeenBuggy(commit, commitsAll, modulesAll, bugsAll)){
				this.logCoupNum++;
			}
		}
	}

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
	public void calcPeriod() {
		int periodFrom = Integer.MAX_VALUE;
		int periodTo = commitsAll.get(intervalCommit[1]).date;
		for (ChangeOnModule changeOnModule : changesOnModule.values()) {
			if (changeOnModule.date < periodFrom) {
				periodFrom = changeOnModule.date;
			}
		}
		if (periodFrom < commitsAll.get(intervalCommit[0]).date) {
			periodFrom = commitsAll.get(intervalCommit[0]).date;
		}
		this.period = (periodTo - periodFrom) / (60 * 60 * 24);
	}

	public void calcAvgInterval() {
		calcPeriod();
		calcModuleHistories();
		this.avgInterval = this.period / this.moduleHistories;
	}

	public void calcMaxInterval() {
		int maxInterval = 0;
		List<ChangeOnModule> changesOnModuleSorted = changesOnModuleInInterval.stream().sorted(Comparator.comparingInt(a -> a.date)).collect(Collectors.toList());
		if (changesOnModuleSorted.size() < 2) {
			this.maxInterval = 0;
			return;
		}
		for (int i = 0; i < changesOnModuleSorted.size() - 1; i++) {
			int interval = changesOnModuleSorted.get(i + 1).date - changesOnModuleSorted.get(i).date;
			if (maxInterval < interval) {
				maxInterval = interval;
			}
		}
		this.maxInterval = maxInterval / (60 * 60 * 24 * 7);
	}

	public void calcMinInterval() {
		int minInterval = Integer.MAX_VALUE;
		List<ChangeOnModule> changesOnModuleSorted = changesOnModuleInInterval.stream().sorted(Comparator.comparingInt(a -> a.date)).collect(Collectors.toList());
		if (changesOnModuleSorted.size() < 2) {
			this.minInterval = 0;
			return;
		}
		for (int i = 0; i < changesOnModuleSorted.size() - 1; i++) {
			int interval = changesOnModuleSorted.get(i + 1).date - changesOnModuleSorted.get(i).date;
			if (interval < minInterval) {
				minInterval = interval;
			}
		}
		this.minInterval = minInterval / (60 * 60 * 24 * 7);
	}
*/

    public void calcCommitsInInterval(Commits commitsAll, String revisionMethod_referHistoryFrom, String revisionMethodTarget) {
        List<Commit> commits = new ArrayList<Commit>();

        Commit commit_referHistoryFrom = commitsAll.get(revisionMethod_referHistoryFrom);
        int dateBegin = commit_referHistoryFrom.date;
        Commit commitTarget = commitsAll.get(revisionMethodTarget);
        int dateEnd = commitTarget.date;
        for (ChangeOnModule changeOnModule : changesOnModule.values()) {
            Commit commit = commitsAll.get(changeOnModule.idCommit);
            if (dateBegin <= commit.date & commit.date <= dateEnd) {
                commits.add(commit);
            }
        }
        this.commitsInInterval = commits;
    }

    public void calcModificationsInInterval(Commits commitsAll, String revisionMethod_referHistoryFrom, String revisionMethod_target) {
        List<ChangeOnModule> modificationsResult = new ArrayList<>();

        int dateBegin = commitsAll.get(revisionMethod_referHistoryFrom).date;
        int dateEnd = commitsAll.get(revisionMethod_target).date;
        for (ChangeOnModule changeOnModule : changesOnModule.values()) {
            Commit commit = commitsAll.get(changeOnModule.idCommit);
            if (dateBegin <= commit.date & commit.date <= dateEnd) {
                modificationsResult.add(changeOnModule);
            }
        }
        this.changesOnModuleInInterval = modificationsResult;
    }

    public void calcIsBuggy(Commits commitsAll, String revisionMethod_target, String revisionMethod_referBugReportsUntil, Bugs bugsAll) {
        for (String oneOfPath : calcPaths()) {
            List<BugAtomic> bugAtomics = bugsAll.identifyAtomicBugs(oneOfPath);
            for (BugAtomic bugAtomic : bugAtomics) {
                Commit commitFix = commitsAll.get(bugAtomic.idCommitFix);
                Commit commitTimePoint = commitsAll.get(revisionMethod_target);
                Commit commitLastBugFix = commitsAll.get(revisionMethod_referBugReportsUntil);
                for (String idCommit : bugAtomic.idsCommitInduce) {
                    Commit commitInduce = commitsAll.get(idCommit);
                    if (commitInduce.date < commitTimePoint.date & commitTimePoint.date < commitFix.date & commitFix.date < commitLastBugFix.date)
                        isBuggy = 1;
                }
            }
        }
    }

    public Set<String> calcPaths() {
        Set<String> paths = new HashSet<>();
        for (ChangeOnModule changeOnModule : this.changesOnModule.values()) {
            if (!Objects.equals(changeOnModule.type, "DELETE")) paths.add(changeOnModule.pathNew);
        }
        return paths;
    }

    public void calcHasBeenBuggy(Commits commitsAll, String revisionMethod_target, Bugs bugsAll) {
        List<BugAtomic> bugAtomics = bugsAll.identifyAtomicBugs(path);
        if (bugAtomics == null) return;
        for (BugAtomic bugAtomic : bugAtomics) {
            Commit commitFix = commitsAll.get(bugAtomic.idCommitFix);
            Commit commitTimePoint = commitsAll.get(revisionMethod_target);
            if (commitFix.date < commitTimePoint.date) {
                this.hasBeenBuggy = 1;
            }
        }
    }

    public void calcCompilationUnit() {
        String sourceClass = "public class Dummy{" + this.source + "}";
        ASTParser parser = ASTParser.newParser(AST.JLS14);
        parser.setSource(sourceClass.toCharArray());
        this.compilationUnit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
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

    public List<SourceCodeChange> identifyChanges(ChangeOnModule changeOnModule) {
        String sourcePrev = null;
        String sourceCurrent = null;
        String strPre = null;
        String strPost = null;
        if (changeOnModule.sourceOld.equals("")) {
            String regex = "\\n|\\r\\n";
            String tmp = changeOnModule.sourceNew;
            String[] lines = tmp.split(regex, 0);

            boolean inComment = false;
            int count = 0;
            for (String line : lines) {
                if (line.matches(".*/\\*.*")) {
                    inComment = true;
                    count++;
                } else if (line.matches(".*\\*/.*")) {
                    inComment = false;
                    count++;
                } else if (inComment) {
                    count++;
                } else if (line.matches(".*//.*")) {
                    count++;
                } else {
                    break;
                }
            }
            tmp = "";
            for (int i = count; i < lines.length; i++) {
                tmp = tmp + lines[i] + "\n";
            }

            Pattern patternPre = Pattern.compile("[\\s\\S.]*?(?=\\{)");
            Matcher matcherPre = patternPre.matcher(tmp);
            if (matcherPre.find()) {
                strPre = matcherPre.group();
            }
            Pattern patternPost = Pattern.compile("(?<=\\{)[\\s\\S.]*");
            Matcher matcherPost = patternPost.matcher(tmp);
            if (matcherPost.find()) {
                strPost = matcherPost.group();
            }
            sourcePrev = "public class Test{" + strPre +
                    "{" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "}" +
                    "}";
            sourceCurrent = "public class Test{" + strPre +
                    "{" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    "dummy();\n" +
                    strPost +
                    "}";
        } else {
            sourcePrev = "public class Dummy{" + changeOnModule.sourceOld + "}";
            sourceCurrent = "public class Dummy{" + changeOnModule.sourceNew + "}";
        }

        FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);
        try {
            distiller.extractClassifiedSourceCodeChanges(sourcePrev, sourceCurrent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distiller.getSourceCodeChanges();
    }

    private int countASTNode = 0;

    public void calcAST() {
        NodeAST4Experiment nodeAST4Experiment = new NodeAST4Experiment();
        nodeAST4Experiment.num = countASTNode++;
        nodeAST4Experiment.numType = compilationUnit.getNodeType();
        String nameClass = compilationUnit.getClass().toString();
        if (0 < nameClass.split("\\.").length) {
            nodeAST4Experiment.nameType = nameClass.split("\\.")[nameClass.split("\\.").length - 1];
        } else {
            nodeAST4Experiment.nameType = nameClass;
        }
        nodeAST4Experiment.source = compilationUnit.toString();
        calcChildren(compilationUnit, nodeAST4Experiment);
    }

    public void calcChildren(ASTNode node, NodeAST4Experiment nodeAST4Experiment) {
        for (ASTNode nodeChild : getChildren(node)) {
            NodeAST4Experiment nodeAST4ExperimentChild = new NodeAST4Experiment();
            if (nodeChild.getNodeType() == 31) {
                countASTNode = 0;
                this.ast = nodeAST4ExperimentChild;
            }
            nodeAST4ExperimentChild.num = countASTNode++;
            nodeAST4ExperimentChild.numType = nodeChild.getNodeType();
            String nameClass = nodeChild.getClass().toString();
            if (0 < nameClass.split("\\.").length) {
                nodeAST4ExperimentChild.nameType = nameClass.split("\\.")[nameClass.split("\\.").length - 1];
            } else {
                nodeAST4ExperimentChild.nameType = nameClass;
            }
            nodeAST4ExperimentChild.source = nodeChild.toString();
            nodeAST4ExperimentChild.parent = nodeAST4Experiment;
            nodeAST4Experiment.children.add(nodeAST4ExperimentChild);
            calcChildren(nodeChild, nodeAST4ExperimentChild);
        }
    }

    public List<ASTNode> getChildren(ASTNode node) {
        List<ASTNode> children = new ArrayList<ASTNode>();
        List list = node.structuralPropertiesForType();

        for (int i = 0; i < list.size(); i++) {
            Object child = node.getStructuralProperty((StructuralPropertyDescriptor) list.get(i));
            if (child instanceof ASTNode) {
                children.add((ASTNode) child);
            } else if (child instanceof List) {
                for (Object object : (List) child) {
                    children.add((ASTNode) object);
                }
            }
        }
        return children;
    }

    public void calcCommitGraph(Commits commitsAll, Modules modulesAll, String revisionMethod_target, Bugs bugs) throws IOException {
            Map<String, Integer> id2Num = new HashMap<>();
            this.commitGraph = new ArrayList<>();
            this.changesOnModuleInInterval = this.changesOnModuleInInterval.stream().sorted(Comparator.comparingInt(ChangeOnModule::getDate).reversed()).collect(Collectors.toList());
            List<ChangeOnModule> changeOnModulesHead = identifyChangeOnModuleHead(commitsAll, revisionMethod_target);
            List<ChangeOnModule> changeOnModulesTarget = identifyChangeOnModuleHead(commitsAll, revisionMethod_target);
            for (ChangeOnModule changeOnModule : this.changesOnModuleInInterval) {
                if (checkIfTheChangeIs(changeOnModulesTarget, changeOnModule)) {
                    changeOnModulesTarget.add(changeOnModule);
                }
            }
            for (int i = 0; i < changeOnModulesTarget.size(); i++) {
                ChangeOnModule changeOnModule = changeOnModulesTarget.get(i);
                id2Num.put(changeOnModule.idCommit + changeOnModule.idCommitParent, i + 1);
            }
            for (ChangeOnModule changeOnModule : changeOnModulesTarget) {
                NodeCommit4Experiment nodeCommit4Experiment = new NodeCommit4Experiment();
                //id
                nodeCommit4Experiment.idCommit = changeOnModule.idCommit;
                nodeCommit4Experiment.idCommitParent = changeOnModule.idCommitParent;
                //node and edge
                nodeCommit4Experiment.num = id2Num.get(changeOnModule.idCommit + changeOnModule.idCommitParent);
                for (ChangeOnModule changeOnModuleParent : changeOnModule.parentsModification.values()) {
                    nodeCommit4Experiment.parents.add(id2Num.get(changeOnModuleParent.idCommit + changeOnModuleParent.idCommitParent));
                }
                //content
                //1. semantic type
                JdtTreeGenerator jdtTreeGenerator = new JdtTreeGenerator();
                String sourcePrev = "public class Test{" + changeOnModule.sourceOld + "}";
                String sourceCurrent = "public class Test{" + changeOnModule.sourceNew + "}";
                ITree iTreePrev = jdtTreeGenerator.generateFrom().string(sourcePrev).getRoot();
                ITree iTreeCurrent = jdtTreeGenerator.generateFrom().string(sourceCurrent).getRoot();
                com.github.gumtreediff.matchers.Matcher defaultMatcher = Matchers.getInstance().getMatcher();
                MappingStore mappings = defaultMatcher.match(iTreePrev, iTreeCurrent);
                EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                EditScript actions = editScriptGenerator.computeActions(mappings);
                for(Action action: actions){
                    System.out.println(action.getName());
                    System.out.println(action.getNode().getType());
                }
                //2. author
                nodeCommit4Experiment.author = commitsAll.get(changeOnModule.idCommit).author;

                //4. interval
                int interval = 0;
                for (ChangeOnModule changeOnModuleParent : changeOnModule.parentsModification.values()) {
                    interval += (changeOnModule.date - changeOnModuleParent.date) / (60 * 60 * 24);
                }
                nodeCommit4Experiment.interval = interval;
                //5. code churn
                nodeCommit4Experiment.churn[0] = changeOnModule.calcNOAddedLines();
                nodeCommit4Experiment.churn[1] = changeOnModule.calcNODeletedLines();
                nodeCommit4Experiment.churn[2] = nodeCommit4Experiment.churn[0] - nodeCommit4Experiment.churn[1];
                //6. co-change
                for (ChangeOnModule changeOnModuleCoChange : commitsAll.get(changeOnModule.idCommit).idParent2Modifications.get(changeOnModule.idCommitParent).values()) {
                    //pathOld
                    Module modulePathOld = modulesAll.get(changeOnModuleCoChange.pathOld);
                    if (modulePathOld != null) nodeCommit4Experiment.coupling.add(modulePathOld.num);
                    //pathNew
                    Module modulePathNew = modulesAll.get(changeOnModuleCoChange.pathNew);
                    if (modulePathNew != null) nodeCommit4Experiment.coupling.add(modulePathNew.num);
                }
                //others
                nodeCommit4Experiment.isMerge = changeOnModule.isMerge;
                nodeCommit4Experiment.isFixingBug = bugs.calculateIsFix(changeOnModule.idCommit);

                commitGraph.add(nodeCommit4Experiment);
            }
            Commit commitHead = commitsAll.get(revisionMethod_target);
            NodeCommit4Experiment nodeCommit4ExperimentHead = new NodeCommit4Experiment();
            nodeCommit4ExperimentHead.num = 0;
            nodeCommit4ExperimentHead.interval = (commitHead.date - changeOnModulesTarget.get(0).date) / (60 * 60 * 24);
            for (ChangeOnModule changeOnModuleParent : changeOnModulesHead) {
                nodeCommit4ExperimentHead.parents.add(id2Num.get(changeOnModuleParent.idCommit + changeOnModuleParent.idCommitParent));
            }
            this.commitGraph.add(nodeCommit4ExperimentHead);
    }

    public boolean checkIfTheChangeIs(List<ChangeOnModule> changeOnModulesTarget, ChangeOnModule changeOnModule) {
        for (ChangeOnModule changeOnModuleChild : changeOnModule.childrenModification.values()) {
            for (ChangeOnModule changeOnModuleTarget : changeOnModulesTarget) {
                if (Objects.equals(changeOnModuleChild.idCommit, changeOnModuleTarget.idCommit)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ChangeOnModule> identifyChangeOnModuleHead(Commits commitsAll, String revisionMethod_target) {
        List<ChangeOnModule> changesOnModulesHead = new ArrayList<>();
        Commit commit = commitsAll.get(revisionMethod_target);
        List<String> idsCommit = commitsInInterval.stream().map(a -> a.id).collect(Collectors.toList());
        while (true) {
            if (Objects.equals(commit, null)) {
                break;
            } else if (idsCommit.contains(commit.id)) {
                for (ChangeOnModule changeOnModule : changesOnModuleInInterval) {
                    if (Objects.equals(changeOnModule.idCommit, commit.id)) {
                        changesOnModulesHead.add(changeOnModule);
                    }
                }
                break;
            }
            commit = commitsAll.get(commit.idParentMaster);
        }
        return changesOnModulesHead;
    }
}