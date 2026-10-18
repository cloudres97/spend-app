package com.cloudres.spend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Converts one Chinese expense sentence into an editable draft. Never writes a ledger entry. */
public final class VoiceExpenseParser {
    private VoiceExpenseParser(){}
    private static final String DIGITS="零〇一二两三四五六七八九";
    private static final String N="(?:[0-9]+(?:\\.[0-9]+)?|[零〇一二两三四五六七八九十百千万]+(?:点[零〇一二两三四五六七八九]+)?)";
    public static final class Draft {
        public Long cents;
        public String category="其他",source="未指定",date,title="语音支出",warning="";
        public boolean blocked;
    }
    public static Draft parse(String original,LocalDate today){
        Draft d=new Draft();d.date=today.toString();String t=original==null?"":original.trim();
        if(t.isEmpty()||t.length()>500){d.warning="没有识别到有效语句，请重试。";d.blocked=true;return d;}
        if(Pattern.compile("收入|收到了|收到.*(?:元|块)|退款|退了|退回|收款|转入|取消记账|不要记|别记").matcher(t).find()) {d.warning="这句话涉及收入、退款或取消；本次没有记账。";d.blocked=true;return d;}
        d.category=ExpenseCategories.guess(t);d.title=ExpenseCategories.title(t,d.category);
        boolean wx=t.contains("微信"),ali=t.contains("支付宝"),cash=t.contains("现金");
        if((wx?1:0)+(ali?1:0)+(cash?1:0)==1)d.source=wx?"微信":ali?"支付宝":"现金/其他";
        else if(wx||ali||cash)warn(d,"支付方式有多个，请选择。");
        boolean yesterday=t.contains("昨天"),before=t.contains("前天"),now=t.contains("今天");
        int dateHints=(yesterday?1:0)+(before?1:0)+(now?1:0);
        Matcher dm=Pattern.compile("(?:([0-9]{4}|[零〇一二三四五六七八九]{4})年)?([0-9]{1,2}|[一二三四五六七八九十]{1,3})月([0-9]{1,2}|[一二三四五六七八九十]{1,3})[日号]").matcher(t);
        if(dateHints>1){d.date="";warn(d,"日期有多个，请选择。");}
        else if(dm.find()){
            if(dateHints>0){d.date="";warn(d,"日期有多个，请选择。");}
            else try{d.date=LocalDate.of(dm.group(1)==null?today.getYear():number(dm.group(1)).intValueExact(),number(dm.group(2)).intValueExact(),number(dm.group(3)).intValueExact()).toString();if(dm.find()){d.date="";warn(d,"日期有多个，请选择。");}}catch(Exception e){d.date="";warn(d,"日期不正确，请选择。");}
        }else if(before)d.date=today.minusDays(2).toString();else if(yesterday)d.date=today.minusDays(1).toString();
        if(t.contains("上周")||t.contains("上个月")||t.contains("去年")){d.date="";warn(d,"这类日期暂不能自动识别，请选择日期。");}
        if(t.contains("明天")||t.contains("后天")){d.date="";warn(d,"未来支出请确认日期。");}
        // Avoid treating dates as amounts. Currency labels are preferred; bare numbers need an explicit spend verb.
        String amounts=t.replaceAll("(?<=\\d),(?=\\d{3}(?:\\D|$))","");
        List<Long> values=new ArrayList<>();List<int[]> spans=new ArrayList<>();boolean invalid=false;
        Pattern yuan=Pattern.compile("("+N+")(?:元|块)(?:钱)?(?:("+N+")(?:毛|角)(?:("+N+")分)?|("+N+")分|([0-9零〇一二两三四五六七八九]{1,2})(?![0-9零〇一二两三四五六七八九毛角分]))?");
        Matcher m=yuan.matcher(amounts);
        while(m.find()){
            spans.add(new int[]{m.start(),m.end()});
            try{
                if(m.start()>0&&"-负".indexOf(amounts.charAt(m.start()-1))>=0)throw new IllegalArgumentException();
                BigDecimal v=number(m.group(1));
                if(m.group(2)!=null){if(v.stripTrailingZeros().scale()>0)throw new IllegalArgumentException();BigDecimal mao=number(m.group(2));if(mao.compareTo(BigDecimal.TEN)>=0)throw new IllegalArgumentException();v=v.add(mao.movePointLeft(1));if(m.group(3)!=null){BigDecimal fen=number(m.group(3));if(fen.compareTo(BigDecimal.TEN)>=0)throw new IllegalArgumentException();v=v.add(fen.movePointLeft(2));}}
                else if(m.group(4)!=null){BigDecimal fen=number(m.group(4));if(v.stripTrailingZeros().scale()>0||fen.compareTo(BigDecimal.TEN)>=0)throw new IllegalArgumentException();v=v.add(fen.movePointLeft(2));}
                else if(m.group(5)!=null){if(v.stripTrailingZeros().scale()>0)throw new IllegalArgumentException();String tail=digits(m.group(5));v=v.add(new BigDecimal("0."+tail));}
                values.add(validCents(v));
            }catch(Exception e){invalid=true;}
        }
        Matcher fractions=Pattern.compile("("+N+")(?:毛|角)(?:("+N+")分(?!钟))?|("+N+")分(?!钟)").matcher(amounts);
        while(fractions.find()){
            boolean used=false;for(int[] span:spans)if(fractions.start()>=span[0]&&fractions.start()<span[1])used=true;if(used)continue;
            spans.add(new int[]{fractions.start(),fractions.end()});
            try{if(fractions.start()>0&&"-负".indexOf(amounts.charAt(fractions.start()-1))>=0)throw new IllegalArgumentException();BigDecimal v=fractions.group(3)!=null?number(fractions.group(3)).movePointLeft(2):number(fractions.group(1)).movePointLeft(1).add(fractions.group(2)==null?BigDecimal.ZERO:number(fractions.group(2)).movePointLeft(2));values.add(validCents(v));}catch(Exception e){invalid=true;}
        }
        if(values.isEmpty()&&!invalid){Matcher verb=Pattern.compile("(?:花了|花费了?|支出了?|消费了?|金额[是为]?)[\\s：:]*([0-9]+(?:\\.[0-9]+)?|[零〇一二两三四五六七八九十百千万]+(?:点[零〇一二两三四五六七八九]+)?)(?![0-9零〇一二两三四五六七八九十百千万点]|分钟|小时|次|个|件|公里|站|岁|号|月|日)").matcher(amounts);while(verb.find())try{values.add(validCents(number(verb.group(1))));}catch(Exception e){invalid=true;}}
        if(invalid){d.cents=null;warn(d,"金额格式不正确，请核对。");}
        else if(values.size()==1)d.cents=values.get(0);
        else if(values.size()>1)warn(d,"检测到多个金额；每次请只说一笔支出，或在下方选择金额。");
        else warn(d,"没有识别到金额，请说出“25元”等明确金额。");
        return d;
    }
    private static void warn(Draft d,String s){d.warning+=(d.warning.isEmpty()?"":"\n")+s;}
    private static long validCents(BigDecimal n){long c=n.movePointRight(2).longValueExact();if(c<=0||c>100000000L)throw new IllegalArgumentException();return c;}
    private static String digits(String s){StringBuilder b=new StringBuilder();for(char c:s.toCharArray()){if(c>='0'&&c<='9')b.append(c);else b.append(digit(c));}return b.toString();}
    private static int digit(char c){if(c=='〇'||c=='零')return 0;if(c=='两')return 2;int p="一二三四五六七八九".indexOf(c);if(p>=0)return p+1;throw new IllegalArgumentException();}
    private static BigDecimal number(String s){
        if(s.matches("[0-9]+(?:\\.[0-9]+)?"))return new BigDecimal(s);
        if(s.matches(".*[百千万][一二两三四五六七八九]$"))throw new IllegalArgumentException();
        int point=s.indexOf('点');if(point>=0)return number(s.substring(0,point)).add(new BigDecimal("0."+digits(s.substring(point+1))));
        if(!s.matches(".*[十百千万].*"))return new BigDecimal(digits(s));
        long total=0,section=0,num=0;boolean pending=false;
        for(char c:s.toCharArray()){
            int unit=c=='十'?10:c=='百'?100:c=='千'?1000:c=='万'?10000:0;
            if(unit==0){num=digit(c);pending=true;}
            else if(unit==10000){section+=num;total+=(section==0?1:section)*unit;section=0;num=0;pending=false;}
            else {section+=(pending?num:1)*unit;num=0;pending=false;}
        }
        return BigDecimal.valueOf(total+section+num);
    }
}
