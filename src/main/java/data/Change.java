package data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Change {
    List<Integer> before;
    List<Integer> after;

    public Change(){
        before = new ArrayList<>();
        after = new ArrayList<>();
    }
}
