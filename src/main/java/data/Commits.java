package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.tongfei.progressbar.ProgressBar;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RenameDetector;
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

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Commits implements Map<String, Commit> {
    private final TreeMap<String, Commit> commits = new TreeMap<>();
    public void loadCommits(String pathRepository, String idCommitHead) throws IOException,  GitAPIException {
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(pathRepository + "/.git"))
                .build();
        final Git git = new Git(repository);

        AnyObjectId objectIdCommitHead = ObjectId.fromString(idCommitHead);
        List<RevCommit> commitsAll = StreamSupport
                .stream(git.log().add(objectIdCommitHead).call().spliterator(), false)
                .collect(Collectors.toList());
        Collections.reverse(commitsAll);//past2future

        //それぞれのコミットについて、変更内容を取得する。
        for (RevCommit revCommit : ProgressBar.wrap(commitsAll, "loadCommits")) {
            Commit commit = new Commit();
            commit.id = revCommit.getName();
            commit.date = revCommit.getCommitTime();
            commit.author = revCommit.getAuthorIdent().getName();
            commit.isMerge = revCommit.getParentCount() > 1;
            commit.parents.addAll(Arrays.stream(revCommit.getParents()).map(RevCommit::getName).collect(Collectors.toList()));
            commits.put(commit.id, commit);

            loadModifications(repository, revCommit);
        }
        for (String idCommit : commits.keySet()) {
            for (String idCommitParent : commits.get(idCommit).parents) {
                commits.get(idCommitParent).childs.add(idCommit);
            }
        }
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
            loadModification(repository, revCommit, diffEntry);
        }
    }

    public void loadModification(Repository repository, RevCommit revCommit, DiffEntry diffEntry) throws IOException {
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
        commits.get(modification.idCommit).modifications.put(modification.idCommit, modification.pathOld , modification.pathNew, modification);
    }

    public void save(String pathCommits){
        for(String id : ProgressBar.wrap(commits.keySet(), "saveCommits")) {
            try (FileOutputStream fos = new FileOutputStream(pathCommits+"/"+id+".json");
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)){
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(writer, commits.get(id));
            } catch (IOException e) {
                e.printStackTrace();
            }
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
}
