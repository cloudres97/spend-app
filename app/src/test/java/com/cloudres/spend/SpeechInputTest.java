package com.cloudres.spend;
import android.speech.SpeechRecognizer;
import org.junit.Test;
import static org.junit.Assert.*;
public class SpeechInputTest {
    @Test public void distinguishesPermissionAndAudioFailures(){assertTrue(SpeechInput.errorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS).contains("权限"));assertTrue(SpeechInput.errorMessage(SpeechRecognizer.ERROR_AUDIO).contains("占用"));}
    @Test public void distinguishesMissingLanguageAndMissingModel(){assertTrue(SpeechInput.errorMessage(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED).contains("不支持普通话"));assertTrue(SpeechInput.errorMessage(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE).contains("中文模型"));}
    @Test public void explainsNetworkAndServiceFailures(){assertTrue(SpeechInput.errorMessage(SpeechRecognizer.ERROR_NETWORK).contains("网络"));assertTrue(SpeechInput.errorMessage(SpeechRecognizer.ERROR_CLIENT).contains("更换服务"));assertTrue(SpeechInput.errorMessage(999).contains("999"));}
}
