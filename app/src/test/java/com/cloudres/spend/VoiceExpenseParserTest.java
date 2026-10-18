package com.cloudres.spend;
import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;
public class VoiceExpenseParserTest {
    private final LocalDate today=LocalDate.of(2026,10,18);
    private VoiceExpenseParser.Draft parse(String text){return VoiceExpenseParser.parse(text,today);}
    @Test public void fillsCompleteArabicDraft(){VoiceExpenseParser.Draft d=parse("今天午饭花了25元，用微信支付");assertEquals(Long.valueOf(2500),d.cents);assertEquals("餐饮",d.category);assertEquals("午餐",d.title);assertEquals("2026-10-18",d.date);assertEquals("微信",d.source);assertEquals("",d.warning);}
    @Test public void understandsChineseAndYesterday(){VoiceExpenseParser.Draft d=parse("昨天午饭二十五块，支付宝支付");assertEquals(Long.valueOf(2500),d.cents);assertEquals("2026-10-17",d.date);assertEquals("支付宝",d.source);}
    @Test public void understandsColloquialFraction(){assertEquals(Long.valueOf(2850),parse("午餐二十八块五").cents);assertEquals(Long.valueOf(2556),parse("午餐25块56").cents);}
    @Test public void understandsYuanJiaoFen(){assertEquals(Long.valueOf(20356),parse("买药两百零三元五毛六分").cents);assertEquals(Long.valueOf(2505),parse("午餐25元五分").cents);}
    @Test public void understandsChineseDecimalAndSmallAmounts(){assertEquals(Long.valueOf(2560),parse("午餐二十五点六元").cents);assertEquals(Long.valueOf(35),parse("支出三毛五分").cents);}
    @Test public void ignoresQuantitiesAndDuration(){assertEquals(Long.valueOf(600),parse("地铁坐了30分钟，花了6元").cents);assertNull(parse("地铁坐了3站").cents);assertNull(parse("走路花了30分钟").cents);}
    @Test public void refusesMultipleAmounts(){VoiceExpenseParser.Draft d=parse("午饭25元，地铁6元");assertNull(d.cents);assertTrue(d.warning.contains("多个金额"));assertNull(parse("早饭25元晚饭25元").cents);}
    @Test public void doesNotInventPaymentSource(){assertEquals("未指定",parse("地铁六元").source);assertEquals("未指定",parse("微信支付宝支付25元").source);assertEquals("现金/其他",parse("现金买菜三十元").source);}
    @Test public void handlesDatesAndYearBoundary(){assertEquals("2026-10-17",parse("10月17号打车30元").date);assertEquals("2026-10-17",parse("十月十七号打车30元").date);assertEquals("2025-10-17",parse("2025年10月17日打车30元").date);assertEquals("2025-12-31",VoiceExpenseParser.parse("昨天午饭25元",LocalDate.of(2026,1,1)).date);}
    @Test public void rejectsInvalidOrConflictingDates(){assertEquals("",parse("2月30号午饭25元").date);assertEquals("",parse("昨天10月17号午饭25元").date);assertEquals("",parse("昨天今天午饭25元").date);assertEquals("",parse("明天午饭25元").date);assertEquals("",parse("上周午饭25元").date);}
    @Test public void rejectsInvalidMoney(){for(String s:new String[]{"午饭0元","午饭负25元","午饭-25元","支出-5毛","午饭3.14159元","午饭一百二元","午饭99999999元"})assertNull(s,parse(s).cents);}
    @Test public void rejectsIncomeRefundAndCancel(){for(String s:new String[]{"收入25元","收到25元","退款25元","不要记午饭25元","取消记账"})assertTrue(s,parse(s).blocked);}
    @Test public void classifiesCategoriesAndKeepsUnknown(){assertEquals("交通",parse("打车28元").category);assertEquals("餐饮",parse("奶茶十五元").category);assertEquals("购物",parse("买苹果手机5000元").category);assertEquals("通讯",parse("话费50元").category);assertEquals("其他",parse("支出25元").category);}
    @Test public void parsesExplicitSpendVerb(){assertEquals(Long.valueOf(2500),parse("午饭花了25").cents);assertEquals(Long.valueOf(2560),parse("午饭花了二十五点六").cents);}
    @Test public void handlesThousandsAndUnknownInput(){assertEquals(Long.valueOf(205000),parse("购物两千零五十元").cents);assertEquals(Long.valueOf(123450),parse("房租1,234.50元").cents);assertTrue(parse("").blocked);assertTrue(parse("字".repeat(501)).blocked);}
}
