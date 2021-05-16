package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;
import me.tongfei.progressbar.ProgressBar;
import misc.DeserializerModification;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static util.FileUtil.findFiles;
import static util.FileUtil.readFile;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Commits implements Map<String, Commit> {
    private final TreeMap<String, Commit> commits = new TreeMap<>();
    public void loadCommitsFromRepository(String pathRepository, String idCommitHead, String pathCommits) throws IOException,  GitAPIException {
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(pathRepository + "/.git"))
                .build();
        final Git git = new Git(repository);

        AnyObjectId objectIdCommitHead = ObjectId.fromString(idCommitHead);
        List<RevCommit> commitsAll = StreamSupport
                .stream(git.log().add(objectIdCommitHead).call().spliterator(), false)
                .collect(Collectors.toList());
        //Collections.reverse(commitsAll);//past2future
        Collections.shuffle(commitsAll);
        Runtime r = Runtime.getRuntime();
        int NOfCPU = r.availableProcessors();

        List<CommitsThread> commitsThreads = new LinkedList<>();
        List<List<RevCommit>> revcommitsSplitted = Lists.partition(commitsAll, commitsAll.size() / NOfCPU);
        for (int i = 0; i < NOfCPU; i++) {
            commitsThreads.add(new CommitsThread(repository, revcommitsSplitted.get(i), pathCommits));
            commitsThreads.get(i).start();
        }
        for (int i = 0; i < NOfCPU; i++) {
            try {
                commitsThreads.get(i).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

/*

        //それぞれのコミットについて、変更内容を取得する。
        for (RevCommit revCommit : ProgressBar.wrap(commitsAll, "loadCommitsFromRepository")) {
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
                //if(0<revCommit.getParentCount()) {
                //    RevCommit revCommitParent = revCommit.getParent(0);
                //    Modifications modifications = test(repository, revCommit, revCommitParent);
                //    commit.idParent2Modifications.put(revCommitParent.getName(), modifications);
                //}
                for(RevCommit revCommitParent: revCommit.getParents()) {
                    Modifications  modifications = test(repository, revCommit, revCommitParent);
                    commit.idParent2Modifications.put(revCommitParent.getName(), modifications);
                }
//
//            for (int i = 0; i < revCommit.getParentCount(); i++) {
//                if (i == 0) {
//                    Modifications modifications = test(repository, revCommit, revCommit.getParent(i));
//                    commit.idParent2Modifications.put(revCommit.getParent(i).getName(), modifications);
//                } else {
//                    Modifications modifications = new Modifications();
//                    commit.idParent2Modifications.put(revCommit.getParent(i).getName(), modifications);
//                }
//            }
//
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
            //commit.save(pathCommits+"/"+commit.id+".json");
            commits.put(commit.id, commit);
        }
   */
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

    public void loadModifications(Repository repository, RevCommit revCommit) throws IOException {
        DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(repository);
        diffFormatter.setDetectRenames(true);
        diffFormatter.setDiffAlgorithm(new HistogramDiff());
        ObjectReader reader = repository.newObjectReader();
        //diffEntryを取得
        List<DiffEntry> diffEntries;
        AbstractTreeIterator oldTreeIter = 0 < revCommit.getParentCount() ? new CanonicalTreeParser(null, reader, revCommit.getParent(0).getTree()) : new EmptyTreeIterator();
        AbstractTreeIterator newTreeIter = new CanonicalTreeParser(null, reader, revCommit.getTree());
        diffEntries = diffFormatter.scan(oldTreeIter, newTreeIter);
        //renameを検出。明示的。
        RenameDetector rd = new RenameDetector(repository);
        rd.setRenameScore(30);
        rd.addAll(diffEntries);
        diffEntries = rd.compute();

        for (DiffEntry diffEntry : diffEntries) {
            loadModification(repository, revCommit, diffEntry, diffFormatter);
        }
    }

    public void loadModification(Repository repository, RevCommit revCommit, DiffEntry diffEntry, DiffFormatter diffFormatter) throws IOException {
        Modification modification = new Modification();
        modification.idCommit = revCommit.getName();
        modification.date = revCommit.getCommitTime();
        modification.author = revCommit.getAuthorIdent().getName();
        modification.isMerge = revCommit.getParentCount() > 1;
        modification.type=diffEntry.getChangeType().toString();
        modification.pathOld=diffEntry.getOldPath();
        modification.pathNew=diffEntry.getNewPath();
        //コミット直前のソースコードを取得
        if(diffEntry.getOldId().name().equals("0000000000000000000000000000000000000000")){
            modification.sourceOld = null;
        }else{
            ObjectLoader loader = repository.open(diffEntry.getOldId().toObjectId());
            modification.sourceOld = new String(loader.getBytes());
        }
        //コミット直後のソースコードを取得
        if(diffEntry.getNewId().name().equals("0000000000000000000000000000000000000000")){
            modification.sourceNew = null;
        }else{
            ObjectLoader loader = repository.open(diffEntry.getNewId().toObjectId());
            modification.sourceNew = new String(loader.getBytes());
        }

        for(Edit changeOriginal : diffFormatter.toFileHeader(diffEntry).toEditList()){
            Change change = new Change();
            for(int i = changeOriginal.getBeginA(); i< changeOriginal.getEndA(); i++){
                change.before.add(i);
            }
            for(int i = changeOriginal.getBeginB(); i< changeOriginal.getEndB(); i++){
                change.after.add(i);
            }
            modification.changes.add(change);
        }
        //commits.get(modification.idCommit).modifications.put(modification.idCommit, modification.pathOld , modification.pathNew, modification);
    }

    public void saveToFile(String pathCommits){
        File dir = new File(pathCommits);
        dir.mkdirs();
        for(Entry<String, Commit> entry : ProgressBar.wrap(commits.entrySet(), "saveCommits")) {
            entry.getValue().save(pathCommits+"/"+entry.getKey()+".json");
        }
    }

    @Override
    public int size() {
        return commits.size();
    }

    @Override
    public boolean isEmpty() {
        return commits.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return commits.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return commits.containsValue(value);
    }

    @Override
    public Commit get(Object key) {
        return commits.get(key);
    }

    @Override
    public Commit put(String key, Commit value) {
        return commits.put(key, value);
    }

    @Override
    public Commit remove(Object key) {
        return commits.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Commit> m) {
        commits.putAll(m);
    }

    @Override
    public void clear() {
        commits.clear();
    }

    @Override
    public Set<String> keySet() {
        return commits.keySet();
    }

    @Override
    public Collection<Commit> values() {
        return commits.values();
    }

    @Override
    public Set<Entry<String, Commit>> entrySet() {
        return commits.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return commits.equals(o);
    }

    @Override
    public int hashCode() {
        return commits.hashCode();
    }

    public void loadCommitsFromFile(String pathCommits) {
        List<String> paths = findFiles(pathCommits, "json");
        int count=0;
        for(String path: ProgressBar.wrap(paths, "loadCommitsFromFile")) {
            try {
                String strFile = readFile(path);
                ObjectMapper mapper = new ObjectMapper();
                SimpleModule simpleModule = new SimpleModule();
                simpleModule.addKeyDeserializer(MultiKey.class, new DeserializerModification());
                mapper.registerModule(simpleModule);
                Commit commit = mapper.readValue(strFile, new TypeReference<Commit>() {});
                commits.put(commit.id, commit);
                count++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
