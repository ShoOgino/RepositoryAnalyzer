package data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

import static util.FileUtil.readFile;

public class Bugs implements Map<String, Bug>{
    private Map<String, Bug> bugs = new HashMap<>();
    public void loadBugsFromFile(String pathBugs) {
        try {
            String strBugs = readFile(pathBugs);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, Map<String, List<String>>>> temp = mapper.readValue(strBugs, new TypeReference<Map<String, Map<String, Map<String, List<String>>>>>() {});
            for(String path: temp.keySet()){
                for(String idReport: temp.get(path).keySet()){
                    for(String idCommitFix: temp.get(path).get(idReport).keySet()){
                        BugAtomic bugAtomicTemp = new BugAtomic(path, idCommitFix, temp.get(path).get(idReport).get(idCommitFix));
                        if(bugs.keySet().contains(idReport)){
                            bugs.get(idReport).bugAtomics.add(bugAtomicTemp);
                        }else{
                            Bug bug = new Bug();
                            bug.id=idReport;
                            bug.bugAtomics.add(bugAtomicTemp);
                            bugs.put(idReport, bug);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int size() {
        return bugs.size();
    }

    @Override
    public boolean isEmpty() {
        return bugs.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return bugs.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return bugs.containsValue(value);
    }

    @Override
    public Bug get(Object key) {
        return bugs.get(key);
    }


    public List<BugAtomic> identifyAtomicBugs(String path){
        List<BugAtomic> bugAtomicsIdentified =new ArrayList<>();
        for(Bug bug: bugs.values()){
            for(BugAtomic bugAtomic: bug.bugAtomics){
                if(bugAtomic.path.equals(path)){
                    bugAtomicsIdentified.add(bugAtomic);
                }
            }
        }
        return bugAtomicsIdentified;
    }

    public List<Bug> identifyBug(String path) {
        List<Bug> bugsIdentified =new ArrayList<>();
        for(Bug bug: bugs.values()){
            for(BugAtomic bugAtomic: bug.bugAtomics){
                if(bugAtomic.path.equals(path)){
                    bugsIdentified.add(bug);
                    break;
                }
            }
        }
        return bugsIdentified;
    }

    @Override
    public Bug put(String key, Bug value) {
        return bugs.put(key, value);
    }

    @Override
    public Bug remove(Object key) {
        return bugs.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Bug> m) {
        bugs.putAll(m);
    }

    @Override
    public void clear() {
        bugs.clear();
    }

    @Override
    public Set<String> keySet() {
        return bugs.keySet();
    }

    @Override
    public Collection<Bug> values() {
        return bugs.values();
    }

    @Override
    public Set<Entry<String, Bug>> entrySet() {
        return bugs.entrySet();
    }
}