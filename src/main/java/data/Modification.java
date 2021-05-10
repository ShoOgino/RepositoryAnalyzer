package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.collections4.keyvalue.MultiKey;

import java.util.*;

@Data
public class Modification {
	public String idCommit;
	public String idCommitParent;
	public int date;
	public String author;
	public boolean isMerge;
	public String type;
	public String pathNew;
	public String pathOld;
	public String sourceNew;
	public String sourceOld;
	@JsonIgnore
	public Changes changes;
	public String pathNewParent;
	@JsonIgnore
	public Modifications parentsModification;
	public List<String> parents;
	@JsonIgnore
	public Modifications childrenModification;
	public List<String> children;

	public Modification() {
		this.idCommit= "";
		this.idCommitParent = "";
		this.date=0;
		this.author="";
		this.isMerge=false;
		this.pathOld= "";
		this.pathNew= "";
		this.sourceOld= "";
		this.sourceNew= "";
		this.changes = new Changes();
		this.pathNewParent = "";
		this.parentsModification = new Modifications();
		this.parents = new ArrayList<>();
		this.childrenModification = new Modifications();
		this.children = new ArrayList<>();
	}

	public void loadAncestors(Modifications modifications){
		modifications.put(this.idCommitParent, this.idCommit, this.pathOld, this.pathNew, this);
		for(Modification modification: this.parentsModification.values()) {
			if(!modifications.containsValue(modification))modification.loadAncestors(modifications);
		}
	}

	public int calcNOAddedLines(){
		return changes.calcNOAddedLines();
	}

	public int calcNODeletedLines(){
		return changes.calcNODeletedLines();
	}

}
