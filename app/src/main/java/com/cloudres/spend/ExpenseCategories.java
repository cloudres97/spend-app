package com.cloudres.spend;

public final class ExpenseCategories {
    private ExpenseCategories(){}
    public static final String[] ALL={"餐饮","交通","购物","日用","娱乐","居住","医疗","学习","运动","通讯","旅行","其他"};
    private static final String[][] RULES={
        {"通讯","话费","手机费","流量费","宽带"},
        {"购物","手机","电脑","耳机","衣服","鞋子","淘宝","京东","购物","数码"},
        {"医疗","医院","挂号","看病","药店","买药","医药","体检","医疗"},
        {"居住","房租","水费","电费","物业","燃气","房贷"},
        {"交通","地铁","公交","打车","出租车","滴滴","高铁","火车票","加油","停车","油费","交通"},
        {"旅行","酒店","机票","旅游","旅行","住宿"},
        {"运动","健身","游泳","球场","运动","瑜伽"},
        {"学习","学费","课程","培训","书店","买书","学习"},
        {"娱乐","电影","游戏","演唱会","会员","娱乐","KTV"},
        {"日用","纸巾","洗衣液","洗发水","牙膏","日用品","日用"},
        {"餐饮","午餐","午饭","早餐","早饭","晚餐","晚饭","吃饭","外卖","咖啡","奶茶","水果","超市买菜","买菜","食物","餐饮","肯德基","麦当劳","美团外卖","饿了么","面包","零食"}
    };
    public static String guess(String text){if(text==null)return "其他";for(String[] r:RULES)for(int i=1;i<r.length;i++)if(text.toLowerCase(java.util.Locale.ROOT).contains(r[i].toLowerCase(java.util.Locale.ROOT)))return r[0];return "其他";}
    public static String title(String text,String category){
        for(String[] pair:new String[][]{{"午饭","午餐"},{"午餐","午餐"},{"早餐","早餐"},{"早饭","早餐"},{"晚饭","晚餐"},{"晚餐","晚餐"},{"地铁","地铁"},{"公交","公交"},{"打车","打车"},{"咖啡","咖啡"},{"奶茶","奶茶"},{"买菜","买菜"},{"加油","加油"},{"停车","停车"}})if(text.contains(pair[0]))return pair[1];
        return category.equals("其他")?"语音支出":category+"支出";
    }
    public static boolean contains(String category){for(String c:ALL)if(c.equals(category))return true;return false;}
}
