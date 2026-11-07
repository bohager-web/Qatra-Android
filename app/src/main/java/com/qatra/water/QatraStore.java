package com.qatra.water;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Single local source of truth shared by the activity, widget and reminder receiver.
 * No account, network connection or cloud storage is required.
 */
public final class QatraStore {
    private static final String PREFS = "qatra_store";
    private static final String KEY_GOAL = "daily_goal_ml";
    private static final String KEY_REMINDERS = "reminders_enabled";
    private static final String KEY_REMINDER_INTERVAL = "reminder_interval_hours";
    private static final String KEY_REMINDER_START = "reminder_start_hour";
    private static final String KEY_REMINDER_END = "reminder_end_hour";
    private static final String TOTAL_PREFIX = "total_";
    private static final String ENTRIES_PREFIX = "entries_";

    public static final int DEFAULT_GOAL_ML = 2500;
    public static final int MIN_GOAL_ML = 500;
    public static final int MAX_GOAL_ML = 10000;
    public static final int MAX_SINGLE_ENTRY_ML = 5000;

    private final SharedPreferences preferences;

    public QatraStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized int getGoalMl() {
        return clamp(preferences.getInt(KEY_GOAL, DEFAULT_GOAL_ML), MIN_GOAL_ML, MAX_GOAL_ML);
    }

    public synchronized void setGoalMl(int goalMl) {
        preferences.edit().putInt(KEY_GOAL, clamp(goalMl, MIN_GOAL_ML, MAX_GOAL_ML)).apply();
    }

    public synchronized int getCurrentTotalMl() {
        return getTotalMl(LocalDate.now());
    }

    public synchronized int getTotalMl(LocalDate date) {
        return Math.max(0, preferences.getInt(totalKey(date), 0));
    }

    public synchronized float getCurrentProgress() {
        int goal = getGoalMl();
        return goal <= 0 ? 0f : Math.min(1f, getCurrentTotalMl() / (float) goal);
    }

    public synchronized void addWater(int amountMl) {
        int safeAmount = clamp(amountMl, 1, MAX_SINGLE_ENTRY_ML);
        LocalDate today = LocalDate.now();
        List<Integer> entries = readEntries(today);
        entries.add(safeAmount);
        writeDay(today, entries);
    }

    public synchronized int undoLast() {
        LocalDate today = LocalDate.now();
        List<Integer> entries = readEntries(today);
        if (entries.isEmpty()) {
            return 0;
        }
        int removed = entries.remove(entries.size() - 1);
        writeDay(today, entries);
        return removed;
    }

    public synchronized boolean hasEntriesToday() {
        return !readEntries(LocalDate.now()).isEmpty();
    }

    public synchronized List<DayRecord> getRecentDays(int numberOfDays) {
        int count = Math.max(1, Math.min(numberOfDays, 90));
        List<DayRecord> result = new ArrayList<>(count);
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("ar"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ar"));
        for (int i = 0; i < count; i++) {
            LocalDate date = today.minusDays(i);
            result.add(new DayRecord(
                    date,
                    dayFormatter.format(date),
                    dateFormatter.format(date),
                    getTotalMl(date),
                    getGoalMl()
            ));
        }
        return result;
    }

    public synchronized int getSevenDayAverageMl() {
        int sum = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            sum += getTotalMl(today.minusDays(i));
        }
        return Math.round(sum / 7f);
    }

    public synchronized int getSevenDayTotalMl() {
        int sum = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            sum += getTotalMl(today.minusDays(i));
        }
        return sum;
    }

    public synchronized int getGoalDaysInLastSeven() {
        int goal = getGoalMl();
        int days = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            if (getTotalMl(today.minusDays(i)) >= goal) {
                days++;
            }
        }
        return days;
    }

    public synchronized int getCurrentStreak() {
        int goal = getGoalMl();
        LocalDate cursor = LocalDate.now();
        // The current day is still in progress, so a not-yet-complete today does not break the streak.
        if (getTotalMl(cursor) < goal) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (streak < 365 && getTotalMl(cursor) >= goal) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    public synchronized boolean isReminderEnabled() {
        return preferences.getBoolean(KEY_REMINDERS, false);
    }

    public synchronized void setReminderEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_REMINDERS, enabled).apply();
    }

    public synchronized int getReminderIntervalHours() {
        return clamp(preferences.getInt(KEY_REMINDER_INTERVAL, 2), 1, 6);
    }

    public synchronized void setReminderIntervalHours(int hours) {
        preferences.edit().putInt(KEY_REMINDER_INTERVAL, clamp(hours, 1, 6)).apply();
    }

    public synchronized int getReminderStartHour() {
        return clamp(preferences.getInt(KEY_REMINDER_START, 8), 0, 23);
    }

    public synchronized void setReminderStartHour(int hour) {
        preferences.edit().putInt(KEY_REMINDER_START, clamp(hour, 0, 23)).apply();
    }

    public synchronized int getReminderEndHour() {
        return clamp(preferences.getInt(KEY_REMINDER_END, 22), 0, 23);
    }

    public synchronized void setReminderEndHour(int hour) {
        preferences.edit().putInt(KEY_REMINDER_END, clamp(hour, 0, 23)).apply();
    }

    private void writeDay(LocalDate date, List<Integer> entries) {
        int total = 0;
        StringBuilder serialized = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            int value = clamp(entries.get(i), 1, MAX_SINGLE_ENTRY_ML);
            total += value;
            if (i > 0) {
                serialized.append(',');
            }
            serialized.append(value);
        }
        preferences.edit()
                .putInt(totalKey(date), total)
                .putString(entriesKey(date), serialized.toString())
                .apply();
    }

    private List<Integer> readEntries(LocalDate date) {
        String raw = preferences.getString(entriesKey(date), "");
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> entries = new ArrayList<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value > 0 && value <= MAX_SINGLE_ENTRY_ML) {
                    entries.add(value);
                }
            } catch (NumberFormatException ignored) {
                // Ignore a malformed legacy value rather than losing the rest of the day's data.
            }
        }
        return entries;
    }

    private static String totalKey(LocalDate date) {
        return TOTAL_PREFIX + date;
    }

    private static String entriesKey(LocalDate date) {
        return ENTRIES_PREFIX + date;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class DayRecord {
        public final LocalDate date;
        public final String dayName;
        public final String displayDate;
        public final int amountMl;
        public final int goalMl;

        DayRecord(LocalDate date, String dayName, String displayDate, int amountMl, int goalMl) {
            this.date = date;
            this.dayName = dayName;
            this.displayDate = displayDate;
            this.amountMl = amountMl;
            this.goalMl = goalMl;
        }

        public int percentage() {
            if (goalMl <= 0) {
                return 0;
            }
            return Math.min(100, Math.round(amountMl * 100f / goalMl));
        }
    }
}
