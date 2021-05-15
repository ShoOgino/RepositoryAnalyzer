package data;

import lombok.SneakyThrows;
import me.tongfei.progressbar.ProgressBar;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CommitsThread  extends Thread{
    Repository repository;
    List<RevCommit> revcommits;
    String pathCommits;

    public CommitsThread(Repository repository, List<RevCommit> revcommits, String pathCommits){
        this.repository = repository;
        this.revcommits = revcommits;
        this.pathCommits = pathCommits;
    }

    @SneakyThrows
    public void run() {
        System.out.println("thread started");
        //それぞれのコミットについて、変更内容を取得する。
        for (RevCommit revCommit : ProgressBar.wrap(revcommits, "loadCommitsFromRepository")) {
            Commit commit = new Commit();
            commit.id = revCommit.getName();
            commit.date = revCommit.getCommitTime();
            commit.author = revCommit.getAuthorIdent().getName();
            commit.isMerge = revCommit.getParentCount() > 1;
            commit.idParentMaster = revCommit.getParentCount()==0 ? "0000000000000000000000000000000000000000" : revCommit.getParent(0).getName();
            if(0==revCommit.getParentCount()){
                DiffFormatter diffFormatter = new DiffFormatter(System.out);
                diffFormatter.setRepository(repository);
                diffFormatter.setDetectRenames(true);
                diffFormatter.setDiffAlgorithm(new HistogramDiff());
                ObjectReader reader = repository.newObjectReader();
                //diffEntryを取得
                List<DiffEntry> diffEntries;
                AbstractTreeIterator oldTreeIter =new EmptyTreeIterator();
                AbstractTreeIterator newTreeIter = new CanonicalTreeParser(null, reader, revCommit.getTree());
                diffEntries = diffFormatter.scan(oldTreeIter, newTreeIter);
                //renameを検出。明示的。
                RenameDetector rd = new RenameDetector(repository);
                rd.setRenameScore(30);
                rd.addAll(diffEntries);
                diffEntries = rd.compute();

                Modifications modifications = new Modifications();
                for (DiffEntry diffEntry : diffEntries) {
                    Modification modification = new Modification();
                    modification.idCommit = revCommit.getName();
                    modification.idCommitParent = "0000000000000000000000000000000000000000";
                    modification.date = revCommit.getCommitTime();
                    modification.author = revCommit.getAuthorIdent().getName();
                    modification.isMerge = revCommit.getParentCount() > 1;
                    modification.type = diffEntry.getChangeType().toString();

                    modification.pathOld = diffEntry.getOldPath();
                    modification.pathNew = diffEntry.getNewPath();
                    modification.pathNewParent = diffEntry.getOldPath();
                    //コミット直前のソースコードを取得
                    if (diffEntry.getOldId().name().equals("0000000000000000000000000000000000000000")) {
                        modification.sourceOld = null;
                    } else {
                        ObjectLoader loader = repository.open(diffEntry.getOldId().toObjectId());
                        modification.sourceOld = new String(loader.getBytes());
                    }
                    //コミット直後のソースコードを取得
                    if (diffEntry.getNewId().name().equals("0000000000000000000000000000000000000000")) {
                        modification.sourceNew = null;
                    } else {
                        ObjectLoader loader = repository.open(diffEntry.getNewId().toObjectId());
                        modification.sourceNew = new String(loader.getBytes());
                    }

                    for (Edit changeOriginal : diffFormatter.toFileHeader(diffEntry).toEditList()) {
                        Change change = new Change();
                        for (int i = changeOriginal.getBeginA(); i < changeOriginal.getEndA(); i++) {
                            change.before.add(i);
                        }
                        for (int i = changeOriginal.getBeginB(); i < changeOriginal.getEndB(); i++) {
                            change.after.add(i);
                        }
                        modification.changes.add(change);
                    }
                    modifications.put(modification.idCommitParent, modification.idCommit, modification.pathOld, modification.pathNew, modification);
                }
                commit.idParent2Modifications.put("0000000000000000000000000000000000000000", modifications);
            }else {
                for(RevCommit revCommitParent: revCommit.getParents()) {
                    Modifications  modifications = test(repository, revCommit, revCommitParent);
                    commit.idParent2Modifications.put(revCommitParent.getName(), modifications);
                }
                for (RevCommit revCommitParentRenew : revCommit.getParents()) {
                    for (RevCommit revCommitParentRefer : revCommit.getParents()) {
                        if (Objects.equals(revCommitParentRenew.getName(), revCommitParentRefer.getName())) continue;
                        Modifications modificationsRenew = commit.idParent2Modifications.get(revCommitParentRenew.getName());
                        Modifications modificationsRefer = commit.idParent2Modifications.get(revCommitParentRefer.getName());
                        for (Modification modification : modificationsRefer.values()) {
                            if (0 == modificationsRenew.findFromPathNew(modification.pathNew).size() & !modification.type.equals("DELETE")) {
                                Modification m = new Modification();
                                m.idCommit = revCommit.getName();
                                m.idCommitParent = revCommitParentRenew.getName();
                                m.date = revCommit.getCommitTime();
                                m.author = revCommit.getAuthorIdent().getName();
                                m.isMerge = revCommit.getParentCount() > 1;
                                m.type = "UNCHANGE";
                                m.pathOld = modification.pathNew;
                                m.pathNew = modification.pathNew;
                                m.sourceOld = modification.sourceNew;
                                m.sourceNew = modification.sourceNew;
                                m.pathNewParent = modification.pathNew;
                                modificationsRenew.put(m.idCommitParent, m.idCommit, m.pathOld, m.pathNew, m);
                            }
                        }
                    }
                }
            }
        commit.save(pathCommits+"/"+commit.id+".json");
        //commits.put(commit.id, commit);
        }
        System.out.println("thread ends");
    }

    public Modifications test(Repository repository, RevCommit revCommit, RevCommit revCommitParent) throws IOException {
        Modifications modifications = new Modifications();
        try(DiffFormatter diffFormatter = new DiffFormatter(System.out)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);
            diffFormatter.setDiffAlgorithm(new HistogramDiff());
            ObjectReader reader = repository.newObjectReader();
            //diffEntryを取得
            AbstractTreeIterator oldTreeIter = new CanonicalTreeParser(null, reader, revCommitParent.getTree());
            AbstractTreeIterator newTreeIter = new CanonicalTreeParser(null, reader, revCommit.getTree());
            //renameを検出。明示的。
            RenameDetector rd = new RenameDetector(repository);
            rd.setRenameScore(30);
            rd.addAll(diffFormatter.scan(oldTreeIter, newTreeIter));
            List<DiffEntry> diffEntries = rd.compute();
            for (DiffEntry diffEntry : diffEntries) {
                Modification modification = new Modification();
                modification.idCommit = revCommit.getName();
                modification.idCommitParent = revCommitParent.getName();
                modification.date = revCommit.getCommitTime();
                modification.author = revCommit.getAuthorIdent().getName();
                modification.isMerge = revCommit.getParentCount() > 1;
                modification.type = diffEntry.getChangeType().toString();

                modification.pathOld = diffEntry.getOldPath();
                modification.pathNew = diffEntry.getNewPath();
                if (modification.type.equals("ADD") & modification.isMerge)
                    modification.pathNewParent = diffEntry.getNewPath();
                else modification.pathNewParent = diffEntry.getOldPath();

                //コミット直前のソースコードを取得
                if (diffEntry.getOldId().name().equals("0000000000000000000000000000000000000000")) {
                    modification.sourceOld = null;
                } else {
                    ObjectLoader loader = repository.open(diffEntry.getOldId().toObjectId());
                    modification.sourceOld = new String(loader.getBytes());
                }
                //コミット直後のソースコードを取得
                if (diffEntry.getNewId().name().equals("0000000000000000000000000000000000000000")) {
                    modification.sourceNew = null;
                } else {
                    ObjectLoader loader = repository.open(diffEntry.getNewId().toObjectId());
                    modification.sourceNew = new String(loader.getBytes());
                }

                for (Edit changeOriginal : diffFormatter.toFileHeader(diffEntry).toEditList()) {
                    Change change = new Change();
                    for (int i = changeOriginal.getBeginA(); i < changeOriginal.getEndA(); i++) {
                        change.before.add(i);
                    }
                    for (int i = changeOriginal.getBeginB(); i < changeOriginal.getEndB(); i++) {
                        change.after.add(i);
                    }
                    modification.changes.add(change);
                }
                modifications.put(modification.idCommitParent, modification.idCommit, modification.pathOld, modification.pathNew, modification);
            }
        }catch (IOException e) {
            // ハンドラに処理を移す
        }
        return modifications;
    }
}
