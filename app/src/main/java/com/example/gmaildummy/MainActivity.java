package com.example.gmaildummy;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends BaseActivity {
    static final String EXTRA_OPEN_DRAWER = "open_drawer";
    private static final int REQUEST_READ = 1, REQUEST_COMPOSE = 2;
    private static final int DRAWER_WIDTH_DP = 304;

    private static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);

    private final List<Mail> mails = MailStore.mails;
    /** Rows of the list: a {@link Mail}, or a category name for an Updates / Promotions / Social card. */
    private final List<Object> rows = new ArrayList<>();
    private final Set<Mail> selected = new LinkedHashSet<>();
    private final List<Removed> lastRemoved = new ArrayList<>();
    private final MailAdapter adapter = new MailAdapter();
    private final Runnable hideSnackbar = this::hideSnackbar;
    private Runnable snackbarUndo;

    private String folder = "Primary";
    private String query = "";
    private boolean searchMode, drawerOpen, fabExtended = true;
    private int bottomInset;

    private View searchBar, selectionBar, account, fab, fabLabel, scrim, drawer, snackbar;
    private ImageView menuButton;
    private EditText searchField;
    private TextView selectionCount, folderLabel, emptyView, snackbarText, mailBadge;
    private ListView list;
    private LinearLayout bottomNav, drawerList;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        searchBar = findViewById(R.id.search_bar);
        selectionBar = findViewById(R.id.selection_bar);
        account = findViewById(R.id.account);
        menuButton = findViewById(R.id.btn_menu);
        searchField = findViewById(R.id.search_field);
        selectionCount = findViewById(R.id.selection_count);
        list = findViewById(R.id.list);
        emptyView = findViewById(R.id.empty);
        fab = findViewById(R.id.fab);
        fabLabel = findViewById(R.id.fab_label);
        snackbar = findViewById(R.id.snackbar);
        snackbarText = findViewById(R.id.snackbar_text);
        bottomNav = findViewById(R.id.bottom_nav);
        scrim = findViewById(R.id.scrim);
        drawer = findViewById(R.id.drawer);
        drawerList = findViewById(R.id.drawer_list);

        setupInsets();
        setupList();
        setupTopBar();
        mailBadge = buildBottomNav(bottomNav, () -> { if (!folder.equals("Primary")) selectFolder("Primary"); });
        setupFab();
        scrim.setOnClickListener(v -> closeDrawer());
        findViewById(R.id.snackbar_action).setOnClickListener(v -> {
            Runnable undo = snackbarUndo;
            hideSnackbar();
            if (undo != null) undo.run();
        });
        refresh();
    }

    private void setupInsets() {
        View content = findViewById(R.id.content);
        findViewById(R.id.root).setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            bottomInset = insets.getSystemWindowInsetBottom();
            content.setPadding(0, top, 0, 0);
            bottomNav.setPadding(0, 0, 0, bottomInset);
            drawer.setPadding(0, top, 0, bottomInset);
            updateListPadding();
            return insets;
        });
    }

    private void setupList() {
        folderLabel = (TextView) getLayoutInflater().inflate(R.layout.header_inbox, list, false);
        list.addHeaderView(folderLabel, null, false);
        list.setAdapter(adapter);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int lastFirst, lastTop;
            @Override public void onScrollStateChanged(AbsListView view, int state) { }
            @Override public void onScroll(AbsListView view, int first, int visible, int total) {
                View child = view.getChildAt(0);
                int top = child == null ? 0 : child.getTop();
                if (first == 0 && top >= 0) setFabExtended(true);
                else if (first > lastFirst || (first == lastFirst && top < lastTop)) setFabExtended(false);
                else if (first < lastFirst || (first == lastFirst && top > lastTop)) setFabExtended(true);
                lastFirst = first;
                lastTop = top;
            }
        });
    }

    private void setupTopBar() {
        menuButton.setOnClickListener(v -> { if (searchMode) exitSearch(); else openDrawer(); });
        searchField.setOnClickListener(v -> { if (!searchMode) enterSearch(); });
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (!searchMode) return;
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                refresh();
            }
        });
        searchField.setOnEditorActionListener((v, actionId, event) -> { hideKeyboard(); return true; });
        account.setOnClickListener(v -> toast(MailStore.ME));
        findViewById(R.id.btn_close_selection).setOnClickListener(v -> { selected.clear(); refresh(); });
        findViewById(R.id.btn_archive).setOnClickListener(v -> removeSelected("archived"));
        findViewById(R.id.btn_delete).setOnClickListener(v -> removeSelected("moved to Bin"));
        findViewById(R.id.btn_mark_unread).setOnClickListener(v -> toggleReadSelected());
    }

    private void setupFab() {
        fab.setOnClickListener(v -> startActivityForResult(new Intent(this, ComposeActivity.class), REQUEST_COMPOSE));
        LayoutTransition fabTransition = new LayoutTransition();
        fabTransition.enableTransitionType(LayoutTransition.CHANGING);
        ((ViewGroup) fab).setLayoutTransition(fabTransition);
        LayoutTransition frameTransition = new LayoutTransition();
        frameTransition.enableTransitionType(LayoutTransition.CHANGING);
        ((ViewGroup) fab.getParent()).setLayoutTransition(frameTransition);
    }

    private void setFabExtended(boolean extended) {
        if (fabExtended == extended) return;
        fabExtended = extended;
        fabLabel.setVisibility(extended ? View.VISIBLE : View.GONE);
        fab.setPadding(dp(18), 0, dp(extended ? 24 : 18), 0);
    }

    private void refresh() {
        rows.clear();
        boolean inbox = folder.equals("Primary") && !searchMode;
        Set<String> placed = new HashSet<>();
        for (Mail m : mails) {
            if (inbox && isCategory(m.category)) {
                // Gmail shows each tab with new mail as one card, where its newest mail would be.
                if (placed.add(m.category) && unreadIn(m.category) > 0) rows.add(m.category);
            } else if (matches(m)) {
                rows.add(m);
            }
        }
        folderLabel.setText(searchMode ? (query.isEmpty() ? "All mail" : "Results") : folder);
        emptyView.setText(searchMode ? "No results for “" + query + "”" : "Nothing in " + folder);
        emptyView.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();

        boolean selecting = !selected.isEmpty();
        selectionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        searchBar.setVisibility(selecting ? View.GONE : View.VISIBLE);
        selectionCount.setText(String.valueOf(selected.size()));
        showUnreadBadge(mailBadge, unreadIn("Primary"));
        buildDrawer();
    }

    private static boolean isCategory(String category) {
        return category.equals("Promotions") || category.equals("Social") || category.equals("Updates");
    }

    private boolean matches(Mail m) {
        if (searchMode) {
            return query.isEmpty()
                    || (m.sender + " " + m.subject + " " + m.snippet).toLowerCase(Locale.ROOT).contains(query);
        }
        switch (folder) {
            case "Primary": case "Promotions": case "Social": case "Updates": case "Sent": case "Drafts": case "Scheduled":
                return m.category.equals(folder);
            case "All inboxes": return !m.isOutgoing();
            case "All mail": return !m.category.equals("Drafts");
            case "Starred": return m.starred;
            case "Important": return m.important;
            default: return false;
        }
    }

    private int countIn(String category) {
        int count = 0;
        for (Mail m : mails) if (m.category.equals(category)) count++;
        return count;
    }

    private int unreadIn(String category) {
        int count = 0;
        for (Mail m : mails) if (m.unread && m.category.equals(category)) count++;
        return count;
    }

    private Mail newestIn(String category) {
        for (Mail m : mails) if (m.category.equals(category)) return m;
        return null;
    }

    private static int categoryIcon(String category) {
        switch (category) {
            case "Promotions": return R.drawable.ic_tag;
            case "Social": return R.drawable.ic_people;
            default: return R.drawable.ic_info;
        }
    }

    /** Icon, badge background and badge text colours for a category. */
    private int[] categoryColors(String category) {
        switch (category) {
            case "Promotions":
                return new int[] { color(R.color.cat_promotions), color(R.color.cat_promotions_bg), color(R.color.cat_promotions_text) };
            case "Social":
                return new int[] { color(R.color.cat_social), color(R.color.cat_social_bg), color(R.color.cat_social_text) };
            default:
                return new int[] { color(R.color.cat_updates), color(R.color.cat_updates_bg), color(R.color.cat_updates_text) };
        }
    }

    private void onMailClick(Mail m) {
        if (!selected.isEmpty()) { toggleSelected(m); return; }
        if (m.category.equals("Drafts")) {
            startActivityForResult(new Intent(this, ComposeActivity.class)
                    .putExtra(ComposeActivity.EXTRA_MODE, ComposeActivity.MODE_DRAFT)
                    .putExtra(ReadActivity.EXTRA_ID, m.id), REQUEST_COMPOSE);
        } else {
            startActivityForResult(new Intent(this, ReadActivity.class).putExtra(ReadActivity.EXTRA_ID, m.id), REQUEST_READ);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    // Meet's menu button comes back here to open the drawer.
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(EXTRA_OPEN_DRAWER, false)) openDrawer();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        String action = data.getStringExtra(ReadActivity.EXTRA_ACTION);
        Mail m = MailStore.find(data.getLongExtra(ReadActivity.EXTRA_ID, -1));
        if (action != null && m != null) {
            selected.clear();
            selected.add(m);
            removeSelected(action.equals("archive") ? "archived" : "moved to Bin");
        } else if (data.hasExtra(ComposeActivity.EXTRA_SENT_ID)) {
            Mail sent = MailStore.find(data.getLongExtra(ComposeActivity.EXTRA_SENT_ID, -1));
            showSnackbar("Sent", sent == null ? null : () -> undoSend(sent));
        } else if (data.hasExtra(ReadActivity.EXTRA_MESSAGE)) {
            showSnackbar(data.getStringExtra(ReadActivity.EXTRA_MESSAGE), null);
        }
    }

    // Gmail's Undo after sending: the message goes back to Drafts and reopens in compose.
    private void undoSend(Mail sent) {
        int index = mails.indexOf(sent);
        if (index < 0) return;
        Mail draft = sent.copyAs("Drafts");
        mails.set(index, draft);
        refresh();
        startActivityForResult(new Intent(this, ComposeActivity.class)
                .putExtra(ComposeActivity.EXTRA_MODE, ComposeActivity.MODE_DRAFT)
                .putExtra(ReadActivity.EXTRA_ID, draft.id), REQUEST_COMPOSE);
    }

    private void toggleSelected(Mail m) {
        if (!selected.remove(m)) selected.add(m);
        refresh();
    }

    private void toggleReadSelected() {
        boolean anyRead = false;
        for (Mail m : selected) if (!m.unread) anyRead = true;
        for (Mail m : selected) m.unread = anyRead;
        selected.clear();
        refresh();
    }

    private void removeSelected(String verb) {
        lastRemoved.clear();
        for (int i = 0; i < mails.size(); i++) {
            if (selected.contains(mails.get(i))) lastRemoved.add(new Removed(i, mails.get(i)));
        }
        mails.removeAll(selected);
        selected.clear();
        refresh();
        showSnackbar(lastRemoved.size() + " " + verb, this::undoRemove);
    }

    private void undoRemove() {
        for (Removed r : lastRemoved) mails.add(Math.min(r.index, mails.size()), r.mail);
        lastRemoved.clear();
        hideSnackbar();
        refresh();
    }

    /** Shows {@code text}, with an Undo action when {@code undo} is given. */
    private void showSnackbar(String text, Runnable undo) {
        snackbarUndo = undo;
        snackbarText.setText(text);
        findViewById(R.id.snackbar_action).setVisibility(undo != null ? View.VISIBLE : View.GONE);
        snackbar.setVisibility(View.VISIBLE);
        snackbar.removeCallbacks(hideSnackbar);
        snackbar.postDelayed(hideSnackbar, 4000);
        fab.animate().translationY(-dp(56)).setDuration(150).start();
    }

    private void hideSnackbar() {
        snackbarUndo = null;
        snackbar.removeCallbacks(hideSnackbar);
        snackbar.setVisibility(View.GONE);
        fab.animate().translationY(0).setDuration(150).start();
    }

    private void enterSearch() {
        searchMode = true;
        selected.clear();
        hideSnackbar();
        menuButton.setImageResource(R.drawable.ic_arrow_back);
        account.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
        searchField.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        searchField.setFocusable(true);
        searchField.setFocusableInTouchMode(true);
        searchField.requestFocus();
        searchField.post(() -> imm().showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT));
        updateListPadding();
        refresh();
    }

    private void exitSearch() {
        searchMode = false;
        query = "";
        searchField.setText("");
        searchField.clearFocus();
        searchField.setFocusable(false);
        searchField.setGravity(Gravity.CENTER);
        hideKeyboard();
        menuButton.setImageResource(R.drawable.ic_menu);
        account.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
        updateListPadding();
        refresh();
    }

    private void updateListPadding() {
        list.setPadding(0, 0, 0, searchMode ? bottomInset + dp(8) : dp(96));
    }

    private void selectFolder(String name) {
        folder = name;
        selected.clear();
        refresh();
        list.setSelection(0);
        setFabExtended(true);
    }

    private void openDrawer() {
        drawerOpen = true;
        drawer.setVisibility(View.VISIBLE);
        scrim.setVisibility(View.VISIBLE);
        drawer.setTranslationX(-dp(DRAWER_WIDTH_DP));
        drawer.animate().translationX(0).setDuration(250).setInterpolator(new DecelerateInterpolator()).start();
        scrim.animate().alpha(1).setDuration(250).start();
    }

    private void closeDrawer() {
        drawerOpen = false;
        drawer.animate().translationX(-dp(DRAWER_WIDTH_DP)).setDuration(200)
                .withEndAction(() -> { if (!drawerOpen) drawer.setVisibility(View.GONE); }).start();
        scrim.animate().alpha(0).setDuration(200)
                .withEndAction(() -> { if (!drawerOpen) scrim.setVisibility(View.GONE); }).start();
    }

    private void buildDrawer() {
        drawerList.removeAllViews();
        // The Gmail logo: the four-colour M followed by the grey wordmark.
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.HORIZONTAL);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(28), 0, dp(16), 0);
        title.setContentDescription("Gmail");
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_gmail_logo);
        title.addView(logo, new LinearLayout.LayoutParams(dp(32), dp(24)));
        TextView wordmark = new TextView(this);
        wordmark.setText("Gmail");
        wordmark.setTextColor(color(R.color.on_surface_variant));
        wordmark.setTextSize(22);
        wordmark.setTypeface(REGULAR);
        LinearLayout.LayoutParams wordmarkParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wordmarkParams.setMarginStart(dp(12));
        title.addView(wordmark, wordmarkParams);
        drawerList.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        int primaryUnread = unreadIn("Primary");
        drawerItem(R.drawable.ic_all_inbox, "All inboxes", count(primaryUnread), false);
        drawerDivider();
        drawerItem(R.drawable.ic_inbox, "Primary", count(primaryUnread), false);
        drawerItem(R.drawable.ic_tag, "Promotions", newCount("Promotions"), true);
        drawerItem(R.drawable.ic_people, "Social", newCount("Social"), true);
        drawerItem(R.drawable.ic_info, "Updates", newCount("Updates"), true);
        drawerHeader("All labels");
        drawerItem(R.drawable.ic_star_border, "Starred", "", false);
        drawerItem(R.drawable.ic_schedule, "Snoozed", "", false);
        drawerItem(R.drawable.ic_label_important, "Important", "", false);
        drawerItem(R.drawable.ic_send, "Sent", "", false);
        drawerItem(R.drawable.ic_schedule, "Scheduled", count(countIn("Scheduled")), false);
        drawerItem(R.drawable.ic_draft, "Drafts", count(countIn("Drafts")), false);
        drawerItem(R.drawable.ic_mail_outline, "All mail", "", false);
        drawerItem(R.drawable.ic_report, "Spam", "", false);
        drawerItem(R.drawable.ic_delete, "Bin", "", false);
        drawerHeader("Google apps");
        drawerItem(R.drawable.ic_calendar, "Calendar", "", false);
        drawerItem(R.drawable.ic_person, "Contacts", "", false);
        drawerDivider();
        drawerItem(R.drawable.ic_settings, "Settings", "", false);
        drawerItem(R.drawable.ic_help, "Help & feedback", "", false);
    }

    private String count(int n) { return n > 0 ? String.valueOf(n) : ""; }

    private String newCount(String category) {
        int n = unreadIn(category);
        return n > 0 ? n + " new" : "";
    }

    private void drawerItem(int icon, String label, String count, boolean categoryPill) {
        boolean active = label.equals(folder) && !searchMode;
        int fg = color(active ? R.color.on_drawer_selected : R.color.on_surface_variant);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(20), 0);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33808080),
                active ? pill(color(R.color.drawer_selected), 28) : null, pill(0xFFFFFFFF, 28)));

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(icon);
        iconView.setImageTintList(ColorStateList.valueOf(fg));
        row.addView(iconView, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setSingleLine(true);
        labelView.setTypeface(active ? BOLD : MEDIUM);
        labelView.setTextColor(active ? fg : color(R.color.on_surface));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMarginStart(dp(20));
        row.addView(labelView, labelParams);

        if (!count.isEmpty()) {
            TextView countView = new TextView(this);
            countView.setText(count);
            countView.setTextSize(12);
            countView.setTypeface(active ? BOLD : MEDIUM);
            countView.setGravity(Gravity.CENTER);
            if (categoryPill) {
                int[] colors = categoryColors(label);
                countView.setTextColor(colors[2]);
                countView.setBackground(pill(colors[1], 10));
                countView.setPadding(dp(8), 0, dp(8), 0);
                row.addView(countView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(20)));
            } else {
                countView.setTextColor(fg);
                row.addView(countView);
            }
        }

        row.setOnClickListener(v -> onDrawerItem(label));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        params.setMargins(dp(12), 0, dp(12), 0);
        drawerList.addView(row, params);
    }

    private void drawerHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(14);
        header.setTypeface(MEDIUM);
        header.setTextColor(color(R.color.on_surface_variant));
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(28), dp(8), dp(16), 0);
        drawerList.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
    }

    private void drawerDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(color(R.color.divider));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.setMargins(dp(28), dp(8), dp(28), dp(8));
        drawerList.addView(divider, params);
    }

    private void onDrawerItem(String label) {
        switch (label) {
            case "Settings":
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case "Calendar": case "Contacts": case "Help & feedback":
                toast(label);
                break;
            default:
                if (searchMode) exitSearch();
                selectFolder(label);
        }
        closeDrawer();
    }

    @Override public void onBackPressed() {
        if (drawerOpen) closeDrawer();
        else if (!selected.isEmpty()) { selected.clear(); refresh(); }
        else if (searchMode) exitSearch();
        else if (!folder.equals("Primary")) selectFolder("Primary");
        else super.onBackPressed();
    }

    private void hideKeyboard() {
        imm().hideSoftInputFromWindow(searchField.getWindowToken(), 0);
    }

    private InputMethodManager imm() {
        return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    // Gmail marks drafts in the list with a red "Draft" in place of the sender.
    private CharSequence draftLabel(Mail m) {
        SpannableString label = new SpannableString(m.to.equals("me") ? "Draft" : "Draft, to " + m.to);
        label.setSpan(new ForegroundColorSpan(color(R.color.draft)), 0, 5, 0);
        return label;
    }

    private class MailAdapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int position) { return rows.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int position) { return rows.get(position) instanceof Mail ? 0 : 1; }

        @Override public View getView(int position, View row, ViewGroup parent) {
            Object item = rows.get(position);
            if (item instanceof Mail) {
                if (row == null) {
                    row = getLayoutInflater().inflate(R.layout.item_mail, parent, false);
                    row.setTag(new Holder(row));
                }
                bindMail((Holder) row.getTag(), (Mail) item);
            } else {
                if (row == null) row = getLayoutInflater().inflate(R.layout.item_category, parent, false);
                bindCategory(row, (String) item);
            }
            return row;
        }
    }

    private void bindMail(Holder h, Mail m) {
        boolean isSelected = selected.contains(m);
        h.card.setBackground(pill(color(isSelected ? R.color.selected_row : R.color.card), 4));
        h.avatar.setBackground(oval(isSelected ? color(R.color.check_bg) : m.color));
        h.avatar.setText(isSelected ? "" : m.isOutgoing() ? "A" : m.initial());
        h.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        if (m.category.equals("Drafts")) h.sender.setText(draftLabel(m));
        else h.sender.setText(m.category.equals("Sent") || m.category.equals("Scheduled") ? "To: " + m.to : m.sender);
        h.threadCount.setText(m.threadCount > 1 ? String.valueOf(m.threadCount) : "");
        h.important.setVisibility(m.important ? View.VISIBLE : View.GONE);
        h.subject.setText(m.subject);
        h.time.setText(m.time);
        h.snippet.setText(m.snippet);
        h.unreadDot.setVisibility(m.unread ? View.VISIBLE : View.GONE);
        h.unreadDot.setBackground(oval(color(R.color.unread_dot)));
        int text = color(m.unread ? R.color.on_surface : R.color.on_surface_variant);
        Typeface face = m.unread ? BOLD : REGULAR;
        for (TextView t : new TextView[] { h.sender, h.subject, h.time }) {
            t.setTypeface(face);
            t.setTextColor(text);
        }

        h.star.setImageResource(m.starred ? R.drawable.ic_star : R.drawable.ic_star_border);
        h.star.setImageTintList(ColorStateList.valueOf(color(m.starred ? R.color.star_active : R.color.on_surface_variant)));
        h.attachment.setVisibility(m.attachment == null ? View.GONE : View.VISIBLE);
        if (m.attachment != null) h.attachmentName.setText(m.attachment);

        h.card.setOnClickListener(v -> onMailClick(m));
        h.card.setOnLongClickListener(v -> { toggleSelected(m); return true; });
        h.avatarFrame.setOnClickListener(v -> toggleSelected(m));
        h.star.setOnClickListener(v -> { m.starred = !m.starred; refresh(); });
    }

    private void bindCategory(View row, String category) {
        int[] colors = categoryColors(category);
        ImageView icon = row.findViewById(R.id.category_icon);
        icon.setImageResource(categoryIcon(category));
        icon.setImageTintList(ColorStateList.valueOf(colors[0]));
        ((TextView) row.findViewById(R.id.category_title)).setText(category);
        Mail newest = newestIn(category);
        ((TextView) row.findViewById(R.id.category_snippet))
                .setText(newest == null ? "" : newest.sender + " — " + newest.subject);
        TextView badge = row.findViewById(R.id.category_badge);
        badge.setText(unreadIn(category) + " new");
        badge.setTextColor(colors[2]);
        badge.setBackground(pill(colors[1], 14));
        row.setOnClickListener(v -> selectFolder(category));
    }

    private static class Holder {
        final View card, avatarFrame, check, attachment, unreadDot, important;
        final TextView avatar, sender, threadCount, time, subject, snippet, attachmentName;
        final ImageView star;

        Holder(View row) {
            card = row.findViewById(R.id.card);
            avatarFrame = row.findViewById(R.id.avatar_frame);
            avatar = row.findViewById(R.id.avatar);
            check = row.findViewById(R.id.avatar_check);
            important = row.findViewById(R.id.important);
            sender = row.findViewById(R.id.sender);
            threadCount = row.findViewById(R.id.thread_count);
            time = row.findViewById(R.id.time);
            unreadDot = row.findViewById(R.id.unread_dot);
            subject = row.findViewById(R.id.subject);
            snippet = row.findViewById(R.id.snippet);
            star = row.findViewById(R.id.star);
            attachment = row.findViewById(R.id.attachment);
            attachmentName = row.findViewById(R.id.attachment_name);
        }
    }

    private static class Removed {
        final int index;
        final Mail mail;

        Removed(int index, Mail mail) { this.index = index; this.mail = mail; }
    }
}
