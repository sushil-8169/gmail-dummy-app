package com.example.gmaildummy;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Gmail-style settings, with the Light / Dark / System default theme under Personalisation. */
public class SettingsActivity extends BaseActivity {
    private static final int[] THEME_ORDER = { ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM };

    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_settings);
        SystemBars.padRoot(findViewById(R.id.root));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        list = findViewById(R.id.settings_list);

        section("Personalisation");
        LinearLayout personalisation = group();
        row(personalisation, "Theme", ThemeMode.label(ThemeMode.get(this)), v -> chooseTheme());
        row(personalisation, "Inbox type", "Default inbox", v -> toast("Inbox type"));
        row(personalisation, "Swipe actions", "Archive and delete", v -> toast("Swipe actions"));

        section("Account");
        LinearLayout account = group();
        row(account, MailStore.ME, "Manage your Google Account", v -> toast(MailStore.ME));

        section("About");
        LinearLayout about = group();
        row(about, "About Gmail", "Version 1.0", v -> toast("Gmail dummy app"));
    }

    private void chooseTheme() {
        int current = ThemeMode.get(this);
        int checked = 0;
        String[] labels = new String[THEME_ORDER.length];
        for (int i = 0; i < THEME_ORDER.length; i++) {
            labels[i] = ThemeMode.label(THEME_ORDER[i]);
            if (THEME_ORDER[i] == current) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("Theme")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (THEME_ORDER[which] != current) {
                        ThemeMode.set(this, THEME_ORDER[which]);
                        recreate();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void section(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(14);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.setTextColor(color(R.color.primary));
        header.setPadding(dp(16), dp(24), dp(16), dp(8));
        list.addView(header);
    }

    // Rows in a section are stacked cards with a small gap, like the inbox list.
    private LinearLayout group() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        list.addView(group);
        return group;
    }

    private void row(LinearLayout group, String title, String subtitle, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33808080), pill(color(R.color.card), 4), null));
        row.setOnClickListener(onClick);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(color(R.color.on_surface));
        row.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextSize(14);
        subtitleView.setTextColor(color(R.color.on_surface_variant));
        subtitleView.setPadding(0, dp(2), 0, 0);
        row.addView(subtitleView);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(3);
        group.addView(row, params);
    }
}
