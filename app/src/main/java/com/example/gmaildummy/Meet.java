package com.example.gmaildummy;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Meeting codes in Meet's abc-mnop-xyz form, and copying or sharing their links. */
final class Meet {
    private static final Random RANDOM = new Random();
    private static final Pattern CODE = Pattern.compile("([a-z]{3})-?([a-z]{4})-?([a-z]{3})");

    private Meet() { }

    static String newCode() {
        return letters(3) + "-" + letters(4) + "-" + letters(3);
    }

    private static String letters(int count) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < count; i++) s.append((char) ('a' + RANDOM.nextInt(26)));
        return s.toString();
    }

    static String link(String code) {
        return "meet.google.com/" + code;
    }

    /** A typed code or meeting link as abc-mnop-xyz, or null if it isn't one. */
    static String parse(String typed) {
        String s = typed.trim().toLowerCase(Locale.ROOT).replace(" ", "")
                .replaceFirst("^(https?://)?meet\\.google\\.com/", "");
        Matcher m = CODE.matcher(s);
        return m.matches() ? m.group(1) + "-" + m.group(2) + "-" + m.group(3) : null;
    }

    static void copy(Context context, String code) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Meeting link", "https://" + link(code)));
        // Android 13 and later confirm copies themselves.
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    static void share(Activity activity, String code) {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Join my meeting: https://" + link(code));
        activity.startActivity(Intent.createChooser(send, "Share invite"));
    }
}
