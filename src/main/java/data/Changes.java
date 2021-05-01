package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Changes implements List<Change>{
    List<Change> changes;

    public Changes(){
        this.changes = new ArrayList<>();
    }

    public int calcNOAddedLines(){
        int NOAddedLines=0;
        for(Change change: changes){
            NOAddedLines+=change.after.size();
        }
        return NOAddedLines;
    }

    public int calcNODeletedLines(){
        int NODeletedLines=0;
        for(Change change: changes){
            NODeletedLines+=change.before.size();
        }
        return NODeletedLines;
    }


    @Override
    public int size() {
        return changes.size();
    }

    @Override
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return changes.contains(o);
    }

    @Override
    public Iterator iterator() {
        return changes.iterator();
    }

    @Override
    public Change[] toArray() {
        return changes.toArray(new Change[0]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return changes.toArray(a);
    }

    @Override
    public boolean add(Change change) {
        return changes.add(change);
    }

    @Override
    public boolean remove(Object o) {
        return changes.remove(o);
    }

    @Override
    public boolean addAll(Collection c) {
        return changes.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection c) {
        return changes.addAll(index, c);
    }

    @Override
    public void clear() {
        changes.clear();
    }

    @Override
    public Change get(int index) {
        return changes.get(index);
    }

    @Override
    public Change set(int index, Change element) {
        return changes.set(index, element);
    }

    @Override
    public void add(int index, Change element) {
        changes.add(index, element);
    }

    @Override
    public Change remove(int index) {
        return changes.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return changes.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return changes.lastIndexOf(o);
    }

    @Override
    public ListIterator listIterator() {
        return changes.listIterator();
    }

    @Override
    public ListIterator listIterator(int index) {
        return changes.listIterator(index);
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        return changes.subList(fromIndex, toIndex);
    }

    @Override
    public boolean retainAll(Collection c) {
        return retainAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
        return removeAll(c);
    }

    @Override
    public boolean containsAll(Collection c) {
        return changes.containsAll(c);
    }
}
