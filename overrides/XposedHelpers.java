package de.robv.android.xposed;

/**
 * Compile-only compatibility declarations for legacy LSPosed/Xposed API.
 * Method return descriptors MUST match the runtime API exactly.
 * Never package these stub classes inside the app.
 */
public final class XposedHelpers {
    public static Class<?> findClass(String name, ClassLoader cl) { return null; }
    public static XC_MethodHook.Unhook findAndHookMethod(String name, ClassLoader cl, String method, Object... args) { return null; }
    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String method, Object... args) { return null; }
    public static Object callMethod(Object instance, String method, Object... args) { return null; }
    public static Object callStaticMethod(Class<?> cls, String method, Object... args) { return null; }
    public static Object getStaticObjectField(Class<?> cls, String name) { return null; }
}
