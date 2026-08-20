package com.alpay.chattabs;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
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
    private TextView taskStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        int pad = dp(16);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.bg));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(18), pad, dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(text("ChatTabs 3", 30, true, R.color.text_primary));
        TextView subtitle = text("Multi-task testi: her slot kendi Android task'ında resmî ChatGPT'yi tutmaya çalışır.", 15, false, R.color.text_secondary);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        taskStatus = text("", 14, true, R.color.text_primary);
        taskStatus.setPadding(0, 0, 0, dp(8));
        root.addView(taskStatus);

        Button add = new Button(this);
        add.setText("+ Bağımsız ChatGPT slotu ekle");
        add.setAllCaps(false);
        add.setTextSize(16);
        add.setOnClickListener(v -> showAddDialog());
        root.addView(add, matchWrap());

        TextView how = text(
                "Kullanım: slota ilk kez bas → o task içinde ChatGPT açılır → istediğin sohbeti bir kez elle aç ve orada bırak. " +
                "Sonra başka slota geç. Aynı slota tekrar bastığında ChatTabs yeni pencere açmak yerine o task'ı öne getirir. " +
                "Bu sürüm sohbet adını aramaz ve Accessibility kullanmaz.",
                13, false, R.color.text_secondary);
        how.setPadding(0, dp(10), 0, dp(12));
        root.addView(how);

        Button reset = new Button(this);
        reset.setText("Açık ChatTabs tasklarını sıfırla");
        reset.setAllCaps(false);
        reset.setOnClickListener(v -> confirmResetTasks());
        root.addView(reset, matchWrap());

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap();
        listParams.setMargins(0, dp(14), 0, 0);
        root.addView(listContainer, listParams);

        TextView footer = text(
                "Ana ekrana ekle düğmesi her slot için ayrı ikon üretir. Widget'taki ilk 6 ve uygulama ikonuna uzun basınca çıkan ilk 4 kayıt da aynı task yönlendiricisini kullanır.",
                13, false, R.color.text_secondary);
        footer.setPadding(0, dp(14), 0, 0);
        root.addView(footer);

        setContentView(scroll);
    }

    private void refresh() {
        if (taskStatus != null) {
            int count = countSlotTasks();
            taskStatus.setText("Açık bağımsız slot taskı: " + count);
        }
        if (listContainer == null) return;
        listContainer.removeAllViews();
        List<Conversation> items = ConversationStore.load(this);
        ShortcutPublisher.publish(this, items);
        ChatTabsWidgetProvider.updateAll(this);

        if (items.isEmpty()) {
            TextView empty = text("Henüz slot yok. HTB, SOC, B1, Kültür gibi bir ad vererek ekle.", 15, false, R.color.text_secondary);
            empty.setPadding(0, dp(10), 0, dp(18));
            listContainer.addView(empty);
            return;
        }

        for (int i = 0; i < items.size(); i++) addSlotRow(items.get(i), i, items.size());
    }

    private void addSlotRow(Conversation item, int index, int total) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.panel_bg);
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.setMargins(0, 0, 0, dp(10));
        listContainer.addView(card, cardParams);

        card.addView(text(item.title, 17, true, R.color.text_primary));
        TextView detail = text("Slot #" + item.id + " · sohbet adı değil, bağımsız task etiketi", 12, false, R.color.text_secondary);
        detail.setPadding(0, dp(2), 0, dp(8));
        card.addView(detail);

        LinearLayout primary = new LinearLayout(this);
        primary.setOrientation(LinearLayout.HORIZONTAL);
        primary.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(primary, matchWrap());

        Button open = smallButton("Aç / geri dön");
        open.setOnClickListener(v -> openSlot(item));
        primary.addView(open);

        Button pin = smallButton("Ana ekrana ekle");
        pin.setOnClickListener(v -> PinShortcutPublisher.request(this, item));
        primary.addView(pin);

        LinearLayout secondary = new LinearLayout(this);
        secondary.setOrientation(LinearLayout.HORIZONTAL);
        secondary.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams secondParams = matchWrap();
        secondParams.setMargins(0, dp(6), 0, 0);
        card.addView(secondary, secondParams);

        Button up = smallButton("↑");
        up.setEnabled(index > 0);
        up.setOnClickListener(v -> { ConversationStore.move(this, index, index - 1); refresh(); });
        secondary.addView(up);

        Button down = smallButton("↓");
        down.setEnabled(index < total - 1);
        down.setOnClickListener(v -> { ConversationStore.move(this, index, index + 1); refresh(); });
        secondary.addView(down);

        Button edit = smallButton("Adı değiştir");
        edit.setOnClickListener(v -> showEditDialog(item, index));
        secondary.addView(edit);

        Button delete = smallButton("Sil");
        delete.setTextColor(getColor(R.color.danger));
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Slotu kaldır")
                .setMessage(item.title + " ChatTabs listesinden kaldırılsın mı? Açık task ayrıca kapatılmaz.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (d, w) -> { ConversationStore.delete(this, index); refresh(); })
                .show());
        secondary.addView(delete);
    }

    private void showAddDialog() {
        EditText input = new EditText(this);
        input.setHint("HTB, SOC, B1, Kültür…");
        input.setSingleLine(true);
        input.setPadding(dp(24), dp(8), dp(24), dp(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Bağımsız ChatGPT slotu")
                .setMessage("Bu yalnızca task etiketi. ChatGPT sohbetini ilk açılışta kendin seçip o task'ta bırakacaksın.")
                .setView(input)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Ekle", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = cleanTitle(input.getText().toString());
            if (name.isEmpty()) {
                toast("Slot için kısa bir ad yaz.");
                return;
            }
            ConversationStore.add(this, name, "");
            dialog.dismiss();
            refresh();
        }));
        dialog.show();
    }

    private void showEditDialog(Conversation item, int index) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(item.title);
        input.setSelection(input.length());
        input.setPadding(dp(24), dp(8), dp(24), dp(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Slot adını değiştir")
                .setView(input)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = cleanTitle(input.getText().toString());
            if (name.isEmpty()) return;
            List<Conversation> items = ConversationStore.load(this);
            if (index >= 0 && index < items.size()) {
                items.set(index, new Conversation(item.id, name, ""));
                ConversationStore.save(this, items);
                dialog.dismiss();
                refresh();
            }
        }));
        dialog.show();
    }

    private void openSlot(Conversation item) {
        Intent intent = new Intent(this, OpenConversationActivity.class)
                .setAction("MAIN_SLOT_" + item.id)
                .setData(Uri.parse(OpenConversationActivity.slotUri(item.id)))
                .putExtra(OpenConversationActivity.EXTRA_ID, item.id)
                .putExtra(OpenConversationActivity.EXTRA_TITLE, item.title);
        startActivity(intent);
    }

    private int countSlotTasks() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return 0;
        int count = 0;
        for (ActivityManager.AppTask task : manager.getAppTasks()) {
            try {
                Intent base = task.getTaskInfo().baseIntent;
                Uri data = base == null ? null : base.getData();
                if (data != null && "chattabs".equals(data.getScheme()) && "slot".equals(data.getHost())) count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    private void confirmResetTasks() {
        new AlertDialog.Builder(this)
                .setTitle("Slot tasklarını sıfırla")
                .setMessage("ChatTabs'in oluşturduğu bağımsız tasklar Recent Apps'tan kaldırılacak. Sohbetlerin ChatGPT hesabından silinmez.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sıfırla", (d, w) -> {
                    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    if (manager != null) {
                        for (ActivityManager.AppTask task : manager.getAppTasks()) {
                            try {
                                Intent base = task.getTaskInfo().baseIntent;
                                Uri data = base == null ? null : base.getData();
                                if (data != null && "chattabs".equals(data.getScheme()) && "slot".equals(data.getHost())) {
                                    task.finishAndRemoveTask();
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    refresh();
                })
                .show();
    }

    private String cleanTitle(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.length() > 40) t = t.substring(0, 40);
        return t;
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
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
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
