package de.robv.android.xposed;

import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedInterface;

public record CompatHooker(@NonNull XC_MethodHook callback) implements XposedInterface.Hooker {
    @Override
    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
        var param = new XC_MethodHook.MethodHookParam<>();
        param.method = chain.getExecutable();
        param.thisObject = chain.getThisObject();
        param.args = chain.getArgs().toArray();

        try {
            callback.callBeforeHookedMethod(param);
        } catch (Throwable t) {
            XposedBridge.api.log(Log.WARN, "CompatHooker", "Exception in hooker", t);
            param.result = null;
            param.throwable = null;
            param.returnEarly = false;
        }

        if (!param.returnEarly) {
            try {
                param.result = param.thisObject == null ?
                        chain.proceed(param.args) : chain.proceedWith(param.thisObject, param.args);
                param.throwable = null;
            } catch (Throwable t) {
                param.result = null;
                param.throwable = t;
            }
        }

        Object lastResult = param.result;
        Throwable lastThrowable = param.throwable;

        try {
            callback.callAfterHookedMethod(param);
        } catch (Throwable t) {
            XposedBridge.api.log(Log.WARN, "CompatHooker", "Exception in hooker", t);
            if (lastThrowable != null) throw lastThrowable;
            else return lastResult;
        }

        if (param.throwable != null) throw param.throwable;
        else return param.result;
    }
}
