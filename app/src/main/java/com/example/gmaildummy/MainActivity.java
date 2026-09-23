package com.example.gmaildummy;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.Toast;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int ON_SURFACE = 0xFF1F1F1F;
    private static final int ON_SURFACE_VARIANT = 0xFF444746;
    private static final int SELECTED = 0xFFD3E3FD;
    private static final int ON_SELECTED = 0xFF001D35;
    private static final int CHECK = 0xFF0B57D0;
    private static final int STAR = 0xFFF4B400;
    private static final int GREEN = 0xFF188038, BLUE = 0xFF1A73E8, ORANGE = 0xFFE37400;
    private static final int DRAWER_WIDTH_DP = 304;

    private static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);

    private final List<Mail> mails = new ArrayList<>();
    private final List<Mail> shown = new ArrayList<>();
    private final Set<Mail> selected = new LinkedHashSet<>();
    private final List<Removed> lastRemoved = new ArrayList<>();
    private final MailAdapter adapter = new MailAdapter();
    private final Runnable hideSnackbar = this::hideSnackbar;

    private String folder = "Primary";
    private String query = "";
    private boolean searchMode, drawerOpen, fabExtended = true;
    private int bottomInset;

    private View searchBar, selectionBar, account, fab, fabLabel, scrim, drawer, snackbar;
    private ImageView menuButton;
    private EditText searchField;
    private TextView selectionCount, folderLabel, emptyView, snackbarText, mailBadge;
    private ListView list;
    private LinearLayout bottomNav, drawerList, categories;

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

        setupEdgeToEdge();
        seedMails();
        setupList();
        setupTopBar();
        setupBottomNav();
        setupFab();
        scrim.setOnClickListener(v -> closeDrawer());
        findViewById(R.id.snackbar_action).setOnClickListener(v -> undoRemove());
        refresh();
    }

    // Draw behind the status and navigation bars (enforced from Android 15) and pad the UI by the insets.
    private void setupEdgeToEdge() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 27) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
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

    private void seedMails() {
        mails.add(new Mail("Primary", "Priya Sharma", "Weekend plans", "Are we still on for brunch this Saturday? I found a lovely new place downtown.", "10:42 AM", 0xFF8E24AA, true, false, null));
        mails.add(new Mail("Primary", "Design Team", "Q4 product launch deck ✨", "Hi all, the latest mockups are ready for review. Please leave your comments by Friday.", "9:18 AM", 0xFF00897B, true, false, "Q4_Launch_Deck.pdf"));
        mails.add(new Mail("Primary", "Alex Johnson", "Re: Project timeline", "Thanks for the update. I’ll share the revised timeline this afternoon.", "8:05 AM", 0xFF039BE5, true, true, null));
        mails.add(new Mail("Primary", "Rahul Verma", "Invoice for September", "Please find attached the invoice for September. Let me know if you have any questions.", "Sep 22", 0xFFF4511E, false, false, "Invoice_Sep_2026.pdf"));
        mails.add(new Mail("Primary", "Neha Kapoor", "Birthday party pics 🎉", "Here are all the photos from Saturday! Thanks again for coming.", "Sep 22", 0xFFD81B60, false, true, null));
        mails.add(new Mail("Primary", "Google", "Security alert", "A new sign-in on Pixel 9 was detected. If this was you, you don’t need to do anything.", "Sep 21", 0xFF1A73E8, false, false, null));
        mails.add(new Mail("Primary", "Mom", "Dinner on Sunday?", "Your dad is making his famous biryani. Let me know if you can come!", "Sep 20", 0xFF43A047, false, false, null));
        mails.add(new Mail("Primary", "Karan Mehta", "Offsite agenda", "Sharing the draft agenda for next week’s team offsite. Feel free to add topics.", "Sep 19", 0xFF3949AB, false, false, null));
        mails.add(new Mail("Primary", "Sarah Lee", "Coffee next week?", "It’s been a while! Would love to catch up if you’re free Tuesday or Wednesday.", "Sep 18", 0xFF6D4C41, false, false, null));
        mails.add(new Mail("Promotions", "Spotify", "Your Daily Mix is ready", "A fresh playlist picked just for you is waiting.", "7:30 AM", 0xFF1DB954, true, false, null));
        mails.add(new Mail("Promotions", "Medium Daily Digest", "Stories you might enjoy", "The latest ideas and perspectives from writers you follow.", "6:10 AM", 0xFF212121, true, false, null));
        mails.add(new Mail("Social", "LinkedIn", "You appeared in 12 searches this week", "See who’s looking at your profile and grow your network.", "9:02 AM", 0xFF0A66C2, true, false, null));
        mails.add(new Mail("Social", "Meetup", "New event: Android Devs Bangalore", "Join us for talks on Compose, performance and more.", "Sep 21", 0xFFE53935, false, false, null));
        mails.add(new Mail("Updates", "GitHub", "[gmail-dummy-app] Build succeeded", "Build APK workflow completed successfully on main.", "Sep 22", 0xFF24292F, false, false, null));
        mails.add(new Mail("Updates", "Amazon.in", "Your order has shipped", "Your package is on its way and will arrive by Thursday.", "Sep 20", 0xFFFF9900, false, false, null));
    }

    private void setupList() {
        View header = getLayoutInflater().inflate(R.layout.header_inbox, list, false);
        folderLabel = header.findViewById(R.id.folder_label);
        categories = header.findViewById(R.id.categories);
        list.addHeaderView(header, null, false);
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
        account.setOnClickListener(v -> toast("alex.johnson@gmail.com"));
        findViewById(R.id.btn_close_selection).setOnClickListener(v -> { selected.clear(); refresh(); });
        findViewById(R.id.btn_archive).setOnClickListener(v -> removeSelected("archived"));
        findViewById(R.id.btn_delete).setOnClickListener(v -> removeSelected("moved to Bin"));
        findViewById(R.id.btn_mark_unread).setOnClickListener(v -> toggleReadSelected());
    }

    private void setupBottomNav() {
        mailBadge = addNavItem(R.drawable.ic_mail, "Mail", true);
        addNavItem(R.drawable.ic_chat, "Chat", false);
        addNavItem(R.drawable.ic_videocam, "Meet", false);
    }

    private TextView addNavItem(int icon, String label, boolean active) {
        View item = getLayoutInflater().inflate(R.layout.item_nav, bottomNav, false);
        ImageView iconView = item.findViewById(R.id.nav_icon);
        iconView.setImageResource(icon);
        iconView.setImageTintList(ColorStateList.valueOf(active ? ON_SELECTED : ON_SURFACE_VARIANT));
        if (active) item.findViewById(R.id.nav_indicator).setBackgroundResource(R.drawable.bg_nav_indicator);
        TextView labelView = item.findViewById(R.id.nav_label);
        labelView.setText(label);
        labelView.setTextColor(active ? ON_SURFACE : ON_SURFACE_VARIANT);
        if (!active) item.setOnClickListener(v -> toast(label));
        bottomNav.addView(item);
        return item.findViewById(R.id.nav_badge);
    }

    private void setupFab() {
        fab.setOnClickListener(v -> toast("Compose"));
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
        fab.setPadding(dp(16), 0, dp(extended ? 20 : 16), 0);
    }

    private void refresh() {
        shown.clear();
        for (Mail m : mails) if (matches(m)) shown.add(m);
        boolean inbox = folder.equals("Primary") && !searchMode;
        folderLabel.setText(searchMode ? (query.isEmpty() ? "All mail" : "Results") : folder);
        buildCategories(inbox);
        emptyView.setText(searchMode ? "No results for “" + query + "”" : "Nothing in " + folder);
        emptyView.setVisibility(shown.isEmpty() && categories.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();

        boolean selecting = !selected.isEmpty();
        selectionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        searchBar.setVisibility(selecting ? View.GONE : View.VISIBLE);
        selectionCount.setText(String.valueOf(selected.size()));

        int unread = unreadIn("Primary");
        mailBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        mailBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        buildDrawer();
    }

    private boolean matches(Mail m) {
        if (searchMode) {
            return query.isEmpty()
                    || (m.sender + " " + m.subject + " " + m.snippet).toLowerCase(Locale.ROOT).contains(query);
        }
        switch (folder) {
            case "Primary": case "Promotions": case "Social": case "Updates": return m.category.equals(folder);
            case "All inboxes": case "All mail": return true;
            case "Starred": return m.starred;
            default: return false;
        }
    }

    private int unreadIn(String category) {
        int count = 0;
        for (Mail m : mails) if (m.unread && m.category.equals(category)) count++;
        return count;
    }

    // Gmail lists the other tabs that have new mail above the Primary inbox.
    private void buildCategories(boolean visible) {
        categories.removeAllViews();
        if (!visible) return;
        addCategory(R.drawable.ic_tag, GREEN, "Promotions");
        addCategory(R.drawable.ic_people, BLUE, "Social");
        addCategory(R.drawable.ic_info, ORANGE, "Updates");
    }

    private void addCategory(int icon, int color, String name) {
        int unread = unreadIn(name);
        if (unread == 0) return;
        List<String> senders = new ArrayList<>();
        for (Mail m : mails) if (m.category.equals(name) && !senders.contains(m.sender)) senders.add(m.sender);
        View row = getLayoutInflater().inflate(R.layout.item_category, categories, false);
        ImageView iconView = row.findViewById(R.id.category_icon);
        iconView.setImageResource(icon);
        iconView.setImageTintList(ColorStateList.valueOf(color));
        ((TextView) row.findViewById(R.id.category_title)).setText(name);
        ((TextView) row.findViewById(R.id.category_snippet)).setText(TextUtils.join(", ", senders));
        TextView badge = row.findViewById(R.id.category_badge);
        badge.setText(unread + " new");
        badge.setBackground(pill(color, 10));
        row.setOnClickListener(v -> selectFolder(name));
        categories.addView(row);
    }

    private void onMailClick(Mail m) {
        if (!selected.isEmpty()) { toggleSelected(m); return; }
        m.unread = false;
        refresh();
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
        showSnackbar(lastRemoved.size() + " " + verb);
    }

    private void undoRemove() {
        for (Removed r : lastRemoved) mails.add(Math.min(r.index, mails.size()), r.mail);
        lastRemoved.clear();
        hideSnackbar();
        refresh();
    }

    private void showSnackbar(String text) {
        snackbarText.setText(text);
        snackbar.setVisibility(View.VISIBLE);
        snackbar.removeCallbacks(hideSnackbar);
        snackbar.postDelayed(hideSnackbar, 4000);
        fab.animate().translationY(-dp(56)).setDuration(150).start();
    }

    private void hideSnackbar() {
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
        hideKeyboard();
        menuButton.setImageResource(R.drawable.ic_menu);
        account.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
        updateListPadding();
        refresh();
    }

    private void updateListPadding() {
        list.setPadding(0, 0, 0, searchMode ? bottomInset + dp(8) : dp(88));
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
        TextView title = new TextView(this);
        title.setText("Gmail");
        title.setTextColor(0xFFC5221F);
        title.setTextSize(22);
        title.setTypeface(REGULAR);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(28), 0, dp(16), 0);
        drawerList.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        int primaryUnread = unreadIn("Primary");
        drawerItem(R.drawable.ic_all_inbox, "All inboxes", count(primaryUnread), 0);
        drawerDivider();
        drawerItem(R.drawable.ic_inbox, "Primary", count(primaryUnread), 0);
        drawerItem(R.drawable.ic_tag, "Promotions", newCount("Promotions"), GREEN);
        drawerItem(R.drawable.ic_people, "Social", newCount("Social"), BLUE);
        drawerItem(R.drawable.ic_info, "Updates", newCount("Updates"), ORANGE);
        drawerHeader("All labels");
        drawerItem(R.drawable.ic_star_border, "Starred", "", 0);
        drawerItem(R.drawable.ic_schedule, "Snoozed", "", 0);
        drawerItem(R.drawable.ic_label_important, "Important", "", 0);
        drawerItem(R.drawable.ic_send, "Sent", "", 0);
        drawerItem(R.drawable.ic_schedule, "Scheduled", "", 0);
        drawerItem(R.drawable.ic_draft, "Drafts", "", 0);
        drawerItem(R.drawable.ic_mail_outline, "All mail", "", 0);
        drawerItem(R.drawable.ic_report, "Spam", "", 0);
        drawerItem(R.drawable.ic_delete, "Bin", "", 0);
        drawerHeader("Google apps");
        drawerItem(R.drawable.ic_calendar, "Calendar", "", 0);
        drawerItem(R.drawable.ic_person, "Contacts", "", 0);
        drawerDivider();
        drawerItem(R.drawable.ic_settings, "Settings", "", 0);
        drawerItem(R.drawable.ic_help, "Help & feedback", "", 0);
    }

    private String count(int n) { return n > 0 ? String.valueOf(n) : ""; }

    private String newCount(String category) {
        int n = unreadIn(category);
        return n > 0 ? n + " new" : "";
    }

    private void drawerItem(int icon, String label, String count, int pillColor) {
        boolean active = label.equals(folder) && !searchMode;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(20), 0);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1F1F1F1F),
                active ? pill(SELECTED, 28) : null, pill(Color.WHITE, 28)));

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(icon);
        iconView.setImageTintList(ColorStateList.valueOf(active ? ON_SELECTED : ON_SURFACE_VARIANT));
        row.addView(iconView, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setSingleLine(true);
        labelView.setTypeface(active ? BOLD : MEDIUM);
        labelView.setTextColor(active ? ON_SELECTED : ON_SURFACE_VARIANT);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMarginStart(dp(20));
        row.addView(labelView, labelParams);

        if (!count.isEmpty()) {
            TextView countView = new TextView(this);
            countView.setText(count);
            countView.setTextSize(12);
            countView.setTypeface(active ? BOLD : MEDIUM);
            countView.setGravity(Gravity.CENTER);
            if (pillColor != 0) {
                countView.setTextColor(Color.WHITE);
                countView.setBackground(pill(pillColor, 10));
                countView.setPadding(dp(8), 0, dp(8), 0);
                row.addView(countView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(20)));
            } else {
                countView.setTextColor(active ? ON_SELECTED : ON_SURFACE_VARIANT);
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
        header.setTextColor(ON_SURFACE_VARIANT);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(28), dp(8), dp(16), 0);
        drawerList.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
    }

    private void drawerDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(0xFFE1E3E1);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.setMargins(dp(28), dp(8), dp(28), dp(8));
        drawerList.addView(divider, params);
    }

    private void onDrawerItem(String label) {
        switch (label) {
            case "Calendar": case "Contacts": case "Settings": case "Help & feedback":
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

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable pill(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(radiusDp));
        d.setColor(color);
        return d;
    }

    private static GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private class MailAdapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Mail getItem(int position) { return shown.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View row, ViewGroup parent) {
            Holder h;
            if (row == null) {
                row = getLayoutInflater().inflate(R.layout.item_mail, parent, false);
                h = new Holder(row);
                row.setTag(h);
            } else {
                h = (Holder) row.getTag();
            }
            Mail m = shown.get(position);
            boolean isSelected = selected.contains(m);
            row.setBackgroundColor(isSelected ? SELECTED : Color.TRANSPARENT);
            h.avatar.setBackground(oval(isSelected ? CHECK : m.color));
            h.avatar.setText(isSelected ? "" : m.initial());
            h.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            h.sender.setText(m.sender);
            h.subject.setText(m.subject);
            h.time.setText(m.time);
            h.snippet.setText(m.snippet);
            int color = m.unread ? ON_SURFACE : ON_SURFACE_VARIANT;
            Typeface face = m.unread ? BOLD : REGULAR;
            for (TextView t : new TextView[] { h.sender, h.subject, h.time }) {
                t.setTypeface(face);
                t.setTextColor(color);
            }

            h.star.setImageResource(m.starred ? R.drawable.ic_star : R.drawable.ic_star_border);
            h.star.setImageTintList(ColorStateList.valueOf(m.starred ? STAR : ON_SURFACE_VARIANT));
            h.attachment.setVisibility(m.attachment == null ? View.GONE : View.VISIBLE);
            if (m.attachment != null) h.attachmentName.setText(m.attachment);

            row.setOnClickListener(v -> onMailClick(m));
            row.setOnLongClickListener(v -> { toggleSelected(m); return true; });
            h.avatarFrame.setOnClickListener(v -> toggleSelected(m));
            h.star.setOnClickListener(v -> { m.starred = !m.starred; refresh(); });
            return row;
        }
    }

    private static class Holder {
        final View avatarFrame, check, attachment;
        final TextView avatar, sender, time, subject, snippet, attachmentName;
        final ImageView star;

        Holder(View row) {
            avatarFrame = row.findViewById(R.id.avatar_frame);
            avatar = row.findViewById(R.id.avatar);
            check = row.findViewById(R.id.avatar_check);
            sender = row.findViewById(R.id.sender);
            time = row.findViewById(R.id.time);
            subject = row.findViewById(R.id.subject);
            snippet = row.findViewById(R.id.snippet);
            star = row.findViewById(R.id.star);
            attachment = row.findViewById(R.id.attachment);
            attachmentName = row.findViewById(R.id.attachment_name);
        }
    }

    private static class Mail {
        final String category, sender, subject, snippet, time, attachment;
        final int color;
        boolean unread, starred;

        Mail(String category, String sender, String subject, String snippet, String time,
             int color, boolean unread, boolean starred, String attachment) {
            this.category = category; this.sender = sender; this.subject = subject; this.snippet = snippet;
            this.time = time; this.color = color; this.unread = unread; this.starred = starred;
            this.attachment = attachment;
        }

        String initial() { return sender.substring(0, 1).toUpperCase(Locale.ROOT); }
    }

    private static class Removed {
        final int index;
        final Mail mail;

        Removed(int index, Mail mail) { this.index = index; this.mail = mail; }
    }
}
