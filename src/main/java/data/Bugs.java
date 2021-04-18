package data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

import static util.FileUtil.readFile;

public class Bugs {
    private HashMap<String, HashMap<String, String[]>> bugs = new HashMap<>();
    public HashMap<String, String[]> get(String path){
        return bugs.get(path);
    }
    public void loadBugs(String pathBugs) {
        try {
            String strBugs = readFile(pathBugs);
            ObjectMapper mapper = new ObjectMapper();
            bugs = mapper.readValue(strBugs, new TypeReference<HashMap<String, HashMap<String, String[]>>>() {
            });
        } catch (
                JsonParseException e) {
            e.printStackTrace();
        } catch (
                JsonMappingException e) {
            e.printStackTrace();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
}