package dev.oneui.applock.ui;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import dev.oneui.applock.store.LockProvider;
import java.util.List;

/**
 * Explicitly opt-in foreground package monitor. Does not retrieve window contents,
 * key events or text. This is NOT equivalent to platform task-level App Lock.
 */
public final class AppMonitorService extends AccessibilityService {
    private static final String MODULE = "dev.oneui.applock";
    private long lastLaunch;
    private String lastPackage = "";

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int kind = event.getEventType();
        if (kind != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                kind != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        CharSequence name = event.getPackageName();
        if (name == null) return;
        String target = name.toString();
        if (excluded(target)) return;
        if (!getSharedPreferences("locks", MODE_PRIVATE).getBoolean(target, false)) return;
        if (LockProvider.isAuthorized(this, target)) return;

        long now = SystemClock.elapsedRealtime();
        if (target.equals(lastPackage) && now - lastLaunch < 3000L) return;
        lastPackage = target;
        lastLaunch = now;
        try {
            Intent auth = new Intent(this, GuardAuthActivity.class);
            auth.setAction("dev.oneui.applock.GUARD");
            auth.putExtra("target_package", target);
            auth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(auth);
        } catch (Throwable error) {
            android.util.Log.e("AppLockMonitor", "Could not enforce lock for " + target, error);
        }
    }

    private static boolean excluded(String pkg) {
        return pkg.equals(MODULE)
                || pkg.equals("android")
                || pkg.equals("com.android.systemui")
                || pkg.equals("com.android.settings")
                || pkg.equals("com.sec.android.app.launcher")
                || pkg.equals("com.samsung.android.biometrics.app.setting")
                || pkg.equals("com.android.permissioncontroller")
                || pkg.equals("org.lsposed.manager");
    }

    @Override public void onInterrupt() {}
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo i = getServiceInfo();
        if (i != null) {
            i.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED;
            i.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            i.flags = AccessibilityServiceInfo.DEFAULT;
            i.notificationTimeout = 100;
            setServiceInfo(i);
        }
    }

    public static boolean enabled(Context ctx) {
        AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        ComponentName expected = new ComponentName(ctx, AppMonitorService.class);
        List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo svc : list) {
            if (svc.getResolveInfo() != null && svc.getResolveInfo().serviceInfo != null) {
                ComponentName found = new ComponentName(
                        svc.getResolveInfo().serviceInfo.packageName,
                        svc.getResolveInfo().serviceInfo.name);
                if (expected.equals(found)) return true;
            }
        }
        return false;
    }
}
