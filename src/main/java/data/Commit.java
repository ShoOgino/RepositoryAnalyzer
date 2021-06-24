package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Commit {
	public String id = null;
	public Integer date = null;
	public Person author =null ;
	public boolean isMerge = false;
	public Map<String, ChangesOnModule> idParent2Modifications = new HashMap<>();
	public String idParentMaster = null;
	public Commit() {
	}

	public void save(String path, String option){
		try (FileOutputStream fos = new FileOutputStream(path);
			 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			 BufferedWriter writer = new BufferedWriter(osw)){
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			mapper.writeValue(writer, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
