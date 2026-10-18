package com.cloudres.spend;
import org.junit.Test;
import static org.junit.Assert.*;
public class PaymentParserTest {
    private static final String WX="com.tencent.mm", ALI="com.eg.android.AlipayGphone";
    @Test public void capturesExplicitDebit(){assertEquals(1850,PaymentParser.parse(WX,"微信支付","支付成功，支出18.50元",false).cents);}
    @Test public void capturesNativeSuccessScreen(){assertEquals(1299,PaymentParser.parse(ALI,"","支付成功\n¥12.99\n完成",true).cents);}
    @Test public void supportsMerchantAndThousands(){PaymentParser.Payment p=PaymentParser.parse(ALI,"支付宝","付款成功 ¥1,234.50\n收款方：测试商店",false);assertNotNull(p);assertEquals(123450,p.cents);assertEquals("测试商店",p.merchant);}
    @Test public void repeatedAmountIsNotAmbiguous(){assertEquals(100,PaymentParser.parse(WX,"微信支付","付款成功1.00元\n实付¥1.00",false).cents);}
    @Test public void skipsIncomeRefundsTransfersOtpAndSummaries(){for(String s:new String[]{"收款到账18.50元","退款成功18.50元","转账成功18.50元","红包支出18.50元","支付验证码123456 金额18.50元","今日支出18.50元","支付失败18.50元","待付款18.50元"})assertNull(s,PaymentParser.parse(WX,"微信支付",s,false));}
    @Test public void rejectsAmbiguousPrices(){assertNull(PaymentParser.parse(WX,"微信支付","付款成功，原价20.00元，实付18.50元",false));}
    @Test public void rejectsChatAndOtherPackages(){assertNull(PaymentParser.parse(WX,"张三","付款成功18.50元",false));assertNull(PaymentParser.parse("com.example.fake","微信支付","付款成功18.50元",false));}
    @Test public void rejectsHistoryAndNonSuccessScreens(){assertNull(PaymentParser.parse(WX,"","账单详情\n支付成功\n18.50元\n完成",true));assertNull(PaymentParser.parse(WX,"","午餐18.50元",true));}
    @Test public void rejectsHiddenAmountAndZero(){assertNull(PaymentParser.parse(WX,"微信支付","支付成功 ***元",false));assertNull(PaymentParser.parse(ALI,"支付宝","支付成功0.00元",false));}
    @Test public void rejectsExcessiveOrHugeText(){assertNull(PaymentParser.parse(ALI,"支付宝","付款成功99999999.99元",false));assertNull(PaymentParser.parse(WX,"微信支付","x".repeat(20001),false));}
}
