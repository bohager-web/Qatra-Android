package com.qatra.water;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends android.app.Activity {
    private static final int REQUEST_NOTIFICATIONS = 901;
    private static final int BLUE = 0xFF1396F3;
    private static final int DARK_BLUE = 0xFF0875C9;
    private static final int LIGHT_BLUE = 0xFFEAF7FF;
    private static final int BACKGROUND = 0xFFF4FBFF;
    private static final int TEXT = 0xFF16324A;
    private static final int MUTED = 0xFF6A8498;

    private QatraStore store;
    private LinearLayout pageContainer;
    private ScrollView scrollView;
    private TextView navHome;
    private TextView navHistory;
    private TextView navStats;
    private DropProgressView dropView;
    private TextView amountView;
    private TextView goalView;
    private TextView encouragementView;
    private Button undoButton;
    private String currentPage = "home";

    private final BroadcastReceiver dataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshCurrentPage(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        store = new QatraStore(this);
        setContentView(buildAppLayout());
        showHome(false);
        registerDataReceiver();
        ReminderScheduler.apply(this);
        QatraWidgetProvider.updateAll(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (store != null) {
            refreshCurrentPage(false);
            QatraWidgetProvider.updateAll(this);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(dataChangedReceiver);
        } catch (IllegalArgumentException ignored) {
            // Receiver was not registered or the activity is already tearing down.
        }
        super.onDestroy();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(Color.WHITE);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private View buildAppLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BACKGROUND);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout contentFrame = new FrameLayout(this);
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pageContainer = new LinearLayout(this);
        pageContainer.setOrientation(LinearLayout.VERTICAL);
        pageContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        pageContainer.setPadding(dp(18), dp(8), dp(18), dp(28));
        scrollView.addView(pageContainer, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentFrame.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(contentFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(buildBottomNavigation(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));
        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        header.setPadding(dp(18), dp(12), dp(18), dp(8));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        brand.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher_legacy);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        logoParams.setMarginEnd(dp(9));
        brand.addView(logo, logoParams);

        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("قطرة", 24, DARK_BLUE, true);
        TextView subtitle = text("كل قطرة تصنع فرقًا", 12, MUTED, false);
        brandText.addView(title);
        brandText.addView(subtitle);
        brand.addView(brandText);
        header.addView(brand, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView settings = text("الإعدادات", 13, DARK_BLUE, true);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(rounded(Color.WHITE, 16, 0xFFD8F0FF, 1));
        settings.setElevation(dp(2));
        settings.setPadding(dp(13), dp(9), dp(13), dp(9));
        settings.setOnClickListener(v -> showSettingsDialog());
        header.addView(settings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return header;
    }

    private View buildBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(7), dp(10), dp(8));
        nav.setBackground(rounded(Color.WHITE, 24, 0xFFE4F2FA, 1));
        nav.setElevation(dp(10));

        navHome = createNavItem("الرئيسية", () -> showHome(false));
        navHistory = createNavItem("السجل", this::showHistory);
        navStats = createNavItem("الإحصائيات", this::showStats);
        nav.addView(navHome, weightedNavParams());
        nav.addView(navHistory, weightedNavParams());
        nav.addView(navStats, weightedNavParams());
        return nav;
    }

    private TextView createNavItem(String label, Runnable action) {
        TextView item = text(label, 13, MUTED, true);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(8), dp(10), dp(8), dp(10));
        item.setOnClickListener(v -> action.run());
        return item;
    }

    private LinearLayout.LayoutParams weightedNavParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private void showHome(boolean animate) {
        currentPage = "home";
        selectNavigation(navHome);
        pageContainer.removeAllViews();
        scrollView.scrollTo(0, 0);

        TextView greeting = text("مرحبًا بك 👋", 22, TEXT, true);
        greeting.setGravity(Gravity.START);
        pageContainer.addView(greeting, fullWidthMargins(0, 6, 0, 2));
        TextView intro = text("سجّل ما تشربه، وخلّ قطرتك تمتلئ خلال اليوم.", 14, MUTED, false);
        intro.setGravity(Gravity.START);
        pageContainer.addView(intro, fullWidthMargins(0, 0, 0, 14));

        LinearLayout progressCard = card();
        progressCard.setGravity(Gravity.CENTER_HORIZONTAL);
        dropView = new DropProgressView(this);
        progressCard.addView(dropView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(272)));
        amountView = text("", 30, TEXT, true);
        amountView.setGravity(Gravity.CENTER);
        progressCard.addView(amountView, fullWidthMargins(0, -10, 0, 2));
        goalView = text("", 14, MUTED, false);
        goalView.setGravity(Gravity.CENTER);
        progressCard.addView(goalView, fullWidthMargins(0, 0, 0, 8));
        encouragementView = text("", 14, DARK_BLUE, true);
        encouragementView.setGravity(Gravity.CENTER);
        encouragementView.setBackground(rounded(LIGHT_BLUE, 15, 0, 0));
        encouragementView.setPadding(dp(12), dp(8), dp(12), dp(8));
        progressCard.addView(encouragementView, fullWidthMargins(14, 0, 14, 6));
        pageContainer.addView(progressCard, fullWidthMargins(0, 0, 0, 18));

        TextView quickTitle = text("إضافة سريعة", 18, TEXT, true);
        quickTitle.setGravity(Gravity.START);
        pageContainer.addView(quickTitle, fullWidthMargins(0, 0, 0, 8));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setRowCount(2);
        grid.setUseDefaultMargins(false);
        grid.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int[] amounts = {150, 250, 330, 500, 750, 1000};
        for (int i = 0; i < amounts.length; i++) {
            final int amount = amounts[i];
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setText(String.format(Locale.US, "+%d مل", amount));
            button.setTextSize(15);
            button.setTextColor(DARK_BLUE);
            button.setTypeface(Typeface.create("sans", Typeface.BOLD));
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(4), 0, dp(4), 0);
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            button.setBackground(rounded(Color.WHITE, 18, 0xFFCAEAFE, 1));
            button.setElevation(dp(3));
            button.setOnClickListener(v -> addWater(amount));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(66);
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            params.rowSpec = GridLayout.spec(i / 3);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(button, params);
        }
        pageContainer.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button custom = primaryButton("إضافة كمية أخرى");
        custom.setOnClickListener(v -> showCustomAmountDialog());
        pageContainer.addView(custom, fullWidthMargins(4, 10, 4, 8));

        undoButton = secondaryButton("تراجع عن آخر إضافة");
        undoButton.setOnClickListener(v -> undoWater());
        pageContainer.addView(undoButton, fullWidthMargins(4, 0, 4, 18));

        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        summaryRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        summaryRow.addView(smallStatCard("متوسط 7 أيام",
                store.getSevenDayAverageMl() + " مل"), rowWeightMargins());
        summaryRow.addView(smallStatCard("سلسلة الإنجاز",
                store.getCurrentStreak() + " يوم"), rowWeightMargins());
        pageContainer.addView(summaryRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        refreshHome(animate);
    }

    private void refreshHome(boolean animate) {
        if (dropView == null || amountView == null) {
            return;
        }
        int total = store.getCurrentTotalMl();
        int goal = store.getGoalMl();
        int percent = goal <= 0 ? 0 : Math.min(100, Math.round(total * 100f / goal));
        float progress = goal <= 0 ? 0f : Math.min(1f, total / (float) goal);
        dropView.setProgress(progress, animate);
        amountView.setText(String.format(Locale.US, "%d / %d مل", total, goal));
        goalView.setText(String.format(Locale.US, "حققت %d%% من هدف اليوم", percent));
        if (total >= goal) {
            encouragementView.setText("رائع! حققت هدف اليوم 🎉");
        } else if (percent >= 75) {
            encouragementView.setText("باقي القليل، استمر 💧");
        } else if (percent >= 40) {
            encouragementView.setText("أداء جميل، أنت في الطريق الصحيح");
        } else {
            encouragementView.setText("ابدأ بقطرة، ثم أكمل بهدوء");
        }
        undoButton.setEnabled(store.hasEntriesToday());
        undoButton.setAlpha(store.hasEntriesToday() ? 1f : 0.45f);
    }

    private void showHistory() {
        currentPage = "history";
        selectNavigation(navHistory);
        pageContainer.removeAllViews();
        scrollView.scrollTo(0, 0);

        pageContainer.addView(sectionTitle("سجل الشرب"), fullWidthMargins(0, 4, 0, 2));
        TextView subtitle = text("آخر 14 يومًا محفوظة على هاتفك", 14, MUTED, false);
        subtitle.setGravity(Gravity.START);
        pageContainer.addView(subtitle, fullWidthMargins(0, 0, 0, 14));

        List<QatraStore.DayRecord> days = store.getRecentDays(14);
        for (int i = 0; i < days.size(); i++) {
            QatraStore.DayRecord day = days.get(i);
            LinearLayout item = card();
            item.setPadding(dp(15), dp(13), dp(15), dp(13));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView dayName = text(i == 0 ? "اليوم" : day.dayName, 16, TEXT, true);
            dayName.setGravity(Gravity.START);
            TextView date = text(day.displayDate, 12, MUTED, false);
            date.setGravity(Gravity.START);
            labels.addView(dayName);
            labels.addView(date);
            row.addView(labels, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView value = text(String.format(Locale.US, "%d مل", day.amountMl),
                    17, day.amountMl >= day.goalMl ? DARK_BLUE : TEXT, true);
            row.addView(value);
            item.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            progress.setProgress(day.percentage());
            progress.setProgressTintList(ColorStateList.valueOf(BLUE));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFE4F2FA));
            item.addView(progress, fullWidthMargins(0, 10, 0, 2));

            TextView percent = text(day.percentage() + "% من الهدف", 12, MUTED, false);
            percent.setGravity(Gravity.START);
            item.addView(percent);
            pageContainer.addView(item, fullWidthMargins(0, 0, 0, 10));
        }
    }

    private void showStats() {
        currentPage = "stats";
        selectNavigation(navStats);
        pageContainer.removeAllViews();
        scrollView.scrollTo(0, 0);

        pageContainer.addView(sectionTitle("إحصائياتك"), fullWidthMargins(0, 4, 0, 2));
        TextView subtitle = text("ملخص آخر سبعة أيام", 14, MUTED, false);
        subtitle.setGravity(Gravity.START);
        pageContainer.addView(subtitle, fullWidthMargins(0, 0, 0, 14));

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.VERTICAL);
        metrics.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        firstRow.addView(smallStatCard("المتوسط اليومي",
                store.getSevenDayAverageMl() + " مل"), rowWeightMargins());
        firstRow.addView(smallStatCard("إجمالي الأسبوع",
                formatLiters(store.getSevenDayTotalMl())), rowWeightMargins());
        metrics.addView(firstRow);

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.addView(smallStatCard("أيام تحقيق الهدف",
                store.getGoalDaysInLastSeven() + " من 7"), rowWeightMargins());
        secondRow.addView(smallStatCard("السلسلة الحالية",
                store.getCurrentStreak() + " يوم"), rowWeightMargins());
        metrics.addView(secondRow);
        pageContainer.addView(metrics, fullWidthMargins(-4, 0, -4, 12));

        LinearLayout chartCard = card();
        TextView chartTitle = text("كمية الماء اليومية", 17, TEXT, true);
        chartTitle.setGravity(Gravity.START);
        chartCard.addView(chartTitle, fullWidthMargins(0, 0, 0, 6));
        WeeklyChartView chart = new WeeklyChartView(this, store.getRecentDays(7), store.getGoalMl());
        chartCard.addView(chart, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(230)));
        TextView chartNote = text("الخط الأزرق الفاتح يمثل هدفك اليومي.", 12, MUTED, false);
        chartNote.setGravity(Gravity.START);
        chartCard.addView(chartNote);
        pageContainer.addView(chartCard, fullWidthMargins(0, 0, 0, 12));

        LinearLayout insight = card();
        insight.setBackground(rounded(LIGHT_BLUE, 22, 0xFFC8EBFF, 1));
        TextView insightTitle = text("ملاحظة قطرة", 16, DARK_BLUE, true);
        insightTitle.setGravity(Gravity.START);
        insight.addView(insightTitle);
        TextView insightText = text(buildInsight(), 14, TEXT, false);
        insightText.setGravity(Gravity.START);
        insightText.setLineSpacing(0, 1.25f);
        insight.addView(insightText, fullWidthMargins(0, 6, 0, 0));
        pageContainer.addView(insight);
    }

    private String buildInsight() {
        int average = store.getSevenDayAverageMl();
        int goal = store.getGoalMl();
        if (average >= goal) {
            return "متوسطك الأسبوعي وصل إلى الهدف. حافظ على عادتك بدون المبالغة في الشرب.";
        }
        int difference = goal - average;
        return String.format(Locale.US,
                "للوصول إلى هدفك، حاول إضافة نحو %d مل إلى متوسطك اليومي على دفعات مريحة.",
                difference);
    }

    private void addWater(int amount) {
        store.addWater(amount);
        refreshHome(true);
        QatraWidgetProvider.updateAll(this);
        broadcastDataChanged();
        Toast.makeText(this,
                String.format(Locale.US, "تمت إضافة %d مل", amount), Toast.LENGTH_SHORT).show();
    }

    private void undoWater() {
        int removed = store.undoLast();
        if (removed <= 0) {
            Toast.makeText(this, "لا توجد إضافة للتراجع عنها", Toast.LENGTH_SHORT).show();
            return;
        }
        refreshHome(true);
        QatraWidgetProvider.updateAll(this);
        broadcastDataChanged();
        Toast.makeText(this,
                String.format(Locale.US, "تم التراجع عن %d مل", removed), Toast.LENGTH_SHORT).show();
    }

    private void showCustomAmountDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("مثال: 400");
        input.setGravity(Gravity.CENTER);
        input.setSelectAllOnFocus(true);
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setBackground(rounded(0xFFF7FCFF, 16, 0xFFC8E9FC, 1));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(dp(20), dp(8), dp(20), 0);
        wrapper.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("كمية مخصصة بالمل")
                .setView(wrapper)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إضافة", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int amount = parseInt(input.getText().toString(), 0);
                    if (amount < 1 || amount > QatraStore.MAX_SINGLE_ENTRY_ML) {
                        input.setError("أدخل كمية بين 1 و5000 مل");
                        return;
                    }
                    dialog.dismiss();
                    addWater(amount);
                }));
        dialog.getWindow();
        dialog.show();
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void showSettingsDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        form.setPadding(dp(22), dp(8), dp(22), dp(4));

        form.addView(formLabel("الهدف اليومي بالمل"));
        EditText goalInput = numericInput(String.valueOf(store.getGoalMl()), "2500");
        form.addView(goalInput, fullWidthMargins(0, 4, 0, 14));

        Switch reminderSwitch = new Switch(this);
        reminderSwitch.setText("تفعيل تذكيرات شرب الماء");
        reminderSwitch.setTextSize(15);
        reminderSwitch.setTextColor(TEXT);
        reminderSwitch.setChecked(store.isReminderEnabled());
        reminderSwitch.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        form.addView(reminderSwitch, fullWidthMargins(0, 0, 0, 14));

        form.addView(formLabel("التذكير كل"));
        Spinner intervalSpinner = new Spinner(this);
        String[] intervalLabels = {"ساعة", "ساعتين", "3 ساعات", "4 ساعات", "5 ساعات", "6 ساعات"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, intervalLabels);
        intervalSpinner.setAdapter(adapter);
        intervalSpinner.setSelection(store.getReminderIntervalHours() - 1);
        form.addView(intervalSpinner, fullWidthMargins(0, 2, 0, 14));

        LinearLayout hoursRow = new LinearLayout(this);
        hoursRow.setOrientation(LinearLayout.HORIZONTAL);
        hoursRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout startBox = new LinearLayout(this);
        startBox.setOrientation(LinearLayout.VERTICAL);
        startBox.addView(formLabel("بداية التذكير"));
        EditText startInput = numericInput(String.valueOf(store.getReminderStartHour()), "8");
        startBox.addView(startInput);
        hoursRow.addView(startBox, rowWeightMargins());

        LinearLayout endBox = new LinearLayout(this);
        endBox.setOrientation(LinearLayout.VERTICAL);
        endBox.addView(formLabel("نهاية التذكير"));
        EditText endInput = numericInput(String.valueOf(store.getReminderEndHour()), "22");
        endBox.addView(endInput);
        hoursRow.addView(endBox, rowWeightMargins());
        form.addView(hoursRow);

        ScrollView formScroll = new ScrollView(this);
        formScroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("إعدادات قطرة")
                .setView(formScroll)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int goal = parseInt(goalInput.getText().toString(), QatraStore.DEFAULT_GOAL_ML);
                    if (goal < QatraStore.MIN_GOAL_ML || goal > QatraStore.MAX_GOAL_ML) {
                        goalInput.setError("الهدف يجب أن يكون بين 500 و10000 مل");
                        return;
                    }
                    int start = parseInt(startInput.getText().toString(), 8);
                    int end = parseInt(endInput.getText().toString(), 22);
                    if (start < 0 || start > 23 || end < 0 || end > 23) {
                        Toast.makeText(this, "استخدم ساعات بين 0 و23", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    store.setGoalMl(goal);
                    store.setReminderEnabled(reminderSwitch.isChecked());
                    store.setReminderIntervalHours(intervalSpinner.getSelectedItemPosition() + 1);
                    store.setReminderStartHour(start);
                    store.setReminderEndHour(end);
                    ReminderScheduler.apply(this);
                    if (reminderSwitch.isChecked()) {
                        requestNotificationPermissionIfNeeded();
                    }
                    QatraWidgetProvider.updateAll(this);
                    broadcastDataChanged();
                    dialog.dismiss();
                    refreshCurrentPage(true);
                    Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }

    private EditText numericInput(String value, String hint) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(value);
        input.setHint(hint);
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setGravity(Gravity.CENTER);
        input.setSelectAllOnFocus(true);
        input.setBackground(rounded(0xFFF7FCFF, 14, 0xFFC8E9FC, 1));
        input.setPadding(dp(10), dp(9), dp(10), dp(9));
        return input;
    }

    private TextView formLabel(String label) {
        TextView view = text(label, 13, MUTED, true);
        view.setGravity(Gravity.START);
        return view;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ReminderScheduler.apply(this);
            } else {
                Toast.makeText(this,
                        "لن تظهر التذكيرات حتى تسمح بالإشعارات من إعدادات الهاتف.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshCurrentPage(boolean animate) {
        switch (currentPage) {
            case "history":
                showHistory();
                break;
            case "stats":
                showStats();
                break;
            default:
                if (dropView == null) {
                    showHome(animate);
                } else {
                    refreshHome(animate);
                }
                break;
        }
    }

    private void broadcastDataChanged() {
        sendBroadcast(new Intent(QatraWidgetProvider.ACTION_DATA_CHANGED)
                .setPackage(getPackageName()));
    }

    private void registerDataReceiver() {
        IntentFilter filter = new IntentFilter(QatraWidgetProvider.ACTION_DATA_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(dataChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dataChangedReceiver, filter);
        }
    }

    private void selectNavigation(TextView selected) {
        TextView[] items = {navHome, navHistory, navStats};
        for (TextView item : items) {
            if (item == null) {
                continue;
            }
            boolean active = item == selected;
            item.setTextColor(active ? DARK_BLUE : MUTED);
            item.setBackground(active ? rounded(LIGHT_BLUE, 16, 0, 0) : null);
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackground(rounded(Color.WHITE, 24, 0xFFE0F0F8, 1));
        card.setElevation(dp(4));
        return card;
    }

    private View smallStatCard(String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(14), dp(10), dp(14));
        card.setBackground(rounded(Color.WHITE, 20, 0xFFDDEFF9, 1));
        card.setElevation(dp(2));
        TextView labelView = text(label, 12, MUTED, false);
        labelView.setGravity(Gravity.CENTER);
        TextView valueView = text(value, 18, DARK_BLUE, true);
        valueView.setGravity(Gravity.CENTER);
        card.addView(labelView);
        card.addView(valueView, fullWidthMargins(0, 4, 0, 0));
        return card;
    }

    private LinearLayout.LayoutParams rowWeightMargins() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        return params;
    }

    private TextView sectionTitle(String title) {
        TextView view = text(title, 24, TEXT, true);
        view.setGravity(Gravity.START);
        return view;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.create("sans", Typeface.BOLD));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), dp(13), dp(12), dp(13));
        button.setBackground(rounded(BLUE, 18, 0, 0));
        button.setElevation(dp(3));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(DARK_BLUE);
        button.setTypeface(Typeface.create("sans", Typeface.BOLD));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setBackground(rounded(Color.WHITE, 18, 0xFFC8E9FC, 1));
        return button;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return view;
    }

    private LinearLayout.LayoutParams fullWidthMargins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0 && strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private String formatLiters(int ml) {
        if (ml < 1000) {
            return ml + " مل";
        }
        return String.format(Locale.US, "%.1f لتر", ml / 1000f);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
