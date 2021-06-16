package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.*;

@Data
public class ChangeOnModule {
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
	public Diffs diffs;
	public String pathNewParent;
	@JsonIgnore
	public ChangesOnModule parentsModification;
	public List<String> parents;
	@JsonIgnore
	public ChangesOnModule childrenModification;
	public List<String> children;

	public ChangeOnModule() {
		this.idCommit= "";
		this.idCommitParent = "";
		this.date=0;
		this.author="";
		this.isMerge=false;
		this.pathOld= "";
		this.pathNew= "";
		this.sourceOld= "";
		this.sourceNew= "";
		this.diffs = new Diffs();
		this.pathNewParent = "";
		this.parentsModification = new ChangesOnModule();
		this.parents = new ArrayList<>();
		this.childrenModification = new ChangesOnModule();
		this.children = new ArrayList<>();
	}

	public void loadAncestors(ChangesOnModule changesOnModule){
		changesOnModule.put(this.idCommitParent, this.idCommit, this.pathOld, this.pathNew, this);
		for(ChangeOnModule changeOnModule : this.parentsModification.values()) {
			if(!changesOnModule.containsValue(changeOnModule)) changeOnModule.loadAncestors(changesOnModule);
		}
	}

	public int calcNOAddedLines(){
		return diffs.calcNOAddedLines();
	}

	public int calcNODeletedLines(){
		return diffs.calcNODeletedLines();
	}

}
