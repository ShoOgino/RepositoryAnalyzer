package data;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Commit{
	public String id;
	public int date;
	public String author;
	public boolean isMerge;
	public Map<String, Modifications> idParent2Modifications = new HashMap<>();
	public String idParentMaster;
	public Commit() {
	}

	public void save(String path){
		try (FileOutputStream fos = new FileOutputStream(path);
			 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			 BufferedWriter writer = new BufferedWriter(osw)){
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(writer, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
