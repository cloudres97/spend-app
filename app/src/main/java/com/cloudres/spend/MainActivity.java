package com.cloudres.spend;

import android.app.*;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import java.time.LocalDate;
import java.math.BigDecimal;
import android.provider.Settings;
import android.app.NotificationManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private final int ink=Color.rgb(34,43,35), muted=Color.rgb(112,120,111), green=Color.rgb(64,91,67), pale=Color.rgb(235,240,231), bg=Color.rgb(247,248,245), line=Color.rgb(228,231,224);
    private LinearLayout root, body, nav;
    private JSONArray entries;
    private String page="home";
    private static final int PICK_CSV=41, VOICE_REQUEST=43;
    private AlertDialog editor;
    private EditText editorTitle,editorAmount,editorDate;
    private String[] editorCategory,editorSource;
    private JSONObject editorTarget,editorInitial;
    private String editorNote="";
    private final String[] categories=ExpenseCategories.ALL;
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); load(); render();if(b!=null){page=b.getString("page","home");render();String draft=b.getString("editor_draft");if(draft!=null)try{JSONObject initial=new JSONObject(draft);String original=b.getString("editor_target");showEditor(initial,original==null?null:new JSONObject(original),b.getString("editor_note",""));}catch(JSONException ignored){}}}
    private void load(){entries=LedgerStore.read(this);}
    private void save(){load();}
    @Override protected void onResume(){super.onResume();load();render();}
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(new Date());}
    private String month(){return new SimpleDateFormat("yyyy-MM",Locale.CHINA).format(new Date());}
    private double amount(JSONObject o){return o.optDouble("amount",0);}
    private double sum(String datePrefix,String source){double s=0;for(int i=0;i<entries.length();i++){JSONObject o=entries.optJSONObject(i);if(o==null)continue;if(o.optString("date").startsWith(datePrefix)&&(source.equals("全部")||source.equals(o.optString("source"))))s+=amount(o);}return s;}
    private String money(double v){return String.format(Locale.CHINA,"%,.2f",v);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setIncludeFontPadding(false);return t;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView button(String label,Runnable action,boolean primary){TextView t=text(label,14,primary?Color.WHITE:green,true);t.setGravity(Gravity.CENTER);pad(t,14,13,14,13);t.setBackground(shape(primary?green:pale,14));t.setOnClickListener(v->action.run());return t;}
    private void render(){root=col();root.setBackgroundColor(bg);setContentView(root);
        LinearLayout header=row();pad(header,22,18,22,14);TextView brand=text("日账",25,ink,true);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView date=text(new SimpleDateFormat("M月d日 E",Locale.CHINA).format(new Date()),13,muted,false);header.addView(date);root.addView(header);
        ScrollView scroll=new ScrollView(this);body=col();pad(body,20,4,20,22);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=row();nav.setBackgroundColor(Color.WHITE);nav.setElevation(dp(8));String[][] tabs={{"⌂","概览","home"},{"☷","账单","records"},{"＋","设置","settings"}};for(String[] tab:tabs){LinearLayout item=col();item.setGravity(Gravity.CENTER);pad(item,0,12,0,10);TextView ic=text(tab[0],19,page.equals(tab[2])?green:muted,true);ic.setGravity(Gravity.CENTER);TextView label=text(tab[1],11,page.equals(tab[2])?green:muted,page.equals(tab[2]));label.setGravity(Gravity.CENTER);item.addView(ic);item.addView(label);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);nav.addView(item,p);item.setOnClickListener(v->{page=tab[2];render();});}root.addView(nav);
        if(page.equals("home"))home();else if(page.equals("records"))records();else settings();}
    private void home(){body.addView(text("今日支出",14,muted,false));TextView total=text("¥ "+money(sum(today(),"全部")),36,ink,true);total.setPadding(0,dp(10),0,dp(6));body.addView(total);body.addView(text("只统计已录入的账单",12,muted,false));
        LinearLayout card=col();pad(card,16,16,16,16);card.setBackground(shape(pale,18));LinearLayout r=row();r.addView(text("本月累计",14,green,true),new LinearLayout.LayoutParams(0,-2,1));r.addView(text("预算 ¥ 3,500",12,muted,false));card.addView(r);TextView monthTotal=text("¥ "+money(sum(month(),"全部")),27,ink,true);monthTotal.setPadding(0,dp(12),0,dp(10));card.addView(monthTotal);View track=new View(this);track.setBackground(shape(Color.rgb(211,220,206),20));View fill=new View(this);double ratio=Math.min(1,sum(month(),"全部")/3500.0);fill.setBackground(shape(green,20));FrameLayout bar=new FrameLayout(this);bar.addView(track,new FrameLayout.LayoutParams(-1,dp(6)));FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.78*ratio),dp(6));bar.addView(fill,fp);card.addView(bar,lp(-1,6));TextView used=text(String.format(Locale.CHINA,"预算已使用 %.0f%%",ratio*100),12,muted,false);used.setPadding(0,dp(9),0,0);card.addView(used);LinearLayout.LayoutParams cp=lp(-1,-2);cp.topMargin=dp(20);body.addView(card,cp);
        section("支付渠道", "本月");LinearLayout channels=row();channels.addView(channel("微信支付",sum(month(),"微信"),"●",Color.rgb(7,173,95)),new LinearLayout.LayoutParams(0,dp(92),1));View gap=new View(this);channels.addView(gap,lp(10,1));channels.addView(channel("支付宝",sum(month(),"支付宝"),"●",Color.rgb(22,126,229)),new LinearLayout.LayoutParams(0,dp(92),1));body.addView(channels);
        section("最近支出","查看全部");int shown=0;for(int i=entries.length()-1;i>=0&&shown<4;i--){JSONObject o=entries.optJSONObject(i);if(o!=null){expenseRow(o);shown++;}}if(shown==0){TextView empty=text("还没有账单，先记下第一笔吧。",14,muted,false);pad(empty,0,18,0,18);body.addView(empty);}
        LinearLayout actions=row();actions.addView(button("＋ 记一笔",()->addDialog(),true),new LinearLayout.LayoutParams(0,-2,1));View gap2=new View(this);actions.addView(gap2,lp(10,1));actions.addView(button("语音记账",()->startVoice(),false),new LinearLayout.LayoutParams(0,-2,1));spaced(actions);spaced(button("导入微信 / 支付宝账单 CSV",()->pickCsv(),false));
    }
    private View channel(String title,double amount,String icon,int color){LinearLayout c=col();pad(c,13,14,8,12);c.setBackground(shape(Color.WHITE,14));c.addView(text(icon+"  "+title,12,color,true));TextView n=text("¥ "+money(amount),19,ink,true);n.setPadding(0,dp(12),0,0);c.addView(n);return c;}
    private TextView section(String title,String action){TextView t=text(title+"                                 "+action,15,ink,true);t.setPadding(0,dp(24),0,dp(12));body.addView(t);return t;}
    private void expenseRow(JSONObject o){LinearLayout r=row();pad(r,0,13,0,13);TextView icon=text("●",12,o.optString("source").equals("微信")?Color.rgb(7,173,95):Color.rgb(22,126,229),true);r.addView(icon,lp(22,-2));LinearLayout info=col();info.addView(text(o.optString("title","消费"),14,ink,true));TextView sub=text(o.optString("category","其他")+" · "+o.optString("date")+" · "+o.optString("source"),11,muted,false);sub.setPadding(0,dp(5),0,0);info.addView(sub);r.addView(info,new LinearLayout.LayoutParams(0,-2,1));r.addView(text("− ¥"+money(amount(o)),14,ink,true));View divider=new View(this);divider.setBackgroundColor(line);body.addView(r);body.addView(divider,lp(-1,1));r.setOnClickListener(v->showEditor(o,o,""));r.setOnLongClickListener(v->{new AlertDialog.Builder(this).setMessage("删除这笔支出？").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{remove(o);save();render();}).show();return true;});}
    private void remove(JSONObject target){LedgerStore.remove(this,target);load();}
    private void records(){
        body.addView(text("所有账单",24,ink,true));body.addView(text("点开修改分类 · 长按删除",12,muted,false));
        LinearLayout filters=row();for(String src:new String[]{"全部","微信","支付宝"}){TextView b=button(src,()->showRecords(src),false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),dp(16),dp(3),dp(8));filters.addView(b,p);}body.addView(filters);
        LinearLayout actions=row();for(String action:new String[]{"＋ 记一笔","语音记账","导入CSV"}){TextView b=button(action,()->{if(action.equals("语音记账"))startVoice();else if(action.equals("导入CSV"))pickCsv();else addDialog();},false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(2),0,dp(2),dp(8));actions.addView(b,p);}body.addView(actions);showRecords("全部");
    }
    private void showRecords(String source){int keep=Math.min(4,body.getChildCount());body.removeViews(keep,body.getChildCount()-keep);body.addView(text("合计 ¥ "+money(sum("",source)),14,muted,true));for(int i=entries.length()-1;i>=0;i--){JSONObject o=entries.optJSONObject(i);if(o!=null&&(source.equals("全部")||source.equals(o.optString("source"))))expenseRow(o);}}
    private void settings(){body.addView(text("数据与设置",24,ink,true));body.addView(text("账单仅保存在这台设备上。",14,muted,false));automationSettings();section("数据导入","CSV");body.addView(text("从微信支付或支付宝导出的账单文件中选择 CSV。导入前请检查文件格式；首版使用通用列名识别，若官方账单列名不同，可能需要后续适配。",13,muted,false));TextView importBtn=button("选择账单 CSV",()->pickCsv(),true);LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(14);body.addView(importBtn,p);TextView add=button("手动记账",()->addDialog(),false);p=lp(-1,-2);p.topMargin=dp(10);body.addView(add,p);TextView exp=button("导出本地备份 JSON",()->exportBackup(),false);p=lp(-1,-2);p.topMargin=dp(10);body.addView(exp,p);spaced(button("语音记账",()->startVoice(),false));section("关于日账","v1.2");body.addView(text("账单保存在本机。语音识别由手机服务处理，可能联网；未接入微信或支付宝个人账单授权 API。",13,muted,false));}
    private void addDialog(){showEditor(null,null,"");}
    private String[] sources={"微信","支付宝","现金/其他","未指定"};
    private void showEditor(JSONObject initial,JSONObject target,String note){
        if(editor!=null&&editor.isShowing())return;
        JSONObject seed=initial==null?new JSONObject():initial;editorInitial=seed;editorTarget=target;editorNote=note;
        LinearLayout form=col();pad(form,18,12,18,16);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        if(!note.isEmpty()){TextView explanation=text(note,13,muted,false);pad(explanation,0,0,0,12);form.addView(explanation);}
        form.addView(text("金额（元）",13,muted,true));editorAmount=new EditText(this);editorAmount.setSingleLine();editorAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);editorAmount.setHint("例如 25.00");editorAmount.setText(seed.optString("amount",""));form.addView(editorAmount);
        form.addView(text("支出分类 · 点选即可",13,muted,true));String category=seed.optString("category","其他");editorCategory=new String[]{ExpenseCategories.contains(category)?category:"其他"};form.addView(categoryChoices(editorCategory));
        form.addView(text("消费内容（可选）",13,muted,true));editorTitle=new EditText(this);editorTitle.setSingleLine();editorTitle.setHint("留空则使用所选分类");editorTitle.setText(seed.optString("title",""));form.addView(editorTitle);
        form.addView(text("日期 · 点击选择",13,muted,true));editorDate=new EditText(this);editorDate.setSingleLine();editorDate.setFocusable(false);editorDate.setHint("选择日期");editorDate.setText(seed.optString("date",today()));editorDate.setOnClickListener(v->{LocalDate current;try{current=LocalDate.parse(editorDate.getText().toString());}catch(Exception e){current=LocalDate.now();}new DatePickerDialog(this,(picker,y,m,d)->editorDate.setText(LocalDate.of(y,m+1,d).toString()),current.getYear(),current.getMonthValue()-1,current.getDayOfMonth()).show();});form.addView(editorDate);
        form.addView(text("支付方式",13,muted,true));editorSource=new String[]{seed.optString("source","未指定")};if(!java.util.Arrays.asList(sources).contains(editorSource[0]))editorSource[0]="未指定";
        Spinner source=new Spinner(this);source.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,sources));source.setSelection(java.util.Arrays.asList(sources).indexOf(editorSource[0]));source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> a,View v,int pos,long id){editorSource[0]=sources[pos];}public void onNothingSelected(AdapterView<?> a){}});form.addView(source);
        editor=new AlertDialog.Builder(this).setTitle(target==null?"记一笔支出":"修改这笔支出").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        editor.setOnShowListener(d->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->saveEditor()));editor.setOnDismissListener(d->editor=null);editor.show();editor.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private View categoryChoices(String[] chosen){GridLayout grid=new GridLayout(this);grid.setColumnCount(3);java.util.List<TextView> buttons=new ArrayList<>();for(String c:categories){TextView b=button(c.equals("餐饮")?"餐饮 / 食物":c,()->{},false);b.setTextSize(13);b.setMinHeight(dp(44));GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=-2;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(3),dp(4),dp(3),dp(4));grid.addView(b,p);buttons.add(b);b.setTag(c);b.setOnClickListener(v->{chosen[0]=c;refreshChoices(buttons,chosen[0]);});}refreshChoices(buttons,chosen[0]);return grid;}
    private void refreshChoices(java.util.List<TextView> buttons,String chosen){for(TextView b:buttons){boolean selected=chosen.equals(b.getTag());b.setSelected(selected);b.setTextColor(selected?Color.WHITE:green);b.setBackground(shape(selected?green:pale,12));b.setContentDescription(b.getText()+(selected?"，已选择":"，未选择"));}}
    private JSONObject editorDraft()throws JSONException{JSONObject o=new JSONObject(editorInitial.toString());o.put("title",editorTitle.getText().toString().trim());o.put("amount",editorAmount.getText().toString().trim());o.put("date",editorDate.getText().toString().trim());o.put("category",editorCategory[0]);o.put("source",editorSource[0]);return o;}
    private void saveEditor(){
        try{JSONObject o=editorDraft();String a=o.getString("amount");if(!a.matches("[0-9]+(?:\\.[0-9]{1,2})?"))throw new IllegalArgumentException();long cents=new BigDecimal(a).movePointRight(2).longValueExact();if(cents<=0||cents>100000000L)throw new IllegalArgumentException();
            String date=o.getString("date");try{LocalDate.parse(date);}catch(Exception e){editorDate.setError("请选择有效日期");return;}
            o.put("amount",cents/100.0);o.put("cents",cents);if(o.optString("title").isEmpty())o.put("title",editorCategory[0]+"支出");
            if(editorTarget==null){o.put("id",UUID.randomUUID().toString());LedgerStore.append(this,o);}else if(!LedgerStore.update(this,editorTarget,o)){toast("该记录已被删除，请重新打开");return;}
            editor.dismiss();load();render();toast("已保存");
        }catch(Exception e){editorAmount.setError("请输入大于 0、最多两位小数的金额（不超过100万元）");editorAmount.requestFocus();}
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("page",page);if(editor!=null&&editor.isShowing())try{out.putString("editor_draft",editorDraft().toString());out.putString("editor_note",editorNote);if(editorTarget!=null)out.putString("editor_target",editorTarget.toString());}catch(JSONException ignored){}}
    private void startVoice(){
        android.content.SharedPreferences p=getSharedPreferences("spend",0);if(!p.getBoolean("voice_disclosed",false)){new AlertDialog.Builder(this).setTitle("语音记账").setMessage("将调用手机的语音识别服务。语音由该服务处理，可能发送到其服务器；离线偏好不保证完全离线。日账不录音、不保存音频，只使用返回的文字填入一笔支出，确认保存后才入账。例：昨天午饭二十五块，微信支付。").setNegativeButton("取消",null).setPositiveButton("同意并开始",(d,w)->{p.edit().putBoolean("voice_disclosed",true).apply();launchVoice();}).show();}else launchVoice();
    }
    private void launchVoice(){Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"说一笔支出，例如：今天午饭花了25元，用微信支付");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);try{startActivityForResult(i,VOICE_REQUEST);}catch(ActivityNotFoundException e){new AlertDialog.Builder(this).setTitle("语音服务不可用").setMessage("手机上没有可用的语音识别服务。请先在手机设置中配置支持中文的语音识别服务；日账暂未保存任何记录。").setPositiveButton("知道了",null).show();}catch(SecurityException e){toast("语音服务未获得所需权限，请检查手机设置");}}
    private void voiceResult(int result,Intent data){if(result!=RESULT_OK||data==null){toast("语音已取消或未识别，没有保存记录");return;}ArrayList<String> texts=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(texts==null||texts.isEmpty()||texts.get(0).trim().isEmpty()){toast("没有听清，请重试");return;}String spoken=texts.get(0);VoiceExpenseParser.Draft draft=VoiceExpenseParser.parse(spoken,LocalDate.now());if(draft.blocked){new AlertDialog.Builder(this).setMessage(draft.warning).setPositiveButton("知道了",null).show();return;}try{JSONObject initial=new JSONObject();initial.put("amount",draft.cents==null?"":BigDecimal.valueOf(draft.cents,2).toPlainString());initial.put("date",draft.date);initial.put("category",draft.category);initial.put("source",draft.source);initial.put("title",draft.title);initial.put("channel","voice");showEditor(initial,null,"识别："+spoken+"\n"+(draft.warning.isEmpty()?"已填好，请核对后保存。":draft.warning));}catch(JSONException e){toast("语音解析失败，请重试");}}
    private void pickCsv(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_CSV);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==VOICE_REQUEST){voiceResult(res,data);return;}if(req==42&&res==RESULT_OK&&data!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(LedgerStore.read(this).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));toast("备份已导出");}catch(Exception e){toast("备份导出失败");}return;}if(req!=PICK_CSV||res!=RESULT_OK||data==null)return;try(InputStream in=getContentResolver().openInputStream(data.getData());BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"))){String header=br.readLine();if(header==null)throw new Exception("文件为空");String[] h=split(header);int ai=find(h,"金额","amount","实付","支出");int di=find(h,"日期","时间","date","交易时间");int ti=find(h,"商品","说明","备注","交易对方","title","商品说明");int si=find(h,"支付方式","来源","source","付款方式");int ci=find(h,"分类","category");if(ai<0)throw new Exception("未识别金额列。请使用包含“金额/amount”的 CSV 表头。");int count=0;String line;while((line=br.readLine())!=null){String[] v=split(line);if(ai>=v.length)continue;String av=v[ai].replace("¥","").replace("￥","").replace(",","").trim();if(av.isEmpty()||av.equals("--"))continue;double a;try{a=Math.abs(Double.parseDouble(av));}catch(Exception ex){continue;}if(a==0)continue;JSONObject o=new JSONObject();o.put("amount",a);o.put("title",value(v,ti,"消费"));o.put("date",normalizeDate(value(v,di,today())));String src=value(v,si,"");o.put("source",src.contains("微信")?"微信":src.contains("支付宝")?"支付宝":"其他");o.put("category",value(v,ci,"其他"));LedgerStore.append(this,o);count++;}save();render();new AlertDialog.Builder(this).setMessage("已导入 "+count+" 条记录。请核对日期、金额和收支方向。").setPositiveButton("完成",null).show();}catch(Exception e){new AlertDialog.Builder(this).setTitle("导入失败").setMessage(e.getMessage()==null?"无法读取该文件":e.getMessage()).setPositiveButton("确定",null).show();}}
    private String[] split(String s){return s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)",-1);}
    private int find(String[] h,String... keys){for(int i=0;i<h.length;i++){String x=h[i].replace("\"","").trim().toLowerCase(Locale.ROOT);for(String k:keys)if(x.contains(k.toLowerCase(Locale.ROOT)))return i;}return -1;}
    private String value(String[] v,int i,String d){if(i<0||i>=v.length)return d;return v[i].replace("\"","").trim();}
    private String normalizeDate(String s){try{String x=s.trim().replace("/","-");if(x.length()>=10)return x.substring(0,10);return today();}catch(Exception e){return today();}}
    private void exportBackup(){try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"daily-spend-backup.json");startActivityForResult(i,42);}catch(Exception e){toast("暂不支持导出");}}

    private void spaced(View v){LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(10);body.addView(v,p);}
    private boolean notificationGranted(){if(android.os.Build.VERSION.SDK_INT>=27)return ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).isNotificationListenerAccessGranted(new ComponentName(this,PaymentNotificationService.class));String listeners=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return listeners!=null&&java.util.Arrays.asList(listeners.split(":")).contains(new ComponentName(this,PaymentNotificationService.class).flattenToString());}
    private boolean screenGranted(){AccessibilityManager m=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);for(AccessibilityServiceInfo i:m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){android.content.pm.ServiceInfo si=i.getResolveInfo().serviceInfo;if(getPackageName().equals(si.packageName)&&PaymentAccessibilityService.class.getName().equals(si.name))return true;}return false;}
    private void openSettings(Intent i){try{startActivity(i);}catch(Exception e){toast("请在系统设置中为日账开启对应权限");}}
    private void consent(String channel){String message=channel.equals("notification")?"开启后，系统通知使用权可能允许日账访问通知。日账仅处理微信和支付宝的支付提醒，在本机识别金额并自动入账，不上传原文。不明金额、收入、退款和转账暂不入账。是否同意并前往系统设置？":"开启后，日账仅尝试读取微信和支付宝当前可访问的支付成功页文字，在本机自动记账。不点击、不操作付款、不截屏、不上传。不属于辅助残障人士的工具，Android 16 保护的敏感内容无法读取。是否同意并前往系统设置？";
        new AlertDialog.Builder(this).setTitle(channel.equals("notification")?"通知监听授权说明":"支付页面识别授权说明").setMessage(message).setNegativeButton("取消",null).setPositiveButton("同意并设置",(d,w)->{getSharedPreferences("spend",0).edit().putBoolean("enable_"+channel,true).putLong("enabled_at_"+channel,System.currentTimeMillis()).apply();if(channel.equals("notification")){Intent i;if(android.os.Build.VERSION.SDK_INT>=30){i=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);i.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(this,PaymentNotificationService.class).flattenToString());}else i=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);openSettings(i);}else openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}).show();}
    private String statusLine(String channel){android.content.SharedPreferences p=getSharedPreferences("spend",0);long ts=p.getLong("last_"+channel,0);return ts==0?"尚未收到可检测事件":new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date(ts))+" · "+p.getString("result_"+channel,"");}
    private void automationSettings(){
        section("自动记账","试用版");android.content.SharedPreferences p=getSharedPreferences("spend",0);
        body.addView(text("首次授权后自动运行，不用逐笔确认。只采集今后的明确支出，历史账单不会自动同步。",13,muted,false));
        spaced(text("通知监听："+(p.getBoolean("enable_notification",false)?(notificationGranted()?"已开启":"等待系统授权"):"已暂停"),15,ink,true));spaced(text(statusLine("notification"),12,muted,false));
        spaced(button("开启 / 配置通知监听",()->consent("notification"),true));
        spaced(text("页面识别："+(p.getBoolean("enable_screen",false)?(screenGranted()?"已开启":"等待系统授权"):"已暂停"),15,ink,true));spaced(text(statusLine("screen"),12,muted,false));
        spaced(button("开启 / 配置页面识别",()->consent("screen"),false));
        spaced(text("已自动记账 "+p.getLong("auto_count",0)+" 笔（累计）\n未知格式、退款、转账、红包暂不自动入账。跨渠道同额且商户不明时暂不计第二笔，可能漏记短时间同额交易。Android 16 若隐藏金额，日账无法识别。",12,muted,false));
        spaced(button("暂停全部自动采集",()->{p.edit().putBoolean("enable_notification",false).putBoolean("enable_screen",false).apply();render();toast("已暂停；也可在系统设置中撤销权限");},false));
        spaced(button("刷新监听状态",()->{load();render();},false));
        spaced(button("三星后台运行设置",()->openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))),false));
        spaced(text("三星 S26+ / Android 16：在日账应用信息中检查电池后台限制；如系统提供“从不自动休眠的应用”，把日账加入。强制停止应用后需重新打开。不同 One UI 版本菜单可能不同。",12,muted,false));
        spaced(button("检测规则自检（不写入账本）",()->{boolean a=PaymentParser.parse("com.tencent.mm","微信支付","支付成功，支出18.50元",false)!=null;boolean b=PaymentParser.parse("com.eg.android.AlipayGphone","","支付成功\n¥18.50\n完成",true)!=null;new AlertDialog.Builder(this).setTitle("本地规则自检").setMessage("通知规则："+(a?"通过":"失败")+"\n页面规则："+(b?"通过":"失败")+"\n此检查使用虚构文本，不代表真机权限或实际支付已验证。").setPositiveButton("确定",null).show();},false));
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
