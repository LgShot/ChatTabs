package com.alpay.chattabs;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConversationStore {
    private static final String PREFS = "chat_tabs";
    private static final String KEY = "conversations";

    private ConversationStore() {}

    public static List<Conversation> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "[]");
        List<Conversation> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) result.add(Conversation.fromJson(obj));
            }
        } catch (Exception ignored) {
            // Corrupt local data should never make the launcher unusable.
        }
        return result;
    }

    public static void save(Context context, List<Conversation> items) {
        JSONArray array = new JSONArray();
        for (Conversation item : items) {
            try { array.put(item.toJson()); } catch (Exception ignored) {}
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, array.toString()).apply();
        ChatTabsWidgetProvider.updateAll(context);
        ShortcutPublisher.publish(context, items);
    }

    public static void add(Context context, String title, String url) {
        List<Conversation> items = load(context);
        items.add(new Conversation(System.currentTimeMillis(), title.trim(), url.trim()));
        save(context, items);
    }

    public static void delete(Context context, int index) {
        List<Conversation> items = load(context);
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            save(context, items);
        }
    }

    public static void move(Context context, int from, int to) {
        List<Conversation> items = load(context);
        if (from >= 0 && from < items.size() && to >= 0 && to < items.size()) {
            Collections.swap(items, from, to);
            save(context, items);
        }
    }
}
