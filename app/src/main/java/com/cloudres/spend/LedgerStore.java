package com.cloudres.spend;
import android.content.*;
import org.json.*;
public final class LedgerStore {
    private LedgerStore(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("spend",0);}
    public static synchronized JSONArray read(Context c){try{return new JSONArray(prefs(c).getString("entries","[]"));}catch(JSONException e){return new JSONArray();}}
    public static synchronized void append(Context c,JSONObject o){JSONArray a=read(c);a.put(o);prefs(c).edit().putString("entries",a.toString()).apply();}
    public static synchronized void remove(Context c,JSONObject target){JSONArray a=read(c),b=new JSONArray();boolean removed=false;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(!removed&&o!=null&&o.toString().equals(target.toString()))removed=true;else b.put(o);}prefs(c).edit().putString("entries",b.toString()).apply();}
    public static synchronized boolean update(Context c,JSONObject target,JSONObject replacement){JSONArray a=read(c);for(int i=0;i<a.length();i++){JSONObject old=a.optJSONObject(i);if(old!=null&&old.toString().equals(target.toString()))try{a.put(i,replacement);prefs(c).edit().putString("entries",a.toString()).apply();return true;}catch(JSONException e){return false;}}return false;}
}
