package com.example.gmaildummy;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gmail's To / Cc / Bcc field: each address becomes a chip once you type a comma, pick a suggestion,
 * press Next or leave the field. Backspace in the empty field removes the last chip; tapping a chip
 * offers to remove it. Unrecognised addresses are shown in red.
 */
public class RecipientField extends ViewGroup {
    private static final Pattern NAMED = Pattern.compile("(.*)<(.+)>");
    private static final int[] AVATAR_COLORS = { 0xFF1A73E8, 0xFFD93025, 0xFF188038, 0xFFE37400, 0xFF8E24AA, 0xFF00897B };

    private final List<Recipient> recipients = new ArrayList<>();
    private final List<MailStore.Contact> contacts = MailStore.contacts();
    private final AutoCompleteTextView input;
    private int[] lefts = new int[0], tops = new int[0];

    public RecipientField(Context context, AttributeSet attrs) {
        super(context, attrs);
        input = new AutoCompleteTextView(context);
        input.setBackground(null);
        input.setPadding(0, 0, 0, 0);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(context.getColor(R.color.on_surface));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        input.setThreshold(1);
        input.setAdapter(new Suggestions());
        input.setOnItemClickListener((parent, view, position, id) -> {
            input.setText("", false);
            add((MailStore.Contact) parent.getItemAtPosition(position));
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                String text = s.toString();
                int cut = Math.max(text.lastIndexOf(','), text.lastIndexOf(';'));
                if (cut < 0) return;
                for (String token : text.substring(0, cut).split("[,;]")) addToken(token);
                s.delete(0, cut + 1);
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> commit());
        input.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) commit(); });
        input.setOnKeyListener((v, keyCode, event) -> {
            boolean atStart = input.getSelectionStart() == 0 && input.getSelectionEnd() == 0;
            if (keyCode != KeyEvent.KEYCODE_DEL || event.getAction() != KeyEvent.ACTION_DOWN
                    || !atStart || recipients.isEmpty()) return false;
            remove(recipients.get(recipients.size() - 1));
            return true;
        });
        addView(input);
        setOnClickListener(v -> focus());
    }

    // ---- Used by the compose screen ----

    void setRecipients(String list) {
        for (Recipient r : new ArrayList<>(recipients)) remove(r);
        if (list != null) for (String token : list.split(",")) addToken(token);
    }

    void add(MailStore.Contact c) {
        add(new Recipient(c.name, c.email, c.color, true));
    }

    /** Recipients as Gmail lists them, e.g. "Priya Sharma, sam@work.dev". Commits any half-typed address first. */
    String joined() {
        commit();
        List<String> names = new ArrayList<>();
        for (Recipient r : recipients) names.add(r.display());
        return TextUtils.join(", ", names);
    }

    boolean isEmpty() {
        return recipients.isEmpty() && input.getText().toString().trim().isEmpty();
    }

    /** The first address that doesn't look like an email address, or null. */
    String firstInvalid() {
        commit();
        for (Recipient r : recipients) if (!r.valid) return r.email;
        return null;
    }

    void focus() {
        input.requestFocus();
        input.post(() -> ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT));
    }

    /** Turns typed text into chips. Returns whether there was anything to commit. */
    private boolean commit() {
        String text = input.getText().toString();
        if (text.trim().isEmpty()) return false;
        input.setText("", false);
        for (String token : text.split("[,;]")) addToken(token);
        return true;
    }

    private void addToken(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) return;
        String name = null, email = text;
        Matcher named = NAMED.matcher(text);
        if (named.matches()) {
            name = named.group(1).replace("\"", "").trim();
            email = named.group(2).trim();
            if (name.isEmpty()) name = null;
        }
        for (MailStore.Contact c : contacts) {
            if (c.email.equalsIgnoreCase(email) || c.name.equalsIgnoreCase(text)) { add(c); return; }
        }
        int color = AVATAR_COLORS[(email.toLowerCase(Locale.ROOT).hashCode() & 0x7fffffff) % AVATAR_COLORS.length];
        add(new Recipient(name, email, color, Patterns.EMAIL_ADDRESS.matcher(email).matches()));
    }

    private boolean contains(String email) {
        for (Recipient r : recipients) if (r.email.equalsIgnoreCase(email)) return true;
        return false;
    }

    private void add(Recipient r) {
        if (contains(r.email)) return;
        recipients.add(r);
        r.chip = chip(r);
        addView(r.chip, getChildCount() - 1);
    }

    private void remove(Recipient r) {
        recipients.remove(r);
        removeView(r.chip);
    }

    private View chip(Recipient r) {
        Context context = getContext();
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(4), dp(4), dp(12), dp(4));
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(16));
        shape.setColor(context.getColor(R.color.surface));
        shape.setStroke(dp(1), context.getColor(r.valid ? R.color.outline : R.color.draft));
        chip.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33808080), shape, null));

        TextView avatar = new TextView(context);
        avatar.setText(r.display().substring(0, 1).toUpperCase(Locale.ROOT));
        avatar.setTextColor(0xFFFFFFFF);
        avatar.setTextSize(12);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(BaseActivity.oval(r.valid ? r.color : context.getColor(R.color.draft)));
        chip.addView(avatar, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView name = new TextView(context);
        name.setText(r.display());
        name.setTextSize(14);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setMaxWidth(dp(200));
        name.setTextColor(context.getColor(r.valid ? R.color.on_surface : R.color.draft));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        nameParams.setMarginStart(dp(8));
        chip.addView(name, nameParams);

        chip.setContentDescription(r.display() + ", " + r.email);
        chip.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(context, v);
            menu.getMenu().add(0, 0, 0, r.email).setEnabled(false);
            menu.getMenu().add(0, 1, 1, "Remove");
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) remove(r);
                return true;
            });
            menu.show();
        });
        return chip;
    }

    // ---- Flow layout: chips wrap onto rows of equal height; the text box fills the rest of the last row ----

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int row = dp(52), gap = dp(6), minInput = dp(96);
        int start = getPaddingLeft(), end = width - getPaddingRight();
        int count = getChildCount();
        if (lefts.length < count) { lefts = new int[count]; tops = new int[count]; }
        int x = start, line = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childWidth;
            if (child == input) {
                if (end - x < minInput && x > start) { line++; x = start; }
                childWidth = Math.max(end - x, 0);
                child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(row, MeasureSpec.EXACTLY));
            } else {
                child.measure(MeasureSpec.makeMeasureSpec(Math.max(end - start, 0), MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(row, MeasureSpec.AT_MOST));
                childWidth = child.getMeasuredWidth();
                if (x + childWidth > end && x > start) { line++; x = start; }
            }
            lefts[i] = x;
            tops[i] = getPaddingTop() + line * row + (row - child.getMeasuredHeight()) / 2;
            x += childWidth + gap;
        }
        setMeasuredDimension(width, getPaddingTop() + (line + 1) * row + getPaddingBottom());
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(lefts[i], tops[i], lefts[i] + child.getMeasuredWidth(), tops[i] + child.getMeasuredHeight());
        }
        // Suggestions drop down below the whole field, across the full screen width.
        int[] location = new int[2];
        getLocationOnScreen(location);
        input.setDropDownWidth(getResources().getDisplayMetrics().widthPixels);
        input.setDropDownHorizontalOffset(-location[0]);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getId() != NO_ID) input.setDropDownAnchor(getId());
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Recipient {
        final String name, email;
        final int color;
        final boolean valid;
        View chip;

        Recipient(String name, String email, int color, boolean valid) {
            this.name = name; this.email = email; this.color = color; this.valid = valid;
        }

        String display() { return name != null ? name : email; }
    }

    /** Contacts whose name or address contains what you've typed, skipping ones already added. */
    private final class Suggestions extends BaseAdapter implements Filterable {
        private List<MailStore.Contact> shown = new ArrayList<>();

        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int position) { return shown.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View row, ViewGroup parent) {
            Context context = getContext();
            if (row == null) {
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setGravity(Gravity.CENTER_VERTICAL);
                layout.setPadding(dp(16), dp(8), dp(16), dp(8));

                TextView avatar = new TextView(context);
                avatar.setTextColor(0xFFFFFFFF);
                avatar.setTextSize(16);
                avatar.setGravity(Gravity.CENTER);
                layout.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));

                LinearLayout text = new LinearLayout(context);
                text.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
                textParams.setMarginStart(dp(16));
                layout.addView(text, textParams);

                TextView name = new TextView(context);
                name.setTextSize(16);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                name.setTextColor(context.getColor(R.color.on_surface));
                text.addView(name);

                TextView email = new TextView(context);
                email.setTextSize(14);
                email.setSingleLine(true);
                email.setEllipsize(TextUtils.TruncateAt.END);
                email.setTextColor(context.getColor(R.color.on_surface_variant));
                text.addView(email);
                row = layout;
            }
            MailStore.Contact c = shown.get(position);
            LinearLayout layout = (LinearLayout) row;
            TextView avatar = (TextView) layout.getChildAt(0);
            LinearLayout text = (LinearLayout) layout.getChildAt(1);
            avatar.setText(c.name.substring(0, 1).toUpperCase(Locale.ROOT));
            avatar.setBackground(BaseActivity.oval(c.color));
            ((TextView) text.getChildAt(0)).setText(c.name);
            ((TextView) text.getChildAt(1)).setText(c.email);
            return row;
        }

        @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence typed) {
                    List<MailStore.Contact> matches = new ArrayList<>();
                    String needle = typed == null ? "" : typed.toString().trim().toLowerCase(Locale.ROOT);
                    if (!needle.isEmpty()) {
                        for (MailStore.Contact c : contacts) {
                            if (c.name.toLowerCase(Locale.ROOT).contains(needle)
                                    || c.email.toLowerCase(Locale.ROOT).contains(needle)) matches.add(c);
                        }
                    }
                    FilterResults results = new FilterResults();
                    results.values = matches;
                    results.count = matches.size();
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override protected void publishResults(CharSequence typed, FilterResults results) {
                    List<MailStore.Contact> matches = new ArrayList<>();
                    if (results.values != null) {
                        for (MailStore.Contact c : (List<MailStore.Contact>) results.values) {
                            if (!contains(c.email)) matches.add(c);
                        }
                    }
                    shown = matches;
                    if (shown.isEmpty()) notifyDataSetInvalidated(); else notifyDataSetChanged();
                }

                @Override public CharSequence convertResultToString(Object result) {
                    return ((MailStore.Contact) result).email;
                }
            };
        }
    }
}
