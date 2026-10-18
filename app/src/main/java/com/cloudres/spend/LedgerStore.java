package com.cloudres.spend;

import android.content.*;
import org.json.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public final class LedgerStore {
    private LedgerStore(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("spend",0);}
    public static synchronized JSONArray read(Context c){try{return new JSONArray(prefs(c).getString("entries","[]"));}catch(JSONException e){return new JSONArray();}}
    public static synchronized void append(Context c,JSONObject o){JSONArray a=read(c);a.put(o);prefs(c).edit().putString("entries",a.toString()).apply();}
    public static synchronized void remove(Context c,JSONObject target){JSONArray a=read(c),b=new JSONArray();boolean removed=false;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(!removed&&o!=null&&o.toString().equals(target.toString()))removed=true;else b.put(o);}prefs(c).edit().putString("entries",b.toString()).apply();}
    public static synchronized boolean update(Context c,JSONObject target,JSONObject replacement){JSONArray a=read(c);for(int i=0;i<a.length();i++){JSONObject old=a.optJSONObject(i);if(old!=null&&old.toString().equals(target.toString()))try{a.put(i,replacement);prefs(c).edit().putString("entries",a.toString()).apply();return true;}catch(JSONException e){return false;}}return false;}
    public static String hash(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder r=new StringBuilder();for(byte v:b)r.append(String.format(Locale.ROOT,"%02x",v&255));return r.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public static synchronized void status(Context c,String channel,String result){SharedPreferences p=prefs(c);p.edit().putLong("last_"+channel,System.currentTimeMillis()).putString("result_"+channel,result).putLong("events_"+channel,p.getLong("events_"+channel,0)+1).apply();}
    public static synchronized boolean capture(Context c,PaymentParser.Payment p,String channel,String eventId,long when){
        if(!prefs(c).getBoolean("enable_"+channel,false))return false;
        String id=hash(channel+":"+eventId);JSONArray a=read(c);
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o==null)continue;
            if(id.equals(o.optString("eventId"))){status(c,channel,"已忽略重复事件");return false;}
            // Never merge same-amount payments solely on time. Cross-channel merge also requires an identical merchant.
            if(p.source.equals(o.optString("source"))&&p.cents==o.optLong("cents",-1)&&!channel.equals(o.optString("channel"))&&Math.abs(when-o.optLong("capturedAt",0))<=15000){
                String previous=o.optString("merchant");
                if(!p.merchant.isEmpty()&&p.merchant.equals(previous)){status(c,channel,"已合并通知与页面记录");return false;}
                if(p.merchant.isEmpty()||previous.isEmpty()){status(c,channel,"跨渠道同额候选：暂未计入第二笔，避免重复");return false;}
            }
        }
        try{JSONObject o=new JSONObject();o.put("eventId",id);o.put("amount",p.cents/100.0);o.put("cents",p.cents);o.put("source",p.source);o.put("title",p.merchant.isEmpty()?p.source+"支出":p.merchant);o.put("merchant",p.merchant);o.put("category",ExpenseCategories.guess(p.merchant));o.put("date",new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(new Date(when)));o.put("capturedAt",when);o.put("channel",channel);a.put(o);prefs(c).edit().putString("entries",a.toString()).putLong("auto_count",prefs(c).getLong("auto_count",0)+1).apply();status(c,channel,"已自动记账");return true;}catch(JSONException e){status(c,channel,"记录失败");return false;}
    }
}
