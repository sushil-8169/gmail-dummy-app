package com.example.gmaildummy;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** Gmail's conversation view for a single message. */
public class ReadActivity extends Activity {
    static final String EXTRA_ID = "mail_id";
    static final String EXTRA_ACTION = "action";
    static final String EXTRA_MESSAGE = "message";
    private static final int REQUEST_COMPOSE = 1;

    private Mail mail;
    private ImageView star;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mail = MailStore.find(getIntent().getLongExtra(EXTRA_ID, -1));
        if (mail == null) { finish(); return; }
        mail.unread = false;

        setContentView(R.layout.activity_read);
        SystemBars.edgeToEdge(this);
        SystemBars.padRoot(findViewById(R.id.root));

        TextView subject = findViewById(R.id.subject);
        SpannableStringBuilder title = new SpannableStringBuilder(mail.subject).append("  ");
        int chipStart = title.length();
        title.append(labelFor(mail));
        title.setSpan(new ChipSpan(), chipStart, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        subject.setText(title);

        star = findViewById(R.id.star);
        star.setOnClickListener(v -> { mail.starred = !mail.starred; bindStar(); });
        bindStar();

        TextView avatar = findViewById(R.id.avatar);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(mail.color);
        avatar.setBackground(circle);
        avatar.setText(mail.isOutgoing() ? "A" : mail.initial());

        String name = mail.isOutgoing() ? "me" : mail.sender;
        SpannableStringBuilder sender = new SpannableStringBuilder(name);
        sender.setSpan(new TypefaceSpan("sans-serif-medium"), 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int timeStart = sender.length() + 2;
        sender.append("  ").append(mail.time);
        sender.setSpan(new AbsoluteSizeSpan(12, true), timeStart, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sender.setSpan(new ForegroundColorSpan(0xFF444746), timeStart, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.sender)).setText(sender);

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
        findViewById(R.id.btn_more).setOnClickListener(v -> toast("More options"));
        findViewById(R.id.btn_more_sender).setOnClickListener(v -> toast("More options"));
        findViewById(R.id.btn_reply_top).setOnClickListener(v -> reply(ComposeActivity.MODE_REPLY));
        setupReplyButton(R.id.btn_reply, ComposeActivity.MODE_REPLY);
        setupReplyButton(R.id.btn_reply_all, ComposeActivity.MODE_REPLY_ALL);
        setupReplyButton(R.id.btn_forward, ComposeActivity.MODE_FORWARD);
    }

    private static String labelFor(Mail m) {
        return m.category.equals("Primary") ? "Inbox" : m.category;
    }

    private void bindStar() {
        star.setImageResource(mail.starred ? R.drawable.ic_star : R.drawable.ic_star_border);
        star.setImageTintList(ColorStateList.valueOf(mail.starred ? 0xFFF4B400 : 0xFF444746));
    }

    // The pill buttons use 18dp icons, smaller than the 24dp vector default.
    private void setupReplyButton(int id, int mode) {
        TextView button = findViewById(id);
        Drawable icon = button.getCompoundDrawablesRelative()[0];
        int size = dp(18);
        icon.setBounds(0, 0, size, size);
        button.setCompoundDrawablesRelative(icon, null, null, null);
        button.setOnClickListener(v -> reply(mode));
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

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Draws the folder label ("Inbox") as a small grey chip inline after the subject, like Gmail. */
    private class ChipSpan extends ReplacementSpan {
        private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Paint textPaint(Paint base) {
            Paint p = new Paint(base);
            p.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
            p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            p.setColor(0xFF444746);
            return p;
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            return Math.round(textPaint(paint).measureText(text, start, end)) + dp(12);
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                                   float x, int top, int y, int bottom, Paint paint) {
            Paint p = textPaint(paint);
            float width = p.measureText(text, start, end) + dp(12);
            float centerY = y + (paint.ascent() + paint.descent()) / 2f;
            float half = dp(10);
            chipPaint.setColor(0xFFE1E3E1);
            canvas.drawRoundRect(new RectF(x, centerY - half, x + width, centerY + half), dp(4), dp(4), chipPaint);
            float baseline = centerY - (p.ascent() + p.descent()) / 2f;
            canvas.drawText(text, start, end, x + dp(6), baseline, p);
        }
    }
}
