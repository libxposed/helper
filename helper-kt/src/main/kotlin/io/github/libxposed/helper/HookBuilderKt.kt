package io.github.libxposed.helper

import dalvik.system.BaseDexClassLoader
import io.github.libxposed.XposedInterface
import java.io.Serializable
import kotlin.experimental.ExperimentalTypeInference
import java.lang.Class
import java.lang.reflect.Method
import java.lang.reflect.Constructor
import java.lang.reflect.Field

@DslMarker
annotation class Hooker

@Hooker
@Suppress("unused")
sealed interface HookBuilderKt {
    @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
    var lastMatchResult: MatchResultKt

    @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
    var exceptionHandler: (Throwable) -> Boolean

    @RequiresOptIn(message = "Dex analysis is time-consuming, please use it carefully.")
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
    annotation class DexAnalysis

    @RequiresOptIn(message = "Annotation analysis is time-consuming, please use it carefully.")
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
    annotation class AnnotationAnalysis

    @DslMarker
    annotation class Matcher

    sealed interface MatchResultKt : Serializable, Cloneable {
        val matchedClasses: Map<String, Class<*>>
        val matchedFields: Map<String, Field>
        val matchedMethods: Map<String, Method>
        val matchedConstructors: Map<String, Constructor<*>>
    }

    sealed interface BaseMatcherKt<T> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var matchFirst: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var missReplacement: T
    }

    @Matcher
    sealed interface ReflectMatcherKt<T> : BaseMatcherKt<T> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var key: String

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isPublic: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isPrivate: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isProtected: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isPackage: Boolean
    }

    sealed interface ContainerSyntaxKt<T> {
        operator fun plus(list: ContainerSyntaxKt<T>): ContainerSyntaxKt<T>
        operator fun plus(list: Iterable<T>): ContainerSyntaxKt<T>
        operator fun plus(element: T): ContainerSyntaxKt<T>
        operator fun minus(list: ContainerSyntaxKt<T>): ContainerSyntaxKt<T>
        operator fun minus(list: Iterable<T>): ContainerSyntaxKt<T>
        operator fun minus(element: T): ContainerSyntaxKt<T>
        operator fun times(list: ContainerSyntaxKt<T>): ContainerSyntaxKt<T>
        operator fun times(list: Iterable<T>): ContainerSyntaxKt<T>
        operator fun times(element: T): ContainerSyntaxKt<T>
        operator fun div(list: ContainerSyntaxKt<T>): ContainerSyntaxKt<T>
        operator fun div(list: Iterable<T>): ContainerSyntaxKt<T>
        operator fun div(element: T): ContainerSyntaxKt<T>
    }

    sealed interface ClassMatcherKt : ReflectMatcherKt<ClassKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var name: StringKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var superClass: ClassKt

        val containsMethods: ContainerSyntaxKt<MethodKt>
        val containsConstructors: ContainerSyntaxKt<ConstructorKt>
        val containsFields: ContainerSyntaxKt<FieldKt>
        val interfaces: ContainerSyntaxKt<ClassKt>

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isAbstract: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isStatic: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isFinal: Boolean
    }

    @Matcher
    sealed interface StringMatcherKt : BaseMatcherKt<StringKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var exact: String

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var prefix: String
    }

    interface MemberMatcherKt<T> : ReflectMatcherKt<T> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var declaringClass: ClassKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isSynthetic: Boolean
    }


    sealed interface FieldMatcherKt : MemberMatcherKt<FieldKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var name: StringKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var type: ClassKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isStatic: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isFinal: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isTransient: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isVolatile: Boolean
    }

    sealed interface ExecutableMatcherKt<T> : MemberMatcherKt<T> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var parameterCounts: Int

        val parameterTypes: ContainerSyntaxKt<IndexedValue<ClassKt>>

        @DexAnalysis
        val referredStrings: ContainerSyntaxKt<StringKt>

        @DexAnalysis
        val assignedFields: ContainerSyntaxKt<FieldKt>

        @DexAnalysis
        val invokedMethods: ContainerSyntaxKt<MethodKt>

        @DexAnalysis
        val invokedConstructor: ContainerSyntaxKt<ConstructorKt>

        @DexAnalysis
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var containsOpcodes: Array<Byte>

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isVarargs: Boolean
    }

    sealed interface MethodMatcherKt : ExecutableMatcherKt<MethodKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var name: StringKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var returnType: ClassKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isAbstract: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isStatic: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isFinal: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isSynchronized: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isNative: Boolean
    }

    sealed interface ConstructorMatcherKt : ExecutableMatcherKt<ConstructorKt>

    @Hooker
    sealed interface DummyHooker

    sealed interface BaseMatchKt<T, U>

    @Hooker
    sealed interface ReflectMatchKt<T, U> : BaseMatchKt<T, U> {
        val key: String?
        operator fun plus(match: T): ContainerSyntaxKt<T>
        operator fun minus(match: T): ContainerSyntaxKt<T>
        operator fun times(match: T): ContainerSyntaxKt<T>
        operator fun div(match: T): ContainerSyntaxKt<T>
        fun onMatch(handler: DummyHooker.(U) -> Unit): T
    }

    @OptIn(ExperimentalTypeInference::class)
    @Hooker
    sealed interface LazySequenceKt<T, U, V> where V : BaseMatcherKt<T> {
        fun first(): T
        fun first(init: V.() -> Unit): T
        fun all(init: V.() -> Unit): LazySequenceKt<T, U, V>

        @OverloadResolutionByLambdaReturnType
        fun onMatch(handler: DummyHooker.(Sequence<U>) -> Unit): LazySequenceKt<T, U, V>

        @OverloadResolutionByLambdaReturnType
        fun onMatch(handler: DummyHooker.(Sequence<U>) -> U): T

        fun conjunction(): ContainerSyntaxKt<T>
        fun disjunction(): ContainerSyntaxKt<T>
    }


    sealed interface ClassKt : BaseMatchKt<ClassKt, Class<*>> {
        val name: StringKt
        val superClass: ClassKt
        val interfaces: LazySequenceKt<ClassKt, Class<*>, ClassMatcherKt>
        val declaredMethods: LazySequenceKt<MethodKt, Method, MethodMatcherKt>
        val declaredConstructors: LazySequenceKt<ConstructorKt, Constructor<*>, ConstructorMatcherKt>
        val declaredFields: LazySequenceKt<FieldKt, Field, FieldMatcherKt>
        val arrayType: ClassKt
    }

    sealed interface MemberMatchKt<T, U> : BaseMatchKt<T, U> {
        val declaringClass: ClassKt
    }

    sealed interface ExecutableMatchKt<T, U> : MemberMatchKt<T, U> {
        val parameterTypes: LazySequenceKt<ClassKt, Class<*>, ClassMatcherKt>

        @DexAnalysis
        val referredStrings: LazySequenceKt<StringKt, String, StringMatcherKt>

        @DexAnalysis
        val assignedFields: LazySequenceKt<FieldKt, Field, FieldMatcherKt>

        @DexAnalysis
        val accessedFields: LazySequenceKt<FieldKt, Field, FieldMatcherKt>

        @DexAnalysis
        val invokedMethods: LazySequenceKt<MethodKt, Method, MethodMatcherKt>

        @DexAnalysis
        val invokedConstructors: LazySequenceKt<ConstructorKt, Constructor<*>, ConstructorMatcherKt>
    }

    sealed interface MethodKt : ExecutableMatchKt<MethodKt, Method> {
        val name: StringKt
        val returnType: ClassKt
    }

    sealed interface ConstructorKt : ExecutableMatchKt<ConstructorKt, Constructor<*>>

    sealed interface FieldKt : MemberMatchKt<FieldKt, Field> {
        val name: StringKt
        val type: ClassKt
    }

    sealed interface StringKt

    fun methods(init: MethodMatcherKt.() -> Unit): LazySequenceKt<MethodKt, Method, MethodMatcherKt>
    fun firstMethod(init: MethodMatcherKt.() -> Unit): MethodKt
    fun classes(init: ClassMatcherKt.() -> Unit): LazySequenceKt<ClassKt, Class<*>, ClassMatcherKt>
    fun firstClass(init: ClassMatcherKt.() -> Unit): ClassKt
    fun fields(init: FieldMatcherKt.() -> Unit): LazySequenceKt<FieldKt, Field, FieldMatcherKt>
    fun firstField(init: FieldMatcherKt.() -> Unit): FieldKt
    fun constructors(init: ConstructorMatcherKt.() -> Unit): LazySequenceKt<ConstructorKt, Constructor<*>, ConstructorMatcherKt>
    fun firstConstructor(init: ConstructorMatcherKt.() -> Unit): ConstructorKt
    fun string(init: StringMatcherKt.() -> Unit): StringKt
    val String.exact: StringKt
    val String.prefix: StringKt
    val String.exactClass: ClassKt
    val Class<*>.exact: ClassKt
    val Method.exact: MethodKt
    val Constructor<*>.exact: ConstructorKt
    val Field.exact: FieldKt
}

fun XposedInterface.buildHooks(
    classLoader: BaseDexClassLoader,
    sourcePath: String,
    init: HookBuilderKt.() -> Unit
): HookBuilderKt.MatchResultKt {
    val builder = HookBuilderKtImpl(this, classLoader, sourcePath)
    builder.init()
    return builder.build()
}
