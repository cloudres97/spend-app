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
    private final int ink=0xff422b36, muted=0xff775663, accent=0xff964964,
            pale=0xfff9e3e9, bg=0xfffff8f4, line=0xfff1dce2;
    private JSONArray entries;
    private boolean statistics;
    private boolean monthly;
    private final int[] icons = {R.drawable.ic_food, R.drawable.ic_travel, R.drawable.ic_shop,
            R.drawable.ic_daily, R.drawable.ic_play, R.drawable.ic_home, R.drawable.ic_health,
            R.drawable.ic_study, R.drawable.ic_sport, R.drawable.ic_phone, R.drawable.ic_trip, R.drawable.ic_paw};
    private final int[] categoryColors = {0xffce7792,0xff7192b6,0xffa784bd,0xff77a696,
            0xffd89763,0xff857bc0,0xffc15d7b,0xff8d9d69,0xff599888,0xff8174aa,0xffb78166,0xff927c8e};
    private AlertDialog editor;
    private EditText editorAmount;
    private TextView editorDate;
    private String[] editorCategory;
    private JSONObject editorTarget, editorInitial;
    private final String[] categories=ExpenseCategories.ALL;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        statistics = state != null && state.getBoolean("statistics");
        monthly = state != null && state.getBoolean("monthly");
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
        out.putBoolean("monthly", monthly);
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
    private TextView button(String label,Runnable action,boolean primary){TextView t=text(label,14,primary?Color.WHITE:accent,true);t.setGravity(Gravity.CENTER);pad(t,14,13,14,13);t.setBackground(shape(primary?accent:pale,14));t.setOnClickListener(v->action.run());return t;}
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
        LinearLayout header = row();
        ImageView mascot = new ImageView(this);
        mascot.setImageResource(R.drawable.ic_launcher);
        mascot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(mascot, lp(56, 56));
        LinearLayout title = col();
        pad(title, 10, 0, 0, 0);
        title.addView(text(statistics ? "支出统计" : getString(R.string.app_name), 28, ink, true));
        TextView subtitle = text(statistics ? "看看小钱都花在哪儿" : "每天记一点，花费看得见", 13, muted, false);
        pad(subtitle, 0, 5, 0, 0);
        title.addView(subtitle);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header);
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
            ImageButton add = new ImageButton(this);
            add.setImageResource(R.drawable.ic_paw);
            add.setScaleType(ImageView.ScaleType.FIT_CENTER);
            add.setColorFilter(Color.WHITE);
            add.setOnClickListener(v -> showEditor(null, null));

            add.setContentDescription("记一笔");
            pad(add, 9, 9, 9, 9);
            add.setBackground(shape(accent, 40));
            LinearLayout.LayoutParams action = lp(80, 80);
            action.gravity = Gravity.CENTER_HORIZONTAL;
            action.bottomMargin = dp(8);
            root.addView(add, action);
        }
        View separator = new View(this);
        separator.setBackgroundColor(line);
        root.addView(separator, lp(-1, 1));
        LinearLayout tabs = row();
        tabs.addView(tab("账单", R.drawable.ic_ledger, false), new LinearLayout.LayoutParams(0, -1, 1));
        tabs.addView(tab("统计", R.drawable.ic_stats, true), new LinearLayout.LayoutParams(0, -1, 1));
        root.addView(tabs, lp(-1, 58));
    }

    private TextView tab(String label, int icon, boolean stats) {
        boolean selected = statistics == stats;
        int tint = selected ? accent : muted;
        TextView tab = text(label, 13, tint, selected);
        tab.setGravity(Gravity.CENTER);
        pad(tab, 0, 8, 0, 6);
        tab.setSelected(selected);
        tab.setOnClickListener(v -> switchTab(stats));
        android.graphics.drawable.Drawable glyph = getDrawable(icon).mutate();
        glyph.setBounds(0, 0, dp(23), dp(23));
        glyph.setTint(tint);
        tab.setCompoundDrawablesRelative(null, glyph, null, null);
        tab.setCompoundDrawablePadding(dp(4));
        tab.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(pale), null, shape(Color.WHITE, 12)));
        return tab;
    }

    private ImageView categoryIcon(int category) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(icons[category]);
        icon.setBackground(shape((categoryColors[category] & 0x00ffffff) | 0x26000000, 16));
        pad(icon, 10, 10, 10, 10);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return icon;
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
        LocalDate start = monthly ? end.withDayOfMonth(1) : end.minusDays(6);
        double[] daily = new double[monthly ? end.getDayOfMonth() : 7];
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
        LinearLayout periods = row();
        periods.addView(periodButton("周", false), new LinearLayout.LayoutParams(0, dp(44), 1));
        periods.addView(new View(this), lp(8, 1));
        periods.addView(periodButton("月", true), new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams periodSpace = lp(-1, -2);
        periodSpace.bottomMargin = dp(14);
        content.addView(periods, periodSpace);
        String period = monthly ? "本月" : "最近 7 天";
        content.addView(text(period + " · " + start + " 至 " + end, 13, muted, false));
        TextView headline = text("¥" + money(total), 30, ink, true);
        pad(headline, 0, 8, 0, 20);
        content.addView(headline);

        LinearLayout trend = chartCard(content, "每日支出");
        if (total == 0) {
            trend.addView(text("这段时间还没有支出记录。", 15, muted, false));
            return;
        }
        View trendChart = new Chart(daily, false);
        StringBuilder description = new StringBuilder("每日支出");
        for (int i = 0; i < daily.length; i++) {
            description.append("，").append(start.plusDays(i)).append("，").append(money(daily[i])).append("元");
        }
        trendChart.setContentDescription(description.toString());
        trend.addView(trendChart, lp(-1, 140));
        FrameLayout days = new FrameLayout(this);
        for (int i = 0; i < daily.length; i++) {
            LocalDate day = start.plusDays(i);
            boolean showLabel = daily.length <= 7 || i == 0 || i == daily.length - 1
                    || (i % 5 == 0 && i < daily.length - 3);
            String dateLabel = showLabel ? day.getMonthValue() + "/" + day.getDayOfMonth() : "";
            TextView label = text(dateLabel, 11, muted, false);
            label.setGravity(Gravity.CENTER);
            if (!showLabel) continue;
            final int index = i;
            days.addView(label, new FrameLayout.LayoutParams(-2, -2));
            days.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
                float center = (index + 0.5f) * days.getWidth() / daily.length;
                label.setTranslationX(Math.max(0, Math.min(days.getWidth() - label.getWidth(),
                        center - label.getWidth() / 2f)));
            });
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
            ImageView icon = categoryIcon(i);
            LinearLayout.LayoutParams iconSpace = lp(36, 36);
            iconSpace.leftMargin = dp(8);
            legend.addView(icon, iconSpace);
            TextView name = text(categories[i], 15, ink, false);
            pad(name, 8, 0, 4, 0);
            legend.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            legend.addView(text(String.format(Locale.CHINA, "¥%s  %.0f%%", money(byCategory[i]),
                    byCategory[i] * 100 / total), 14, muted, false));
            breakdown.addView(legend);
        }
    }

    private TextView periodButton(String label, boolean month) {
        TextView control = button(label, () -> {
            if (monthly == month) return;
            monthly = month;
            render();
        }, monthly == month);
        control.setSelected(monthly == month);
        pad(control, 0, 0, 0, 0);
        return control;
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
            paint.setColor(accent);
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
        pad(item, 12, 12, 12, 12);
        item.setBackground(shape(Color.WHITE, 20));
        int category = categoryIndex(entry.optString("category"));
        item.addView(categoryIcon(category), lp(46, 46));
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
        LinearLayout.LayoutParams card = lp(-1, -2);
        card.bottomMargin = dp(8);
        records.addView(item, card);
    }
    private void showEditor(JSONObject initial,JSONObject target){
        if(editor!=null&&editor.isShowing())return;
        JSONObject seed=initial==null?new JSONObject():initial;editorInitial=seed;editorTarget=target;
        LinearLayout form=col();pad(form,18,12,18,16);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        form.addView(text("金额（元）", 15, muted, true));
        editorAmount = new EditText(this);
        editorAmount.setSingleLine();
        editorAmount.setTextSize(36);
        editorAmount.setTextColor(ink);
        editorAmount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        editorAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editorAmount.setHint("0.00");
        editorAmount.setText(seed.optString("amount", ""));
        form.addView(editorAmount);
        form.addView(text("支出分类 · 点选即可", 13, muted, true));
        String category = seed.optString("category", target == null ? "餐饮" : "其他");
        editorCategory = new String[]{ExpenseCategories.contains(category) ? category : "其他"};
        form.addView(categoryChoices(editorCategory));

        TextView dateLabel = text("日期", 13, muted, true);
        pad(dateLabel, 0, 10, 0, 8);
        form.addView(dateLabel);
        editorDate = button(seed.optString("date", today()), this::showCalendar, false);
        editorDate.setTextSize(17);
        editorDate.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_calendar, 0, 0, 0);
        editorDate.setCompoundDrawablePadding(dp(8));
        editorDate.setContentDescription("选择日期，" + editorDate.getText());
        form.addView(editorDate, lp(-1, -2));
        editor=new AlertDialog.Builder(this).setTitle(target==null?"记一笔支出":"修改这笔支出").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        if (target != null) editor.setButton(AlertDialog.BUTTON_NEUTRAL, "删除", (d, w) -> confirmDelete(target));
        editor.setOnShowListener(dialog -> {
            Button save = editor.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextSize(18);
            save.setTextColor(Color.WHITE);
            save.setMinHeight(dp(50));
            save.setMinWidth(dp(92));
            save.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x40ffffff), shape(accent, 14), null));
            android.graphics.drawable.Drawable check = getDrawable(R.drawable.ic_check).mutate();
            check.setTint(Color.WHITE);
            check.setBounds(0, 0, dp(22), dp(22));
            save.setCompoundDrawablesRelative(check, null, null, null);
            save.setCompoundDrawablePadding(dp(6));
            pad(save, 12, 0, 12, 0);
            save.setOnClickListener(v -> saveEditor());
        });
        editor.setOnDismissListener(d -> editor = null);
        editor.show();
        editor.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void showCalendar() {
        LocalDate current;
        try {
            current = LocalDate.parse(editorDate.getText());
        } catch (java.time.format.DateTimeParseException ignored) {
            current = LocalDate.now();
        }
        DatePickerDialog calendar = new DatePickerDialog(this, R.style.MeowCalendar,
                (picker, year, month, day) -> {
                    String selected = LocalDate.of(year, month + 1, day).toString();
                    editorDate.setText(selected);
                    editorDate.setContentDescription("选择日期，" + selected);
                }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());
        calendar.show();
    }

    private View categoryChoices(String[] chosen){GridLayout grid=new GridLayout(this);grid.setColumnCount(3);java.util.List<TextView> buttons=new ArrayList<>();for(String c:categories){TextView b=button(c,()->{},false);b.setCompoundDrawablesRelativeWithIntrinsicBounds(0,icons[categoryIndex(c)],0,0);b.setCompoundDrawablePadding(dp(5));b.setTextSize(16);b.setMinHeight(dp(50));GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=-2;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(3),dp(4),dp(3),dp(4));grid.addView(b,p);buttons.add(b);b.setTag(c);b.setOnClickListener(v->{chosen[0]=c;refreshChoices(buttons,chosen[0]);});}refreshChoices(buttons,chosen[0]);return grid;}
    private void refreshChoices(java.util.List<TextView> buttons,String chosen){for(TextView b:buttons){boolean selected=chosen.equals(b.getTag());b.setSelected(selected);b.setTextColor(selected?Color.WHITE:accent);b.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(selected?Color.WHITE:accent));b.setBackground(shape(selected?accent:pale,12));b.setContentDescription(b.getText()+(selected?"，已选择":"，未选择"));}}
    private JSONObject editorDraft()throws JSONException{JSONObject o=new JSONObject(editorInitial.toString());o.put("amount",editorAmount.getText().toString().trim());o.put("date",editorDate.getText().toString().trim());o.put("category",editorCategory[0]);return o;}
    private void saveEditor(){
        try{JSONObject o=editorDraft();String a=o.getString("amount");if(!a.matches("[0-9]+(?:\\.[0-9]{1,2})?"))throw new IllegalArgumentException();long cents=new BigDecimal(a).movePointRight(2).longValueExact();if(cents<=0||cents>100000000L)throw new IllegalArgumentException();
            String date=o.getString("date");try{LocalDate.parse(date);}catch(Exception e){editorDate.setError("请选择有效日期");return;}
            o.put("amount",cents/100.0);o.put("cents",cents);String oldCategory=editorInitial.optString("category","其他");if(o.optString("title").isEmpty()||o.optString("title").equals(oldCategory+"支出"))o.put("title",editorCategory[0]+"支出");
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








