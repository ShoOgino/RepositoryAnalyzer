package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;
import me.tongfei.progressbar.ProgressBar;
import misc.DeserializerModification;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static util.FileUtil.findFiles;
import static util.FileUtil.readFile;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Commits implements Map<String, Commit> {
    private final TreeMap<String, Commit> commits = new TreeMap<>();
    public void loadCommitsFromRepository(Repository repository, String pathCommits){
        List<RevCommit> commitsAll = new ArrayList<RevCommit>();
        Collection<Ref> allRefs = repository.getAllRefs().values();
        try (RevWalk revWalk = new RevWalk(repository)) {
            for(Ref ref : allRefs) {
                revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
            }
            for(RevCommit commit : revWalk){
                commitsAll.add(commit);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        for(String path: ProgressBar.wrap(paths, "loadCommitsFromFile")) {
            try {
                String strFile = readFile(path);
                ObjectMapper mapper = new ObjectMapper();
                SimpleModule simpleModule = new SimpleModule();
                simpleModule.addKeyDeserializer(MultiKey.class, new DeserializerModification());
                mapper.registerModule(simpleModule);
                Commit commit = mapper.readValue(strFile, new TypeReference<Commit>() {});
                commits.put(commit.id, commit);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
