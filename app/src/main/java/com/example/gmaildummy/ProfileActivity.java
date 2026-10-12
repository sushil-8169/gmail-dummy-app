package com.example.gmaildummy;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The signed-in account's profile, opened from the account avatar. */
public class ProfileActivity extends BaseActivity {
    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_profile);
        SystemBars.padRoot(findViewById(R.id.root));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.profile_avatar)).setText(MailStore.ME_INITIAL);
        ((TextView) findViewById(R.id.profile_name)).setText(MailStore.ME_NAME);
        ((TextView) findViewById(R.id.profile_email)).setText(MailStore.ME);
        findViewById(R.id.btn_manage_account).setOnClickListener(v -> toast("Manage your Google Account"));

        list = findViewById(R.id.profile_list);
        section("Basic info");
        row("Name", MailStore.ME_NAME);
        row("Email", MailStore.ME);
    }

    private void section(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(14);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.setTextColor(color(R.color.primary));
        header.setPadding(dp(16), dp(8), dp(16), dp(8));
        list.addView(header);
    }

    private void row(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33808080), pill(color(R.color.card), 4), null));
        row.setOnClickListener(v -> toast(value));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setTextColor(color(R.color.on_surface_variant));
        row.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setTextColor(color(R.color.on_surface));
        valueView.setPadding(0, dp(2), 0, 0);
        row.addView(valueView);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(3);
        list.addView(row, params);
    }
}
