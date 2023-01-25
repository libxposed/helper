@file:Suppress("unused")

package io.github.libxposed.helper.kt

import dalvik.system.BaseDexClassLoader
import io.github.libxposed.api.XposedInterface
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

    sealed interface ContainerSyntaxKt<Match> where Match : BaseMatchKt<Match, *> {
        infix fun and(element: Match): ContainerSyntaxKt<Match>
        infix fun and(element: ContainerSyntaxKt<Match>): ContainerSyntaxKt<Match>
        infix fun or(element: Match): ContainerSyntaxKt<Match>
        infix fun or(element: ContainerSyntaxKt<Match>): ContainerSyntaxKt<Match>
        operator fun not(): ContainerSyntaxKt<Match>
    }

    sealed interface TypeMatcherKt<Match> : ReflectMatcherKt<Match>
            where Match : TypeMatchKt<Match> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var name: StringMatchKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var superClass: ClassMatchKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var containsMethods: ContainerSyntaxKt<MethodMatchKt>

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var containsConstructors: ContainerSyntaxKt<ConstructorMatchKt>

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var containsFields: ContainerSyntaxKt<FieldMatchKt>

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var interfaces: ContainerSyntaxKt<ClassMatchKt>

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isAbstract: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isStatic: Boolean

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isFinal: Boolean
    }

    sealed interface ClassMatcherKt : TypeMatcherKt<ClassMatchKt>

    sealed interface ParameterMatcherKt : TypeMatcherKt<ParameterMatchKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var index: Int
    }

    @Matcher
    sealed interface StringMatcherKt : BaseMatcherKt<StringMatchKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var exact: String

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var prefix: String
    }

    interface MemberMatcherKt<Match> :
        ReflectMatcherKt<Match> where Match : MemberMatchKt<Match, *> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var declaringClass: ClassMatchKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isSynthetic: Boolean
    }


    sealed interface FieldMatcherKt : MemberMatcherKt<FieldMatchKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var name: StringMatchKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var type: ClassMatchKt

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

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var parameterTypes: ContainerSyntaxKt<ParameterMatchKt>

        @DexAnalysis
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var referredStrings: ContainerSyntaxKt<StringMatchKt>

        @DexAnalysis
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var assignedFields: ContainerSyntaxKt<FieldMatchKt>

        @DexAnalysis
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var accessedFields: ContainerSyntaxKt<FieldMatchKt>

        @DexAnalysis
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var invokedMethods: ContainerSyntaxKt<MethodMatchKt>

        @DexAnalysis
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var invokedConstructor: ContainerSyntaxKt<ConstructorMatchKt>

        @DexAnalysis
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var containsOpcodes: Array<Byte>

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var isVarargs: Boolean
    }

    sealed interface MethodMatcherKt : ExecutableMatcherKt<MethodMatchKt> {
        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var name: StringMatchKt

        @get:Deprecated("Write only", level = DeprecationLevel.HIDDEN)
        var returnType: ClassMatchKt

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

    sealed interface ConstructorMatcherKt : ExecutableMatcherKt<ConstructorMatchKt>

    @Hooker
    sealed interface DummyHooker

    sealed interface BaseMatchKt<Self, Reflect> where Self : BaseMatchKt<Self, Reflect> {
        operator fun unaryPlus(): ContainerSyntaxKt<Self>
        operator fun unaryMinus(): ContainerSyntaxKt<Self>
    }

    @Hooker
    sealed interface ReflectMatchKt<Self, Reflect> :
        BaseMatchKt<Self, Reflect> where Self : ReflectMatchKt<Self, Reflect> {
        val key: String?
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

        operator fun unaryPlus(): ContainerSyntaxKt<Match>
        operator fun unaryMinus(): ContainerSyntaxKt<Match>
    }

    sealed interface TypeMatchKt<Self> :
        ReflectMatchKt<Self, Class<*>> where Self : TypeMatchKt<Self> {
        val name: StringMatchKt
        val superClass: ClassMatchKt
        val interfaces: LazySequenceKt<ClassMatchKt, Class<*>, ClassMatcherKt>
        val declaredMethods: LazySequenceKt<MethodMatchKt, Method, MethodMatcherKt>
        val declaredConstructors: LazySequenceKt<ConstructorMatchKt, Constructor<*>, ConstructorMatcherKt>
        val declaredFields: LazySequenceKt<FieldMatchKt, Field, FieldMatcherKt>
        val arrayType: ClassMatchKt
    }

    sealed interface ClassMatchKt : TypeMatchKt<ClassMatchKt> {
        operator fun get(index: Int): ParameterMatchKt
    }

    sealed interface ParameterMatchKt : TypeMatchKt<ParameterMatchKt>

    sealed interface MemberMatchKt<Self, Reflect> :
        ReflectMatchKt<Self, Reflect> where Self : MemberMatchKt<Self, Reflect>, Reflect : Member {
        val declaringClass: ClassMatchKt
    }

    sealed interface ExecutableMatchKt<Self, Reflect> :
        MemberMatchKt<Self, Reflect> where Self : ExecutableMatchKt<Self, Reflect>, Reflect : Member {
        val parameterTypes: LazySequenceKt<ParameterMatchKt, Class<*>, ParameterMatcherKt>

        @DexAnalysis
        val referredStrings: LazySequenceKt<StringMatchKt, String, StringMatcherKt>

        @DexAnalysis
        val assignedFields: LazySequenceKt<FieldMatchKt, Field, FieldMatcherKt>

        @DexAnalysis
        val accessedFields: LazySequenceKt<FieldMatchKt, Field, FieldMatcherKt>

        @DexAnalysis
        val invokedMethods: LazySequenceKt<MethodMatchKt, Method, MethodMatcherKt>

        @DexAnalysis
        val invokedConstructors: LazySequenceKt<ConstructorMatchKt, Constructor<*>, ConstructorMatcherKt>
    }

    sealed interface MethodMatchKt : ExecutableMatchKt<MethodMatchKt, Method> {
        val name: StringMatchKt
        val returnType: ClassMatchKt
    }

    sealed interface ConstructorMatchKt : ExecutableMatchKt<ConstructorMatchKt, Constructor<*>>

    sealed interface FieldMatchKt : MemberMatchKt<FieldMatchKt, Field> {
        val name: StringMatchKt
        val type: ClassMatchKt
    }

    sealed interface StringMatchKt : BaseMatchKt<StringMatchKt, String>

    fun methods(init: MethodMatcherKt.() -> Unit): LazySequenceKt<MethodMatchKt, Method, MethodMatcherKt>
    fun firstMethod(init: MethodMatcherKt.() -> Unit): MethodMatchKt
    fun classes(init: ClassMatcherKt.() -> Unit): LazySequenceKt<ClassMatchKt, Class<*>, ClassMatcherKt>
    fun firstClass(init: ClassMatcherKt.() -> Unit): ClassMatchKt
    fun fields(init: FieldMatcherKt.() -> Unit): LazySequenceKt<FieldMatchKt, Field, FieldMatcherKt>
    fun firstField(init: FieldMatcherKt.() -> Unit): FieldMatchKt
    fun constructors(init: ConstructorMatcherKt.() -> Unit): LazySequenceKt<ConstructorMatchKt, Constructor<*>, ConstructorMatcherKt>
    fun firstConstructor(init: ConstructorMatcherKt.() -> Unit): ConstructorMatchKt
    fun string(init: StringMatcherKt.() -> Unit): StringMatchKt
    val String.exact: StringMatchKt
    val String.prefix: StringMatchKt
    val String.exactClass: ClassMatchKt
    val Class<*>.exact: ClassMatchKt
    val Method.exact: MethodMatchKt
    val Constructor<*>.exact: ConstructorMatchKt
    val Field.exact: FieldMatchKt
}

fun XposedInterface.buildHooks(
    classLoader: BaseDexClassLoader, sourcePath: String, init: HookBuilderKt.() -> Unit
): HookBuilderKt.MatchResultKt {
    val builder = HookBuilderKtImpl(this, classLoader, sourcePath)
    builder.init()
    return builder.build()
}
