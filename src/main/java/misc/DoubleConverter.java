package misc;

public class DoubleConverter implements net.sf.jsefa.common.converter.SimpleTypeConverter {

 private static final DoubleConverter INSTANCE = new DoubleConverter();
 public static DoubleConverter create() {
 return INSTANCE;
 }
 private DoubleConverter() {
 }
 @Override
 public Object fromString(String s) {
 return Double.parseDouble(s);
 }
 @Override
 public String toString(Object d) {
 return d.toString();
 }

}
