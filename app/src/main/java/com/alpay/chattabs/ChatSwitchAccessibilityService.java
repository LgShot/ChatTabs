package com.alpay.chattabs;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

public class ChatSwitchAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String activeTitle = "";
    private int stage = 0;
    private int scrollAttempts = 0;
    private long lastActionAt = 0L;
    private boolean coordinateMenuTapUsed = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        resetState("");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!"com.openai.chatgpt".contentEquals(event.getPackageName())) return;
        if (!SwitchRequestStore.hasFreshRequest(this)) return;

        String requested = SwitchRequestStore.title(this);
        if (!requested.equals(activeTitle)) resetState(requested);

        long now = System.currentTimeMillis();
        if (now - lastActionAt < 280L) return;
        lastActionAt = now;
        handler.postDelayed(this::attemptSwitch, 120L);
    }

    private void resetState(String title) {
        activeTitle = title == null ? "" : title;
        stage = 0;
        scrollAttempts = 0;
        coordinateMenuTapUsed = false;
        lastActionAt = 0L;
    }

    private void attemptSwitch() {
        if (!SwitchRequestStore.hasFreshRequest(this)) return;
        String target = SwitchRequestStore.title(this);
        if (target.isEmpty()) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        AccessibilityNodeInfo targetNode = findClickableTarget(root, target);
        if (targetNode != null && clickNodeOrParent(targetNode)) {
            complete();
            return;
        }

        long age = System.currentTimeMillis() - SwitchRequestStore.requestedAt(this);
        if (age > 15000L) {
            fail("Sohbet bulunamadı. ChatTabs'taki başlığın ChatGPT'deki sohbet başlığıyla aynı olduğundan emin ol.");
            return;
        }

        if (stage == 0) {
            AccessibilityNodeInfo search = findControl(root, new String[]{"search", "ara"}, true);
            if (search != null && clickNodeOrParent(search)) {
                stage = 2;
                return;
            }

            AccessibilityNodeInfo menu = findControl(root,
                    new String[]{"menu", "menü", "sidebar", "side bar", "navigation", "gezinme", "history", "geçmiş", "chats", "sohbetler"},
                    true);
            if (menu != null && clickNodeOrParent(menu)) {
                stage = 1;
                return;
            }

            if (!coordinateMenuTapUsed) {
                coordinateMenuTapUsed = true;
                stage = 1;
                tapTopLeft();
                return;
            }
        }

        if (stage == 1) {
            AccessibilityNodeInfo search = findControl(root, new String[]{"search", "ara"}, true);
            if (search != null && clickNodeOrParent(search)) {
                stage = 2;
                return;
            }

            AccessibilityNodeInfo scrollable = findScrollable(root);
            if (scrollable != null && scrollAttempts < 6) {
                scrollAttempts++;
                scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                return;
            }
        }

        if (stage == 2) {
            AccessibilityNodeInfo input = findEditable(root);
            if (input != null) {
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, target);
                if (input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                    stage = 3;
                    return;
                }
            }
        }

        if (stage == 3) {
            AccessibilityNodeInfo result = findClickableTarget(root, target);
            if (result != null && clickNodeOrParent(result)) complete();
        }
    }

    private AccessibilityNodeInfo findClickableTarget(AccessibilityNodeInfo root, String target) {
        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText(target);
        if (matches == null || matches.isEmpty()) return null;
        String wanted = norm(target);
        AccessibilityNodeInfo fallback = null;
        for (AccessibilityNodeInfo node : matches) {
            if (node == null) continue;
            String text = node.getText() == null ? "" : node.getText().toString();
            String desc = node.getContentDescription() == null ? "" : node.getContentDescription().toString();
            String value = !text.isEmpty() ? text : desc;
            String normalized = norm(value);
            if (normalized.equals(wanted) && hasClickableAncestor(node)) return node;
            if (fallback == null && normalized.contains(wanted) && hasClickableAncestor(node)) fallback = node;
        }
        return fallback;
    }

    private AccessibilityNodeInfo findControl(AccessibilityNodeInfo root, String[] keywords, boolean topBias) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        AccessibilityNodeInfo fallback = null;

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            String text = node.getText() == null ? "" : node.getText().toString();
            String desc = node.getContentDescription() == null ? "" : node.getContentDescription().toString();
            String combined = norm(text + " " + desc);
            boolean matches = false;
            for (String keyword : keywords) {
                if (combined.contains(norm(keyword))) {
                    matches = true;
                    break;
                }
            }
            if (matches && hasClickableAncestor(node)) {
                Rect r = new Rect();
                node.getBoundsInScreen(r);
                if (!topBias || (r.centerY() < screenHeight * 0.35f && r.centerX() < screenWidth * 0.65f)) return node;
                if (fallback == null) fallback = node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.addLast(child);
            }
        }
        return fallback;
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isEditable()) return node;
            String cls = node.getClassName() == null ? "" : node.getClassName().toString();
            if (cls.contains("EditText")) return node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.addLast(child);
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isScrollable()) return node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.addLast(child);
            }
        }
        return null;
    }

    private boolean hasClickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int i = 0; i < 6 && current != null; i++) {
            if (current.isClickable()) return true;
            current = current.getParent();
        }
        return false;
    }

    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int i = 0; i < 6 && current != null; i++) {
            if (current.isClickable() && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            current = current.getParent();
        }
        return false;
    }

    private void tapTopLeft() {
        float density = getResources().getDisplayMetrics().density;
        Path path = new Path();
        path.moveTo(28f * density, 58f * density);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 70))
                .build();
        dispatchGesture(gesture, null, null);
    }

    private String norm(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return s.replaceAll("\\s+", " ");
    }

    private void complete() {
        SwitchRequestStore.clear(this);
        resetState("");
    }

    private void fail(String message) {
        SwitchRequestStore.clear(this);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        resetState("");
    }

    @Override
    public void onInterrupt() {
    }
}
