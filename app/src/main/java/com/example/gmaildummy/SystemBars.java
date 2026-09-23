package com.example.gmaildummy;

import android.app.Activity;
import android.os.Build;
import android.view.View;

/** Draws behind the status and navigation bars (enforced from Android 15) with dark bar icons. */
final class SystemBars {
    private SystemBars() { }

    static void edgeToEdge(Activity activity) {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 27) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    /** Pads {@code root} so its content clears the system bars (and the keyboard with adjustResize). */
    static void padRoot(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
    }
}
