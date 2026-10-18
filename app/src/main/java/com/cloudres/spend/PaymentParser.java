package com.cloudres.spend;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;

/** Conservative local parser. No network, OCR, or access to app databases. */
public final class PaymentParser {
    private PaymentParser() {}
    public static String source(String pkg) {
        if ("com.tencent.mm".equals(pkg)) return "微信";
        if ("com.eg.android.AlipayGphone".equals(pkg)) return "支付宝";
        return null;
    }
    public static final class Payment {
        public final long cents;
        public final String source, merchant;
        Payment(long c, String s, String m) { cents=c;source=s;merchant=m; }
    }
    private static final String NUM="([0-9]{1,8}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)";
    private static final Pattern MONEY=Pattern.compile("(?:[¥￥]\\s*"+NUM+"|"+NUM+"\\s*元)");
    private static final Pattern MERCHANT=Pattern.compile("(?:收款方|商户名称|商家名称|交易对方)[：:\\s]+([^\\n]{1,50})");
    public static Payment parse(String pkg, String title, String body, boolean screen) {
        String src=source(pkg); if(src==null||body==null||body.length()>20000) return null;
        String t=body.replace('\u00a0',' ').replace('￥','¥');
        if(t.matches("(?s).*(验证码|校验码|动态密码|退款|退回|撤销|交易关闭|支付失败|付款失败|支付中|待付款|等待付款|转账|红包|收款到账|收款成功|到账通知|收入|每日|今日支出|本月|月度|账单详情|交易时间|交易记录).*") ) return null;
        if(screen) {
            if(!Pattern.compile("(?m)^\\s*(支付成功|付款成功|交易成功)\\s*$").matcher(t).find())return null;
            if(!(t.contains("完成")||t.contains("支付方式")||t.contains("收款方")))return null;
        } else {
            if(title==null||!Arrays.asList("微信支付","微信支付助手","支付助手","支付宝","支付宝支付助手","交易提醒").contains(title.trim()))return null;
            if(!Pattern.compile("支付成功|付款成功|成功付款|成功支付|支出|消费").matcher(t).find())return null;
            if(t.contains("收款")&&!t.contains("收款方"))return null;
        }
        Set<Long> values=new HashSet<>(); Matcher m=MONEY.matcher(t);
        while(m.find()) { try { long c=new BigDecimal((m.group(1)!=null?m.group(1):m.group(2)).replace(",","")).movePointRight(2).longValueExact(); if(c>0) values.add(c); }catch(ArithmeticException|NumberFormatException ignored){} }
        // Some native success screens expose the currency and number as separate text nodes.
        if(values.isEmpty()&&screen){m=Pattern.compile("(?m)^\\s*¥?\\s*([0-9]{1,8}\\.[0-9]{2})\\s*$").matcher(t);while(m.find())try{values.add(new BigDecimal(m.group(1)).movePointRight(2).longValueExact());}catch(ArithmeticException ignored){}}
        if(values.size()!=1)return null;
        long cents=values.iterator().next(); if(cents<=0||cents>100000000L)return null;
        Matcher merchant=MERCHANT.matcher(t);String name=merchant.find()?merchant.group(1).trim():"";
        return new Payment(cents,src,name);
    }
}
