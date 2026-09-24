package com.example.gmaildummy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

/** Meet's "Join with a code": Join stays disabled until you type, and a malformed code shows an error. */
public class JoinMeetingActivity extends BaseActivity {
    private EditText code;
    private TextView join;
    private View error;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_join_meeting);
        SystemBars.padRoot(findViewById(R.id.root));

        code = findViewById(R.id.code);
        join = findViewById(R.id.btn_join);
        error = findViewById(R.id.error);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        join.setOnClickListener(v -> join());
        code.setOnEditorActionListener((v, actionId, event) -> { join(); return true; });
        code.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                error.setVisibility(View.GONE);
                bindJoin();
            }
        });
        bindJoin();

        code.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void bindJoin() {
        boolean enabled = code.getText().toString().trim().length() > 0;
        join.setEnabled(enabled);
        join.setAlpha(enabled ? 1f : 0.38f);
    }

    private void join() {
        if (code.getText().toString().trim().isEmpty()) return;
        String parsed = Meet.parse(code.getText().toString());
        if (parsed == null) {
            error.setVisibility(View.VISIBLE);
            return;
        }
        startActivity(new Intent(this, MeetCallActivity.class).putExtra(MeetCallActivity.EXTRA_CODE, parsed));
        finish();
    }
}
