package io.github.libxposed.helper

import dalvik.system.BaseDexClassLoader
import io.github.libxposed.XposedInterface
import java.io.Serializable
import kotlin.experimental.ExperimentalTypeInference
import java.lang.Class
import java.lang.reflect.Method
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member

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

    sealed interface BaseMatcherKt<Match> where Match : BaseMatchKt<Match, *> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var matchFirst: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var missReplacement: Match
    }

    @Matcher
    sealed interface ReflectMatcherKt<Match> :
        BaseMatcherKt<Match> where Match : ReflectMatchKt<Match, *> {
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

    sealed interface ContainerSyntaxKt<Element> {
        operator fun plus(list: ContainerSyntaxKt<Element>): ContainerSyntaxKt<Element>
        operator fun plus(list: Iterable<Element>): ContainerSyntaxKt<Element>
        operator fun plus(element: Element): ContainerSyntaxKt<Element>
        operator fun minus(list: ContainerSyntaxKt<Element>): ContainerSyntaxKt<Element>
        operator fun minus(list: Iterable<Element>): ContainerSyntaxKt<Element>
        operator fun minus(element: Element): ContainerSyntaxKt<Element>
        operator fun times(list: ContainerSyntaxKt<Element>): ContainerSyntaxKt<Element>
        operator fun times(list: Iterable<Element>): ContainerSyntaxKt<Element>
        operator fun times(element: Element): ContainerSyntaxKt<Element>
        operator fun div(list: ContainerSyntaxKt<Element>): ContainerSyntaxKt<Element>
        operator fun div(list: Iterable<Element>): ContainerSyntaxKt<Element>
        operator fun div(element: Element): ContainerSyntaxKt<Element>
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

    interface MemberMatcherKt<Match> :
        ReflectMatcherKt<Match> where Match : MemberMatchKt<Match, *> {
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

    sealed interface ExecutableMatcherKt<Match> :
        MemberMatcherKt<Match> where Match : ExecutableMatchKt<Match, *> {
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

    sealed interface BaseMatchKt<Self, Reflect>

    @Hooker
    sealed interface ReflectMatchKt<Self, Reflect> : BaseMatchKt<Self, Reflect> {
        val key: String?
        operator fun plus(match: Self): ContainerSyntaxKt<Self>
        operator fun minus(match: Self): ContainerSyntaxKt<Self>
        operator fun times(match: Self): ContainerSyntaxKt<Self>
        operator fun div(match: Self): ContainerSyntaxKt<Self>
        fun onMatch(handler: DummyHooker.(Reflect) -> Unit): Self
    }

    @OptIn(ExperimentalTypeInference::class)
    @Hooker
    sealed interface LazySequenceKt<Match, Reflect, Matcher> where Matcher : BaseMatcherKt<Match>, Match : BaseMatchKt<Match, Reflect> {
        fun first(): Match
        fun first(init: Matcher.() -> Unit): Match
        fun all(init: Matcher.() -> Unit): LazySequenceKt<Match, Reflect, Matcher>

        @OverloadResolutionByLambdaReturnType
        fun onMatch(handler: DummyHooker.(Sequence<Reflect>) -> Unit): LazySequenceKt<Match, Reflect, Matcher>

        @OverloadResolutionByLambdaReturnType
        fun onMatch(handler: DummyHooker.(Sequence<Reflect>) -> Reflect): Match

        fun conjunction(): ContainerSyntaxKt<Match>
        fun disjunction(): ContainerSyntaxKt<Match>
    }


    sealed interface ClassKt : ReflectMatchKt<ClassKt, Class<*>> {
        val name: StringKt
        val superClass: ClassKt
        val interfaces: LazySequenceKt<ClassKt, Class<*>, ClassMatcherKt>
        val declaredMethods: LazySequenceKt<MethodKt, Method, MethodMatcherKt>
        val declaredConstructors: LazySequenceKt<ConstructorKt, Constructor<*>, ConstructorMatcherKt>
        val declaredFields: LazySequenceKt<FieldKt, Field, FieldMatcherKt>
        val arrayType: ClassKt
    }

    sealed interface MemberMatchKt<Self, Reflect> :
        ReflectMatchKt<Self, Reflect> where Reflect : Member {
        val declaringClass: ClassKt
    }

    sealed interface ExecutableMatchKt<Self, Reflect> :
        MemberMatchKt<Self, Reflect> where Reflect : Member {
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

    sealed interface StringKt : BaseMatchKt<StringKt, String>

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
