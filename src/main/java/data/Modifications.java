package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import misc.DeserializerModification;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Modifications implements Map<MultiKey<? extends String>, Modification> {
    @JsonDeserialize(keyUsing = DeserializerModification.class)
    private final MultiKeyMap<String, Modification> modifications = new MultiKeyMap<>();

    @Override
    public int size() {
        return modifications.size();
    }

    @Override
    public boolean isEmpty() {
        return modifications.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return modifications.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return modifications.containsValue(value);
    }

    @Override
    public Modification get(Object key) {
        return modifications.get(key);
    }

    public List<Modification> get(String idCommit) {
        return modifications.values().stream().filter(a->a.idCommit.equals(idCommit)).collect(Collectors.toList());
    }

    public void put(String idCommit, String pathOld, String pathNew, Modification modification){
        modifications.put(new MultiKey(idCommit, pathOld, pathNew), modification);
    }

    @Override
    public Modification put(MultiKey<? extends String> key, Modification value) {
        return modifications.put(key, value);
    }

    @Override
    public Modification remove(Object key) {
        return modifications.remove(key);
    }

    @Override
    public void putAll(Map<? extends MultiKey<? extends String>, ? extends Modification> m) {
        modifications.putAll(m);
    }

    @Override
    public void clear(){
        modifications.clear();
    }

    @Override
    public Set<MultiKey<? extends String>> keySet() {
        return modifications.keySet();
    }

    @Override
    public Collection<Modification> values(){
        return modifications.values();
    }

    @Override
    public Set<Entry<MultiKey<? extends String>, Modification>> entrySet() {
        return modifications.entrySet();
    }
}
