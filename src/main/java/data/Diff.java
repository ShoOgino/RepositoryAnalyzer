package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Diff {
    List<Integer> before;
    List<Integer> after;

    public Diff(){
        before = new ArrayList<>();
        after = new ArrayList<>();
    }
}
