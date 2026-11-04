package com.cloudres.spend;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
    private boolean statistics;
    private final String[] icons = {"🍜","🚲","🛍️","🧴","🎮","🏠","🩹","📚","⚽","📱","🧳","✨"};
    private final int[] categoryColors = {0xffd67457,0xff4c92b2,0xffad74ad,0xff64a393,
            0xffd38b37,0xff6e85b7,0xffc66d86,0xff85964a,0xff438b67,0xff8076ad,0xffb67f4c,0xff73857b};
    private AlertDialog editor;
    private EditText editorTitle, editorAmount, editorDate;
    private String[] editorCategory, editorSource;
    private JSONObject editorTarget, editorInitial;
    private final String[] categories=ExpenseCategories.ALL;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        statistics = state != null && state.getBoolean("statistics");
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
        out.putBoolean("statistics", statistics);
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

        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root.addView(text(statistics ? "支出统计" : "日账", 32, ink, true));
        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams content = new LinearLayout.LayoutParams(-1, 0, 1);
        content.topMargin = dp(16);
        root.addView(scroll, content);
        LinearLayout records = col();
        scroll.addView(records);
        if (statistics) {
            showStatistics(records);
        } else {
            showRecords(records);
            TextView add = button("＋", () -> showEditor(null, null), true);
            add.setTextSize(40);
            add.setContentDescription("记一笔");
            pad(add, 0, 0, 0, 0);
            add.setBackground(shape(green, 40));
            LinearLayout.LayoutParams action = lp(80, 80);
            action.gravity = Gravity.CENTER_HORIZONTAL;
            action.bottomMargin = dp(8);
            root.addView(add, action);
        }
        LinearLayout tabs = row();
        tabs.addView(button("账单", () -> switchTab(false), !statistics), new LinearLayout.LayoutParams(0, dp(48), 1));
        View gap = new View(this);
        tabs.addView(gap, lp(8, 1));
        tabs.addView(button("统计", () -> switchTab(true), statistics), new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(tabs);
    }

    private void switchTab(boolean showStatistics) {
        if (statistics == showStatistics) return;
        statistics = showStatistics;
        render();
    }

    private int categoryIndex(String category) {
        int index = Arrays.asList(categories).indexOf(category);
        return index < 0 ? categories.length - 1 : index;
    }

    private void showRecords(LinearLayout records) {
        if (entries.length() == 0) {
            TextView empty = text("还没有账单，点下方加号记一笔。", 16, muted, false);
            pad(empty, 0, 20, 0, 0);
            records.addView(empty);
        }
        for (int i = entries.length() - 1; i >= 0; i--) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry != null) expenseRow(records, entry);
        }
    }

    private void showStatistics(LinearLayout content) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        double[] daily = new double[7];
        double[] byCategory = new double[categories.length];
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry == null) continue;
            try {
                long day = LocalDate.parse(entry.optString("date")).toEpochDay() - start.toEpochDay();
                double value = amount(entry);
                if (day < 0 || day >= daily.length || !Double.isFinite(value) || value <= 0) continue;
                daily[(int) day] += value;
                byCategory[categoryIndex(entry.optString("category"))] += value;
            } catch (java.time.format.DateTimeParseException ignored) {}
        }
        double total = Arrays.stream(daily).sum();
        content.addView(text("最近 7 天 · " + start + " 至 " + end, 13, muted, false));
        TextView headline = text("¥" + money(total), 30, ink, true);
        pad(headline, 0, 8, 0, 20);
        content.addView(headline);

        LinearLayout trend = chartCard(content, "每日支出");
        if (total == 0) {
            trend.addView(text("这 7 天还没有支出记录。", 15, muted, false));
            return;
        }
        View trendChart = new Chart(daily, false);
        StringBuilder description = new StringBuilder("每日支出");
        for (int i = 0; i < daily.length; i++) {
            description.append("，").append(start.plusDays(i)).append("，").append(money(daily[i])).append("元");
        }
        trendChart.setContentDescription(description.toString());
        trend.addView(trendChart, lp(-1, 140));
        LinearLayout days = row();
        for (int i = 0; i < daily.length; i++) {
            LocalDate day = start.plusDays(i);
            TextView label = text(day.getMonthValue() + "/" + day.getDayOfMonth(), 11, muted, false);
            label.setGravity(Gravity.CENTER);
            days.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        }
        trend.addView(days);

        LinearLayout breakdown = chartCard(content, "分类占比");
        View pie = new Chart(byCategory, true);
        pie.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        breakdown.addView(pie, lp(-1, 170));
        for (int i = 0; i < byCategory.length; i++) {
            if (byCategory[i] <= 0) continue;
            LinearLayout legend = row();
            pad(legend, 0, 8, 0, 8);
            View swatch = new View(this);
            swatch.setBackground(shape(categoryColors[i], 6));
            legend.addView(swatch, lp(12, 12));
            TextView name = text(icons[i] + " " + categories[i], 15, ink, false);
            pad(name, 8, 0, 4, 0);
            legend.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            legend.addView(text(String.format(Locale.CHINA, "¥%s  %.0f%%", money(byCategory[i]),
                    byCategory[i] * 100 / total), 14, muted, false));
            breakdown.addView(legend);
        }
    }

    private LinearLayout chartCard(LinearLayout parent, String title) {
        LinearLayout card = col();
        card.setBackground(shape(Color.WHITE, 20));
        pad(card, 16, 16, 16, 16);
        TextView heading = text(title, 18, ink, true);
        pad(heading, 0, 0, 0, 8);
        card.addView(heading);
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(12);
        parent.addView(card, params);
        return card;
    }

    private final class Chart extends View {
        private final double[] values;
        private final boolean pie;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Chart(double[] values, boolean pie) {
            super(MainActivity.this);
            this.values = values;
            this.pie = pie;
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (pie) {
                float radius = Math.min(getWidth(), getHeight()) / 2f - dp(8);
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                double total = Arrays.stream(values).sum();
                float start = -90;
                paint.setStyle(Paint.Style.FILL);
                for (int i = 0; i < values.length; i++) {
                    if (values[i] <= 0) continue;
                    float sweep = (float) (360 * values[i] / total);
                    paint.setColor(categoryColors[i]);
                    canvas.drawArc(cx-radius, cy-radius, cx+radius, cy+radius, start, sweep, true, paint);
                    start += sweep;
                }
                return;
            }
            float bottom = getHeight() - dp(8);
            float height = bottom - dp(12);
            float step = getWidth() / (float) values.length;
            double max = Arrays.stream(values).max().orElse(1);
            paint.setColor(line);
            paint.setStrokeWidth(dp(1));
            for (int i = 0; i <= 3; i++) {
                float y = bottom - height * i / 3;
                canvas.drawLine(0, y, getWidth(), y, paint);
            }
            path.reset();
            for (int i = 0; i < values.length; i++) {
                float x = step * (i + 0.5f);
                float y = bottom - (float) (values[i] / max) * height;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            paint.setColor(green);
            paint.setStrokeWidth(dp(3));
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < values.length; i++) {
                canvas.drawCircle(step * (i + 0.5f), bottom - (float) (values[i] / max) * height, dp(4), paint);
            }
        }
    }

    private void expenseRow(LinearLayout records, JSONObject entry) {
        LinearLayout item = row();
        item.setMinimumHeight(dp(72));
        pad(item, 0, 12, 0, 12);
        int category = categoryIndex(entry.optString("category"));
        TextView icon = text(icons[category], 23, green, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape((categoryColors[category] & 0x00ffffff) | 0x26000000, 16));
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
    private View categoryChoices(String[] chosen){GridLayout grid=new GridLayout(this);grid.setColumnCount(3);java.util.List<TextView> buttons=new ArrayList<>();for(String c:categories){TextView b=button(icons[categoryIndex(c)]+"\n"+c,()->{},false);b.setTextSize(16);b.setMinHeight(dp(50));GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=-2;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(3),dp(4),dp(3),dp(4));grid.addView(b,p);buttons.add(b);b.setTag(c);b.setOnClickListener(v->{chosen[0]=c;refreshChoices(buttons,chosen[0]);});}refreshChoices(buttons,chosen[0]);return grid;}
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


