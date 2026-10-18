package com.cloudres.spend;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.speech.*;
import android.widget.TextView;
import java.util.*;

/** A foreground, one-shot microphone session. No audio files, background capture or automatic retries. */
public final class SpeechInput {
    public static final String LOCAL="@ondevice", DEFAULT="@default";
    public static final class Engine {
        public final String id,label;
        Engine(String id,String label){this.id=id;this.label=label;}
    }
    public static List<Engine> engines(Context context){
        List<Engine> result=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(context))result.add(new Engine(LOCAL,"设备端识别（中文模型需可用）"));
        if(SpeechRecognizer.isRecognitionAvailable(context))result.add(new Engine(DEFAULT,"系统默认语音识别（可能联网）"));
        for(ResolveInfo r:context.getPackageManager().queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE),0)){
            ServiceInfo s=r.serviceInfo;if(s==null||!s.exported||!s.enabled)continue;
            String id=new ComponentName(s.packageName,s.name).flattenToString();
            result.add(new Engine(id,r.loadLabel(context.getPackageManager()).toString()+"（"+s.packageName+"）"));
        }
        return result;
    }
    public interface Callback {void result(String text);void failed(String reason);}
    private final Activity activity;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private AlertDialog dialog;
    private boolean active;
    private Callback callback;
    private final Runnable timeout=()->fail("识别服务没有响应（等待超过30秒）。请检查服务、中文模型或网络，也可使用键盘语音填单。");
    public SpeechInput(Activity activity){this.activity=activity;}
    public void start(String engine,Callback callback){
        cancel();if(activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){callback.failed(errorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS));return;}this.callback=callback;active=true;
        TextView status=new TextView(activity);int padding=(int)(24*activity.getResources().getDisplayMetrics().density);status.setPadding(padding,padding,padding,padding);status.setTextSize(17);status.setText("正在连接语音服务…");
        dialog=new AlertDialog.Builder(activity).setTitle("语音记账").setView(status).setNegativeButton("取消",(d,w)->cancel()).setPositiveButton("说完了",null).create();
        dialog.setOnCancelListener(d->cancel());dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(active&&recognizer!=null){status.setText("正在识别…");recognizer.stopListening();}}));dialog.show();
        try{
            if(LOCAL.equals(engine)){
                if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(activity))recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(activity);
                else {fail("设备端语音服务不可用，请选择其他识别服务。");return;}
            }else if(DEFAULT.equals(engine))recognizer=SpeechRecognizer.createSpeechRecognizer(activity);
            else {
                ComponentName component=ComponentName.unflattenFromString(engine);boolean found=false;
                if(component!=null)for(Engine candidate:engines(activity))if(engine.equals(candidate.id))found=true;
                if(!found){fail("所选语音服务已不可用，请重新选择。");return;}
                recognizer=SpeechRecognizer.createSpeechRecognizer(activity,component);
            }
            recognizer.setRecognitionListener(new RecognitionListener(){
                public void onReadyForSpeech(Bundle b){if(active)status.setText("请说一笔支出，例如：午饭25元，微信支付");}
                public void onBeginningOfSpeech(){if(active)status.setText("正在听…");}
                public void onRmsChanged(float rms){}
                public void onBufferReceived(byte[] buffer){}
                public void onEndOfSpeech(){if(active)status.setText("正在识别…");}
                public void onError(int error){fail(errorMessage(error));}
                public void onResults(Bundle b){if(!active)return;ArrayList<String> results=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(results==null||results.isEmpty()||results.get(0).trim().isEmpty()){fail("没有听清，请重新说一笔支出。");return;}String text=results.get(0);Callback cb=SpeechInput.this.callback;cancel();cb.result(text);}
                public void onPartialResults(Bundle b){}
                public void onEvent(int type,Bundle b){}
            });
            Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,LOCAL.equals(engine));
            handler.postDelayed(timeout,30000);recognizer.startListening(i);
        }catch(RuntimeException e){fail("语音服务未能启动，请更换识别服务，或使用键盘语音填单。");}
    }
    private void fail(String reason){if(!active)return;Callback cb=callback;cancel();if(cb!=null)cb.failed(reason);}
    public void cancel(){active=false;handler.removeCallbacksAndMessages(null);if(recognizer!=null){recognizer.cancel();recognizer.destroy();recognizer=null;}if(dialog!=null){dialog.dismiss();dialog=null;}callback=null;}
    public static String errorMessage(int code){switch(code){
        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:return "未获得麦克风权限。请在日账的应用权限中允许麦克风，并检查手机的麦克风总开关。";
        case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:return "当前语音服务不支持普通话（zh-CN），请选择支持中文的服务。";
        case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:return "当前语音服务缺少可用的中文模型，请在该服务中下载中文模型，或更换服务。";
        case SpeechRecognizer.ERROR_NETWORK:case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:return "语音服务网络不可用。请检查网络或选择已有中文模型的设备端识别。";
        case SpeechRecognizer.ERROR_NO_MATCH:case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:return "没有听清，请重新说一笔支出。";
        case SpeechRecognizer.ERROR_AUDIO:return "无法使用麦克风，请检查麦克风总开关、权限及是否被其他应用占用。";
        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:return "语音服务忙，请稍后重试。";
        case SpeechRecognizer.ERROR_CLIENT:return "无法连接所选识别服务，请在语音服务检测中更换服务。";
        default:return "语音识别失败（错误码 "+code+"）。可更换识别服务或使用键盘语音填单。";
    }}
}
