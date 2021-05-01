package misc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.apache.commons.collections4.keyvalue.MultiKey;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DeserializerModification extends KeyDeserializer{
    @Override
    public MultiKey<? extends String> deserializeKey(String key, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String stringKeys = key.substring(9);
        stringKeys = stringKeys.substring(0, stringKeys.length()-1);
        String[] keys = stringKeys.split(",");
        String idCommit=keys[0];
        String pathOld=keys[1];
        String pathNew=keys[2];
        return new MultiKey(idCommit, pathOld, pathNew);
    }
}
