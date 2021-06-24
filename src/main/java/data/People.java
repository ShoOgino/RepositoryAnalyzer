package data;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class People extends TreeMap<String, Person> {
    private final TreeMap<String, Person> people = new TreeMap<>();

    public void analyze(Commits commitsAll) {
        for(Commit commit: commitsAll.values()){
            if(!people.containsKey(commit.author.name)){
                people.put(commit.author.name, commit.author);
            }
        }
    }

    public void giveNumberToPerson(){
        int index = 0;
        for (Person person: people.values()) {
            person.num=index;
            index+=1;
            person.numOfPeopleAll = people.size();
        }
    }

    @Override
    public int size() {
        return people.size();
    }

    @Override
    public boolean isEmpty() {
        return people.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return people.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return people.containsValue(value);
    }

    @Override
    public Person get(Object key) {
        return people.get(key);
    }

    @Override
    public Person put(String key, Person value) {
        return people.put(key, value);
    }

    @Override
    public Person remove(Object key) {
        return people.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Person> m) {
        people.putAll(m);
    }

    @Override
    public void clear() {
        people.clear();
    }

    @Override
    public Set<String> keySet() {
        return people.keySet();
    }

    @Override
    public Collection<Person> values() {
        return people.values();
    }

    @Override
    public Set<Map.Entry<String, Person>> entrySet() {
        return people.entrySet();
    }

}
