package com.alpay.chattabs;

import android.content.Context;
import android.content.SharedPreferences;

public final class SwitchRequestStore {
    private static final String PREFS = "native_switch";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";
    private static final String KEY_TIME = "time";

    private SwitchRequestStore() {}

    public static void request(Context context, String title, String url) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TITLE, title == null ? "" : title.trim())
                .putString(KEY_URL, url == null ? "" : url.trim())
                .putLong(KEY_TIME, System.currentTimeMillis())
                .apply();
    }

    public static String title(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TITLE, "");
    }

    public static long requestedAt(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_TIME, 0L);
    }

    public static boolean hasFreshRequest(Context context) {
        String title = title(context);
        long age = System.currentTimeMillis() - requestedAt(context);
        return !title.isEmpty() && age >= 0 && age < 20000L;
    }

    public static void clear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TITLE).remove(KEY_URL).remove(KEY_TIME).apply();
    }
}
