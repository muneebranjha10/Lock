package dev.oneui.applock.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Samsung One UI Home 9: expose the existing title-row padlock button.
 * The GlobalOptionView support gate is independent of AppLock.Factory and
 * AppLock.Companion, which were the only classes hooked in prototype v0.2.
 */
final class LauncherGateHook {
    static void install(ClassLoader loader) {
        try {
            Class<?> anchor = XposedHelpers.findClass(
                    "com.honeyspace.common.interfaces.quickoption.PopupAnchorInfo", loader);
            XposedHelpers.findAndHookMethod(
                    "com.honeyspace.ui.common.quickoption.GlobalOptionView", loader,
                    "isAppLockSupported", anchor, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object info = param.args[0];
                                if (info != null && Boolean.TRUE.equals(
                                        XposedHelpers.callMethod(info, "isApplicationItem"))) {
                                    param.setResult(true);
                                    XposedBridge.log("AppLockLab v0.3: launcher app-lock icon permitted");
                                }
                            } catch (Throwable ex) {
                                XposedBridge.log("AppLockLab v0.3: item eligibility failed: " + ex);
                            }
                        }
                    });
            // Samsung's native title has separate App Info and App Lock slots.
            // Keep the original (i) button available alongside the padlock.
            try {
                XposedHelpers.findAndHookMethod(
                        "com.honeyspace.ui.common.quickoption.GlobalOptionView", loader,
                        "isAppInfoSupported", anchor, new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    Object item = param.args[0];
                                    if (item != null && Boolean.TRUE.equals(
                                            XposedHelpers.callMethod(item, "isApplicationItem"))) {
                                        param.setResult(true);
                                    }
                                } catch (Throwable ex) {
                                    XposedBridge.log("AppLockLab v0.5: app info eligibility: " + ex);
                                }
                            }
                        });
                XposedBridge.log("AppLockLab v0.5: original App Info button gate installed");
            } catch (Throwable ex) {
                XposedBridge.log("AppLockLab v0.5: App Info gate missing: " + ex);
            }
            XposedBridge.log("AppLockLab v0.3: title-row hook installed");
        } catch (Throwable ex) {
            XposedBridge.log("AppLockLab v0.3: title-row hook NOT installed: " + ex);
        }
    }
}
