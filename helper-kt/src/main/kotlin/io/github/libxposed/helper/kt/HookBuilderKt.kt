@file:Suppress("unused")

package io.github.libxposed.helper.kt

import dalvik.system.BaseDexClassLoader
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.helper.HookBuilder.*
import io.github.libxposed.helper.HookBuilderImpl
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import kotlin.experimental.ExperimentalTypeInference


@DslMarker
internal annotation class Hooker

@DslMarker
internal annotation class Matcher

@PublishedApi
internal val wo: Nothing
    get() = throw UnsupportedOperationException("Write-only property")

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

@Hooker
abstract class LazyBind {
    @PublishedApi
    internal val bind = LazyBind {
        onMatch()
    }

    abstract fun onMatch()
}

class ContainerSyntaxKt<MatchKt, Match> @PublishedApi internal constructor(@PublishedApi internal val syntax: ContainerSyntax<Match>) where MatchKt : BaseMatchKt<MatchKt, Match, *, *, *>, Match : BaseMatch<Match, *, *> {
    infix fun and(element: MatchKt) = ContainerSyntaxKt<MatchKt, Match>(syntax.and(element.match))

    infix fun and(element: ContainerSyntaxKt<MatchKt, Match>) =
        ContainerSyntaxKt<MatchKt, Match>(syntax.and(element.syntax))

    infix fun or(element: MatchKt): ContainerSyntaxKt<MatchKt, Match> =
        ContainerSyntaxKt<MatchKt, Match>(syntax.or(element.match))

    infix fun or(element: ContainerSyntaxKt<MatchKt, Match>) =
        ContainerSyntaxKt<MatchKt, Match>(syntax.or(element.syntax))

    operator fun not(): ContainerSyntaxKt<MatchKt, Match> =
        ContainerSyntaxKt<MatchKt, Match>(syntax.not())
}

@Matcher
sealed class BaseMatcherKt<Matcher>(@PublishedApi internal val matcher: Matcher) where Matcher : BaseMatcher<Matcher> {
    var matchFirst: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        set(value) {
            matcher.setMatchFirst(value)
        }
}

sealed class ReflectMatcherKt<Matcher>(matcher: Matcher) :
    BaseMatcherKt<Matcher>(matcher) where Matcher : ReflectMatcher<Matcher> {
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

sealed class TypeMatcherKt<Matcher>(matcher: Matcher) :
    ReflectMatcherKt<Matcher>(matcher) where Matcher : TypeMatcher<Matcher> {
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

class ClassMatcherKt @PublishedApi internal constructor(matcher: ClassMatcher) :
    TypeMatcherKt<ClassMatcher>(matcher)

class ParameterMatcherKt @PublishedApi internal constructor(matcher: ParameterMatcher) :
    TypeMatcherKt<ParameterMatcher>(matcher) {
    var index: Int
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setIndex(value)
        }
}

class StringMatcherKt @PublishedApi internal constructor(matcher: StringMatcher) :
    BaseMatcherKt<StringMatcher>(matcher) {
    var exact: String
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setExact(value)
        }
    var prefix: String
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setPrefix(value)
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
    var parameterTypes: ContainerSyntaxKt<ParameterMatchKt, ParameterMatch>
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            matcher.setParameterTypes(value.syntax)
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
sealed class BaseMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(
    @PublishedApi internal val match: Match
) where Self : BaseMatchKt<Self, Match, Reflect, Matcher, MatcherKt>, Match : BaseMatch<Match, Reflect, Matcher>, Matcher : BaseMatcher<Matcher>, MatcherKt : BaseMatcherKt<Matcher> {
    operator fun unaryPlus() = ContainerSyntaxKt<Self, Match>(match.observe())

    operator fun unaryMinus() = ContainerSyntaxKt<Self, Match>(match.reverse())
}

@Suppress("UNCHECKED_CAST")
sealed class ReflectMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match: Match) :
    BaseMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match) where Self : ReflectMatchKt<Self, Match, Reflect, Matcher, MatcherKt>, Match : ReflectMatch<Match, Reflect, Matcher>, Matcher : ReflectMatcher<Matcher>, MatcherKt : ReflectMatcherKt<Matcher> {
    var key: String?
        inline get() = match.key
        inline set(value) {
            match.key = value
        }

    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(Reflect) -> Unit
    ): Self {
        match.bind(bind.bind) { _, r ->
            bind.handler(r)
        }
        return this as Self
    }

    inline fun onMatch(crossinline handler: DummyHooker.(Reflect) -> Unit): Self {
        match.onMatch {
            DummyHooker.handler(it)
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

sealed class TypeMatchKt<Self, Match, Matcher, MatcherKt>(match: Match) :
    ReflectMatchKt<Self, Match, Class<*>, Matcher, MatcherKt>(match) where Self : TypeMatchKt<Self, Match, Matcher, MatcherKt>, Match : TypeMatch<Match, Matcher>, Matcher : TypeMatcher<Matcher>, MatcherKt : TypeMatcherKt<Matcher> {
    val name: StringMatchKt
        inline get() = StringMatchKt(match.name)
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
}

class ClassMatchKt @PublishedApi internal constructor(match: ClassMatch) :
    TypeMatchKt<ClassMatchKt, ClassMatch, ClassMatcher, ClassMatcherKt>(match) {
    override fun newSelf(match: ClassMatch) = ClassMatchKt(match)

    operator fun get(index: Int): ParameterMatchKt = ParameterMatchKt(match.asParameter(index))
    override fun newMatcher(match: ClassMatcher) = ClassMatcherKt(match)
}

class ParameterMatchKt @PublishedApi internal constructor(match: ParameterMatch) :
    TypeMatchKt<ParameterMatchKt, ParameterMatch, ParameterMatcher, ParameterMatcherKt>(match) {

    override fun newSelf(match: ParameterMatch) = ParameterMatchKt(match)
    override fun newMatcher(match: ParameterMatcher) = ParameterMatcherKt(match)
}

sealed class MemberMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match: Match) :
    ReflectMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match) where Self : MemberMatchKt<Self, Match, Reflect, Matcher, MatcherKt>, Match : MemberMatch<Match, Reflect, Matcher>, Reflect : Member, Matcher : MemberMatcher<Matcher>, MatcherKt : MemberMatcherKt<Matcher> {
    val declaringClass: ClassMatchKt
        inline get() = ClassMatchKt(match.declaringClass)
}

sealed class ExecutableMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match: Match) :
    MemberMatchKt<Self, Match, Reflect, Matcher, MatcherKt>(match) where Self : ExecutableMatchKt<Self, Match, Reflect, Matcher, MatcherKt>, Match : ExecutableMatch<Match, Reflect, Matcher>, Reflect : Member, Matcher : ExecutableMatcher<Matcher>, MatcherKt : ExecutableMatcherKt<Matcher> {
    val parameterTypes: ParameterLazySequenceKt
        inline get() = ParameterLazySequenceKt(match.parameterTypes)

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
    val name: StringMatchKt
        inline get() = StringMatchKt(match.name)
    val returnType: ClassMatchKt
        inline get() = ClassMatchKt(match.returnType)

    override fun newSelf(match: MethodMatch): MethodMatchKt = MethodMatchKt(match)
    override fun newMatcher(match: MethodMatcher) = MethodMatcherKt(match)
}

class ConstructorMatchKt @PublishedApi internal constructor(match: ConstructorMatch) :
    ExecutableMatchKt<ConstructorMatchKt, ConstructorMatch, Constructor<*>, ConstructorMatcher, ConstructorMatcherKt>(
        match
    ) {
    override fun newSelf(match: ConstructorMatch): ConstructorMatchKt = ConstructorMatchKt(match)
    override fun newMatcher(match: ConstructorMatcher) = ConstructorMatcherKt(match)
}

class FieldMatchKt @PublishedApi internal constructor(match: FieldMatch) :
    MemberMatchKt<FieldMatchKt, FieldMatch, Field, FieldMatcher, FieldMatcherKt>(match) {
    val name: StringMatchKt
        inline get() = StringMatchKt(match.name)
    val type: ClassMatchKt
        inline get() = ClassMatchKt(match.type)

    override fun newSelf(match: FieldMatch): FieldMatchKt = FieldMatchKt(match)
    override fun newMatcher(match: FieldMatcher) = FieldMatcherKt(match)
}

class StringMatchKt @PublishedApi internal constructor(match: StringMatch) :
    BaseMatchKt<StringMatchKt, StringMatch, String, StringMatcher, StringMatcherKt>(match)

@Suppress("UNCHECKED_CAST")
@Hooker
@OptIn(ExperimentalTypeInference::class)
sealed class LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(
    @PublishedApi internal val seq: Seq
) where Self : LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>, MatchKt : BaseMatchKt<MatchKt, Match, Reflect, Matcher, MatcherKt>, Reflect : Any, MatcherKt : BaseMatcherKt<Matcher>, Match : BaseMatch<Match, Reflect, Matcher>, Matcher : BaseMatcher<Matcher>, Seq : LazySequence<Seq, Match, Reflect, Matcher> {
    fun first() = newMatch(seq.first())
    fun unaryPlus() = ContainerSyntaxKt<MatchKt, Match>(seq.conjunction())

    fun unaryMinus() = ContainerSyntaxKt<MatchKt, Match>(seq.disjunction())

    @Suppress("UNCHECKED_CAST")
    inline fun <Bind : LazyBind> bind(
        bind: Bind, crossinline handler: Bind.(Sequence<Reflect>) -> Unit
    ): Self {
        seq.bind(bind.bind) { _, r ->
            bind.handler(r.asSequence())
        }
        return this as Self
    }

    @OverloadResolutionByLambdaReturnType
    inline fun onMatch(crossinline handler: DummyHooker.(Sequence<Reflect>) -> Reflect): MatchKt =
        newMatch(seq.onMatch(MatchConsumer<Iterable<Reflect>, Reflect> {
            handler(
                DummyHooker, it.asSequence()
            )
        }))

    @OverloadResolutionByLambdaReturnType
    inline fun onMatch(crossinline handler: DummyHooker.(Sequence<Reflect>) -> Unit): Self =
        newSelf(seq.onMatch(Consumer<Iterable<Reflect>> { t ->
            DummyHooker.handler(
                t.asSequence()
            )
        }))

    inline fun all(crossinline init: MatcherKt.() -> Unit) = newSelf(seq.all {
        newMatcher(it).init()
    })

    inline fun first(crossinline init: MatcherKt.() -> Unit): MatchKt = newMatch(seq.first {
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

    @PublishedApi
    internal abstract fun newMatch(impl: Match): MatchKt

    @PublishedApi
    internal abstract fun newMatcher(impl: Matcher): MatcherKt

    @PublishedApi
    internal abstract fun newSelf(impl: Seq): Self
}

sealed class TypeLazySequenceKt<Base, MatchKt, MatcherKt, Match, Matcher, Seq>(seq: Seq) :
    LazySequenceKt<Base, MatchKt, Class<*>, MatcherKt, Match, Matcher, Seq>(seq) where Base : TypeLazySequenceKt<Base, MatchKt, MatcherKt, Match, Matcher, Seq>, MatchKt : TypeMatchKt<MatchKt, Match, Matcher, MatcherKt>, MatcherKt : TypeMatcherKt<Matcher>, Match : TypeMatch<Match, Matcher>, Matcher : TypeMatcher<Matcher>, Seq : TypeLazySequence<Seq, Match, Matcher> {
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


class ClassLazySequenceKt @PublishedApi internal constructor(impl: ClassLazySequence) :
    TypeLazySequenceKt<ClassLazySequenceKt, ClassMatchKt, ClassMatcherKt, ClassMatch, ClassMatcher, ClassLazySequence>(
        impl
    ) {
    override fun newMatch(impl: ClassMatch) = ClassMatchKt(impl)

    override fun newMatcher(impl: ClassMatcher) = ClassMatcherKt(impl)

    override fun newSelf(impl: ClassLazySequence) = ClassLazySequenceKt(impl)
}

class ParameterLazySequenceKt @PublishedApi internal constructor(impl: ParameterLazySequence) :
    TypeLazySequenceKt<ParameterLazySequenceKt, ParameterMatchKt, ParameterMatcherKt, ParameterMatch, ParameterMatcher, ParameterLazySequence>(
        impl
    ) {
    override fun newMatch(impl: ParameterMatch) = ParameterMatchKt(impl)

    override fun newMatcher(impl: ParameterMatcher) = ParameterMatcherKt(impl)

    override fun newSelf(impl: ParameterLazySequence) = ParameterLazySequenceKt(impl)
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
class HookBuilderKt(ctx: XposedInterface, classLoader: BaseDexClassLoader, sourcePath: String) {
    @PublishedApi
    internal val builder = HookBuilderImpl(ctx, classLoader, sourcePath)

    var lastMatchResult: MatchResultKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = wo
        inline set(value) {
            builder.setLastMatchResult((value.result))
        }

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

    inline fun string(crossinline init: StringMatcherKt.() -> Unit) = StringMatchKt(builder.string {
        StringMatcherKt(it).init()
    })

    val String.exact: StringMatchKt
        inline get() = StringMatchKt(builder.exact(this))
    val Class<*>.exact: ClassMatchKt
        inline get() = ClassMatchKt(builder.exact(this))
    val Method.exact: MethodMatchKt
        inline get() = MethodMatchKt(builder.exact(this))
    val Constructor<*>.exact: ConstructorMatchKt
        inline get() = ConstructorMatchKt(builder.exact(this))
    val Array<Class<*>>.exact: ParameterMatchKt
        inline get() = ParameterMatchKt(builder.exact(*this))
    val Field.exact: FieldMatchKt
        inline get() = FieldMatchKt(builder.exact(this))
    val String.prefix: StringMatchKt
        inline get() = StringMatchKt(builder.prefix(this))
    val String.exactClass: ClassMatchKt
        inline get() = ClassMatchKt(builder.exactClass(this))
    val String.exactMethod: MethodMatchKt
        inline get() = MethodMatchKt(builder.exactMethod(this))
    val String.exactConstructor: ConstructorMatchKt
        inline get() = ConstructorMatchKt(builder.exactConstructor(this))
    val String.exactField: FieldMatchKt
        inline get() = FieldMatchKt(builder.exactField(this))
    val String.exactParameter: ParameterMatchKt
        inline get() = ParameterMatchKt(builder.exactParameter(this))

    class MatchResultKt @PublishedApi internal constructor(val result: MatchResult) {
        val matchedClasses: Map<String, Class<*>>
            inline get() = result.matchedClasses
        val matchedFields: Map<String, Field>
            inline get() = result.matchedFields
        val matchedMethods: Map<String, Method>
            inline get() = result.matchedMethods
        val matchedConstructors: Map<String, Constructor<*>>
            inline get() = result.matchedConstructors
    }

    fun build() = MatchResultKt(builder.build())
}

inline fun XposedInterface.buildHooks(
    classLoader: BaseDexClassLoader, sourcePath: String, init: HookBuilderKt.() -> Unit
): HookBuilderKt.MatchResultKt {
    val builder = HookBuilderKt(this, classLoader, sourcePath)
    builder.init()
    return builder.build()
}
