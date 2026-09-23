package com.example.gmaildummy;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

/** Gmail's conversation view for a single message. */
public class ReadActivity extends BaseActivity {
    static final String EXTRA_ID = "mail_id";
    static final String EXTRA_ACTION = "action";
    static final String EXTRA_MESSAGE = "message";
    private static final int REQUEST_COMPOSE = 1;

    private Mail mail;
    private ImageView star;
    private TextView subject;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mail = MailStore.find(getIntent().getLongExtra(EXTRA_ID, -1));
        if (mail == null) { finish(); return; }
        mail.unread = false;

        setContentView(R.layout.activity_read);
        View root = findViewById(R.id.root);
        LinearLayout nav = findViewById(R.id.bottom_nav);
        showUnreadBadge(buildBottomNav(nav, this::finish), unreadPrimary());
        padForSystemBars(root, root, nav);

        subject = findViewById(R.id.subject);
        bindSubject();

        star = findViewById(R.id.star);
        star.setOnClickListener(v -> { mail.starred = !mail.starred; bindStar(); });
        bindStar();

        TextView avatar = findViewById(R.id.avatar);
        avatar.setBackground(oval(mail.color));
        avatar.setText(mail.isOutgoing() ? "A" : mail.initial());
        ((TextView) findViewById(R.id.sender)).setText(mail.isOutgoing() ? "me" : mail.sender);
        ((TextView) findViewById(R.id.date)).setText(mail.time);
        findViewById(R.id.verified).setVisibility(mail.verified ? View.VISIBLE : View.GONE);

        TextView recipient = findViewById(R.id.recipient);
        TextView details = findViewById(R.id.details);
        recipient.setText("to " + mail.to);
        details.setText("From: " + (mail.isOutgoing() ? MailStore.ME : mail.sender + " • " + mail.email)
                + "\nTo: " + (mail.to.equals("me") ? MailStore.ME : mail.to)
                + "\nDate: " + mail.time);
        recipient.setOnClickListener(v ->
                details.setVisibility(details.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        ((TextView) findViewById(R.id.body)).setText(mail.body);
        if (mail.attachment != null) {
            View card = findViewById(R.id.attachment);
            card.setVisibility(View.VISIBLE);
            card.setOnClickListener(v -> toast(mail.attachment));
            ((TextView) findViewById(R.id.attachment_name)).setText(mail.attachment);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_archive).setOnClickListener(v -> finishWith("archive"));
        findViewById(R.id.btn_delete).setOnClickListener(v -> finishWith("delete"));
        findViewById(R.id.btn_mark_unread).setOnClickListener(v -> { mail.unread = true; finish(); });
        findViewById(R.id.btn_more).setOnClickListener(this::showMailMenu);
        findViewById(R.id.btn_more_sender).setOnClickListener(this::showReplyMenu);
        findViewById(R.id.btn_react).setOnClickListener(v -> toast("Add emoji reaction"));
        findViewById(R.id.btn_emoji).setOnClickListener(v -> toast("Add emoji reaction"));
        findViewById(R.id.btn_reply_top).setOnClickListener(v -> reply(ComposeActivity.MODE_REPLY));
        findViewById(R.id.btn_reply).setOnClickListener(v -> reply(ComposeActivity.MODE_REPLY));
        findViewById(R.id.btn_forward).setOnClickListener(v -> reply(ComposeActivity.MODE_FORWARD));
    }

    private int unreadPrimary() {
        int count = 0;
        for (Mail m : MailStore.mails) if (m.unread && m.category.equals("Primary")) count++;
        return count;
    }

    // Subject, then Gmail's importance marker and the folder chip ("Inbox") flowing inline after it.
    private void bindSubject() {
        SpannableStringBuilder title = new SpannableStringBuilder(mail.subject);
        if (mail.important) {
            title.append(" ");
            int start = title.length();
            title.append("￼");
            title.setSpan(new IconSpan(R.drawable.ic_important, color(R.color.important)), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        title.append(" ");
        int chipStart = title.length();
        title.append(labelFor(mail));
        title.setSpan(new ChipSpan(), chipStart, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        subject.setText(title);
    }

    private static String labelFor(Mail m) {
        return m.category.equals("Primary") ? "Inbox" : m.category;
    }

    private void bindStar() {
        star.setImageResource(mail.starred ? R.drawable.ic_star : R.drawable.ic_star_border);
        star.setImageTintList(ColorStateList.valueOf(color(mail.starred ? R.color.star_active : R.color.on_surface_variant)));
    }

    private void showMailMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, mail.important ? "Mark not important" : "Mark important");
        menu.getMenu().add(0, 2, 0, "Move to");
        menu.getMenu().add(0, 3, 0, "Snooze");
        menu.getMenu().add(0, 4, 0, "Change labels");
        menu.getMenu().add(0, 5, 0, "Report spam");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                mail.important = !mail.important;
                bindSubject();
            } else {
                toast(item.getTitle().toString());
            }
            return true;
        });
        menu.show();
    }

    private void showReplyMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, ComposeActivity.MODE_REPLY, 0, "Reply");
        menu.getMenu().add(0, ComposeActivity.MODE_REPLY_ALL, 0, "Reply all");
        menu.getMenu().add(0, ComposeActivity.MODE_FORWARD, 0, "Forward");
        menu.setOnMenuItemClickListener(item -> { reply(item.getItemId()); return true; });
        menu.show();
    }

    private void reply(int mode) {
        Intent intent = new Intent(this, ComposeActivity.class)
                .putExtra(ComposeActivity.EXTRA_MODE, mode)
                .putExtra(EXTRA_ID, mail.id);
        startActivityForResult(intent, REQUEST_COMPOSE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.hasExtra(EXTRA_MESSAGE)) {
            toast(data.getStringExtra(EXTRA_MESSAGE));
        }
    }

    private void finishWith(String action) {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_ACTION, action).putExtra(EXTRA_ID, mail.id));
        finish();
    }

    private static float centerOf(Paint paint, int baseline) {
        return baseline + (paint.ascent() + paint.descent()) / 2f;
    }

    /** Draws a tinted icon centred on the text line. */
    private class IconSpan extends ReplacementSpan {
        private final Drawable icon;
        private final int size = dp(22);

        IconSpan(int res, int tint) {
            icon = getDrawable(res).mutate();
            icon.setTint(tint);
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            return size;
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                                   float x, int top, int y, int bottom, Paint paint) {
            int cy = Math.round(centerOf(paint, y));
            icon.setBounds(Math.round(x), cy - size / 2, Math.round(x) + size, cy + size / 2);
            icon.draw(canvas);
        }
    }

    /** Draws the folder label ("Inbox") as a small tinted chip inline after the subject, like Gmail. */
    private class ChipSpan extends ReplacementSpan {
        private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Paint textPaint(Paint base) {
            Paint p = new Paint(base);
            p.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
            p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            p.setColor(color(R.color.chip_text));
            return p;
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            return Math.round(textPaint(paint).measureText(text, start, end)) + dp(20);
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                                   float x, int top, int y, int bottom, Paint paint) {
            Paint p = textPaint(paint);
            float left = x + dp(4);
            float width = p.measureText(text, start, end) + dp(16);
            float cy = centerOf(paint, y);
            float half = dp(12);
            chipPaint.setColor(color(R.color.chip_bg));
            canvas.drawRoundRect(new RectF(left, cy - half, left + width, cy + half), dp(6), dp(6), chipPaint);
            float baseline = cy - (p.ascent() + p.descent()) / 2f;
            canvas.drawText(text, start, end, left + dp(8), baseline, p);
        }
    }
}
