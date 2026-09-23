package com.example.gmaildummy;

import android.content.Context;
import android.content.res.Configuration;

/**
 * The Light / Dark / System default choice from Settings > Personalisation.
 * Forcing a mode swaps the context's night qualifier, so values-night resources apply.
 */
final class ThemeMode {
    static final int SYSTEM = 0, LIGHT = 1, DARK = 2;
    private static final String PREFS = "settings", KEY = "theme";

    private ThemeMode() { }

    static int get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, SYSTEM);
    }

    static void set(Context context, int mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, mode).apply();
    }

    static String label(int mode) {
        switch (mode) {
            case LIGHT: return "Light";
            case DARK: return "Dark";
            default: return "System default";
        }
    }

    static Context wrap(Context base) {
        int mode = get(base);
        if (mode == SYSTEM) return base;
        Configuration config = new Configuration(base.getResources().getConfiguration());
        int night = mode == DARK ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        return base.createConfigurationContext(config);
    }

    static boolean isDark(Context context) {
        int night = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }
}
