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
            float w = getWidth(), h = getHeight();
            float scale = Math.min(w / 390f, h / 780f);
            c.save(); c.scale(scale, scale);
            c.clipRect(0, 0, 390, 780);
            drawTopBar(c);
            drawContent(c, h / scale);
            if (drawerOpen) drawDrawer(c, h / scale);
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
                drawHamburger(c, 36, 34, 0xff5f6368);
                setup(16, 0xff5f6368, false); c.drawText("Search in mail", 62, 40, p);
                drawSearch(c, 330, 33, 0xff5f6368);
                rounded(c, 346, 19, 370, 43, 12, 0xff7e57c2);
                setup(13, Color.WHITE, true); c.drawText("A", 354, 36, p);
            }
        }

        private void drawHamburger(Canvas c, float x, float y, int color) {
                p.setColor(color); p.setStrokeWidth(2); p.setStrokeCap(Paint.Cap.ROUND);
                c.drawLine(x - 8, y - 6, x + 8, y - 6, p);
                c.drawLine(x - 8, y, x + 8, y, p);
                c.drawLine(x - 8, y + 6, x + 8, y + 6, p);
            }

        private void drawSearch(Canvas c, float x, float y, int color) {
                p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.2f);
                c.drawCircle(x - 2, y - 2, 7, p); c.drawLine(x + 3, y + 3, x + 9, y + 9, p);
                p.setStyle(Paint.Style.FILL);
        }

        private void drawContent(Canvas c, float viewportHeight) {
            setup(22, text, false); c.drawText("Inbox", 16, 91, p);
            setup(13, 0xff5f6368, false); c.drawText("12,845", 76, 91, p);
            icon(c, "⌄", 348, 91, 20, 0xff5f6368);
            rounded(c, 12, 105, 378, 139, 17, 0xffe8f0fe);
            icon(c, "✉", 25, 128, 16, blue);
            setup(13, blue, true); c.drawText("Primary", 52, 127, p);
            setup(12, 0xff5f6368, false); c.drawText("Updates", 162, 127, p); c.drawText("Promotions", 258, 127, p);
            line(c, 12, 139, 378, 139, 0xffdadce0);
            float y = 139;
            float bottomNav = Math.min(780, viewportHeight);
            float fabBottom = bottomNav - 74;
            float listBottom = fabBottom - 12;
            for (int i = 0; i < mails.size(); i++) {
                if (y + 72 > listBottom) break;
                drawMail(c, mails.get(i), y, i == selected);
                y += 72;
            }
            rounded(c, 294, fabBottom - 56, 378, fabBottom, 18, 0xffd3e3fd);
            drawCompose(c, 318, fabBottom - 28);
            setup(14, blue, true); c.drawText("Compose", 342, fabBottom - 22, p);
            line(c, 0, bottomNav - 64, 390, bottomNav - 64, 0xffdadce0);
            drawInbox(c, 82, bottomNav - 30, blue);
            drawMeet(c, 195, bottomNav - 30, 0xff5f6368);
            drawChat(c, 307, bottomNav - 30, 0xff5f6368);
            setup(11, 0xff5f6368, false); c.drawText("Mail", 67, bottomNav - 10, p); c.drawText("Meet", 181, bottomNav - 10, p); c.drawText("Spaces", 290, bottomNav - 10, p);
        }

        private void drawCompose(Canvas c, float x, float y) {
            p.setColor(blue); p.setStrokeWidth(2.2f); p.setStyle(Paint.Style.STROKE);
            c.drawLine(x - 7, y + 6, x + 5, y - 6, p);
            c.drawLine(x - 8, y + 8, x - 3, y + 7, p);
            c.drawLine(x + 5, y - 6, x + 8, y - 3, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawInbox(Canvas c, float x, float y, int color) {
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            c.drawRoundRect(new RectF(x - 10, y - 7, x + 10, y + 7), 3, 3, p);
            c.drawLine(x - 9, y - 1, x - 3, y + 4, p); c.drawLine(x - 3, y + 4, x + 3, y - 2, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawMeet(Canvas c, float x, float y, int color) {
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            c.drawRoundRect(new RectF(x - 9, y - 7, x + 4, y + 7), 3, 3, p);
            c.drawLine(x + 4, y - 4, x + 10, y - 7, p); c.drawLine(x + 10, y - 7, x + 10, y + 7, p); c.drawLine(x + 10, y + 7, x + 4, y + 4, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawChat(Canvas c, float x, float y, int color) {
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            c.drawRoundRect(new RectF(x - 10, y - 8, x + 10, y + 6), 4, 4, p);
            c.drawLine(x - 5, y + 6, x - 8, y + 11, p); c.drawLine(x - 8, y + 11, x - 1, y + 6, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawMail(Canvas c, Mail mail, float y, boolean active) {
            if (active) rounded(c, 4, y + 2, 386, y + 70, 4, 0xfff1f3f4);
            rounded(c, 16, y + 16, 48, y + 48, 16, mail.color);
            setup(15, Color.WHITE, true); c.drawText(mail.initial, 27, y + 37, p);
            float left = 62, right = 378, starLeft = 346;
            c.save();
            c.clipRect(left, y + 8, starLeft - 4, y + 68);
            setup(14, mail.unread ? text : 0xff5f6368, mail.unread);
            c.drawText(ellipsize(mail.sender, starLeft - left), left, y + 28, p);
            setup(12, mail.unread ? text : 0xff5f6368, mail.unread);
            c.drawText(ellipsize(mail.subject, starLeft - left), left, y + 47, p);
            setup(11, 0xff5f6368, false);
            c.drawText(ellipsize(mail.preview, starLeft - left), left, y + 63, p);
            c.restore();
            setup(11, 0xff5f6368, mail.unread);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(mail.time, right - 2, y + 28, p);
            p.setTextAlign(Paint.Align.LEFT);
            drawStar(c, 359, y + 53, 0xff5f6368);
            line(c, 62, y + 71, 378, y + 71, 0xfff1f3f4);
        }

        private String ellipsize(String value, float maxWidth) {
            if (p.measureText(value) <= maxWidth) return value;
            String suffix = "…";
            int end = value.length();
            while (end > 0 && p.measureText(value.substring(0, end) + suffix) > maxWidth) end--;
            return end == 0 ? suffix : value.substring(0, end) + suffix;
        }

        private void drawStar(Canvas c, float x, float y, int color) {
            Path star = new Path();
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                float radius = i % 2 == 0 ? 8 : 3.5f;
                float px = x + (float) Math.cos(angle) * radius;
                float py = y + (float) Math.sin(angle) * radius;
                if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
            }
            star.close();
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.4f);
            c.drawPath(star, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawDrawer(Canvas c, float viewportHeight) {
            p.setColor(0x55000000); c.drawRect(280, 0, 390, 780, p);
            rounded(c, 0, 0, 310, Math.min(780, viewportHeight), 0, Color.WHITE);
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
            float s = Math.min(getWidth() / 390f, getHeight() / 780f), x = e.getX() / s, y = e.getY() / s;
            if (e.getAction() == MotionEvent.ACTION_DOWN) { downX = x; downY = y; return true; }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (drawerOpen) { if (x > 310) drawerOpen = false; else if (y > 100 && y < 370) drawerOpen = false; invalidate(); return true; }
                if (downY < 70 && downX < 55) drawerOpen = true;
                else if (downY < 70 && downX > 280) searchOpen = true;
                else if (downY > (getHeight() / (getWidth() / 390f)) - 140) Toast.makeText(MainActivity.this, "Compose a new message", Toast.LENGTH_SHORT).show();
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
