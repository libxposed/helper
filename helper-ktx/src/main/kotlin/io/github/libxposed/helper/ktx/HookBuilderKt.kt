@file:Suppress("unused")

package io.github.libxposed.helper.ktx

import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import dalvik.system.BaseDexClassLoader
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.helper.HookBuilder
import io.github.libxposed.helper.HookBuilder.*
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.util.concurrent.ExecutorService


@DslMarker
internal annotation class Hooker

@DslMarker
internal annotation class Matcher

@PublishedApi
internal val wo: Nothing
    get() = throw UnsupportedOperationException("Write-only property")

@Matcher
@Hooker
object DummyHooker


@RequiresOptIn(message = "Dex analysis is time-consuming, please use it carefully.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class DexAnalysis

@RequiresOptIn(message = "Annotation analysis is time-consuming, please use it carefully.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class AnnotationAnalysis

@Matcher
@Hooker
abstract class LazyBind {
    @PublishedApi
    internal val bind = object : HookBuilder.LazyBind {
        override fun onMatch() = this@LazyBind.onMatch()

        override fun onMiss() = this@LazyBind.onMiss()
    }

    abstract fun onMatch()

    abstract fun onMiss()
}

class ParameterKt @PublishedApi internal constructor(@PublishedApi internal val parameter: Parameter) {
    val type: Class<*>
        inline get() = parameter.type

    val declaringExecutable: Member
        inline get() = parameter.declaringExecutable

    val index: Int
        inline get() = parameter.index
}

class ContainerSyntaxKt<MatchKt, Match> @PublishedApi internal constructor(@PublishedApi internal val syntax: ContainerSyntax<Match>) where MatchKt : BaseMatchKt<MatchKt, Match, *>, Match : BaseMatch<Match, *> {
    infix fun and(element: ContainerSyntaxKt<MatchKt, Match>) =
        ContainerSyntaxKt<MatchKt, Match>(syntax.and(element.syntax))

    infix fun or(element: ContainerSyntaxKt<MatchKt, Match>) =
        ContainerSyntaxKt<MatchKt, Match>(syntax.or(element.syntax))

    operator fun not() = ContainerSyntaxKt<MatchKt, Match>(syntax.not())
}

@Matcher
sealed class ReflectMatcherKt<Matcher>(@PublishedApi internal val matcher: Matcher) where Matcher : ReflectMatcher<Matcher> {
    var key: String
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setKey(value)
        }
    var isPublic: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsPublic(value)
        }
    var isPrivate: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsPrivate(value)
        }
    var isProtected: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        set(value) {
            matcher.setIsProtected(value)
        }
    var isPackage: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        set(value) {
            matcher.setIsPackage(value)
        }
}

class ClassMatcherKt @PublishedApi internal constructor(matcher: ClassMatcher) :
    ReflectMatcherKt<ClassMatcher>(matcher) {
    var name: StringMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setName(value.match)
        }
    var superClass: ClassMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setSuperClass(value.match)
        }
    var containsInterfaces: ContainerSyntaxKt<ClassMatchKt, ClassMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setContainsInterfaces(value.syntax)
        }
    var isAbstract: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsAbstract(value)
        }
    var isStatic: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsStatic(value)
        }
    var isFinal: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsFinal(value)
        }
    var isInterface: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsInterface(value)
        }
}

class ParameterMatcherKt @PublishedApi internal constructor(matcher: ParameterMatcher) :
    ReflectMatcherKt<ParameterMatcher>(matcher) {
    var index: Int
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIndex(value)
        }

    var type: ClassMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setType(value.match)
        }

    var isFinal: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        @RequiresApi(Build.VERSION_CODES.O) inline set(value) {
            matcher.setIsFinal(value)
        }

    var isSynthetic: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        @RequiresApi(Build.VERSION_CODES.O) inline set(value) {
            matcher.setIsSynthetic(value)
        }

    var isImplicit: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        @RequiresApi(Build.VERSION_CODES.O) inline set(value) {
            matcher.setIsImplicit(value)
        }

    var isVarargs: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        @RequiresApi(Build.VERSION_CODES.O) inline set(value) {
            matcher.setIsVarargs(value)
        }
}

sealed class MemberMatcherKt<Matcher>(matcher: Matcher) :
    ReflectMatcherKt<Matcher>(matcher) where Matcher : MemberMatcher<Matcher> {
    var declaringClass: ClassMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setDeclaringClass(value.match)
        }
    var isSynthetic: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsSynthetic(value)
        }
    var includeSuper: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIncludeSuper(value)
        }
    var includeInterface: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIncludeInterface(value)
        }
}

class FieldMatcherKt @PublishedApi internal constructor(matcher: FieldMatcher) :
    MemberMatcherKt<FieldMatcher>(matcher) {
    var name: StringMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setName(value.match)
        }
    var type: ClassMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setType(value.match)
        }
    var isStatic: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsStatic(value)
        }
    var isFinal: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsFinal(value)
        }
    var isTransient: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsTransient(value)
        }
    var isVolatile: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsVolatile(value)
        }
}

sealed class ExecutableMatcherKt<Matcher>(matcher: Matcher) :
    MemberMatcherKt<Matcher>(matcher) where Matcher : ExecutableMatcher<Matcher> {
    var parameterCounts: Int
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setParameterCount(value)
        }

    var parameters: ContainerSyntaxKt<ParameterMatchKt, ParameterMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setParameters(value.syntax)
        }

    @DexAnalysis
    var referredStrings: ContainerSyntaxKt<StringMatchKt, StringMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setReferredStrings(value.syntax)
        }

    @DexAnalysis
    var assignedFields: ContainerSyntaxKt<FieldMatchKt, FieldMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setAssignedFields(value.syntax)
        }

    @DexAnalysis
    var accessedFields: ContainerSyntaxKt<FieldMatchKt, FieldMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setAccessedFields(value.syntax)
        }

    @DexAnalysis
    var invokedMethods: ContainerSyntaxKt<MethodMatchKt, MethodMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setInvokedMethods(value.syntax)
        }

    @DexAnalysis
    var invokedConstructor: ContainerSyntaxKt<ConstructorMatchKt, ConstructorMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setInvokedConstructors(value.syntax)
        }

    @DexAnalysis
    var containsOpcodes: ByteArray
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setContainsOpcodes(value)
        }

    var isVarargs: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsVarargs(value)
        }

    fun conjunction(vararg types: Class<*>?) =
        ContainerSyntaxKt<ParameterMatchKt, ParameterMatch>(matcher.conjunction(*types))

    fun conjunction(vararg types: ClassMatchKt?) =
        ContainerSyntaxKt<ParameterMatchKt, ParameterMatch>(matcher.conjunction(*types.map { it?.match }
            .toTypedArray()))

    inline fun firstParameter(crossinline init: ParameterMatcherKt.() -> Unit) =
        ParameterMatchKt(matcher.firstParameter {
            ParameterMatcherKt(it).init()
        })

    inline fun parameters(crossinline init: ParameterMatcherKt.() -> Unit) =
        ParameterLazySequenceKt(matcher.parameters {
            ParameterMatcherKt(it).init()
        })

    operator fun Class<*>.get(index: Int) =
        ContainerSyntaxKt<ParameterMatchKt, ParameterMatch>(matcher.observe(index, this))

    operator fun ClassMatchKt.get(index: Int) =
        ContainerSyntaxKt<ParameterMatchKt, ParameterMatch>(matcher.observe(index, this.match))
}

class MethodMatcherKt @PublishedApi internal constructor(matcher: MethodMatcher) :
    ExecutableMatcherKt<MethodMatcher>(matcher) {
    var name: StringMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setName(value.match)
        }
    var returnType: ClassMatchKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setReturnType(value.match)
        }
    var isAbstract: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsAbstract(value)
        }
    var isStatic: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsStatic(value)
        }
    var isFinal: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsFinal(value)
        }
    var isSynchronized: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsSynchronized(value)
        }
    var isNative: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIsNative(value)
        }
}

class ConstructorMatcherKt @PublishedApi internal constructor(matcher: ConstructorMatcher) :
    ExecutableMatcherKt<ConstructorMatcher>(matcher)

@Hooker
sealed class BaseMatchKt<Self, Match, Reflect>(
    @PublishedApi internal val match: Match
) where Self : BaseMatchKt<Self, Match, Reflect>, Match : BaseMatch<Match, Reflect> {
    operator fun unaryPlus() = ContainerSyntaxKt<Self, Match>(match.observe())

    operator fun unaryMinus() = ContainerSyntaxKt<Self, Match>(match.reverse())
}

@Suppress("UNCHECKED_CAST")
sealed class ReflectMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match: Match) :
    BaseMatchKt<Self, Match, Reflect>(match) where Self : ReflectMatchKt<Self, Match, Reflect, Matcher, MatcherKt>, Match : ReflectMatch<Match, Reflect, Matcher>, Matcher : ReflectMatcher<Matcher>, MatcherKt : ReflectMatcherKt<Matcher> {
    var key: String?
        inline get() = match.key
        inline set(value) {
            match.key = value
        }

    inline fun onMiss(crossinline handler: DummyHooker.() -> Unit): Self {
        match.onMiss {
            DummyHooker.handler()
        }
        return this as Self
    }

    inline fun substituteIfMiss(crossinline substitute: () -> Self): Self {
        match.substituteIfMiss {
            substitute().match
        }
        return this as Self
    }

    inline fun matchFirstIfMiss(crossinline handler: MatcherKt.() -> Unit): Self {
        match.matchFirstIfMiss {
            newMatcher(it).handler()
        }
        return this as Self
    }

    @PublishedApi
    internal abstract fun newSelf(match: Match): Self

    @PublishedApi
    internal abstract fun newMatcher(match: Matcher): MatcherKt
}

class ClassMatchKt @PublishedApi internal constructor(match: ClassMatch) :
    ReflectMatchKt<ClassMatchKt, ClassMatch, Class<*>, ClassMatcher, ClassMatcherKt>(match) {
    val superClass: ClassMatchKt
        inline get() = ClassMatchKt(match.superClass)
    val interfaces: ClassLazySequenceKt
        inline get() = ClassLazySequenceKt(match.interfaces)
    val declaredMethods: MethodLazySequenceKt
        inline get() = MethodLazySequenceKt(match.declaredMethods)
    val declaredConstructors: ConstructorLazySequenceKt
        inline get() = ConstructorLazySequenceKt(match.declaredConstructors)
    val declaredFields: FieldLazySequenceKt
        inline get() = FieldLazySequenceKt(match.declaredFields)
    val arrayType: ClassMatchKt
        inline get() = ClassMatchKt(match.arrayType)

    override fun newSelf(match: ClassMatch) = ClassMatchKt(match)

    override fun newMatcher(match: ClassMatcher) = ClassMatcherKt(match)

    inline fun onMatch(crossinline handler: DummyHooker.(Class<*>) -> Unit): ClassMatchKt {
        match.onMatch {
            DummyHooker.handler(it)
        }
        return this
    }

    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(Class<*>) -> Unit
    ): ClassMatchKt {
        match.bind(bind.bind) { _, r ->
            bind.handler(r)
        }
        return this
    }
}

class ParameterMatchKt @PublishedApi internal constructor(match: ParameterMatch) :
    ReflectMatchKt<ParameterMatchKt, ParameterMatch, Parameter, ParameterMatcher, ParameterMatcherKt>(
        match
    ) {
    val type: ClassMatchKt
        inline get() = ClassMatchKt(match.type)

    override fun newSelf(match: ParameterMatch) = ParameterMatchKt(match)
    override fun newMatcher(match: ParameterMatcher) = ParameterMatcherKt(match)

    inline fun onMatch(crossinline handler: DummyHooker.(ParameterKt) -> Unit): ParameterMatchKt {
        match.onMatch {
            DummyHooker.handler(ParameterKt(it))
        }
        return this
    }

    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(ParameterKt) -> Unit
    ): ParameterMatchKt {
        match.bind(bind.bind) { _, r ->
            bind.handler(ParameterKt(r))
        }
        return this
    }
}

sealed class MemberMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match: Match) :
    ReflectMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match) where Self : MemberMatchKt<Self, Match, Reflect, Matcher, MatcherKt>, Match : MemberMatch<Match, Reflect, Matcher>, Reflect : Member, Matcher : MemberMatcher<Matcher>, MatcherKt : MemberMatcherKt<Matcher> {
    val declaringClass: ClassMatchKt
        inline get() = ClassMatchKt(match.declaringClass)

    @Suppress("UNCHECKED_CAST")
    inline fun onMatch(crossinline handler: DummyHooker.(Reflect) -> Unit): Self {
        match.onMatch {
            DummyHooker.handler(it)
        }
        return this as Self
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(Reflect) -> Unit
    ): Self {
        match.bind(bind.bind) { _, r ->
            bind.handler(r)
        }
        return this as Self
    }
}

sealed class ExecutableMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match: Match) :
    MemberMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match) where Self : ExecutableMatchKt<Self, Match, Reflect, Matcher, MatcherKt>, Match : ExecutableMatch<Match, Reflect, Matcher>, Reflect : Member, Matcher : ExecutableMatcher<Matcher>, MatcherKt : ExecutableMatcherKt<Matcher> {
    val parameterTypes: ClassLazySequenceKt
        inline get() = ClassLazySequenceKt(match.parameterTypes)

    val parameters: ParameterLazySequenceKt
        inline get() = ParameterLazySequenceKt(match.parameters)

    @DexAnalysis
    val assignedFields: FieldLazySequenceKt
        inline get() = FieldLazySequenceKt(match.assignedFields)

    @DexAnalysis
    val accessedFields: FieldLazySequenceKt
        inline get() = FieldLazySequenceKt(match.accessedFields)

    @DexAnalysis
    val invokedMethods: MethodLazySequenceKt
        inline get() = MethodLazySequenceKt(match.invokedMethods)

    @DexAnalysis
    val invokedConstructors: ConstructorLazySequenceKt
        inline get() = ConstructorLazySequenceKt(match.invokedConstructors)
}

class MethodMatchKt @PublishedApi internal constructor(match: MethodMatch) :
    ExecutableMatchKt<MethodMatchKt, MethodMatch, Method, MethodMatcher, MethodMatcherKt>(match) {
    val returnType: ClassMatchKt
        inline get() = ClassMatchKt(match.returnType)

    override fun newSelf(match: MethodMatch) = MethodMatchKt(match)
    override fun newMatcher(match: MethodMatcher) = MethodMatcherKt(match)
}

class ConstructorMatchKt @PublishedApi internal constructor(match: ConstructorMatch) :
    ExecutableMatchKt<ConstructorMatchKt, ConstructorMatch, Constructor<*>, ConstructorMatcher, ConstructorMatcherKt>(
        match
    ) {
    override fun newSelf(match: ConstructorMatch) = ConstructorMatchKt(match)
    override fun newMatcher(match: ConstructorMatcher) = ConstructorMatcherKt(match)
}

class FieldMatchKt @PublishedApi internal constructor(match: FieldMatch) :
    MemberMatchKt<FieldMatchKt, FieldMatch, Field, FieldMatcher, FieldMatcherKt>(match) {
    val type: ClassMatchKt
        inline get() = ClassMatchKt(match.type)

    override fun newSelf(match: FieldMatch) = FieldMatchKt(match)
    override fun newMatcher(match: FieldMatcher) = FieldMatcherKt(match)
}

class StringMatchKt @PublishedApi internal constructor(match: StringMatch) :
    BaseMatchKt<StringMatchKt, StringMatch, String>(match)

@Suppress("UNCHECKED_CAST")
@Hooker
sealed class LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(
    @PublishedApi internal val seq: Seq
) where Self : LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>, MatchKt : ReflectMatchKt<MatchKt, Match, Reflect, Matcher, MatcherKt>, Reflect : Any, MatcherKt : ReflectMatcherKt<Matcher>, Match : ReflectMatch<Match, Reflect, Matcher>, Matcher : ReflectMatcher<Matcher>, Seq : LazySequence<Seq, Match, Reflect, Matcher> {
    fun first() = newMatch(seq.first())
    fun unaryPlus() = ContainerSyntaxKt<MatchKt, Match>(seq.conjunction())

    fun unaryMinus() = ContainerSyntaxKt<MatchKt, Match>(seq.disjunction())

    inline fun all(crossinline init: MatcherKt.() -> Unit) = newSelf(seq.all {
        newMatcher(it).init()
    })

    inline fun first(crossinline init: MatcherKt.() -> Unit) = newMatch(seq.first {
        newMatcher(it).init()
    })

    inline fun substituteIfMiss(crossinline substitute: () -> Self): Self {
        seq.substituteIfMiss {
            substitute().seq
        }
        return this as Self
    }

    inline fun matchIfMiss(crossinline handler: MatcherKt.() -> Unit): Self {
        seq.matchIfMiss {
            newMatcher(it).handler()
        }
        return this as Self
    }

    inline fun onMiss(crossinline handler: DummyHooker.() -> Unit): Self {
        seq.onMiss {
            DummyHooker.handler()
        }
        return this as Self
    }

    @PublishedApi
    internal abstract fun newMatch(impl: Match): MatchKt

    @PublishedApi
    internal abstract fun newMatcher(impl: Matcher): MatcherKt

    @PublishedApi
    internal abstract fun newSelf(impl: Seq): Self
}

class ClassLazySequenceKt @PublishedApi internal constructor(seq: ClassLazySequence) :
    LazySequenceKt<ClassLazySequenceKt, ClassMatchKt, Class<*>, ClassMatcherKt, ClassMatch, ClassMatcher, ClassLazySequence>(
        seq
    ) {
    inline fun methods(crossinline init: MethodMatcherKt.() -> Unit) =
        MethodLazySequenceKt(seq.methods {
            MethodMatcherKt(it).init()
        })

    inline fun firstMethod(crossinline init: MethodMatcherKt.() -> Unit) =
        MethodMatchKt(seq.firstMethod {
            MethodMatcherKt(it).init()
        })

    inline fun fields(crossinline init: FieldMatcherKt.() -> Unit) =
        FieldLazySequenceKt(seq.fields {
            FieldMatcherKt(it).init()
        })

    inline fun firstField(crossinline init: FieldMatcherKt.() -> Unit) =
        FieldMatchKt(seq.firstField {
            FieldMatcherKt(it).init()
        })

    inline fun constructors(crossinline init: ConstructorMatcherKt.() -> Unit) =
        ConstructorLazySequenceKt(seq.constructors {
            ConstructorMatcherKt(it).init()
        })

    inline fun firstConstructor(crossinline init: ConstructorMatcherKt.() -> Unit) =
        ConstructorMatchKt(seq.firstConstructor {
            ConstructorMatcherKt(it).init()
        })

    override fun newMatch(impl: ClassMatch) = ClassMatchKt(impl)

    override fun newMatcher(impl: ClassMatcher) = ClassMatcherKt(impl)

    override fun newSelf(impl: ClassLazySequence) = ClassLazySequenceKt(impl)

    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(Sequence<Class<*>>) -> Unit
    ): ClassLazySequenceKt {
        seq.bind(bind.bind) { _, r ->
            bind.handler(r.asSequence())
        }
        return this
    }

    inline fun onMatch(crossinline handler: DummyHooker.(Sequence<Class<*>>) -> Unit): ClassLazySequenceKt {
        seq.onMatch {
            DummyHooker.handler(it.asSequence())
        }
        return this
    }
}

sealed class MemberLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(seq: Seq) :
    LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(seq) where Self : MemberLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>, MatchKt : MemberMatchKt<MatchKt, Match, Reflect, Matcher, MatcherKt>, Reflect : Member, MatcherKt : MemberMatcherKt<Matcher>, Match : MemberMatch<Match, Reflect, Matcher>, Matcher : MemberMatcher<Matcher>, Seq : MemberLazySequence<Seq, Match, Reflect, Matcher> {
    inline fun declaringClasses(crossinline init: ClassMatcherKt.() -> Unit) =
        ClassLazySequenceKt(seq.declaringClasses {
            ClassMatcherKt(it).init()
        })

    inline fun firstDeclaringClass(crossinline init: ClassMatcherKt.() -> Unit) =
        ClassMatchKt(seq.firstDeclaringClass {
            ClassMatcherKt(it).init()
        })

    @Suppress("UNCHECKED_CAST")
    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(Sequence<Reflect>) -> Unit
    ): Self {
        seq.bind(bind.bind) { _, r ->
            bind.handler(r.asSequence())
        }
        return this as Self
    }

    @Suppress("UNCHECKED_CAST")
    inline fun onMatch(crossinline handler: DummyHooker.(Sequence<Reflect>) -> Unit): Self {
        seq.onMatch {
            DummyHooker.handler(it.asSequence())
        }
        return this as Self
    }
}

sealed class ExecutableLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(seq: Seq) :
    MemberLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(seq) where Self : ExecutableLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>, MatchKt : ExecutableMatchKt<MatchKt, Match, Reflect, Matcher, MatcherKt>, Reflect : Member, MatcherKt : ExecutableMatcherKt<Matcher>, Match : ExecutableMatch<Match, Reflect, Matcher>, Matcher : ExecutableMatcher<Matcher>, Seq : ExecutableLazySequence<Seq, Match, Reflect, Matcher> {
    inline fun parameters(crossinline init: ParameterMatcherKt.() -> Unit) =
        ParameterLazySequenceKt(seq.parameters {
            ParameterMatcherKt(it).init()
        })

    inline fun firstParameter(crossinline init: ParameterMatcherKt.() -> Unit) =
        ParameterMatchKt(seq.firstParameter {
            ParameterMatcherKt(it).init()
        })
}

class ParameterLazySequenceKt @PublishedApi internal constructor(impl: ParameterLazySequence) :
    LazySequenceKt<ParameterLazySequenceKt, ParameterMatchKt, Parameter, ParameterMatcherKt, ParameterMatch, ParameterMatcher, ParameterLazySequence>(
        impl
    ) {
    inline fun types(crossinline init: ClassMatcherKt.() -> Unit) = ClassLazySequenceKt(seq.types {
        ClassMatcherKt(it).init()
    })

    inline fun firstType(crossinline init: ClassMatcherKt.() -> Unit) = ClassMatchKt(seq.firstType {
        ClassMatcherKt(it).init()
    })

    override fun newMatch(impl: ParameterMatch) = ParameterMatchKt(impl)

    override fun newMatcher(impl: ParameterMatcher) = ParameterMatcherKt(impl)

    override fun newSelf(impl: ParameterLazySequence) = ParameterLazySequenceKt(impl)

    @Suppress("UNCHECKED_CAST")
    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(Sequence<ParameterKt>) -> Unit
    ): ParameterLazySequenceKt {
        seq.bind(bind.bind) { _, r ->
            bind.handler(r.asSequence().map { ParameterKt(it) })
        }
        return this
    }

    inline fun onMatch(crossinline handler: DummyHooker.(Sequence<ParameterKt>) -> Unit): ParameterLazySequenceKt {
        seq.onMatch { r ->
            DummyHooker.handler(r.asSequence().map { ParameterKt(it) })
        }
        return this
    }
}

class FieldLazySequenceKt @PublishedApi internal constructor(impl: FieldLazySequence) :
    MemberLazySequenceKt<FieldLazySequenceKt, FieldMatchKt, Field, FieldMatcherKt, FieldMatch, FieldMatcher, FieldLazySequence>(
        impl
    ) {
    override fun newMatch(impl: FieldMatch) = FieldMatchKt(impl)

    override fun newMatcher(impl: FieldMatcher) = FieldMatcherKt(impl)

    override fun newSelf(impl: FieldLazySequence) = FieldLazySequenceKt(impl)
    inline fun types(crossinline init: ClassMatcherKt.() -> Unit) = ClassLazySequenceKt(seq.types {
        ClassMatcherKt(it).init()
    })

    inline fun firstType(crossinline init: ClassMatcherKt.() -> Unit) = ClassMatchKt(seq.firstType {
        ClassMatcherKt(it).init()
    })
}

class MethodLazySequenceKt @PublishedApi internal constructor(impl: MethodLazySequence) :
    ExecutableLazySequenceKt<MethodLazySequenceKt, MethodMatchKt, Method, MethodMatcherKt, MethodMatch, MethodMatcher, MethodLazySequence>(
        impl
    ) {
    override fun newMatch(impl: MethodMatch) = MethodMatchKt(impl)

    override fun newMatcher(impl: MethodMatcher) = MethodMatcherKt(impl)

    override fun newSelf(impl: MethodLazySequence) = MethodLazySequenceKt(impl)
    inline fun returnTypes(crossinline init: ClassMatcherKt.() -> Unit) =
        ClassLazySequenceKt(seq.returnTypes {
            ClassMatcherKt(it).init()
        })

    inline fun firstReturnType(crossinline init: ClassMatcherKt.() -> Unit) =
        ClassMatchKt(seq.firstReturnType {
            ClassMatcherKt(it).init()
        })
}

class ConstructorLazySequenceKt @PublishedApi internal constructor(seq: ConstructorLazySequence) :
    ExecutableLazySequenceKt<ConstructorLazySequenceKt, ConstructorMatchKt, Constructor<*>, ConstructorMatcherKt, ConstructorMatch, ConstructorMatcher, ConstructorLazySequence>(
        seq
    ) {
    override fun newMatch(impl: ConstructorMatch) = ConstructorMatchKt(impl)

    override fun newMatcher(impl: ConstructorMatcher) = ConstructorMatcherKt(impl)

    override fun newSelf(impl: ConstructorLazySequence) = ConstructorLazySequenceKt(impl)
}

@Hooker
class HookBuilderKt(@PublishedApi internal val builder: HookBuilder) {
    var exceptionHandler: (Throwable) -> Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(crossinline value) {
            builder.setExceptionHandler {
                value(it)
            }
        }

    @DexAnalysis
    var forceDexAnalysis: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            builder.setForceDexAnalysis(value)
        }

    var executorService: ExecutorService
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            builder.setExecutorService(value)
        }

    var callbackHandler: Handler
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            builder.setCallbackHandler(value)
        }

    var cacheInputStream: InputStream
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            builder.setCacheInputStream(value)
        }

    var cacheOutputStream: OutputStream
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            builder.setCacheOutputStream(value)
        }

    var cacheChecker: (Map<String, Any>) -> Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(crossinline value) {
            builder.setCacheChecker {
                value(it)
            }
        }

    inline fun methods(crossinline init: MethodMatcherKt.() -> Unit) =
        MethodLazySequenceKt(builder.methods {
            MethodMatcherKt(it).init()
        })

    inline fun firstMethod(crossinline init: MethodMatcherKt.() -> Unit) =
        MethodMatchKt(builder.firstMethod {
            MethodMatcherKt(it).init()
        })

    inline fun classes(crossinline init: ClassMatcherKt.() -> Unit) =
        ClassLazySequenceKt(builder.classes {
            ClassMatcherKt(it).init()
        })

    inline fun firstClass(crossinline init: ClassMatcherKt.() -> Unit) =
        ClassMatchKt(builder.firstClass {
            ClassMatcherKt(it).init()
        })

    inline fun fields(crossinline init: FieldMatcherKt.() -> Unit) =
        FieldLazySequenceKt(builder.fields {
            FieldMatcherKt(it).init()
        })

    inline fun firstField(crossinline init: FieldMatcherKt.() -> Unit) =
        FieldMatchKt(builder.firstField {
            FieldMatcherKt(it).init()
        })

    inline fun constructors(crossinline init: ConstructorMatcherKt.() -> Unit) =
        ConstructorLazySequenceKt(builder.constructors {
            ConstructorMatcherKt(it).init()
        })

    inline fun firstConstructor(crossinline init: ConstructorMatcherKt.() -> Unit) =
        ConstructorMatchKt(builder.firstConstructor {
            ConstructorMatcherKt(it).init()
        })

    val String.exact: StringMatchKt
        inline get() = StringMatchKt(builder.exact(this))
    val Class<*>.exact: ClassMatchKt
        inline get() = ClassMatchKt(builder.exact(this))
    val Method.exact: MethodMatchKt
        inline get() = MethodMatchKt(builder.exact(this))
    val Constructor<*>.exact: ConstructorMatchKt
        inline get() = ConstructorMatchKt(builder.exact(this))
    val Field.exact: FieldMatchKt
        inline get() = FieldMatchKt(builder.exact(this))
    val String.prefix: StringMatchKt
        inline get() = StringMatchKt(builder.prefix(this))
    val String.firstPrefix: StringMatchKt
        inline get() = StringMatchKt(builder.firstPrefix(this))
    val String.exactClass: ClassMatchKt
        inline get() = ClassMatchKt(builder.exactClass(this))
    val String.exactMethod: MethodMatchKt
        inline get() = MethodMatchKt(builder.exactMethod(this))
    val String.exactConstructor: ConstructorMatchKt
        inline get() = ConstructorMatchKt(builder.exactConstructor(this))
    val String.exactField: FieldMatchKt
        inline get() = FieldMatchKt(builder.exactField(this))
}

inline fun XposedInterface.buildHooks(
    classLoader: BaseDexClassLoader, sourcePath: String, crossinline init: HookBuilderKt.() -> Unit
) = buildHooks(this, classLoader, sourcePath) {
    HookBuilderKt(it).init()
}
