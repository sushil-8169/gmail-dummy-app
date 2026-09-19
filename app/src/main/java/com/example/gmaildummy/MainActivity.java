package com.example.gmaildummy;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(new InboxView());
    }

    private class InboxView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Mail> mails = new ArrayList<>();
        private boolean drawerOpen = false, searchOpen = false;
        private int selected = -1;
        private float downX, downY;
        private final int blue = Color.rgb(26, 115, 232), text = Color.rgb(32, 33, 36);

        InboxView() {
            super(MainActivity.this);
            p.setTypeface(android.graphics.Typeface.create("sans", 0));
            mails.addAll(Arrays.asList(
                new Mail("Google", "Your Google Account is ready to use", "Welcome to your new Google Account. Here are a few tips to help you get started.", "10:42 AM", "G", 0xffdb4437, true),
                new Mail("Design team", "Q4 product launch ✨", "The latest mockups are ready for review. Let us know what you think!", "9:18 AM", "D", 0xff0f9d58, true),
                new Mail("Priya Sharma", "Weekend plans", "Are we still on for brunch this Saturday? I found a lovely new place downtown.", "8:05 AM", "P", 0xffab47bc, true),
                new Mail("Google Photos", "Your memories from this week", "Take a look back at the moments you captured this week.", "Yesterday", "G", 0xff4285f4, false),
                new Mail("Medium Daily Digest", "Stories you might enjoy", "The latest ideas and perspectives from writers you follow.", "Yesterday", "M", 0xfff4b400, false),
                new Mail("Alex Johnson", "Re: Project timeline", "Thanks for the update. I’ll share the revised timeline this afternoon.", "Sep 17", "A", 0xff00acc1, false),
                new Mail("Spotify", "Made For You", "A fresh playlist picked just for you is waiting.", "Sep 16", "S", 0xff1db954, false)
            ));
            setBackgroundColor(Color.WHITE);
        }

        private void setup(float size, int color, boolean bold) {
            p.setTextSize(size); p.setColor(color);
            p.setTypeface(android.graphics.Typeface.create("sans", bold ? 1 : 0));
        }
        private void rounded(Canvas c, float l, float t, float r, float b, float radius, int color) {
            p.setColor(color); c.drawRoundRect(new RectF(l, t, r, b), radius, radius, p);
        }
        private void line(Canvas c, float x1, float y1, float x2, float y2, int color) {
            p.setColor(color); p.setStrokeWidth(1); c.drawLine(x1, y1, x2, y2, p);
        }
        private void icon(Canvas c, String value, float x, float y, float size, int color) {
            setup(size, color, false); c.drawText(value, x, y, p);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), scale = w / 390f;
            c.save(); c.scale(scale, scale);
            drawTopBar(c);
            drawContent(c);
            if (drawerOpen) drawDrawer(c);
            c.restore();
        }

        private void drawTopBar(Canvas c) {
            if (searchOpen) {
                rounded(c, 12, 10, 378, 58, 28, 0xfff1f3f4);
                icon(c, "‹", 27, 43, 34, 0xff5f6368);
                setup(16, text, false); c.drawText("Search mail", 62, 40, p);
                icon(c, "⋮", 352, 43, 25, 0xff5f6368);
            } else {
                rounded(c, 12, 10, 378, 58, 28, 0xfff1f3f4);
                icon(c, "☰", 28, 42, 22, 0xff5f6368);
                setup(16, 0xff5f6368, false); c.drawText("Search in mail", 62, 40, p);
                icon(c, "⌕", 322, 42, 26, 0xff5f6368);
                rounded(c, 346, 19, 370, 43, 12, 0xff7e57c2);
                setup(13, Color.WHITE, true); c.drawText("A", 354, 36, p);
            }
        }

        private void drawContent(Canvas c) {
            setup(22, text, false); c.drawText("Inbox", 16, 91, p);
            setup(13, 0xff5f6368, false); c.drawText("12,845", 76, 91, p);
            icon(c, "⌄", 348, 91, 20, 0xff5f6368);
            rounded(c, 12, 105, 378, 139, 17, 0xffe8f0fe);
            icon(c, "✉", 25, 128, 16, blue);
            setup(13, blue, true); c.drawText("Primary", 52, 127, p);
            setup(12, 0xff5f6368, false); c.drawText("Updates", 162, 127, p); c.drawText("Promotions", 258, 127, p);
            line(c, 12, 139, 378, 139, 0xffdadce0);
            float y = 139;
            for (int i = 0; i < mails.size(); i++) {
                drawMail(c, mails.get(i), y, i == selected);
                y += 72;
            }
            rounded(c, 294, 638, 378, 694, 18, 0xffd3e3fd);
            icon(c, "✎", 313, 674, 24, blue);
            setup(14, blue, true); c.drawText("Compose", 342, 672, p);
            line(c, 0, 710, 390, 710, 0xffdadce0);
            icon(c, "▣", 73, 739, 22, blue);
            icon(c, "▤", 187, 739, 22, 0xff5f6368);
            icon(c, "◉", 300, 739, 22, 0xff5f6368);
            setup(11, 0xff5f6368, false); c.drawText("Mail", 67, 758, p); c.drawText("Meet", 181, 758, p); c.drawText("Spaces", 290, 758, p);
        }

        private void drawMail(Canvas c, Mail mail, float y, boolean active) {
            if (active) rounded(c, 4, y + 2, 386, y + 70, 4, 0xfff1f3f4);
            rounded(c, 16, y + 16, 48, y + 48, 16, mail.color);
            setup(15, Color.WHITE, true); c.drawText(mail.initial, 27, y + 37, p);
            setup(14, mail.unread ? text : 0xff5f6368, mail.unread);
            c.drawText(mail.sender, 62, y + 28, p);
            setup(12, 0xff5f6368, false); c.drawText(mail.subject, 62, y + 47, p);
            setup(11, 0xff5f6368, false); c.drawText(mail.preview, 62, y + 63, p);
            setup(11, 0xff5f6368, mail.unread); c.drawText(mail.time, 330, y + 28, p);
            icon(c, "☆", 350, y + 56, 22, 0xff5f6368);
            line(c, 62, y + 71, 378, y + 71, 0xfff1f3f4);
        }

        private void drawDrawer(Canvas c) {
            p.setColor(0x55000000); c.drawRect(280, 0, 390, 780, p);
            rounded(c, 0, 0, 310, 780, 0, Color.WHITE);
            setup(22, 0xff5f6368, false); c.drawText("Gmail", 25, 48, p);
            setup(13, 0xff5f6368, false); c.drawText("A  alex.johnson@gmail.com", 25, 81, p);
            line(c, 0, 98, 310, 98, 0xffdadce0);
            drawerRow(c, "▣", "All inboxes", 118, false);
            drawerRow(c, "✉", "Inbox", 164, true);
            drawerRow(c, "★", "Starred", 210, false);
            drawerRow(c, "◷", "Snoozed", 256, false);
            drawerRow(c, "➤", "Sent", 302, false);
            drawerRow(c, "▱", "Drafts", 348, false);
            line(c, 0, 375, 310, 375, 0xffdadce0);
            setup(13, 0xff5f6368, true); c.drawText("Labels", 25, 407, p);
            drawerRow(c, "●", "Work", 441, false);
            drawerRow(c, "●", "Personal", 487, false);
            drawerRow(c, "⚙", "Settings", 548, false);
            drawerRow(c, "?", "Help & feedback", 594, false);
        }

        private void drawerRow(Canvas c, String symbol, String label, float y, boolean selectedRow) {
            if (selectedRow) rounded(c, 8, y - 24, 300, y + 11, 18, 0xffd3e3fd);
            icon(c, symbol, 26, y, 18, selectedRow ? blue : 0xff5f6368);
            setup(14, selectedRow ? text : 0xff3c4043, selectedRow); c.drawText(label, 62, y, p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float s = getWidth() / 390f, x = e.getX() / s, y = e.getY() / s;
            if (e.getAction() == MotionEvent.ACTION_DOWN) { downX = x; downY = y; return true; }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (drawerOpen) { if (x > 310) drawerOpen = false; else if (y > 100 && y < 370) drawerOpen = false; invalidate(); return true; }
                if (downY < 70 && downX < 55) drawerOpen = true;
                else if (downY < 70 && downX > 280) searchOpen = true;
                else if (downY > 638 && downY < 700) Toast.makeText(MainActivity.this, "Compose a new message", Toast.LENGTH_SHORT).show();
                else if (downY > 139 && downY < 643) {
                    selected = Math.max(0, Math.min(mails.size() - 1, (int)((downY - 139) / 72)));
                    mails.get(selected).unread = false;
                }
                invalidate(); return true;
            }
            return true;
        }

        private class Mail {
            String sender, subject, preview, time, initial; int color; boolean unread;
            Mail(String a, String b, String c, String d, String e, int f, boolean g) {
                sender=a; subject=b; preview=c; time=d; initial=e; color=f; unread=g;
            }
        }
    }
}
