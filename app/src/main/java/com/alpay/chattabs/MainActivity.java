package com.alpay.chattabs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout listContainer;
    private TextView nativeStatus;
    private int pad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pad = dp(16);
        buildUi();
        handleIncomingShare(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingShare(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.bg));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(18), pad, dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(text("ChatTabs", 30, true, R.color.text_primary));
        TextView subtitle = text("Resmî ChatGPT Android uygulaması için native konuşma değiştirici.", 15, false, R.color.text_secondary);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        nativeStatus = text("", 14, true, R.color.text_primary);
        nativeStatus.setPadding(0, dp(4), 0, dp(6));
        root.addView(nativeStatus);

        Button access = new Button(this);
        access.setText("Native Switch erişimini aç");
        access.setAllCaps(false);
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access, matchWrap());

        TextView accessInfo = text("Bu izin yalnızca com.openai.chatgpt içinde kayıtlı sohbet başlığını bulup dokunmak için kullanılır. Tarayıcı açılmaz ve ChatTabs mesaj içeriğini kaydetmez.", 13, false, R.color.text_secondary);
        accessInfo.setPadding(0, dp(6), 0, dp(14));
        root.addView(accessInfo);

        Button add = new Button(this);
        add.setText("+ Konuşma ekle");
        add.setAllCaps(false);
        add.setTextSize(16);
        add.setOnClickListener(v -> showAddDialog(null));
        root.addView(add, matchWrap());

        TextView hint = text("Konuşmanın ChatGPT'deki başlığını yazman yeterli. /c/ linkini biliyorsan isteğe bağlı ekleyebilirsin; önce native deep-link denenir, gerekirse Native Switch başlıktan seçer.", 13, false, R.color.text_secondary);
        hint.setPadding(0, dp(12), 0, dp(14));
        root.addView(hint);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer, matchWrap());

        TextView footer = text("Widget'ta ilk 6, ChatTabs ikonuna uzun basınca ilk 4 konuşma görünür. Hepsi resmî ChatGPT Android uygulamasına gider; web fallback yoktur.", 13, false, R.color.text_secondary);
        footer.setPadding(0, dp(16), 0, 0);
        root.addView(footer);

        setContentView(scroll);
    }

    private void refresh() {
        if (nativeStatus != null) {
            boolean enabled = OpenConversationActivity.isNativeSwitchEnabled(this);
            nativeStatus.setText(enabled ? "Native Switch: AÇIK" : "Native Switch: KAPALI — bir kez etkinleştir");
            nativeStatus.setTextColor(getColor(enabled ? R.color.text_primary : R.color.danger));
        }
        if (listContainer == null) return;
        listContainer.removeAllViews();
        List<Conversation> items = ConversationStore.load(this);
        ShortcutPublisher.publish(this, items);
        ChatTabsWidgetProvider.updateAll(this);

        if (items.isEmpty()) {
            TextView empty = text("Henüz konuşma yok. İlk favorinin ChatGPT'deki başlığını ekle.", 15, false, R.color.text_secondary);
            empty.setPadding(0, dp(18), 0, dp(18));
            listContainer.addView(empty);
            return;
        }

        for (int i = 0; i < items.size(); i++) addConversationRow(items.get(i), i, items.size());
    }

    private void addConversationRow(Conversation item, int index, int total) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.panel_bg);
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.setMargins(0, 0, 0, dp(10));
        listContainer.addView(card, cardParams);

        card.addView(text(item.title, 17, true, R.color.text_primary));

        String secondary = item.url.isEmpty() ? "Başlıktan native seçim" : shortUrl(item.url) + " · native link + seçim";
        TextView detail = text(secondary, 12, false, R.color.text_secondary);
        detail.setPadding(0, dp(2), 0, dp(8));
        card.addView(detail);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(actions, matchWrap());

        Button open = smallButton("Aç");
        open.setOnClickListener(v -> openConversation(item));
        actions.addView(open);

        Button up = smallButton("↑");
        up.setEnabled(index > 0);
        up.setOnClickListener(v -> { ConversationStore.move(this, index, index - 1); refresh(); });
        actions.addView(up);

        Button down = smallButton("↓");
        down.setEnabled(index < total - 1);
        down.setOnClickListener(v -> { ConversationStore.move(this, index, index + 1); refresh(); });
        actions.addView(down);

        Button edit = smallButton("Düzenle");
        edit.setOnClickListener(v -> showEditDialog(item, index));
        actions.addView(edit);

        Button delete = smallButton("Sil");
        delete.setTextColor(getColor(R.color.danger));
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Kısayolu kaldır")
                .setMessage(item.title + " ChatTabs'tan kaldırılsın mı? ChatGPT konuşması silinmez.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (d, w) -> { ConversationStore.delete(this, index); refresh(); })
                .show());
        actions.addView(delete);
    }

    private void showEditDialog(Conversation item, int index) {
        LinearLayout box = dialogFields(item.title, item.url);
        EditText title = (EditText) box.getChildAt(0);
        EditText url = (EditText) box.getChildAt(1);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Konuşmayı düzenle")
                .setView(box)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = cleanTitle(title.getText().toString());
            if (name.isEmpty()) { toast("ChatGPT'deki sohbet başlığını yaz."); return; }
            String normalized = normalizeOptionalUrl(url.getText().toString());
            if (normalized == null) return;
            List<Conversation> items = ConversationStore.load(this);
            if (index >= 0 && index < items.size()) {
                items.set(index, new Conversation(item.id, name, normalized));
                ConversationStore.save(this, items);
                dialog.dismiss();
                refresh();
            }
        }));
        dialog.show();
    }

    private void showAddDialog(String initialUrl) {
        String clipboard = initialUrl != null ? initialUrl : clipboardUrl();
        String initial = UrlTools.isPrivateConversationUrl(clipboard) ? UrlTools.normalize(clipboard) : "";
        LinearLayout box = dialogFields("", initial);
        EditText title = (EditText) box.getChildAt(0);
        EditText url = (EditText) box.getChildAt(1);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Konuşma ekle")
                .setMessage("ChatGPT'de görünen sohbet başlığını aynen yaz. Link isteğe bağlıdır.")
                .setView(box)
                .setNegativeButton("Vazgeç", null)
                .setNeutralButton("Linki panodan al", null)
                .setPositiveButton("Ekle", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> url.setText(clipboardUrl()));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = cleanTitle(title.getText().toString());
                if (name.isEmpty()) { toast("ChatGPT'deki sohbet başlığını yaz."); return; }
                String normalized = normalizeOptionalUrl(url.getText().toString());
                if (normalized == null) return;
                ConversationStore.add(this, name, normalized);
                dialog.dismiss();
                refresh();
            });
        });
        dialog.show();
    }

    private LinearLayout dialogFields(String titleValue, String urlValue) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);

        EditText title = new EditText(this);
        title.setHint("ChatGPT sohbet başlığı");
        title.setSingleLine(true);
        title.setText(titleValue);
        box.addView(title, matchWrap());

        EditText url = new EditText(this);
        url.setHint("İsteğe bağlı: https://chatgpt.com/c/...");
        url.setSingleLine(false);
        url.setMinLines(2);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(urlValue);
        box.addView(url, matchWrap());
        return box;
    }

    private String normalizeOptionalUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "";
        String normalized = UrlTools.normalize(value);
        if (UrlTools.isSharedUrl(normalized)) {
            toast("/share/ linki canlı sohbet bağlantısı değildir. Bu alanı boş bırakabilirsin.");
            return null;
        }
        if (!UrlTools.isPrivateConversationUrl(normalized)) {
            toast("Link kullanacaksan https://chatgpt.com/c/... biçiminde olmalı; yoksa alanı boş bırak.");
            return null;
        }
        return normalized;
    }

    private String cleanTitle(String raw) {
        String t = raw == null ? "" : raw.trim();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }

    private void handleIncomingShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        if (!"text/plain".equals(intent.getType())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String normalized = UrlTools.normalize(text);
        if (UrlTools.isPrivateConversationUrl(normalized)) showAddDialog(normalized);
    }

    private String clipboardUrl() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) return "";
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return "";
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        return text == null ? "" : text.toString();
    }

    private void openConversation(Conversation item) {
        Intent intent = new Intent(this, OpenConversationActivity.class);
        intent.putExtra(OpenConversationActivity.EXTRA_TITLE, item.title);
        intent.putExtra(OpenConversationActivity.EXTRA_URL, item.url);
        startActivity(intent);
    }

    private String shortUrl(String raw) {
        try {
            Uri u = Uri.parse(raw);
            String path = u.getPath();
            if (path == null) return raw;
            return "chatgpt.com" + path;
        } catch (Exception e) { return raw; }
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(getColor(color));
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(9), dp(4), dp(9), dp(4));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        p.setMargins(0, 0, dp(4), 0);
        b.setLayoutParams(p);
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
