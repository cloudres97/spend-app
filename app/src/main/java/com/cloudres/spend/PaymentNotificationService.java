package com.cloudres.spend;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.*;

public class PaymentNotificationService extends NotificationListenerService {
    @Override public void onListenerConnected(){getSharedPreferences("spend",0).edit().putBoolean("notification_connected",true).apply();}
    @Override public void onListenerDisconnected(){getSharedPreferences("spend",0).edit().putBoolean("notification_connected",false).apply();}
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null||PaymentParser.source(sbn.getPackageName())==null||!getSharedPreferences("spend",0).getBoolean("enable_notification",false))return;
        Notification n=sbn.getNotification();if(n==null||(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)return;
        long now=System.currentTimeMillis();if(sbn.getPostTime()<getSharedPreferences("spend",0).getLong("enabled_at_notification",now)||Math.abs(now-sbn.getPostTime())>120000)return;
        Bundle e=n.extras; if(e==null)return;
        CharSequence title=e.getCharSequence(Notification.EXTRA_TITLE);StringBuilder text=new StringBuilder();
        for(String key:new String[]{Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT}){CharSequence value=e.getCharSequence(key);if(value!=null&&text.length()<20000)text.append(value).append('\n');}
        PaymentParser.Payment p=PaymentParser.parse(sbn.getPackageName(),title==null?"":title.toString(),text.toString(),false);
        if(p==null){LedgerStore.status(this,"notification","未识别：非明确支出、金额不可见或格式未适配");return;}
        // Android may post the same notification multiple times; post time and payment fields identify the event.
        LedgerStore.capture(this,p,"notification",sbn.getKey()+":"+sbn.getPostTime()+":"+p.cents+":"+p.merchant,sbn.getPostTime());
    }
}
