package data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Modification {
	public String idCommit;
	public int date;
	public String author;
	public boolean isMerge;
	public String type;
	public String pathNew;
	public String pathOld;
	public String sourceNew;
	public String sourceOld;
	public Changes changes;
	public Set<String> parents;
	public Set<String> children;

	public Modification() {
		this.idCommit= "";
		this.date=0;
		this.author="";
		this.isMerge=false;
		this.pathOld= "";
		this.pathNew= "";
		this.sourceOld= "";
		this.sourceNew= "";
		this.changes = new Changes();
		this.parents = new HashSet<>();
		this.children = new HashSet<>();
	}

	public int calcNOAddedLines(){
		return changes.calcNOAddedLines();
	}

	public int calcNODeletedLines(){
		return changes.calcNODeletedLines();
	}

}
