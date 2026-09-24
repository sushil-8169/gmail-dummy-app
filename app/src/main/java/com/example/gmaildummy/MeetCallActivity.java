package com.example.gmaildummy;

import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

/** Meet's in-call screen, always dark. You're the only one here, with mic and camera toggles. */
public class MeetCallActivity extends BaseActivity {
    static final String EXTRA_CODE = "code";
    /** Set for a meeting you just started, to show the "Your meeting's ready" card with its link. */
    static final String EXTRA_NEW = "new";

    private ImageView mic, camera;
    private View micIndicator;
    private boolean micOn = true, cameraOn;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_meet_call);
        SystemBars.edgeToEdge(this, false);
        SystemBars.padRoot(findViewById(R.id.root));

        String extra = getIntent().getStringExtra(EXTRA_CODE);
        String code = extra != null ? extra : Meet.newCode();
        ((TextView) findViewById(R.id.call_code)).setText(code);
        findViewById(R.id.call_avatar).setBackground(oval(MailStore.ME_COLOR));

        mic = findViewById(R.id.btn_mic);
        camera = findViewById(R.id.btn_camera);
        micIndicator = findViewById(R.id.call_mic_indicator);
        mic.setOnClickListener(v -> { micOn = !micOn; bindControls(); });
        camera.setOnClickListener(v -> { cameraOn = !cameraOn; bindControls(); });
        bindControls();

        findViewById(R.id.btn_share_call).setOnClickListener(v -> Meet.share(this, code));
        findViewById(R.id.btn_more_call).setOnClickListener(this::showMoreMenu);
        findViewById(R.id.btn_end_call).setOnClickListener(v -> leave());

        View card = findViewById(R.id.ready_card);
        card.setVisibility(getIntent().getBooleanExtra(EXTRA_NEW, false) ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.ready_link)).setText(Meet.link(code));
        findViewById(R.id.btn_close_ready).setOnClickListener(v -> card.setVisibility(View.GONE));
        findViewById(R.id.btn_copy_ready).setOnClickListener(v -> Meet.copy(this, code));
        findViewById(R.id.btn_share_ready).setOnClickListener(v -> Meet.share(this, code));
    }

    private void bindControls() {
        style(mic, micOn, R.drawable.ic_mic, R.drawable.ic_mic_off,
                micOn ? "Turn off microphone" : "Turn on microphone");
        style(camera, cameraOn, R.drawable.ic_videocam_filled, R.drawable.ic_videocam_off,
                cameraOn ? "Turn off camera" : "Turn on camera");
        micIndicator.setVisibility(micOn ? View.GONE : View.VISIBLE);
    }

    // Meet shows a switched-off mic or camera as a light red button.
    private void style(ImageView button, boolean on, int onIcon, int offIcon, String description) {
        button.setImageResource(on ? onIcon : offIcon);
        button.setImageTintList(ColorStateList.valueOf(color(on ? R.color.call_text : R.color.on_call_off)));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),
                oval(color(on ? R.color.call_button : R.color.call_off)), null));
        button.setContentDescription(description);
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        for (String item : new String[] { "Raise hand", "Captions", "Report a problem", "Settings" }) {
            menu.getMenu().add(item);
        }
        menu.setOnMenuItemClickListener(item -> { toast(item.getTitle().toString()); return true; });
        menu.show();
    }

    private void leave() {
        toast("You left the meeting");
        finish();
    }

    @Override public void onBackPressed() {
        leave();
    }
}
