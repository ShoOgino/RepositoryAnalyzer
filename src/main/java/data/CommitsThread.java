package data;

import lombok.SneakyThrows;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.collections4.keyvalue.MultiKey;
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
            commit.idParentMaster = revCommit.getParentCount() == 0 ? "0000000000000000000000000000000000000000" : revCommit.getParent(0).getName();
            if(revCommit.getParentCount()==0){
                Modifications modifications = calcModificationsBetweenCommits(repository, revCommit, null);
                commit.idParent2Modifications.put("0000000000000000000000000000000000000000", modifications);
            }else {
                for (RevCommit revCommitParent : revCommit.getParents()) {
                    Modifications modifications = calcModificationsBetweenCommits(repository, revCommit, revCommitParent);
                    commit.idParent2Modifications.put(revCommitParent.getName(), modifications);
                }
            }
            for (RevCommit revCommitParentSub : revCommit.getParents()) {
                if(Objects.equals(revCommitParentSub.getName(), commit.idParentMaster))continue;
                Modifications modificationsSub = commit.idParent2Modifications.get(revCommitParentSub.getName());
                Modifications modificationsMain = commit.idParent2Modifications.get(commit.idParentMaster);
                List<String> pathsMain = modificationsMain.values().stream().map(a->a.pathNew).collect(Collectors.toList());
                modificationsSub.entrySet().removeIf(entry -> !pathsMain.contains(entry.getKey().getKey(3)));
                for (Modification modification : modificationsMain.values()) {
                    if(0 == modificationsSub.findFromPathNew(modification.pathNew).size()){
                        if (!modification.type.equals("DELETE")) {
                            Modification m = new Modification();
                            m.idCommit = revCommit.getName();
                            m.idCommitParent = revCommitParentSub.getName();
                            m.date = revCommit.getCommitTime();
                            m.author = revCommit.getAuthorIdent().getName();
                            m.isMerge = revCommit.getParentCount() > 1;
                            m.type = "UNCHANGE";
                            m.pathOld = modification.pathNew;
                            m.pathNew = modification.pathNew;
                            m.sourceOld = modification.sourceNew;
                            m.sourceNew = modification.sourceNew;
                            m.pathNewParent = modification.pathNew;
                            modificationsSub.put(m.idCommitParent, m.idCommit, m.pathOld, m.pathNew, m);
                        }
                    }
                }
            }
        commit.save(pathCommits+"/"+commit.id+".json");
        }
        System.out.println("thread ends");
    }

    public Modifications calcModificationsBetweenCommits(Repository repository, RevCommit revCommit, RevCommit revCommitParent) throws IOException {
        Modifications modifications = new Modifications();
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
                Modification modification = new Modification();
                modification.idCommit = revCommit.getName();
                modification.idCommitParent = revCommitParent == null ? "0000000000000000000000000000000000000000" : revCommitParent.getName();
                modification.date = revCommit.getCommitTime();
                modification.author = revCommit.getAuthorIdent().getName();
                modification.isMerge = revCommit.getParentCount() > 1;
                modification.type = diffEntry.getChangeType().toString();

                modification.pathOld = diffEntry.getOldPath();
                modification.pathNew = diffEntry.getNewPath();
                if (modification.type.equals("ADD") & modification.isMerge) modification.pathNewParent = diffEntry.getNewPath();
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
