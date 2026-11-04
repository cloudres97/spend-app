package com.cloudres.spend;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

public class MainActivity extends Activity {
    private final int ink=0xff182c25, muted=0xff53665d, green=0xff166449,
            pale=0xffe3f1e9, bg=0xfff6faf7, line=0xffdce8df;
    private JSONArray entries;
    private AlertDialog editor;
    private EditText editorTitle, editorAmount, editorDate;
    private String[] editorCategory, editorSource;
    private JSONObject editorTarget, editorInitial;
    private final String[] categories=ExpenseCategories.ALL;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        load();
        render();
        if (state != null && state.containsKey("editor_draft")) {
            try {
                String target = state.getString("editor_target");
                showEditor(new JSONObject(state.getString("editor_draft")),
                        target == null ? null : new JSONObject(target));
            } catch (JSONException ignored) {}
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (editor == null || !editor.isShowing()) return;
        try {
            out.putString("editor_draft", editorDraft().toString());
            if (editorTarget != null) out.putString("editor_target", editorTarget.toString());
        } catch (JSONException ignored) {}
    }

    private void load() { entries = LedgerStore.read(this); }
    private String today() { return LocalDate.now().toString(); }
    private double amount(JSONObject entry) { return entry.optDouble("amount", 0); }
    private String money(double v){return String.format(Locale.CHINA,"%,.2f",v);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setIncludeFontPadding(false);return t;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView button(String label,Runnable action,boolean primary){TextView t=text(label,14,primary?Color.WHITE:green,true);t.setGravity(Gravity.CENTER);pad(t,14,13,14,13);t.setBackground(shape(primary?green:pale,14));t.setOnClickListener(v->action.run());return t;}
    private void render() {
        LinearLayout root = col();
        root.setBackgroundColor(bg);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            pad(view, 20, 16, 20, 16);
            view.setPadding(view.getPaddingLeft() + insets.getSystemWindowInsetLeft(),
                    view.getPaddingTop() + insets.getSystemWindowInsetTop(),
                    view.getPaddingRight() + insets.getSystemWindowInsetRight(),
                    view.getPaddingBottom() + insets.getSystemWindowInsetBottom());
            return insets;
        });
        pad(root, 20, 16, 20, 16);
        setContentView(root);

        root.addView(text("日账", 32, ink, true));
        TextView add = button("＋  记一笔", () -> showEditor(null, null), true);
        add.setTextSize(24);
        LinearLayout.LayoutParams action = lp(-1, 68);
        action.topMargin = dp(16);
        action.bottomMargin = dp(20);
        root.addView(add, action);
        root.addView(text("账单", 20, ink, true));

        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout records = col();
        scroll.addView(records);
        if (entries.length() == 0) {
            TextView empty = text("还没有账单，点上方按钮记一笔。", 16, muted, false);
            pad(empty, 0, 20, 0, 0);
            records.addView(empty);
        }
        for (int i = entries.length() - 1; i >= 0; i--) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry != null) expenseRow(records, entry);
        }
    }

    private void expenseRow(LinearLayout records, JSONObject entry) {
        LinearLayout item = row();
        item.setMinimumHeight(dp(72));
        pad(item, 0, 12, 0, 12);
        TextView icon = text("¥", 24, green, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(pale, 14));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        item.addView(icon, lp(44, 44));
        LinearLayout info = col();
        pad(info, 12, 0, 8, 0);
        info.addView(text(entry.optString("title", "支出"), 17, ink, true));
        TextView details = text(entry.optString("category", "其他") + " · "
                + entry.optString("date"), 13, muted, false);
        pad(details, 0, 5, 0, 0);
        info.addView(details);
        item.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(text("¥" + money(amount(entry)), 17, ink, true));
        item.setOnClickListener(v -> showEditor(entry, entry));
        records.addView(item, lp(-1, -2));
        View divider = new View(this);
        divider.setBackgroundColor(line);
        records.addView(divider, lp(-1, 1));
    }
    private String[] sources={"微信","支付宝","现金/其他","未指定"};
    private void showEditor(JSONObject initial,JSONObject target){
        if(editor!=null&&editor.isShowing())return;
        JSONObject seed=initial==null?new JSONObject():initial;editorInitial=seed;editorTarget=target;
        LinearLayout form=col();pad(form,18,12,18,16);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        form.addView(text("金额（元）",13,muted,true));editorAmount=new EditText(this);editorAmount.setSingleLine();editorAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);editorAmount.setHint("例如 25.00");editorAmount.setText(seed.optString("amount",""));form.addView(editorAmount);
        form.addView(text("支出分类 · 点选即可",13,muted,true));String category=seed.optString("category","其他");editorCategory=new String[]{ExpenseCategories.contains(category)?category:"其他"};form.addView(categoryChoices(editorCategory));
        form.addView(text("消费内容（可选）",13,muted,true));editorTitle=new EditText(this);editorTitle.setSingleLine();editorTitle.setHint("留空则使用所选分类");editorTitle.setText(seed.optString("title",""));form.addView(editorTitle);
        form.addView(text("日期 · 点击选择",13,muted,true));editorDate=new EditText(this);editorDate.setSingleLine();editorDate.setFocusable(false);editorDate.setHint("选择日期");editorDate.setText(seed.optString("date",today()));editorDate.setOnClickListener(v->{LocalDate current;try{current=LocalDate.parse(editorDate.getText().toString());}catch(Exception e){current=LocalDate.now();}new DatePickerDialog(this,(picker,y,m,d)->editorDate.setText(LocalDate.of(y,m+1,d).toString()),current.getYear(),current.getMonthValue()-1,current.getDayOfMonth()).show();});form.addView(editorDate);
        form.addView(text("支付方式",13,muted,true));editorSource=new String[]{seed.optString("source","未指定")};if(!java.util.Arrays.asList(sources).contains(editorSource[0]))editorSource[0]="未指定";
        Spinner source=new Spinner(this);source.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,sources));source.setSelection(java.util.Arrays.asList(sources).indexOf(editorSource[0]));source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> a,View v,int pos,long id){editorSource[0]=sources[pos];}public void onNothingSelected(AdapterView<?> a){}});form.addView(source);
        editor=new AlertDialog.Builder(this).setTitle(target==null?"记一笔支出":"修改这笔支出").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        if (target != null) editor.setButton(AlertDialog.BUTTON_NEUTRAL, "删除", (d, w) -> confirmDelete(target));
        editor.setOnShowListener(d->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->saveEditor()));editor.setOnDismissListener(d->editor=null);editor.show();editor.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private View categoryChoices(String[] chosen){GridLayout grid=new GridLayout(this);grid.setColumnCount(3);java.util.List<TextView> buttons=new ArrayList<>();for(String c:categories){TextView b=button(c,()->{},false);b.setTextSize(16);b.setMinHeight(dp(50));GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=-2;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(3),dp(4),dp(3),dp(4));grid.addView(b,p);buttons.add(b);b.setTag(c);b.setOnClickListener(v->{chosen[0]=c;refreshChoices(buttons,chosen[0]);});}refreshChoices(buttons,chosen[0]);return grid;}
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
    private void confirmDelete(JSONObject target) {
        new AlertDialog.Builder(this).setMessage("删除这笔支出？")
                .setNegativeButton("取消", (dialog, which) -> showEditor(target, target))
                .setPositiveButton("删除", (dialog, which) -> {
                    LedgerStore.remove(this, target);
                    load();
                    render();
                }).show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
