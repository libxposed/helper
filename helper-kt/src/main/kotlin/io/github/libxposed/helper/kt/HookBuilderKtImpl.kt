@file:Suppress("OverridingDeprecatedMember")

package io.github.libxposed.helper.kt

import dalvik.system.BaseDexClassLoader
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.helper.HookBuilder
import io.github.libxposed.helper.HookBuilder.ContainerSyntax
import io.github.libxposed.helper.HookBuilderImpl
import java.lang.reflect.Member
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.Class

class WOException : UnsupportedOperationException("Write-only property")

val wo: Nothing
    @Throws(WOException::class)
    inline get() = throw WOException()

abstract class BaseMatcherKtImpl<Match, MatchKt, Matcher>
    (
    internal val matcher: Matcher
) : HookBuilderKt.BaseMatcherKt<MatchKt> where Matcher : HookBuilder.BaseMatcher<Matcher, Match>, MatchKt : HookBuilderKt.BaseMatchKt<MatchKt, *>, Match : HookBuilder.BaseMatch<Match, *> {
    final override var matchFirst: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMatchFirst(value)
        }
}

abstract class ReflectMatcherKtImpl<Match, MatchKt, Matcher>(
    matcher: Matcher
) : BaseMatcherKtImpl<Match, MatchKt, Matcher>(matcher),
    HookBuilderKt.ReflectMatcherKt<MatchKt> where Matcher : HookBuilder.ReflectMatcher<Matcher, Match>, MatchKt : HookBuilderKt.ReflectMatchKt<MatchKt, *>, Match : HookBuilder.ReflectMatch<Match, *> {
    final override var key: String
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setKey(value)
        }
    final override var isPublic: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsPublic(value)
        }
    final override var isPrivate: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsPrivate(value)
        }
    final override var isProtected: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsProtected(value)
        }
    final override var isPackage: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsPackage(value)
        }
}

open class ContainerSyntaxKtImpl<MatchKt, Match>(internal val syntax: ContainerSyntax<Match>) :
    HookBuilderKt.ContainerSyntaxKt<MatchKt> where MatchKt : HookBuilderKt.BaseMatchKt<MatchKt, *>, Match : HookBuilder.BaseMatch<Match, *> {
    override fun and(element: MatchKt): HookBuilderKt.ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun and(element: HookBuilderKt.ContainerSyntaxKt<MatchKt>): HookBuilderKt.ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun or(element: MatchKt): HookBuilderKt.ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun or(element: HookBuilderKt.ContainerSyntaxKt<MatchKt>): HookBuilderKt.ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun not(): HookBuilderKt.ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }
}

@Suppress("UNCHECKED_CAST")
abstract class TypeMatcherKtImpl<Match, MatchKt, Matcher>(matcher: Matcher) :
    BaseMatcherKtImpl<Match, MatchKt, Matcher>(
        matcher
    ),
    HookBuilderKt.TypeMatcherKt<MatchKt> where Matcher : HookBuilder.TypeMatcher<Matcher, Match>, MatchKt : HookBuilderKt.TypeMatchKt<MatchKt>, Match : HookBuilder.TypeMatch<Match> {
    override var name: HookBuilderKt.StringMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setName((value as StringMatchKtImpl).match)
        }
    override var superClass: HookBuilderKt.ClassMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setSuperClass((value as ClassMatchKtImpl).match)
        }
    override var containsMethods: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.MethodMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setContainsMethods((value as ContainerSyntaxKtImpl<HookBuilderKt.MethodMatchKt, HookBuilder.MethodMatch>).syntax)
        }
    override var containsConstructors: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ConstructorMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setContainsConstructors((value as ContainerSyntaxKtImpl<HookBuilderKt.ConstructorMatchKt, HookBuilder.ConstructorMatch>).syntax)
        }
    override var containsFields: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.FieldMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setContainsFields((value as ContainerSyntaxKtImpl<HookBuilderKt.FieldMatchKt, HookBuilder.FieldMatch>).syntax)
        }
    override var interfaces: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ClassMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setInterfaces((value as ContainerSyntaxKtImpl<HookBuilderKt.ClassMatchKt, HookBuilder.ClassMatch>).syntax)
        }
    override var isAbstract: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsAbstract(value)
        }
    override var isStatic: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsStatic(value)
        }
    override var isFinal: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsFinal(value)
        }
    override var key: String
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setKey(value)
        }
    override var isPublic: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsPublic(value)
        }
    override var isPrivate: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsPrivate(value)
        }
    override var isProtected: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsProtected(value)
        }
    override var isPackage: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsPackage(value)
        }
}

class ClassMatcherKtImpl(matcher: HookBuilder.ClassMatcher) :
    TypeMatcherKtImpl<HookBuilder.ClassMatch, HookBuilderKt.ClassMatchKt, HookBuilder.ClassMatcher>(
        matcher
    ), HookBuilderKt.ClassMatcherKt {
    override var missReplacement: HookBuilderKt.ClassMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as ClassMatchKtImpl).match)
        }
}

class ParameterMatcherKtImpl(matcher: HookBuilder.ParameterMatcher) :
    TypeMatcherKtImpl<HookBuilder.ParameterMatch, HookBuilderKt.ParameterMatchKt, HookBuilder.ParameterMatcher>(
        matcher
    ), HookBuilderKt.ParameterMatcherKt {
    override var index: Int
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIndex(value)
        }
    override var missReplacement: HookBuilderKt.ParameterMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as ParameterMatchKtImpl).match)
        }
}

class StringMatcherKtImpl(matcher: HookBuilder.StringMatcher) :
    BaseMatcherKtImpl<HookBuilder.StringMatch, HookBuilderKt.StringMatchKt, HookBuilder.StringMatcher>(
        matcher
    ),
    HookBuilderKt.StringMatcherKt {
    override var exact: String
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setExact(value)
        }
    override var prefix: String
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setPrefix(value)
        }
    override var missReplacement: HookBuilderKt.StringMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as StringMatchKtImpl).match)
        }
}

abstract class MemberMatcherKtImpl<Match, MatchKt, Matcher>(
    matcher: Matcher
) : ReflectMatcherKtImpl<Match, MatchKt, Matcher>(matcher),
    HookBuilderKt.MemberMatcherKt<MatchKt> where Matcher : HookBuilder.MemberMatcher<Matcher, Match>, MatchKt : HookBuilderKt.MemberMatchKt<MatchKt, *>, Match : HookBuilder.MemberMatch<Match, *> {
    final override var declaringClass: HookBuilderKt.ClassMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setDeclaringClass((value as ClassMatchKtImpl).match)
        }
    final override var isSynthetic: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsSynthetic(value)
        }
}

class FieldMatcherKtImpl(matcher: HookBuilder.FieldMatcher) :
    MemberMatcherKtImpl<HookBuilder.FieldMatch, HookBuilderKt.FieldMatchKt, HookBuilder.FieldMatcher>(
        matcher
    ), HookBuilderKt.FieldMatcherKt {
    override var name: HookBuilderKt.StringMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setName((value as StringMatchKtImpl).match)
        }
    override var type: HookBuilderKt.ClassMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setType((value as ClassMatchKtImpl).match)
        }
    override var isStatic: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsStatic(value)
        }
    override var isFinal: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsFinal(value)
        }
    override var isTransient: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsTransient(value)
        }
    override var isVolatile: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsVolatile(value)
        }
    override var missReplacement: HookBuilderKt.FieldMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as FieldMatchKtImpl).match)
        }
}

@Suppress("UNCHECKED_CAST")
abstract class ExecutableMatcherKtImpl<Match, MatchKt, Matcher>(
    matcher: Matcher
) : MemberMatcherKtImpl<Match, MatchKt, Matcher>(matcher),
    HookBuilderKt.ExecutableMatcherKt<MatchKt> where Matcher : HookBuilder.ExecutableMatcher<Matcher, Match>, MatchKt : HookBuilderKt.ExecutableMatchKt<MatchKt, *>, Match : HookBuilder.ExecutableMatch<Match, *> {
    final override var parameterCounts: Int
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setParameterCount(value)
        }
    final override var parameterTypes: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ParameterMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setParameterTypes((value as ContainerSyntaxKtImpl<HookBuilderKt.ParameterMatchKt, HookBuilder.ParameterMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var referredStrings: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.StringMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setReferredStrings((value as ContainerSyntaxKtImpl<HookBuilderKt.StringMatchKt, HookBuilder.StringMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var assignedFields: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.FieldMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setAssignedFields((value as ContainerSyntaxKtImpl<HookBuilderKt.FieldMatchKt, HookBuilder.FieldMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var accessedFields: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.FieldMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setAccessedFields((value as ContainerSyntaxKtImpl<HookBuilderKt.FieldMatchKt, HookBuilder.FieldMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var invokedMethods: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.MethodMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setInvokedMethods((value as ContainerSyntaxKtImpl<HookBuilderKt.MethodMatchKt, HookBuilder.MethodMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var invokedConstructor: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ConstructorMatchKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setInvokedConstructors((value as ContainerSyntaxKtImpl<HookBuilderKt.ConstructorMatchKt, HookBuilder.ConstructorMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var containsOpcodes: Array<Byte>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setContainsOpcodes(value)
        }
    final override var isVarargs: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsVarargs(value)
        }
}

class MethodMatcherKtImpl(matcher: HookBuilder.MethodMatcher) :
    ExecutableMatcherKtImpl<HookBuilder.MethodMatch, HookBuilderKt.MethodMatchKt, HookBuilder.MethodMatcher>(
        matcher
    ), HookBuilderKt.MethodMatcherKt {
    override var name: HookBuilderKt.StringMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setName((value as StringMatchKtImpl).match)
        }
    override var returnType: HookBuilderKt.ClassMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setReturnType((value as ClassMatchKtImpl).match)
        }
    override var isAbstract: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsAbstract(value)
        }
    override var isStatic: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsStatic(value)
        }
    override var isFinal: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsFinal(value)
        }
    override var isSynchronized: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsSynchronized(value)
        }
    override var isNative: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsNative(value)
        }
    override var missReplacement: HookBuilderKt.MethodMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as MethodMatchKtImpl).match)
        }
}

class ConstructorMatcherKtImpl(matcher: HookBuilder.ConstructorMatcher) :
    ExecutableMatcherKtImpl<HookBuilder.ConstructorMatch, HookBuilderKt.ConstructorMatchKt, HookBuilder.ConstructorMatcher>(
        matcher
    ), HookBuilderKt.ConstructorMatcherKt {
    override var missReplacement: HookBuilderKt.ConstructorMatchKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as ConstructorMatchKtImpl).match)
        }
}

object DummyHookerImpl : HookBuilderKt.DummyHooker

abstract class BaseMatchKtImpl<MatchKt, Reflect, Match>(
    internal val match: Match
) : HookBuilderKt.BaseMatchKt<MatchKt, Reflect> where MatchKt : HookBuilderKt.BaseMatchKt<MatchKt, Reflect>, Match : HookBuilder.BaseMatch<Match, Reflect> {
    override fun unaryPlus(): HookBuilderKt.ContainerSyntaxKt<MatchKt> =
        ContainerSyntaxKtImpl(match.observe())

    override fun unaryMinus(): HookBuilderKt.ContainerSyntaxKt<MatchKt> =
        ContainerSyntaxKtImpl(match.reverse())
}

abstract class ReflectMatchKtImpl<MatchKt, Reflect, Match>(
    impl: Match
) : BaseMatchKtImpl<MatchKt, Reflect, Match>(impl),
    HookBuilderKt.ReflectMatchKt<MatchKt, Reflect> where MatchKt : HookBuilderKt.ReflectMatchKt<MatchKt, Reflect>, Match : HookBuilder.ReflectMatch<Match, Reflect> {
    final override val key: String?
        get() = match.key

    override fun <Bind : HookBuilderKt.LazyBind> bind(
        bind: Bind,
        handler: Bind.(Reflect) -> Unit
    ): MatchKt {
        match.bind(bind.impl) { _, r ->
            bind.handler(r)
        }
        @Suppress("UNCHECKED_CAST")
        return this as MatchKt
    }

    final override fun onMatch(handler: HookBuilderKt.DummyHooker.(Reflect) -> Unit): MatchKt =
        newMatchKt(match.onMatch {
            DummyHookerImpl.handler(it)
        })

    abstract fun newMatchKt(match: Match): MatchKt
}

abstract class LazySequenceKtImpl<MatchKt, Reflect : Any, MatcherKt, Match, Matcher>(
    private val impl: HookBuilder.LazySequence<Match, Reflect, Matcher>
) : HookBuilderKt.LazySequenceKt<MatchKt, Reflect, MatcherKt> where MatchKt : HookBuilderKt.BaseMatchKt<MatchKt, Reflect>, MatcherKt : HookBuilderKt.BaseMatcherKt<MatchKt>, Match : HookBuilder.BaseMatch<Match, Reflect>, Matcher : HookBuilder.BaseMatcher<Matcher, Match> {
    override fun first(): MatchKt = newImpl(impl.first())
    override fun unaryPlus(): HookBuilderKt.ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun unaryMinus(): HookBuilderKt.ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun <Bind : HookBuilderKt.LazyBind> bind(
        bind: Bind,
        handler: Bind.(Sequence<Reflect>) -> Unit
    ): HookBuilderKt.LazySequenceKt<MatchKt, Reflect, MatcherKt> {
        impl.bind(bind.impl) { _, r ->
            bind.handler(r.asSequence())
        }
        return this
    }

    override fun onMatch(handler: HookBuilderKt.DummyHooker.(Sequence<Reflect>) -> Reflect): MatchKt =
        newImpl(impl.onMatch(HookBuilder.MatchConsumer<Iterable<Reflect>, Reflect> {
            handler(
                DummyHookerImpl, it.asSequence()
            )
        }))

    override fun onMatch(handler: HookBuilderKt.DummyHooker.(Sequence<Reflect>) -> Unit): HookBuilderKt.LazySequenceKt<MatchKt, Reflect, MatcherKt> =
        newSequence(impl.onMatch(HookBuilder.Consumer<Iterable<Reflect>> { t ->
            DummyHookerImpl.handler(
                t.asSequence()
            )
        }))

    override fun all(init: MatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<MatchKt, Reflect, MatcherKt> =
        newSequence(impl.all {
            newMatcher(it).init()
        })

    override fun first(init: MatcherKt.() -> Unit): MatchKt = newImpl(impl.first {
        newMatcher(it).init()
    })

    abstract fun newImpl(impl: Match): MatchKt

    abstract fun newMatcher(impl: Matcher): MatcherKt

    abstract fun newSequence(impl: HookBuilder.LazySequence<Match, Reflect, Matcher>): HookBuilderKt.LazySequenceKt<MatchKt, Reflect, MatcherKt>
}

abstract class TypeMatchKtImpl<MatchKt, Match>(
    match: Match
) : ReflectMatchKtImpl<MatchKt, Class<*>, Match>(match),
    HookBuilderKt.TypeMatchKt<MatchKt> where MatchKt : HookBuilderKt.TypeMatchKt<MatchKt>, Match : HookBuilder.TypeMatch<Match> {
    override val name: HookBuilderKt.StringMatchKt
        get() = StringMatchKtImpl(match.name)
    override val superClass: HookBuilderKt.ClassMatchKt
        get() = ClassMatchKtImpl(match.superClass)
    override val interfaces: ClassLazySequenceKtImpl
        get() = ClassLazySequenceKtImpl(match.interfaces)
    override val declaredMethods: HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodMatchKt, Method, HookBuilderKt.MethodMatcherKt>
        get() = MethodLazySequenceKtImpl(match.declaredMethods)
    override val declaredConstructors: HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorMatchKt, Constructor<*>, HookBuilderKt.ConstructorMatcherKt>
        get() = ConstructorLazySequenceKtImpl(match.declaredConstructors)
    override val declaredFields: HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldMatchKt, Field, HookBuilderKt.FieldMatcherKt>
        get() = FieldLazySequenceKtImpl(match.declaredFields)
    override val arrayType: HookBuilderKt.ClassMatchKt
        get() = ClassMatchKtImpl(match.arrayType)
}

class ClassMatchKtImpl(match: HookBuilder.ClassMatch) :
    TypeMatchKtImpl<HookBuilderKt.ClassMatchKt, HookBuilder.ClassMatch>(match),
    HookBuilderKt.ClassMatchKt {

    override fun newMatchKt(match: HookBuilder.ClassMatch): HookBuilderKt.ClassMatchKt =
        ClassMatchKtImpl(match)

    override fun get(index: Int): HookBuilderKt.ParameterMatchKt {
        TODO("Not yet implemented")
    }
}

class ParameterMatchKtImpl(match: HookBuilder.ParameterMatch) :
    TypeMatchKtImpl<HookBuilderKt.ParameterMatchKt, HookBuilder.ParameterMatch>(match),
    HookBuilderKt.ParameterMatchKt {

    override fun newMatchKt(match: HookBuilder.ParameterMatch): HookBuilderKt.ParameterMatchKt =
        ParameterMatchKtImpl(match)
}

abstract class MemberMatchKtImpl<Base, Reflect, Impl : HookBuilder.MemberMatch<Impl, Reflect>>(
    impl: Impl
) : ReflectMatchKtImpl<Base, Reflect, Impl>(impl),
    HookBuilderKt.MemberMatchKt<Base, Reflect> where Base : HookBuilderKt.MemberMatchKt<Base, Reflect>, Reflect : Member {
    final override val declaringClass: HookBuilderKt.ClassMatchKt
        get() = ClassMatchKtImpl(match.declaringClass)
}

class ClassLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.ClassMatch, Class<*>, HookBuilder.ClassMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.ClassMatchKt, Class<*>, HookBuilderKt.ClassMatcherKt, HookBuilder.ClassMatch, HookBuilder.ClassMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.ClassMatch): HookBuilderKt.ClassMatchKt =
        ClassMatchKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.ClassMatcher): HookBuilderKt.ClassMatcherKt =
        ClassMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.ClassMatch, Class<*>, HookBuilder.ClassMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.ClassMatchKt, Class<*>, HookBuilderKt.ClassMatcherKt> =
        ClassLazySequenceKtImpl(impl)
}

class ParameterLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.ParameterMatch, Class<*>, HookBuilder.ParameterMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.ParameterMatchKt, Class<*>, HookBuilderKt.ParameterMatcherKt, HookBuilder.ParameterMatch, HookBuilder.ParameterMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.ParameterMatch) = ParameterMatchKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.ParameterMatcher) = ParameterMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.ParameterMatch, Class<*>, HookBuilder.ParameterMatcher>) =
        ParameterLazySequenceKtImpl(impl)
}

class FieldLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.FieldMatch, Field, HookBuilder.FieldMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.FieldMatchKt, Field, HookBuilderKt.FieldMatcherKt, HookBuilder.FieldMatch, HookBuilder.FieldMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.FieldMatch): HookBuilderKt.FieldMatchKt =
        FieldMatchKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.FieldMatcher): HookBuilderKt.FieldMatcherKt =
        FieldMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.FieldMatch, Field, HookBuilder.FieldMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldMatchKt, Field, HookBuilderKt.FieldMatcherKt> =
        FieldLazySequenceKtImpl(impl)
}

class MethodLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.MethodMatch, Method, HookBuilder.MethodMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.MethodMatchKt, Method, HookBuilderKt.MethodMatcherKt, HookBuilder.MethodMatch, HookBuilder.MethodMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.MethodMatch): HookBuilderKt.MethodMatchKt =
        MethodMatchKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.MethodMatcher): HookBuilderKt.MethodMatcherKt =
        MethodMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.MethodMatch, Method, HookBuilder.MethodMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodMatchKt, Method, HookBuilderKt.MethodMatcherKt> =
        MethodLazySequenceKtImpl(impl)
}

class ConstructorLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.ConstructorMatch, Constructor<*>, HookBuilder.ConstructorMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.ConstructorMatchKt, Constructor<*>, HookBuilderKt.ConstructorMatcherKt, HookBuilder.ConstructorMatch, HookBuilder.ConstructorMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.ConstructorMatch): HookBuilderKt.ConstructorMatchKt =
        ConstructorMatchKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.ConstructorMatcher): HookBuilderKt.ConstructorMatcherKt =
        ConstructorMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.ConstructorMatch, Constructor<*>, HookBuilder.ConstructorMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorMatchKt, Constructor<*>, HookBuilderKt.ConstructorMatcherKt> =
        ConstructorLazySequenceKtImpl(impl)
}

class StringLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.StringMatch, String, HookBuilder.StringMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.StringMatchKt, String, HookBuilderKt.StringMatcherKt, HookBuilder.StringMatch, HookBuilder.StringMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.StringMatch): HookBuilderKt.StringMatchKt =
        StringMatchKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.StringMatcher): HookBuilderKt.StringMatcherKt =
        StringMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.StringMatch, String, HookBuilder.StringMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.StringMatchKt, String, HookBuilderKt.StringMatcherKt> =
        StringLazySequenceKtImpl(impl)
}

abstract class ExecutableMatchKtImpl<MatchKt, Reflect, Match : HookBuilder.ExecutableMatch<Match, Reflect>>(
    impl: Match
) : MemberMatchKtImpl<MatchKt, Reflect, Match>(
    impl
),
    HookBuilderKt.ExecutableMatchKt<MatchKt, Reflect> where MatchKt : HookBuilderKt.ExecutableMatchKt<MatchKt, Reflect>, Reflect : Member {
    final override val parameterTypes: HookBuilderKt.LazySequenceKt<HookBuilderKt.ParameterMatchKt, Class<*>, HookBuilderKt.ParameterMatcherKt>
        get() = ParameterLazySequenceKtImpl(match.parameterTypes)

    @HookBuilderKt.DexAnalysis
    final override val referredStrings: HookBuilderKt.LazySequenceKt<HookBuilderKt.StringMatchKt, String, HookBuilderKt.StringMatcherKt>
        get() = StringLazySequenceKtImpl(match.referredStrings)

    @HookBuilderKt.DexAnalysis
    final override val assignedFields: HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldMatchKt, Field, HookBuilderKt.FieldMatcherKt>
        get() = FieldLazySequenceKtImpl(match.assignedFields)

    @HookBuilderKt.DexAnalysis
    final override val accessedFields: HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldMatchKt, Field, HookBuilderKt.FieldMatcherKt>
        get() = FieldLazySequenceKtImpl(match.accessedFields)

    @HookBuilderKt.DexAnalysis
    final override val invokedMethods: HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodMatchKt, Method, HookBuilderKt.MethodMatcherKt>
        get() = MethodLazySequenceKtImpl(match.invokedMethods)

    @HookBuilderKt.DexAnalysis
    final override val invokedConstructors: HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorMatchKt, Constructor<*>, HookBuilderKt.ConstructorMatcherKt>
        get() = ConstructorLazySequenceKtImpl(match.invokedConstructors)
}

class MethodMatchKtImpl(match: HookBuilder.MethodMatch) :
    ExecutableMatchKtImpl<HookBuilderKt.MethodMatchKt, Method, HookBuilder.MethodMatch>(
        match
    ), HookBuilderKt.MethodMatchKt {
    override val name: HookBuilderKt.StringMatchKt
        get() = StringMatchKtImpl(match.name)
    override val returnType: HookBuilderKt.ClassMatchKt
        get() = ClassMatchKtImpl(match.returnType)

    override fun newMatchKt(match: HookBuilder.MethodMatch): HookBuilderKt.MethodMatchKt =
        MethodMatchKtImpl(match)
}

class ConstructorMatchKtImpl(match: HookBuilder.ConstructorMatch) :
    ExecutableMatchKtImpl<HookBuilderKt.ConstructorMatchKt, Constructor<*>, HookBuilder.ConstructorMatch>(
        match
    ), HookBuilderKt.ConstructorMatchKt {
    override fun newMatchKt(match: HookBuilder.ConstructorMatch): HookBuilderKt.ConstructorMatchKt =
        ConstructorMatchKtImpl(match)
}

class FieldMatchKtImpl(match: HookBuilder.FieldMatch) :
    MemberMatchKtImpl<HookBuilderKt.FieldMatchKt, Field, HookBuilder.FieldMatch>(
        match
    ), HookBuilderKt.FieldMatchKt {
    override val name: HookBuilderKt.StringMatchKt
        get() = StringMatchKtImpl(match.name)
    override val type: HookBuilderKt.ClassMatchKt
        get() = ClassMatchKtImpl(match.type)

    override fun newMatchKt(match: HookBuilder.FieldMatch): HookBuilderKt.FieldMatchKt =
        FieldMatchKtImpl(match)
}

class StringMatchKtImpl(impl: HookBuilder.StringMatch) :
    BaseMatchKtImpl<HookBuilderKt.StringMatchKt, String, HookBuilder.StringMatch>(impl),
    HookBuilderKt.StringMatchKt

internal class HookBuilderKtImpl(
    ctx: XposedInterface, classLoader: BaseDexClassLoader, sourcePath: String
) : HookBuilderKt {
    private val builder = HookBuilderImpl(ctx, classLoader, sourcePath)

    override var lastMatchResult: HookBuilderKt.MatchResultKt
        get() = TODO("Write Only")
        set(value) {
            builder.setLastMatchResult((value as MatchResultKtImpl).impl)
        }

    override var exceptionHandler: (Throwable) -> Boolean
        get() = TODO("Write Only")
        set(value) {
            builder.setExceptionHandler(value)
        }

    override fun methods(init: HookBuilderKt.MethodMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodMatchKt, Method, HookBuilderKt.MethodMatcherKt> =
        MethodLazySequenceKtImpl(builder.methods {
            MethodMatcherKtImpl(it).init()
        })

    override fun firstMethod(init: HookBuilderKt.MethodMatcherKt.() -> Unit): HookBuilderKt.MethodMatchKt =
        MethodMatchKtImpl(builder.firstMethod {
            MethodMatcherKtImpl(it).init()
        })

    override fun classes(init: HookBuilderKt.ClassMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.ClassMatchKt, Class<*>, HookBuilderKt.ClassMatcherKt> =
        ClassLazySequenceKtImpl(builder.classes {
            ClassMatcherKtImpl(it).init()
        })

    override fun firstClass(init: HookBuilderKt.ClassMatcherKt.() -> Unit): HookBuilderKt.ClassMatchKt =
        ClassMatchKtImpl(builder.firstClass {
            ClassMatcherKtImpl(it).init()
        })

    override fun fields(init: HookBuilderKt.FieldMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldMatchKt, Field, HookBuilderKt.FieldMatcherKt> =
        FieldLazySequenceKtImpl(builder.fields {
            FieldMatcherKtImpl(it).init()
        })

    override fun firstField(init: HookBuilderKt.FieldMatcherKt.() -> Unit): HookBuilderKt.FieldMatchKt =
        FieldMatchKtImpl(builder.firstField {
            FieldMatcherKtImpl(it).init()
        })

    override fun constructors(init: HookBuilderKt.ConstructorMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorMatchKt, Constructor<*>, HookBuilderKt.ConstructorMatcherKt> =
        ConstructorLazySequenceKtImpl(builder.constructors {
            ConstructorMatcherKtImpl(it).init()
        })

    override fun firstConstructor(init: HookBuilderKt.ConstructorMatcherKt.() -> Unit): HookBuilderKt.ConstructorMatchKt =
        ConstructorMatchKtImpl(builder.firstConstructor {
            ConstructorMatcherKtImpl(it).init()
        })

    override fun string(init: HookBuilderKt.StringMatcherKt.() -> Unit): HookBuilderKt.StringMatchKt =
        StringMatchKtImpl(builder.string {
            StringMatcherKtImpl(it).init()
        })

    override val String.exact: HookBuilderKt.StringMatchKt
        get() = StringMatchKtImpl(builder.exact(this))
    override val Class<*>.exact: HookBuilderKt.ClassMatchKt
        get() = ClassMatchKtImpl(builder.exact(this))
    override val Method.exact: HookBuilderKt.MethodMatchKt
        get() = MethodMatchKtImpl(builder.exact(this))
    override val Constructor<*>.exact: HookBuilderKt.ConstructorMatchKt
        get() = ConstructorMatchKtImpl(builder.exact(this))
    override val Field.exact: HookBuilderKt.FieldMatchKt
        get() = FieldMatchKtImpl(builder.exact(this))
    override val String.prefix: HookBuilderKt.StringMatchKt
        get() = StringMatchKtImpl(builder.prefix(this))
    override val String.exactClass: HookBuilderKt.ClassMatchKt
        get() = ClassMatchKtImpl(builder.exactClass(this))

    class MatchResultKtImpl(val impl: HookBuilder.MatchResult) : HookBuilderKt.MatchResultKt {
        override val matchedClasses: Map<String, Class<*>>
            get() = impl.matchedClasses
        override val matchedFields: Map<String, Field>
            get() = impl.matchedFields
        override val matchedMethods: Map<String, Method>
            get() = impl.matchedMethods
        override val matchedConstructors: Map<String, Constructor<*>>
            get() = impl.matchedConstructors
    }

    fun build() = MatchResultKtImpl(builder.build())
}
