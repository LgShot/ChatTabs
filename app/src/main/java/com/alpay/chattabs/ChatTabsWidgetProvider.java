package com.alpay.chattabs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

public class ChatTabsWidgetProvider extends AppWidgetProvider {
    private static final int[] IDS = {R.id.tab1, R.id.tab2, R.id.tab3, R.id.tab4, R.id.tab5, R.id.tab6};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) updateOne(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, ChatTabsWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int widgetId) {
        List<Conversation> items = ConversationStore.load(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_chat_tabs);

        for (int i = 0; i < IDS.length; i++) {
            int viewId = IDS[i];
            if (i < items.size()) {
                Conversation item = items.get(i);
                views.setViewVisibility(viewId, View.VISIBLE);
                views.setTextViewText(viewId, compact(item.title));
                Intent intent = new Intent(context, OpenConversationActivity.class)
                        .setAction("WIDGET_OPEN_" + widgetId + "_" + item.id)
                        .putExtra(OpenConversationActivity.EXTRA_TITLE, item.title)
                        .putExtra(OpenConversationActivity.EXTRA_URL, item.url);
                PendingIntent pending = PendingIntent.getActivity(
                        context,
                        (widgetId * 10) + i,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                views.setOnClickPendingIntent(viewId, pending);
            } else {
                views.setViewVisibility(viewId, View.GONE);
            }
        }

        if (items.isEmpty()) {
            views.setViewVisibility(R.id.tab1, View.VISIBLE);
            views.setTextViewText(R.id.tab1, "+ Chat ekle");
            Intent openApp = new Intent(context, MainActivity.class).setAction("OPEN_FROM_WIDGET");
            PendingIntent pending = PendingIntent.getActivity(context, widgetId * 10, openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.tab1, pending);
        }
        manager.updateAppWidget(widgetId, views);
    }

    private static String compact(String title) {
        String t = title == null ? "Chat" : title.trim();
        if (t.length() <= 12) return t;
        return t.substring(0, 11) + "…";
    }
}
