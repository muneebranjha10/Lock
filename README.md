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
