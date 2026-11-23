# One UI App Lock Lab — experimental LSPosed module

**Not Samsung's native One UI 9.5 App Lock. Not verified secure.**

This is a research prototype for Samsung One UI 9 with a native One UI Home shortcut hook and a best-effort activity overlay guard. It uses Android's own fingerprint / screen lock authentication APIs; it never asks users to enter a PIN into the module itself.

## v0.2 scope fix

The v0.1 build omitted LSPosed's recommended scope declaration. Version 0.2 adds `xposedscope` metadata for `com.sec.android.app.launcher` and bundles `assets/scope.list`.

**On-device steps:**

1. In LSPosed Manager, **disable** the old module. Install the new APK. If Android reports a signature mismatch, uninstall the old app after disabling it, then install the new APK.
2. In LSPosed → Modules → One UI App Lock Lab, enable the module. Recommended scope should now display **One UI Home**.
3. In Scope, **manually** also check one non-sensitive app to test protection.
4. Restart the launcher or reboot. Open **One UI App Lock Lab**, select that test app, and authenticate to lock it.
5. Force stop and open the test app. Test unlocking, cancellation, returning from Recents, and reboot. If the launcher long-press padlock doesn't appear, capture the LSPosed log.
6. If anything misbehaves, disable the module in LSPosed and reboot.

**Do not select** System Framework, SystemUI, Settings, phone/dialer, or LSPosed Manager as locked apps. Selecting an app as “locked” **does not automatically grant the LSPosed scope**; both steps are needed.

## Critical limitations

- Not a Samsung-native, OS-level App Lock. The module may leak app content via splash screen, services, other windows, widgets, Recents, or notifications. Never rely on it for banking apps or sensitive messages/photos.
- Launcher hooks are based on reverse-engineered method signatures from one user's One UI 9 launcher and have **not been verified on-device**.
- The scope fix addresses the missing recommended list only; it does not make the experimental hook reliable.
- The GitHub debug signing key is ephemeral: updating APKs from different Actions runs can fail with a signature mismatch. Disable and uninstall the prior build before installing the next, then reconfigure locks.
- The build workflow reconstructs the prototype sources from `parts/*.b64`, overlays the scope fix, compiles via Gradle, and publishes `releases/Lock-experimental-debug.apk` if successful.

## Rollback
Disable One UI App Lock Lab in LSPosed Manager and reboot. Uninstall the APK when no longer needed.

## v0.3 native launcher padlock experiment (2026-11-22)

The v0.2 module's supported-item hooks were insufficient to display the launcher padlock. Inspection of the supplied One UI Home APK identified `GlobalOptionView.isAppLockSupported(PopupAnchorInfo)` as a separate title-row gate reached from `addSideLockButton(...)`. The v0.3 build adds `LauncherGateHook` for that gate, bundled from `overrides/LauncherGateHook.java` during the GitHub build. Download [v0.3 debug APK](releases/Lock-v0.3-launcher-debug.apk).

This is a **compiled but device-untested** experiment, not a verified working or secure native App Lock. Test with a non-sensitive app. In LSPosed, scope One UI Home plus one test app (do not scope System Framework or SystemUI), then reboot. If the padlock is still missing, share **LSPosed module logs** containing `AppLockLab v0.3` so we can distinguish scope failures from Samsung's launcher runtime gate. The app protection mechanism is a best-effort activity guard and is not suitable for protecting private information.

## v0.4 — diagnosed Xposed API signature fix (2026-11-22)

The supplied KernelSU bug report (Nov 22, 2026, 13:59:15 local) exposed a genuine LSPosed `NoSuchMethodError` in the scoped WhatsApp process: our self-written compile-only stubs declared `XposedHelpers.findAndHookMethod(...)` as returning `void`. The real Xposed API returns `XC_MethodHook.Unhook`. Consequently the previous experimental APK could compile while the hook would fail when invoked at runtime.

v0.4 replaces those compile-time stubs with correctly typed method signatures, validates the descriptors with `javap` during GitHub Actions, and logs when App Lock is loaded into One UI Home or WhatsApp. **The GitHub build and descriptor check passed. This does not verify on-device launcher integration or app security.**

[Download v0.4 APK](releases/Lock-v0.4-api-debug.apk)

For testing, disable/uninstall v0.3 (Actions debug keys are not stable between runs), install v0.4, enable its LSPosed scope for One UI Home and **one non-sensitive test app**, and reboot. Long-press that app; if the padlock remains absent, export **LSPosed Modules Logs** and search for `AppLockLab v0.4`. When the log does not show App Lock loading in the launcher, investigate LSPosed scope/restart before changing more Samsung hooks. Do not treat this as a reliable privacy or security product.

## v0.5 — dark compact confirmation, App Info restoration, opt-in monitor

**Build:** [v0.5 APK](releases/Lock-v0.5-monitor-debug.apk). GitHub Actions compiled this APK and confirmed that `AppMonitorService` is present in its AndroidManifest.

This update addresses feedback from the Galaxy Store demonstration (2026-11-22):

- The original Samsung launcher title-row **App Info** support method is now hooked alongside the native App Lock support gate. Both should be eligible for ordinary app icons; behavior must be verified on the phone.
- The white full-screen Lock activity has been replaced with a compact, dark, rounded confirmation dialog styled to resemble the supplied reference.
- The v0.4 app showed “App locked (scope required)” even if no process had the guard hook. v0.5 now requires an enabled **App Lock monitor** before the module claims to lock an app.
- The monitor is an explicit opt-in Android AccessibilityService that inspects foreground **package names**, not text or screen contents (`canRetrieveWindowContent=false`). It launches the module’s BiometricPrompt/device credential Activity for locked apps without requiring every app to be in the LSPosed scope.
- The launcher padlock still needs **One UI Home** selected in LSPosed. For experimental coverage of another app's activity UI through the separate LSPosed guard, add that app to scope manually as well.

### Install and test

1. Disable the old module and uninstall the previous GitHub Actions debug APK, because the debug signing key may change between builds.
2. Install v0.5, enable One UI Home in the module's LSPosed scope, and reboot.
3. Open **One UI App Lock Lab**, tap the top information card and enable **App Lock monitor** under Android Accessibility settings. The monitoring service has powerful system privileges; enable it only if you trust this experimental app.
4. Long-press a **non-sensitive** test app, confirm that the **(i)** and **padlock** are both visible, then tap padlock → Lock. Authenticate with Samsung's real system fingerprint/PIN prompt.
5. Reopen the test app and confirm the BiometricPrompt appears and cancelling returns home.

### Important limitations

This is **not** the native One UI 9.5 App Lock service or a proven privacy boundary. The accessibility monitor observes foreground application changes and may be too late to block content, and notifications, widgets, shortcuts, deep links and Recent Apps previews are not protected. Some device or Android versions may restrict AccessibilityService background activity launches. Do not use the module to secure banking, private chats or confidential files.

There is no custom PIN collection: only Android BiometricPrompt and device credentials are used. If App Lock monitor cannot be enabled, no active all-app locking is available in this build. If App Info still disappears or a test app does not prompt, capture LSPosed Modules Logs and a short screen recording. 

## v0.6 — reference-focused compact dialog (UI only)

This is an **appearance-only** iteration on the supplied One UI 9.5 reference screenshot. It reduces dialog font sizes, replaces heavy body text with regular-weight type, increases panel width, tightens line spacing/padding, and adjusts the dark panel radius and buttons. Launcher hooks, authentication and app monitoring are unchanged from v0.5.

[Download the v0.6 experimental APK](releases/Lock-v0.6-dialog-debug.apk).

**Why the text differs from the Samsung reference:** Native Samsung App Lock claims to remove widgets and hide notification previews. This experimental module does neither, so its popup says those items are *not hidden* rather than claiming nonexistent privacy protection. Similarly, the title says **Unlock** for an already-locked app and **Lock** for an unlocked app. The physical layout is intended to resemble the reference but is not verified pixel-identical on a user's device. A successfully compiled APK is not proof of real app-lock security.
