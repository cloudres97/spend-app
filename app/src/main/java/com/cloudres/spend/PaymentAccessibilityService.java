package com.cloudres.spend;

import android.accessibilityservice.AccessibilityService;
import android.os.*;
import android.view.accessibility.*;
import java.util.*;

/** Read-only success-screen observer. Does not click, type, scroll, or take screenshots. */
public class PaymentAccessibilityService extends AccessibilityService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String lastSignature="",pendingPackage="";
    private final Runnable scan=this::inspect;
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event==null)return;
        if(!getSharedPreferences("spend",0).getBoolean("enable_screen",false)){lastSignature="";handler.removeCallbacks(scan);return;}
        String pkg=event.getPackageName()==null?"":event.getPackageName().toString();
        if(PaymentParser.source(pkg)==null)return;
        pendingPackage=pkg;handler.removeCallbacks(scan);handler.postDelayed(scan,350);
    }
    private void collect(AccessibilityNodeInfo node,StringBuilder out,int depth,int[] remaining){
        if(node==null||depth>25||remaining[0]--<=0||out.length()>16000)return;
        if(node.isVisibleToUser()&&!node.isPassword()&&!node.isEditable()){
            CharSequence t=node.getText();if(t!=null)out.append(t).append('\n');
        }
        for(int i=0;i<node.getChildCount()&&remaining[0]>0;i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){collect(child,out,depth+1,remaining);child.recycle();}}
    }
    private void inspect(){
        if(!getSharedPreferences("spend",0).getBoolean("enable_screen",false))return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){LedgerStore.status(this,"screen","页面内容不可读取");return;}
        try{
            String pkg=root.getPackageName()==null?"":root.getPackageName().toString();if(!pkg.equals(pendingPackage)||PaymentParser.source(pkg)==null)return;
            StringBuilder text=new StringBuilder();collect(root,text,0,new int[]{300});
            PaymentParser.Payment p=PaymentParser.parse(pkg,"",text.toString(),true);
            if(p==null){lastSignature="";LedgerStore.status(this,"screen","未识别：非支付成功页、内容受保护或格式未适配");return;}
            String signature=pkg+":"+p.cents+":"+p.merchant;
            if(signature.equals(lastSignature))return;
            lastSignature=signature;
            LedgerStore.capture(this,p,"screen",UUID.randomUUID().toString(),System.currentTimeMillis());
        }finally{root.recycle();}
    }
    @Override public void onInterrupt(){handler.removeCallbacks(scan);}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
