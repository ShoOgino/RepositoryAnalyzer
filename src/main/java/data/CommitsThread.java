package data;

import me.tongfei.progressbar.ProgressBar;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CommitsThread  extends Thread{
    Repository repository;
    List<RevCommit> revcommits;
    String pathCommits;

    public CommitsThread(Repository repository, List<RevCommit> revcommits, String pathCommits){
        this.repository = repository;
        this.revcommits = revcommits;
        this.pathCommits = pathCommits;
    }

    public void run() {
        try {
            System.out.println("thread started");
            //それぞれのコミットについて、変更内容を取得する。
            for (RevCommit revCommit : ProgressBar.wrap(revcommits, "loadCommitsFromRepository")) {
                Commit commit = new Commit();
                commit.id = revCommit.getName();
                commit.date = revCommit.getCommitTime();
                commit.author = new Person(revCommit.getAuthorIdent().getName());
                commit.isMerge = revCommit.getParentCount() > 1;
                commit.idParentMaster = revCommit.getParentCount() == 0 ? "0000000000000000000000000000000000000000" : revCommit.getParent(0).getName();
                if (revCommit.getParentCount() == 0) {
                    ChangesOnModule changesOnModule = calcChangeOnModulesBetweenCommits(repository, revCommit, null);
                    commit.idParent2Modifications.put("0000000000000000000000000000000000000000", changesOnModule);
                } else {
                    for (RevCommit revCommitParent : revCommit.getParents()) {
                        ChangesOnModule changesOnModule = calcChangeOnModulesBetweenCommits(repository, revCommit, revCommitParent);
                        commit.idParent2Modifications.put(revCommitParent.getName(), changesOnModule);
                    }
                }
                for (RevCommit revCommitParentSub : revCommit.getParents()) {
                    if (Objects.equals(revCommitParentSub.getName(), commit.idParentMaster)) continue;
                    ChangesOnModule changesOnModuleSub = commit.idParent2Modifications.get(revCommitParentSub.getName());
                    ChangesOnModule changesOnModuleMain = commit.idParent2Modifications.get(commit.idParentMaster);
                    List<String> pathsMain = changesOnModuleMain.values().stream().map(a -> a.pathNew).collect(Collectors.toList());
                    changesOnModuleSub.entrySet().removeIf(entry -> !pathsMain.contains(entry.getKey().getKey(3)));
                    for (ChangeOnModule changeOnModule : changesOnModuleMain.values()) {
                        if (0 == changesOnModuleSub.findFromPathNew(changeOnModule.pathNew).size()) {
                            if (!changeOnModule.type.equals("DELETE")) {
                                ChangeOnModule m = new ChangeOnModule();
                                m.idCommit = revCommit.getName();
                                m.idCommitParent = revCommitParentSub.getName();
                                m.date = revCommit.getCommitTime();
                                m.author = new Person(revCommit.getAuthorIdent().getName());
                                m.isMerge = revCommit.getParentCount() > 1;
                                m.type = "UNCHANGE";
                                m.pathOld = changeOnModule.pathNew;
                                m.pathNew = changeOnModule.pathNew;
                                m.sourceOld = changeOnModule.sourceNew;
                                m.sourceNew = changeOnModule.sourceNew;
                                m.pathNewParent = changeOnModule.pathNew;
                                changesOnModuleSub.put(m.idCommitParent, m.idCommit, m.pathOld, m.pathNew, m);
                            }
                        }
                    }
                }
                commit.save(pathCommits + "/" + commit.id + ".json", "CommitsAll");
            }
            System.out.println("thread ends");
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public ChangesOnModule calcChangeOnModulesBetweenCommits(Repository repository, RevCommit revCommit, RevCommit revCommitParent) throws IOException {
        ChangesOnModule changesOnModule = new ChangesOnModule();
        try(DiffFormatter diffFormatter = new DiffFormatter(System.out)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);
            diffFormatter.setDiffAlgorithm(new HistogramDiff());
            ObjectReader reader = repository.newObjectReader();
            //diffEntryを取得
            AbstractTreeIterator oldTreeIter = revCommitParent ==null ? new EmptyTreeIterator() :new CanonicalTreeParser(null, reader, revCommitParent.getTree());
            AbstractTreeIterator newTreeIter = new CanonicalTreeParser(null, reader, revCommit.getTree());
            //renameを検出。明示的。
            RenameDetector rd = new RenameDetector(repository);
            rd.setRenameScore(30);
            rd.addAll(diffFormatter.scan(oldTreeIter, newTreeIter));
            List<DiffEntry> diffEntries = rd.compute();
            for (DiffEntry diffEntry : diffEntries) {
                ChangeOnModule changeOnModule = new ChangeOnModule();
                changeOnModule.idCommit = revCommit.getName();
                changeOnModule.idCommitParent = revCommitParent == null ? "0000000000000000000000000000000000000000" : revCommitParent.getName();
                changeOnModule.date = revCommit.getCommitTime();
                changeOnModule.author = new Person(revCommit.getAuthorIdent().getName());
                changeOnModule.isMerge = revCommit.getParentCount() > 1;
                changeOnModule.type = diffEntry.getChangeType().toString();

                changeOnModule.pathOld = diffEntry.getOldPath();
                changeOnModule.pathNew = diffEntry.getNewPath();
                if (changeOnModule.type.equals("ADD") & changeOnModule.isMerge) changeOnModule.pathNewParent = diffEntry.getNewPath();
                else changeOnModule.pathNewParent = diffEntry.getOldPath();

                //コミット直前のソースコードを取得
                if (diffEntry.getOldId().name().equals("0000000000000000000000000000000000000000")) {
                    changeOnModule.sourceOld = null;
                } else {
                    ObjectLoader loader = repository.open(diffEntry.getOldId().toObjectId());
                    changeOnModule.sourceOld = new String(loader.getBytes());
                }
                //コミット直後のソースコードを取得
                if (diffEntry.getNewId().name().equals("0000000000000000000000000000000000000000")) {
                    changeOnModule.sourceNew = null;
                } else {
                    ObjectLoader loader = repository.open(diffEntry.getNewId().toObjectId());
                    changeOnModule.sourceNew = new String(loader.getBytes());
                }

                for (Edit changeOriginal : diffFormatter.toFileHeader(diffEntry).toEditList()) {
                    Diff diff = new Diff();
                    for (int i = changeOriginal.getBeginA(); i < changeOriginal.getEndA(); i++) {
                        diff.before.add(i);
                    }
                    for (int i = changeOriginal.getBeginB(); i < changeOriginal.getEndB(); i++) {
                        diff.after.add(i);
                    }
                    changeOnModule.diffs.add(diff);
                }
                changesOnModule.put(changeOnModule.idCommitParent, changeOnModule.idCommit, changeOnModule.pathOld, changeOnModule.pathNew, changeOnModule);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return changesOnModule;
    }
}
