import dalvik.system.BaseDexClassLoader
import io.github.libxposed.api.XposedContext
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.helper.kt.buildHooks

class Test(
    base: XposedContext,
    param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(base, param) {
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        buildHooks(param.classLoader as BaseDexClassLoader, param.appInfo.publicSourceDir) {
            exceptionHandler = {
                android.util.Log.d("BiliRoaming", "exception", it)
                true
            }
        }
    }
}
