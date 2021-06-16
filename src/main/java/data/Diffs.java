package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Diffs implements List<Diff>{
    List<Diff> diffs;

    public Diffs(){
        this.diffs = new ArrayList<>();
    }

    public int calcNOAddedLines(){
        int NOAddedLines=0;
        for(Diff diff : diffs){
            NOAddedLines+= diff.after.size();
        }
        return NOAddedLines;
    }

    public int calcNODeletedLines(){
        int NODeletedLines=0;
        for(Diff diff : diffs){
            NODeletedLines+= diff.before.size();
        }
        return NODeletedLines;
    }


    @Override
    public int size() {
        return diffs.size();
    }

    @Override
    public boolean isEmpty() {
        return diffs.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return diffs.contains(o);
    }

    @Override
    public Iterator iterator() {
        return diffs.iterator();
    }

    @Override
    public Diff[] toArray() {
        return diffs.toArray(new Diff[0]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return diffs.toArray(a);
    }

    @Override
    public boolean add(Diff diff) {
        return diffs.add(diff);
    }

    @Override
    public boolean remove(Object o) {
        return diffs.remove(o);
    }

    @Override
    public boolean addAll(Collection c) {
        return diffs.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection c) {
        return diffs.addAll(index, c);
    }

    @Override
    public void clear() {
        diffs.clear();
    }

    @Override
    public Diff get(int index) {
        return diffs.get(index);
    }

    @Override
    public Diff set(int index, Diff element) {
        return diffs.set(index, element);
    }

    @Override
    public void add(int index, Diff element) {
        diffs.add(index, element);
    }

    @Override
    public Diff remove(int index) {
        return diffs.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return diffs.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return diffs.lastIndexOf(o);
    }

    @Override
    public ListIterator listIterator() {
        return diffs.listIterator();
    }

    @Override
    public ListIterator listIterator(int index) {
        return diffs.listIterator(index);
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        return diffs.subList(fromIndex, toIndex);
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
        return diffs.containsAll(c);
    }
}
