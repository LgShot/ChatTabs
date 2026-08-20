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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
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

        TextView title = text("ChatTabs", 30, true, R.color.text_primary);
        root.addView(title);

        TextView subtitle = text("Favori ChatGPT konuşmaların, telefonda sekme gibi tek dokunuş uzağında.", 15, false, R.color.text_secondary);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle);

        Button add = new Button(this);
        add.setText("+ Konuşma ekle");
        add.setAllCaps(false);
        add.setTextSize(16);
        add.setOnClickListener(v -> showAddDialog(null));
        root.addView(add, matchWrap());

        TextView hint = text("İpucu: ChatGPT'yi tarayıcıda aç → konuşmaya gir → adres çubuğundaki chatgpt.com/c/... linkini kopyala. Paylaşılan /share/ linklerini bilerek kabul etmiyoruz; onlar canlı sohbetin değil, paylaşım kopyasının bağlantısıdır.", 13, false, R.color.text_secondary);
        hint.setPadding(0, dp(12), 0, dp(14));
        root.addView(hint);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer, matchWrap());

        TextView footer = text("Widget: Ana ekrana uzun bas → Widget'lar → ChatTabs. İlk 6 konuşma tek satırda görünür. Uygulama ikonuna uzun basarsan ilk 4 konuşma da hızlı kısayol olarak çıkar. Linkler yalnızca telefonda saklanır; ChatTabs'ın internet izni yoktur.", 13, false, R.color.text_secondary);
        footer.setPadding(0, dp(16), 0, 0);
        root.addView(footer);

        setContentView(scroll);
    }

    private void refresh() {
        if (listContainer == null) return;
        listContainer.removeAllViews();
        List<Conversation> items = ConversationStore.load(this);
        ShortcutPublisher.publish(this, items);
        ChatTabsWidgetProvider.updateAll(this);

        if (items.isEmpty()) {
            TextView empty = text("Henüz konuşma yok. İlk konuşmayı eklediğinde widget otomatik güncellenir.", 15, false, R.color.text_secondary);
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

        TextView name = text(item.title, 17, true, R.color.text_primary);
        card.addView(name);

        TextView url = text(shortUrl(item.url), 12, false, R.color.text_secondary);
        url.setPadding(0, dp(2), 0, dp(8));
        card.addView(url);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(actions, matchWrap());

        Button open = smallButton("Aç");
        open.setOnClickListener(v -> openConversation(item.url));
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
                .setTitle("Konuşmayı kaldır")
                .setMessage(item.title + " kısayolu silinsin mi? ChatGPT konuşmasının kendisi silinmez.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (d, w) -> { ConversationStore.delete(this, index); refresh(); })
                .show());
        actions.addView(delete);
    }

    private void showEditDialog(Conversation item, int index) {
        LinearLayout box = dialogFields(item.title, item.url);
        EditText title = (EditText) box.getChildAt(0);
        EditText url = (EditText) box.getChildAt(1);
        new AlertDialog.Builder(this)
                .setTitle("Konuşmayı düzenle")
                .setView(box)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", null)
                .setOnShowListener(dialog -> ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String normalized = UrlTools.normalize(url.getText().toString());
                    if (!validateUrl(normalized)) return;
                    String name = cleanTitle(title.getText().toString());
                    List<Conversation> items = ConversationStore.load(this);
                    if (index >= 0 && index < items.size()) {
                        items.set(index, new Conversation(item.id, name, normalized));
                        ConversationStore.save(this, items);
                        ((AlertDialog) dialog).dismiss();
                        refresh();
                    }
                }))
                .show();
    }

    private void showAddDialog(String initialUrl) {
        String clipboard = initialUrl != null ? initialUrl : clipboardUrl();
        LinearLayout box = dialogFields("", UrlTools.isPrivateConversationUrl(clipboard) ? UrlTools.normalize(clipboard) : "");
        EditText title = (EditText) box.getChildAt(0);
        EditText url = (EditText) box.getChildAt(1);

        new AlertDialog.Builder(this)
                .setTitle("Konuşma ekle")
                .setMessage("Başlık kısa olsun; widget'ta ilk 6 kayıt gösterilir.")
                .setView(box)
                .setNegativeButton("Vazgeç", null)
                .setNeutralButton("Panodan yapıştır", null)
                .setPositiveButton("Ekle", null)
                .setOnShowListener(dialog -> {
                    AlertDialog d = (AlertDialog) dialog;
                    d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> url.setText(clipboardUrl()));
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String normalized = UrlTools.normalize(url.getText().toString());
                        if (!validateUrl(normalized)) return;
                        String name = cleanTitle(title.getText().toString());
                        ConversationStore.add(this, name, normalized);
                        d.dismiss();
                        refresh();
                    });
                })
                .show();
    }

    private LinearLayout dialogFields(String titleValue, String urlValue) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);

        EditText title = new EditText(this);
        title.setHint("Başlık: RSS, Sağlık, İngilizce…");
        title.setSingleLine(true);
        title.setText(titleValue);
        box.addView(title, matchWrap());

        EditText url = new EditText(this);
        url.setHint("https://chatgpt.com/c/...");
        url.setSingleLine(false);
        url.setMinLines(2);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(urlValue);
        box.addView(url, matchWrap());
        return box;
    }

    private boolean validateUrl(String url) {
        if (UrlTools.isSharedUrl(url)) {
            toast("Bu /share/ bağlantısı canlı sohbet değil. Tarayıcıdaki gerçek /c/ bağlantısını kopyala.");
            return false;
        }
        if (!UrlTools.isPrivateConversationUrl(url)) {
            toast("Geçerli bir ChatGPT konuşma linki gerekli: https://chatgpt.com/c/...");
            return false;
        }
        return true;
    }

    private String cleanTitle(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.isEmpty()) return "Chat";
        return t.length() > 40 ? t.substring(0, 40) : t;
    }

    private void handleIncomingShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        if (!"text/plain".equals(intent.getType())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String normalized = UrlTools.normalize(text);
        if (UrlTools.isSharedUrl(normalized)) {
            toast("ChatGPT paylaşım linki geldi; bu canlı sohbeti devam ettirmez. Gerçek /c/ linkini tarayıcıdan kopyala.");
        } else if (UrlTools.isPrivateConversationUrl(normalized)) {
            showAddDialog(normalized);
        }
    }

    private String clipboardUrl() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) return "";
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return "";
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        return text == null ? "" : text.toString();
    }

    private void openConversation(String url) {
        Intent intent = new Intent(this, OpenConversationActivity.class);
        intent.putExtra(OpenConversationActivity.EXTRA_URL, url);
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
