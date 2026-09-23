package com.example.gmaildummy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/** Gmail's full-screen compose: new message, reply, reply all, forward, or an existing draft. */
public class ComposeActivity extends Activity {
    static final String EXTRA_MODE = "mode";
    static final int MODE_NEW = 0, MODE_REPLY = 1, MODE_REPLY_ALL = 2, MODE_FORWARD = 3, MODE_DRAFT = 4;

    private EditText to, cc, bcc, subject, body;
    private View attachment, ccBcc;
    private TextView attachmentName;
    private Mail draft;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_compose);
        SystemBars.edgeToEdge(this);
        SystemBars.padRoot(findViewById(R.id.root));

        to = findViewById(R.id.to);
        cc = findViewById(R.id.cc);
        bcc = findViewById(R.id.bcc);
        subject = findViewById(R.id.subject);
        body = findViewById(R.id.body);
        ccBcc = findViewById(R.id.cc_bcc);
        attachment = findViewById(R.id.attachment);
        attachmentName = findViewById(R.id.attachment_name);
        ((TextView) findViewById(R.id.from)).setText(MailStore.ME);

        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_NEW);
        Mail source = MailStore.find(getIntent().getLongExtra(ReadActivity.EXTRA_ID, -1));
        if (source != null) prefill(mode, source);

        findViewById(R.id.btn_back).setOnClickListener(v -> close());
        findViewById(R.id.btn_send).setOnClickListener(v -> send());
        findViewById(R.id.btn_attach).setOnClickListener(v -> showAttachment("Scan_2026-09-23.pdf"));
        findViewById(R.id.btn_remove_attachment).setOnClickListener(v -> attachment.setVisibility(View.GONE));
        findViewById(R.id.btn_more).setOnClickListener(v -> toast("More options"));
        findViewById(R.id.btn_expand).setOnClickListener(v -> {
            boolean show = ccBcc.getVisibility() != View.VISIBLE;
            ccBcc.setVisibility(show ? View.VISIBLE : View.GONE);
            v.setRotation(show ? 180 : 0);
            if (show) cc.requestFocus();
        });

        EditText focus = to.length() == 0 ? to : body;
        focus.requestFocus();
        if (focus == body) body.setSelection(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void prefill(int mode, Mail m) {
        String contact = m.isOutgoing() ? m.to : m.email;
        switch (mode) {
            case MODE_DRAFT:
                draft = m;
                to.setText(m.to.equals("me") ? "" : m.to);
                subject.setText(m.subject.equals("(no subject)") ? "" : m.subject);
                body.setText(m.body);
                if (m.attachment != null) showAttachment(m.attachment);
                break;
            case MODE_REPLY:
            case MODE_REPLY_ALL:
                showTitle(mode == MODE_REPLY ? "Reply" : "Reply all");
                to.setText(contact);
                if (mode == MODE_REPLY_ALL && !m.isOutgoing()) {
                    ccBcc.setVisibility(View.VISIBLE);
                    cc.setText("team@phoenix.dev");
                }
                subject.setText(prefixed("Re: ", m.subject));
                body.setText("\n\nOn " + m.time + ", " + m.sender + " <" + m.email + "> wrote:\n> "
                        + m.body.replace("\n", "\n> "));
                break;
            case MODE_FORWARD:
                showTitle("Forward");
                subject.setText(prefixed("Fwd: ", m.subject));
                body.setText("\n\n---------- Forwarded message ---------\nFrom: " + m.sender + " <" + m.email
                        + ">\nDate: " + m.time + "\nSubject: " + m.subject + "\nTo: " + MailStore.ME + "\n\n" + m.body);
                if (m.attachment != null) showAttachment(m.attachment);
                break;
            default:
        }
    }

    private void showTitle(String title) {
        ((TextView) findViewById(R.id.title)).setText(title);
    }

    private static String prefixed(String prefix, String subject) {
        return subject.regionMatches(true, 0, prefix, 0, prefix.length()) ? subject : prefix + subject;
    }

    private void showAttachment(String name) {
        attachmentName.setText(name);
        attachment.setVisibility(View.VISIBLE);
    }

    private String attachmentOrNull() {
        return attachment.getVisibility() == View.VISIBLE ? attachmentName.getText().toString() : null;
    }

    private void send() {
        String recipient = to.getText().toString().trim();
        if (recipient.isEmpty()) {
            toast("Add at least one recipient.");
            to.requestFocus();
            return;
        }
        MailStore.mails.remove(draft);
        MailStore.mails.add(0, outgoing("Sent", recipient));
        setResult(RESULT_OK, new Intent().putExtra(ReadActivity.EXTRA_MESSAGE, "Message sent"));
        finish();
    }

    // Leaving with any content saves it to Drafts, like Gmail.
    private void close() {
        String recipient = to.getText().toString().trim();
        boolean empty = recipient.isEmpty() && subject.length() == 0
                && body.getText().toString().trim().isEmpty() && attachmentOrNull() == null;
        MailStore.mails.remove(draft);
        if (!empty) {
            MailStore.mails.add(0, outgoing("Drafts", recipient.isEmpty() ? "me" : recipient));
            setResult(RESULT_OK, new Intent().putExtra(ReadActivity.EXTRA_MESSAGE, "Draft saved"));
        } else if (draft != null) {
            setResult(RESULT_OK, new Intent().putExtra(ReadActivity.EXTRA_MESSAGE, "Draft discarded"));
        }
        finish();
    }

    private Mail outgoing(String category, String recipient) {
        String text = body.getText().toString();
        String title = subject.getText().toString().trim();
        String snippet = text.trim().replaceAll("\\s+", " ");
        return new Mail(category, "me", title.isEmpty() ? "(no subject)" : title, snippet,
                MailStore.now(), MailStore.ME_COLOR, false, false, attachmentOrNull())
                .from(MailStore.ME).to(recipient).body(text);
    }

    @Override public void onBackPressed() {
        close();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
