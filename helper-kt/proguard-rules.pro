-keep class io.github.libxposed.helper.kt.HookBuilderKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.BaseMatcherKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.DexAnalysis {  }
-keep class io.github.libxposed.helper.kt.AnnotationAnalysis {  }
-keep class io.github.libxposed.helper.kt.DummyHooker { }
-keep class io.github.libxposed.helper.kt.BaseMatchKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.LazySequenceKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.ContainerSyntaxKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.* extends io.github.libxposed.helper.kt.BaseMatcherKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.* extends io.github.libxposed.helper.kt.BaseMatchKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.* extends io.github.libxposed.helper.kt.LazySequenceKt { public <methods>; }
-keep class io.github.libxposed.helper.kt.HookBuilderKtKt { public <methods>; }
-keep abstract class io.github.libxposed.helper.kt.LazyBind { onMatch(); <init>(); }
-keepattributes *
-repackageclasses "libxposed.helper.kt"