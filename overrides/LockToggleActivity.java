package dev.oneui.applock.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Compact dark Samsung-inspired confirmation dialog, based on the user's
 * reference. True Samsung system dialogs cannot be imported from One UI 9.5.
 */
public final class LockToggleActivity extends Activity {
    public static final String EXTRA_PACKAGE = "target_package";
    private String target;
    private boolean currentlyLocked;

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void addParagraph(LinearLayout parent, String value, int topDp, int size) {
        TextView p = text(value, size, 0xFFE1E2E4, false);
        p.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(topDp);
        parent.addView(p, lp);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        target = getIntent().getStringExtra("target_package");
        if (target == null || target.isEmpty() || target.equals(getPackageName())
                || target.equals("android") || target.equals("com.sec.android.app.launcher")
                || target.equals("com.android.systemui") || target.equals("com.android.settings")) {
            finish(); return;
        }
        currentlyLocked = getSharedPreferences("locks", MODE_PRIVATE).getBoolean(target, false);
        String label = target;
        try {
            label = String.valueOf(getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(target, 0)));
        } catch (Exception ignored) { }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(0xFF232623, 30));
        card.setPadding(dp(27), dp(30), dp(27), dp(18));

        TextView title = text((currentlyLocked ? "Unlock " : "Lock ") + label,
                23, Color.WHITE, true);
        card.addView(title);
        addParagraph(card,
                "To open a locked app, you need to verify your identity with biometrics "
                + "or your screen lock PIN, pattern, or password.", 6, 15);
        addParagraph(card, "When this app is locked:", 18, 15);
        addParagraph(card,
                "• Opening the app requires authentication when the App Lock monitor is enabled.\n"
                + "• Widgets, shortcuts and notification content are not hidden by this module.",
                10, 15);
        addParagraph(card,
                "Other apps that already have permission may still access your photos, videos, "
                + "contacts and other data.", 18, 15);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(57));
        bp.topMargin = dp(11);
        card.addView(buttons, bp);
        TextView cancel = text("Cancel", 18, 0xFFFFFFFF, false);
        cancel.setGravity(Gravity.CENTER);
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, -1, 1));
        cancel.setOnClickListener(v -> finish());

        TextView separator = new TextView(this);
        separator.setBackgroundColor(0xFF444744);
        buttons.addView(separator, new LinearLayout.LayoutParams(dp(1), dp(25)));

        TextView confirm = text(currentlyLocked ? "Unlock" : "Lock",
                18, 0xFFFFFFFF, false);
        confirm.setGravity(Gravity.CENTER);
        buttons.addView(confirm, new LinearLayout.LayoutParams(0, -1, 1));
        confirm.setOnClickListener(v -> verifyAndToggle());

        setContentView(card);
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams wp = window.getAttributes();
        wp.dimAmount = 0.61f;
        wp.gravity = Gravity.CENTER;
        window.setAttributes(wp);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(40), dp(470));
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void verifyAndToggle() {
        if (!currentlyLocked && !AppMonitorService.enabled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable App Lock monitor")
                    .setMessage("Your app was not protected in the previous version. "
                            + "Enable App Lock monitor in Android Accessibility settings "
                            + "before locking apps. It reads foreground app names only, "
                            + "not screen contents. This is experimental, not system-level security.")
                    .setNegativeButton("Cancel", (dialog, which) -> {})
                    .setPositiveButton("Open settings", (dialog, which) -> {
                        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                        catch (Exception e) { Toast.makeText(this, "Open Accessibility in Settings", Toast.LENGTH_LONG).show(); }
                    }).show();
            return;
        }
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure()) {
            Toast.makeText(this, "Set a screen lock PIN, pattern or password first", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                    .setTitle("App Lock")
                    .setSubtitle("Verify your identity")
                    .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG |
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
            prompt.authenticate(new CancellationSignal(), getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            getSharedPreferences("locks", MODE_PRIVATE).edit()
                                    .putBoolean(target, !currentlyLocked).apply();
                            Toast.makeText(LockToggleActivity.this,
                                    currentlyLocked ? "App unlocked" : "App locked — monitor enabled",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override public void onAuthenticationError(int code, CharSequence message) {
                            Toast.makeText(LockToggleActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Authentication unavailable: " + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
