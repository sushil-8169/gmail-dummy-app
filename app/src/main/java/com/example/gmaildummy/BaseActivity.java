package com.example.gmaildummy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Applies the chosen theme and edge-to-edge bars, and holds helpers shared by every screen. */
abstract class BaseActivity extends Activity {
    private int appliedTheme;

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(ThemeMode.wrap(base));
    }

    @Override protected void onCreate(Bundle state) {
        appliedTheme = ThemeMode.get(this);
        super.onCreate(state);
        SystemBars.edgeToEdge(this);
    }

    // Screens left in the back stack pick up a theme change made in Settings.
    @Override protected void onResume() {
        super.onResume();
        if (ThemeMode.get(this) != appliedTheme) recreate();
    }

    /** Pads {@code top} below the status bar and {@code bottom} above the navigation bar. */
    void padForSystemBars(View root, View top, View bottom) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            top.setPadding(top.getPaddingLeft(), insets.getSystemWindowInsetTop(), top.getPaddingRight(), top.getPaddingBottom());
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
    }

    /** Gmail's icon-only bottom bar with Mail selected. Returns the Mail item's unread badge. */
    TextView buildBottomNav(LinearLayout nav, Runnable onMail) {
        return buildBottomNav(nav, false, onMail);
    }

    /** Gmail's icon-only bottom bar: Mail (with unread badge) and Meet. Returns the badge. */
    TextView buildBottomNav(LinearLayout nav, boolean meetSelected, Runnable onMail) {
        TextView badge = addNavItem(nav, meetSelected ? R.drawable.ic_mail_outline : R.drawable.ic_mail, "Mail", !meetSelected, onMail);
        addNavItem(nav, meetSelected ? R.drawable.ic_videocam_filled : R.drawable.ic_videocam, "Meet", meetSelected,
                meetSelected ? () -> { } : this::openMeet);
        return badge;
    }

    /** Switches to the Meet tab without a transition, as a tab change should feel. */
    void openMeet() {
        startActivity(new Intent(this, MeetActivity.class));
        overridePendingTransition(0, 0);
    }

    private TextView addNavItem(LinearLayout nav, int icon, String label, boolean active, Runnable onClick) {
        View item = getLayoutInflater().inflate(R.layout.item_nav, nav, false);
        ImageView iconView = item.findViewById(R.id.nav_icon);
        iconView.setImageResource(icon);
        iconView.setImageTintList(ColorStateList.valueOf(color(active ? R.color.on_nav_indicator : R.color.on_surface)));
        if (active) item.findViewById(R.id.nav_indicator).setBackgroundResource(R.drawable.bg_nav_indicator);
        item.setContentDescription(label);
        item.setOnClickListener(v -> onClick.run());
        nav.addView(item);
        return item.findViewById(R.id.nav_badge);
    }

    void showUnreadBadge(TextView badge, int unread) {
        badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        badge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
    }

    int color(int res) {
        return getColor(res);
    }

    int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    GradientDrawable pill(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(radiusDp));
        d.setColor(color);
        return d;
    }

    static GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
