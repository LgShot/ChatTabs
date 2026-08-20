package com.alpay.chattabs;

import org.json.JSONException;
import org.json.JSONObject;

public final class Conversation {
    public final long id;
    public final String title;
    public final String url;

    public Conversation(long id, String title, String url) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.url = url == null ? "" : url;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("url", url);
        return obj;
    }

    public static Conversation fromJson(JSONObject obj) throws JSONException {
        return new Conversation(
                obj.getLong("id"),
                obj.optString("title", "Chat"),
                obj.optString("url", "")
        );
    }
}
