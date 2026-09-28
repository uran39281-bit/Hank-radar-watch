package android.content;
import java.util.*;
public class SharedPreferences {
 public static boolean failWrites;private Map<String,Object> values=new HashMap<>();
 public int getInt(String k,int v){return values.containsKey(k)?(Integer)values.get(k):v;}
 public long getLong(String k,long v){return values.containsKey(k)?(Long)values.get(k):v;}
 public String getString(String k,String v){return values.containsKey(k)?(String)values.get(k):v;}
 public Editor edit(){return new Editor();}
 public class Editor{Map<String,Object> pending=new HashMap<>();public Editor putInt(String k,int v){pending.put(k,v);return this;}public Editor putLong(String k,long v){pending.put(k,v);return this;}public Editor putString(String k,String v){pending.put(k,v);return this;}public boolean commit(){if(failWrites)return false;values.putAll(pending);return true;}public void apply(){commit();}}
}
