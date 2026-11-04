package com.cloudres.spend;

public final class ExpenseCategories {
    private ExpenseCategories(){}
    public static final String[] ALL={"餐饮","交通","购物","日用","娱乐","居住","医疗","学习","运动","通讯","旅行","其他"};
    public static boolean contains(String category){for(String c:ALL)if(c.equals(category))return true;return false;}
}
