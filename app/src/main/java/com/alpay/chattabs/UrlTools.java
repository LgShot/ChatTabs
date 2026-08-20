package com.alpay.chattabs;

import android.net.Uri;

public final class UrlTools {
    private UrlTools() {}

    public static String normalize(String input) {
        if (input == null) return "";
        String value = input.trim();
        int start = value.indexOf("https://");
        if (start > 0) value = value.substring(start).trim();
        int whitespace = firstWhitespace(value);
        if (whitespace > 0) value = value.substring(0, whitespace);
        return value;
    }

    public static boolean isPrivateConversationUrl(String input) {
        try {
            Uri uri = Uri.parse(normalize(input));
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null) return false;
            boolean goodHost = host.equalsIgnoreCase("chatgpt.com") || host.equalsIgnoreCase("www.chatgpt.com") || host.equalsIgnoreCase("chat.openai.com");
            return goodHost && path.startsWith("/c/") && path.length() > 3;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSharedUrl(String input) {
        try {
            Uri uri = Uri.parse(normalize(input));
            String host = uri.getHost();
            String path = uri.getPath();
            return host != null && host.toLowerCase().contains("chatgpt.com") && path != null && path.startsWith("/share/");
        } catch (Exception e) {
            return false;
        }
    }

    private static int firstWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isWhitespace(s.charAt(i))) return i;
        return -1;
    }
}
