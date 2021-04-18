package util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import data.Commit;
import data.Module;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {
    public static String readFile(final String path){
        String value=null;
        try {
            value = Files.lines(Paths.get(path), Charset.forName("UTF-8")).collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            return value;
        }
    }
    public static List<String> findFiles(String dirRoot, String ext, String extIgnore) {
        List<String> pathsFile = new ArrayList<String>();
        try {
            pathsFile.addAll(
                    Files.walk(Paths.get(dirRoot))
                            .map(Path::toString)
                            .filter(p -> p.endsWith(ext))
                            .filter(p -> !p.contains(extIgnore))
                            .map(p -> p.replace("\\", "/"))
                            .collect(Collectors.toList())
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathsFile;
    }

    public static List<String> findFiles(String dirRoot, String ext) {
        List<String> pathsFile = new ArrayList<String>();
        try {
            pathsFile.addAll(
                    Files.walk(Paths.get(dirRoot))
                            .map(Path::toString)
                            .filter(p -> p.endsWith(ext))
                            .map(p -> p.replace("\\", "/"))
                            .collect(Collectors.toList())
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathsFile;
    }


    public static List<String> findFiles(String[] dirsRoot, String ext, String extIgnore) {
        List<String> pathsFile = new ArrayList<String>();
        try {
            for(String dirRoot: dirsRoot) {
                pathsFile.addAll(
                        Files.walk(Paths.get(dirRoot))
                                .map(Path::toString)
                                .filter(p -> p.endsWith(ext))
                                .filter(p -> !p.contains(extIgnore))
                                .map(p -> p.replace("\\", "/"))
                                .collect(Collectors.toList())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathsFile;
    }

}
