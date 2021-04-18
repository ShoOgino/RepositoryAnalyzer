package data;

import java.util.ArrayList;

public class Commit{
	public String id;
	public int date;
	public String author;
	public boolean isMerge;
	public ArrayList<String> parents = new ArrayList<>();
	public ArrayList<String> childs = new ArrayList<>();
	public Modifications modifications = new Modifications();

	public Commit() {
	}
}
