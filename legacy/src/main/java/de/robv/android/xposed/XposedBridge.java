package de.robv.android.xposed;

import android.util.Log;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;

import io.github.libxposed.api.XposedInterface;

public class XposedBridge {
    static XposedInterface api;

    public static void init(XposedInterface api) {
        XposedBridge.api = api;
    }

    public static void log(String text) {
        api.log(Log.INFO, "XposedBridge", text);
    }

    public static void log(Throwable t) {
        api.log(Log.ERROR, "XposedBridge", "", t);
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        if (!(hookMethod instanceof Executable)) {
            throw new IllegalArgumentException("Only methods and constructors can be hooked: " + hookMethod);
        }
        var handle = api.hook((Executable) hookMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                .setPriority(callback.priority)
                .intercept(new CompatHooker(callback));
        return callback.new Unhook(handle);
    }
}
