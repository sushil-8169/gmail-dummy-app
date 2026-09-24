package com.example.gmaildummy;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Gmail's full-screen compose: new message, reply, reply all, forward, or an existing draft. */
public class ComposeActivity extends BaseActivity {
    static final String EXTRA_MODE = "mode";
    static final String EXTRA_SENT_ID = "sent_id";
    static final int MODE_NEW = 0, MODE_REPLY = 1, MODE_REPLY_ALL = 2, MODE_FORWARD = 3, MODE_DRAFT = 4;
    private static final int REQUEST_ATTACH = 1;
    private static final String[] DRIVE_FILES = { "Q4_Launch_Deck.pdf", "Scan_2026-09-23.pdf", "Budget_2026.xlsx", "Team_offsite.jpg" };
    private static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec" };

    private RecipientField to, cc, bcc;
    private EditText subject, body;
    private View attachment, ccBcc, expand, confidentialBanner;
    private ImageView attachmentIcon;
    private TextView attachmentName;
    private Mail draft;
    private boolean confidential;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_compose);
        SystemBars.padRoot(findViewById(R.id.root));

        to = findViewById(R.id.to);
        cc = findViewById(R.id.cc);
        bcc = findViewById(R.id.bcc);
        subject = findViewById(R.id.subject);
        body = findViewById(R.id.body);
        ccBcc = findViewById(R.id.cc_bcc);
        expand = findViewById(R.id.btn_expand);
        confidentialBanner = findViewById(R.id.confidential);
        attachment = findViewById(R.id.attachment);
        attachmentIcon = findViewById(R.id.attachment_icon);
        attachmentName = findViewById(R.id.attachment_name);
        ((TextView) findViewById(R.id.from)).setText(MailStore.ME);

        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_NEW);
        Mail source = MailStore.find(getIntent().getLongExtra(ReadActivity.EXTRA_ID, -1));
        if (source != null) prefill(mode, source);

        findViewById(R.id.btn_back).setOnClickListener(v -> close());
        findViewById(R.id.btn_send).setOnClickListener(v -> send());
        findViewById(R.id.btn_attach).setOnClickListener(this::showAttachMenu);
        findViewById(R.id.btn_remove_attachment).setOnClickListener(v -> attachment.setVisibility(View.GONE));
        findViewById(R.id.btn_more).setOnClickListener(this::showMoreMenu);
        expand.setOnClickListener(v -> showCcBcc(ccBcc.getVisibility() != View.VISIBLE, true));
        confidentialBanner.setOnClickListener(v -> editConfidential());

        if (to.isEmpty()) {
            to.focus();
        } else {
            body.requestFocus();
            body.setSelection(0);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void prefill(int mode, Mail m) {
        String contact = m.isOutgoing() ? m.to : m.email;
        switch (mode) {
            case MODE_DRAFT:
                draft = m;
                to.setRecipients(m.to.equals("me") ? "" : m.to);
                subject.setText(m.subject.equals("(no subject)") ? "" : m.subject);
                body.setText(m.body);
                if (m.attachment != null) showAttachment(m.attachment);
                break;
            case MODE_REPLY:
            case MODE_REPLY_ALL:
                showTitle(mode == MODE_REPLY ? "Reply" : "Reply all");
                to.setRecipients(contact);
                if (mode == MODE_REPLY_ALL && !m.isOutgoing()) {
                    cc.setRecipients("team@phoenix.dev");
                    showCcBcc(true, false);
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

    private void showCcBcc(boolean show, boolean focus) {
        ccBcc.setVisibility(show ? View.VISIBLE : View.GONE);
        expand.setRotation(show ? 180 : 0);
        if (show && focus) cc.focus();
    }

    // ---- Attachments: a real file from the device, or a pretend one from Drive ----

    private void showAttachMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Attach file");
        menu.getMenu().add(0, 2, 1, "Insert from Drive");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) pickFile(); else pickFromDrive();
            return true;
        });
        menu.show();
    }

    private void pickFile() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try {
            startActivityForResult(pick, REQUEST_ATTACH);
        } catch (ActivityNotFoundException e) {
            toast("No app available to pick a file");
        }
    }

    private void pickFromDrive() {
        new AlertDialog.Builder(this)
                .setTitle("Insert from Drive")
                .setItems(DRIVE_FILES, (dialog, which) -> showAttachment(DRIVE_FILES[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ATTACH && resultCode == RESULT_OK && data != null && data.getData() != null) {
            showAttachment(displayName(data.getData()));
        }
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (RuntimeException ignored) {
            // Fall back to the last path segment below.
        }
        String last = uri.getLastPathSegment();
        return last == null ? "Attachment" : last;
    }

    private void showAttachment(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        boolean image = lower.matches(".*\\.(jpe?g|png|gif|webp|heic)$");
        attachmentIcon.setImageResource(lower.endsWith(".pdf") ? R.drawable.ic_pdf
                : image ? R.drawable.ic_image : R.drawable.ic_insert_drive_file);
        attachmentIcon.setImageTintList(ColorStateList.valueOf(
                lower.endsWith(".pdf") || image ? 0xFFD93025 : color(R.color.primary)));
        attachmentName.setText(name);
        attachment.setVisibility(View.VISIBLE);
    }

    private String attachmentOrNull() {
        return attachment.getVisibility() == View.VISIBLE ? attachmentName.getText().toString() : null;
    }

    // ---- Overflow menu, as in Gmail ----

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String[] items = { "Schedule send", "Add from Contacts",
                confidential ? "Turn off confidential mode" : "Confidential mode",
                "Save draft", "Discard", "Settings", "Help & feedback" };
        for (int i = 0; i < items.length; i++) menu.getMenu().add(0, i, i, items[i]);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 0: scheduleSend(); break;
                case 1: addFromContacts(); break;
                case 2: if (confidential) setConfidential(false); else editConfidential(); break;
                case 3: close(); break;
                case 4: discard(); break;
                case 5: startActivity(new Intent(this, SettingsActivity.class)); break;
                default: toast(item.getTitle().toString());
            }
            return true;
        });
        menu.show();
    }

    private void addFromContacts() {
        List<MailStore.Contact> contacts = MailStore.contacts();
        String[] names = new String[contacts.size()];
        for (int i = 0; i < names.length; i++) names[i] = contacts.get(i).name + "\n" + contacts.get(i).email;
        new AlertDialog.Builder(this)
                .setTitle("Add from Contacts")
                .setItems(names, (dialog, which) -> to.add(contacts.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void editConfidential() {
        new AlertDialog.Builder(this)
                .setTitle("Confidential mode")
                .setMessage("Recipients won’t have the option to forward, copy, print or download this email. "
                        + "Content expires in 1 week.")
                .setNegativeButton("Cancel", null)
                .setNeutralButton(confidential ? "Turn off" : null, (dialog, which) -> setConfidential(false))
                .setPositiveButton("Save", (dialog, which) -> setConfidential(true))
                .show();
    }

    private void setConfidential(boolean on) {
        confidential = on;
        confidentialBanner.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    // ---- Schedule send ----

    private void scheduleSend() {
        if (!checkRecipients()) return;
        Calendar[] times = { at(1, 8), at(1, 13), nextMonday() };
        String[] labels = {
                "Tomorrow morning\n" + describe(times[0]),
                "Tomorrow afternoon\n" + describe(times[1]),
                "Monday morning\n" + describe(times[2]),
                "Pick date & time" };
        new AlertDialog.Builder(this)
                .setTitle("Schedule send")
                .setItems(labels, (dialog, which) -> {
                    if (which < times.length) schedule(times[which]); else pickDateTime();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickDateTime() {
        Calendar suggested = at(1, 8);
        DatePickerDialog date = new DatePickerDialog(this, (picker, year, month, day) ->
                new TimePickerDialog(this, (clock, hour, minute) -> {
                    Calendar when = Calendar.getInstance();
                    when.set(year, month, day, hour, minute, 0);
                    if (when.before(Calendar.getInstance())) toast("Choose a time in the future");
                    else schedule(when);
                }, 8, 0, false).show(),
                suggested.get(Calendar.YEAR), suggested.get(Calendar.MONTH), suggested.get(Calendar.DAY_OF_MONTH));
        date.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        date.show();
    }

    private void schedule(Calendar when) {
        MailStore.mails.remove(draft);
        MailStore.mails.add(0, outgoing("Scheduled", recipients(), shortDate(when)));
        finishWith("Send scheduled for " + describe(when));
    }

    private static Calendar at(int daysAhead, int hour) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, daysAhead);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static Calendar nextMonday() {
        int days = (Calendar.MONDAY - Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 7) % 7;
        return at(days == 0 ? 7 : days, 8);
    }

    private static String shortDate(Calendar c) {
        return c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)];
    }

    /** e.g. "25 Sept, 8:00 am" */
    private static String describe(Calendar c) {
        return shortDate(c) + ", "
                + new SimpleDateFormat("h:mm a", Locale.US).format(c.getTime()).toLowerCase(Locale.ROOT);
    }

    // ---- Sending, saving and discarding ----

    private boolean checkRecipients() {
        if (to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) {
            alert("Add at least one recipient.");
            return false;
        }
        for (RecipientField field : new RecipientField[] { to, cc, bcc }) {
            String invalid = field.firstInvalid();
            if (invalid != null) {
                alert("The address ‘" + invalid + "’ is invalid. Check that it is typed correctly.");
                return false;
            }
        }
        return true;
    }

    private void alert(String message) {
        new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", null).show();
    }

    private void send() {
        if (!checkRecipients()) return;
        if (subject.getText().toString().trim().isEmpty() && body.getText().toString().trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("Send this message without a subject or text in the body?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Send", (dialog, which) -> deliver())
                    .show();
            return;
        }
        deliver();
    }

    private void deliver() {
        MailStore.mails.remove(draft);
        Mail sent = outgoing("Sent", recipients(), MailStore.now());
        MailStore.mails.add(0, sent);
        setResult(RESULT_OK, new Intent().putExtra(ReadActivity.EXTRA_MESSAGE, "Sent").putExtra(EXTRA_SENT_ID, sent.id));
        finish();
    }

    /** Who the mail is shown as going to: the To line, or Cc / Bcc when To is empty. */
    private String recipients() {
        String line = to.joined();
        if (line.isEmpty()) line = cc.joined();
        if (line.isEmpty()) line = bcc.joined();
        return line;
    }

    // Leaving with any content saves it to Drafts, like Gmail.
    private void close() {
        String recipient = recipients();
        boolean empty = recipient.isEmpty() && subject.length() == 0
                && body.getText().toString().trim().isEmpty() && attachmentOrNull() == null;
        MailStore.mails.remove(draft);
        if (!empty) {
            MailStore.mails.add(0, outgoing("Drafts", recipient.isEmpty() ? "me" : recipient, MailStore.now()));
            finishWith("Draft saved");
        } else if (draft != null) {
            finishWith("Draft discarded");
        } else {
            finish();
        }
    }

    private void discard() {
        MailStore.mails.remove(draft);
        finishWith("Draft discarded");
    }

    private void finishWith(String message) {
        setResult(RESULT_OK, new Intent().putExtra(ReadActivity.EXTRA_MESSAGE, message));
        finish();
    }

    private Mail outgoing(String category, String recipient, String time) {
        String text = body.getText().toString();
        String title = subject.getText().toString().trim();
        String snippet = text.trim().replaceAll("\\s+", " ");
        return new Mail(category, "me", title.isEmpty() ? "(no subject)" : title, snippet,
                time, MailStore.ME_COLOR, false, false, attachmentOrNull())
                .from(MailStore.ME).to(recipient).body(text);
    }

    @Override public void onBackPressed() {
        close();
    }
}
