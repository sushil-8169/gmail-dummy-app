package com.example.gmaildummy;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Gmail's Meet tab: start or join a meeting, above a swipeable intro carousel. */
public class MeetActivity extends BaseActivity {
    private static final int[] ART = { R.drawable.art_meet_link, R.drawable.art_meet_safe };
    private static final String[] TITLES = { "Get a link that you can share", "Your meeting is safe" };
    private static final String[] BODIES = {
            "Tap New meeting to get a link that you can send to people that you want to meet with",
            "No one can join a meeting unless invited or admitted by the host" };

    private int page;
    private View carousel;
    private ImageView art;
    private TextView title, body, mailBadge;
    private View[] dots;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_meet);
        View root = findViewById(R.id.root);
        LinearLayout nav = findViewById(R.id.bottom_nav);
        mailBadge = buildBottomNav(nav, true, () -> openMail(false));
        padForSystemBars(root, root, nav);

        findViewById(R.id.btn_menu).setOnClickListener(v -> openMail(true));
        findViewById(R.id.account).setOnClickListener(v -> toast(MailStore.ME));
        findViewById(R.id.btn_new_meeting).setOnClickListener(v -> showNewMeetingSheet());
        findViewById(R.id.btn_join).setOnClickListener(v -> startActivity(new Intent(this, JoinMeetingActivity.class)));
        setupCarousel();
    }

    @Override protected void onResume() {
        super.onResume();
        showUnreadBadge(mailBadge, MailStore.unreadIn("Primary"));
    }

    /** Back to the inbox, optionally with the navigation drawer open. */
    private void openMail(boolean withDrawer) {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_DRAWER, withDrawer));
        overridePendingTransition(0, 0);
    }

    @Override public void onBackPressed() {
        finish();
        overridePendingTransition(0, 0);
    }

    // ---- Carousel: swipe or tap a dot to change page ----

    private void setupCarousel() {
        carousel = findViewById(R.id.carousel);
        art = findViewById(R.id.carousel_image);
        title = findViewById(R.id.carousel_title);
        body = findViewById(R.id.carousel_body);
        LinearLayout dotRow = findViewById(R.id.carousel_dots);
        dots = new View[TITLES.length];
        for (int i = 0; i < dots.length; i++) {
            int index = i;
            dots[i] = new View(this);
            dots[i].setContentDescription("Page " + (i + 1) + " of " + dots.length);
            dots[i].setOnClickListener(v -> showPage(index));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(8), dp(8));
            params.setMargins(dp(4), 0, dp(4), 0);
            dotRow.addView(dots[i], params);
        }

        GestureDetector swipe = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) < Math.abs(velocityY)) return false;
                showPage(velocityX < 0 ? page + 1 : page - 1);
                return true;
            }
        });
        carousel.setOnTouchListener((v, event) -> swipe.onTouchEvent(event));
        bindPage();
    }

    private void showPage(int index) {
        int next = (index + TITLES.length) % TITLES.length;
        if (next == page) return;
        page = next;
        carousel.animate().alpha(0).setDuration(120).withEndAction(() -> {
            bindPage();
            carousel.animate().alpha(1).setDuration(180).start();
        }).start();
    }

    private void bindPage() {
        art.setImageResource(ART[page]);
        title.setText(TITLES[page]);
        body.setText(BODIES[page]);
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackground(oval(color(i == page ? R.color.primary : R.color.outline)));
        }
    }

    // ---- New meeting ----

    private void showNewMeetingSheet() {
        Dialog sheet = sheet(R.layout.sheet_new_meeting);
        onSheetClick(sheet, R.id.opt_link, () -> showLinkSheet(Meet.newCode()));
        onSheetClick(sheet, R.id.opt_instant, () -> startCall(Meet.newCode()));
        onSheetClick(sheet, R.id.opt_calendar, () -> toast("Schedule in Google Calendar"));
        onSheetClick(sheet, R.id.opt_close, () -> { });
        sheet.show();
    }

    private void showLinkSheet(String code) {
        Dialog sheet = sheet(R.layout.sheet_meeting_link);
        ((TextView) sheet.findViewById(R.id.link_text)).setText(Meet.link(code));
        sheet.findViewById(R.id.btn_copy).setOnClickListener(v -> Meet.copy(this, code));
        sheet.findViewById(R.id.btn_share).setOnClickListener(v -> Meet.share(this, code));
        sheet.show();
    }

    private void startCall(String code) {
        startActivity(new Intent(this, MeetCallActivity.class)
                .putExtra(MeetCallActivity.EXTRA_CODE, code)
                .putExtra(MeetCallActivity.EXTRA_NEW, true));
    }

    /** A full-width dialog that slides up from the bottom, like a Material bottom sheet. */
    private Dialog sheet(int layout) {
        Dialog sheet = new Dialog(this);
        sheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
        sheet.setContentView(layout);
        Window window = sheet.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(android.R.style.Animation_InputMethod);
        }
        return sheet;
    }

    private static void onSheetClick(Dialog sheet, int id, Runnable action) {
        sheet.findViewById(id).setOnClickListener(v -> {
            sheet.dismiss();
            action.run();
        });
    }
}
