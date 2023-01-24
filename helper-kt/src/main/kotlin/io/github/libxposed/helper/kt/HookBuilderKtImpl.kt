@file:Suppress("OverridingDeprecatedMember")

package io.github.libxposed.helper.kt

import dalvik.system.BaseDexClassLoader
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.helper.HookBuilder
import io.github.libxposed.helper.HookBuilder.ContainerSyntax
import io.github.libxposed.helper.HookBuilderImpl
import java.lang.reflect.Member
import java.lang.reflect.Constructor as ReflectConstructor
import java.lang.reflect.Field as ReflectField
import java.lang.reflect.Method as ReflectMethod
import java.lang.Class as ReflectClass
import kotlin.String as KtString

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
    final override var key: kotlin.String
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
    override var name: HookBuilderKt.StringKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setName((value as StringKtImpl).match)
        }
    override var superClass: HookBuilderKt.ClassKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setSuperClass((value as ClassKtImpl).match)
        }
    override var containsMethods: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.MethodKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setContainsMethods((value as ContainerSyntaxKtImpl<HookBuilderKt.MethodKt, HookBuilder.Method>).syntax)
        }
    override var containsConstructors: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ConstructorKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setContainsConstructors((value as ContainerSyntaxKtImpl<HookBuilderKt.ConstructorKt, HookBuilder.Constructor>).syntax)
        }
    override var containsFields: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.FieldKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setContainsFields((value as ContainerSyntaxKtImpl<HookBuilderKt.FieldKt, HookBuilder.Field>).syntax)
        }
    override var interfaces: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ClassKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setInterfaces((value as ContainerSyntaxKtImpl<HookBuilderKt.ClassKt, HookBuilder.Class>).syntax)
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
    override var key: kotlin.String
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
    TypeMatcherKtImpl<HookBuilder.Class, HookBuilderKt.ClassKt, HookBuilder.ClassMatcher>(
        matcher
    ), HookBuilderKt.ClassMatcherKt {
    override var missReplacement: HookBuilderKt.ClassKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as ClassKtImpl).match)
        }
}

class ParameterMatcherKtImpl(matcher: HookBuilder.ParameterMatcher) :
    TypeMatcherKtImpl<HookBuilder.Parameter, HookBuilderKt.ParameterKt, HookBuilder.ParameterMatcher>(
        matcher
    ), HookBuilderKt.ParameterMatcherKt {
    override var index: Int
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIndex(value)
        }
    override var missReplacement: HookBuilderKt.ParameterKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as ParameterKtImpl).match)
        }
}

class StringMatcherKtImpl(matcher: HookBuilder.StringMatcher) :
    BaseMatcherKtImpl<HookBuilder.String, HookBuilderKt.StringKt, HookBuilder.StringMatcher>(matcher),
    HookBuilderKt.StringMatcherKt {
    override var exact: KtString
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setExact(value)
        }
    override var prefix: KtString
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setPrefix(value)
        }
    override var missReplacement: HookBuilderKt.StringKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as StringKtImpl).match)
        }
}

abstract class MemberMatcherKtImpl<Match, MatchKt, Matcher>(
    matcher: Matcher
) : ReflectMatcherKtImpl<Match, MatchKt, Matcher>(matcher),
    HookBuilderKt.MemberMatcherKt<MatchKt> where Matcher : HookBuilder.MemberMatcher<Matcher, Match>, MatchKt : HookBuilderKt.MemberMatchKt<MatchKt, *>, Match : HookBuilder.MemberMatch<Match, *> {
    final override var declaringClass: HookBuilderKt.ClassKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setDeclaringClass((value as ClassKtImpl).match)
        }
    final override var isSynthetic: Boolean
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setIsSynthetic(value)
        }
}

class FieldMatcherKtImpl(matcher: HookBuilder.FieldMatcher) :
    MemberMatcherKtImpl<HookBuilder.Field, HookBuilderKt.FieldKt, HookBuilder.FieldMatcher>(
        matcher
    ), HookBuilderKt.FieldMatcherKt {
    override var name: HookBuilderKt.StringKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setName((value as StringKtImpl).match)
        }
    override var type: HookBuilderKt.ClassKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setType((value as ClassKtImpl).match)
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
    override var missReplacement: HookBuilderKt.FieldKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as FieldKtImpl).match)
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
    final override var parameterTypes: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ParameterKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setParameterTypes((value as ContainerSyntaxKtImpl<HookBuilderKt.ParameterKt, HookBuilder.Parameter>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var referredStrings: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.StringKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setReferredStrings((value as ContainerSyntaxKtImpl<HookBuilderKt.StringKt, HookBuilder.String>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var assignedFields: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.FieldKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setAssignedFields((value as ContainerSyntaxKtImpl<HookBuilderKt.FieldKt, HookBuilder.Field>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var accessedFields: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.FieldKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setAccessedFields((value as ContainerSyntaxKtImpl<HookBuilderKt.FieldKt, HookBuilder.Field>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var invokedMethods: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.MethodKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setInvokedMethods((value as ContainerSyntaxKtImpl<HookBuilderKt.MethodKt, HookBuilder.Method>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var invokedConstructor: HookBuilderKt.ContainerSyntaxKt<HookBuilderKt.ConstructorKt>
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setInvokedConstructors((value as ContainerSyntaxKtImpl<HookBuilderKt.ConstructorKt, HookBuilder.Constructor>).syntax)
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
    ExecutableMatcherKtImpl<HookBuilder.Method, HookBuilderKt.MethodKt, HookBuilder.MethodMatcher>(
        matcher
    ), HookBuilderKt.MethodMatcherKt {
    override var name: HookBuilderKt.StringKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setName((value as StringKtImpl).match)
        }
    override var returnType: HookBuilderKt.ClassKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setReturnType((value as ClassKtImpl).match)
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
    override var missReplacement: HookBuilderKt.MethodKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as MethodKtImpl).match)
        }
}

class ConstructorMatcherKtImpl(matcher: HookBuilder.ConstructorMatcher) :
    ExecutableMatcherKtImpl<HookBuilder.Constructor, HookBuilderKt.ConstructorKt, HookBuilder.ConstructorMatcher>(
        matcher
    ), HookBuilderKt.ConstructorMatcherKt {
    override var missReplacement: HookBuilderKt.ConstructorKt
        @Throws(WOException::class)
        get() = wo
        set(value) {
            matcher.setMissReplacement((value as ConstructorKtImpl).match)
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
    final override val key: KtString?
        get() = match.key

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
) : ReflectMatchKtImpl<MatchKt, ReflectClass<*>, Match>(match),
    HookBuilderKt.TypeMatchKt<MatchKt> where MatchKt : HookBuilderKt.TypeMatchKt<MatchKt>, Match : HookBuilder.TypeMatch<Match> {
    override val name: HookBuilderKt.StringKt
        get() = StringKtImpl(match.name)
    override val superClass: HookBuilderKt.ClassKt
        get() = ClassKtImpl(match.superClass)
    override val interfaces: ClassLazySequenceKtImpl
        get() = ClassLazySequenceKtImpl(match.interfaces)
    override val declaredMethods: HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodKt, java.lang.reflect.Method, HookBuilderKt.MethodMatcherKt>
        get() = MethodLazySequenceKtImpl(match.declaredMethods)
    override val declaredConstructors: HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorKt, java.lang.reflect.Constructor<*>, HookBuilderKt.ConstructorMatcherKt>
        get() = ConstructorLazySequenceKtImpl(match.declaredConstructors)
    override val declaredFields: HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldKt, java.lang.reflect.Field, HookBuilderKt.FieldMatcherKt>
        get() = FieldLazySequenceKtImpl(match.declaredFields)
    override val arrayType: HookBuilderKt.ClassKt
        get() = ClassKtImpl(match.arrayType)
}

class ClassKtImpl(match: HookBuilder.Class) :
    TypeMatchKtImpl<HookBuilderKt.ClassKt, HookBuilder.Class>(match), HookBuilderKt.ClassKt {

    override fun newMatchKt(match: HookBuilder.Class): HookBuilderKt.ClassKt = ClassKtImpl(match)
    override fun get(index: Int): HookBuilderKt.ParameterKt {
        TODO("Not yet implemented")
    }
}

class ParameterKtImpl(match: HookBuilder.Parameter) :
    TypeMatchKtImpl<HookBuilderKt.ParameterKt, HookBuilder.Parameter>(match),
    HookBuilderKt.ParameterKt {

    override fun newMatchKt(match: HookBuilder.Parameter): HookBuilderKt.ParameterKt =
        ParameterKtImpl(match)
}

abstract class MemberMatchKtImpl<Base, Reflect, Impl : HookBuilder.MemberMatch<Impl, Reflect>>(
    impl: Impl
) : ReflectMatchKtImpl<Base, Reflect, Impl>(impl),
    HookBuilderKt.MemberMatchKt<Base, Reflect> where Base : HookBuilderKt.MemberMatchKt<Base, Reflect>, Reflect : Member {
    final override val declaringClass: HookBuilderKt.ClassKt
        get() = ClassKtImpl(match.declaringClass)
}

class ClassLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.Class, ReflectClass<*>, HookBuilder.ClassMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.ClassKt, ReflectClass<*>, HookBuilderKt.ClassMatcherKt, HookBuilder.Class, HookBuilder.ClassMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.Class): HookBuilderKt.ClassKt = ClassKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.ClassMatcher): HookBuilderKt.ClassMatcherKt =
        ClassMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.Class, ReflectClass<*>, HookBuilder.ClassMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.ClassKt, ReflectClass<*>, HookBuilderKt.ClassMatcherKt> =
        ClassLazySequenceKtImpl(impl)
}

class ParameterLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.Parameter, ReflectClass<*>, HookBuilder.ParameterMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.ParameterKt, ReflectClass<*>, HookBuilderKt.ParameterMatcherKt, HookBuilder.Parameter, HookBuilder.ParameterMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.Parameter) = ParameterKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.ParameterMatcher) = ParameterMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.Parameter, ReflectClass<*>, HookBuilder.ParameterMatcher>) =
        ParameterLazySequenceKtImpl(impl)
}

class FieldLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.Field, ReflectField, HookBuilder.FieldMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.FieldKt, ReflectField, HookBuilderKt.FieldMatcherKt, HookBuilder.Field, HookBuilder.FieldMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.Field): HookBuilderKt.FieldKt = FieldKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.FieldMatcher): HookBuilderKt.FieldMatcherKt =
        FieldMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.Field, ReflectField, HookBuilder.FieldMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldKt, java.lang.reflect.Field, HookBuilderKt.FieldMatcherKt> =
        FieldLazySequenceKtImpl(impl)
}

class MethodLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.Method, ReflectMethod, HookBuilder.MethodMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.MethodKt, ReflectMethod, HookBuilderKt.MethodMatcherKt, HookBuilder.Method, HookBuilder.MethodMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.Method): HookBuilderKt.MethodKt = MethodKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.MethodMatcher): HookBuilderKt.MethodMatcherKt =
        MethodMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.Method, ReflectMethod, HookBuilder.MethodMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodKt, java.lang.reflect.Method, HookBuilderKt.MethodMatcherKt> =
        MethodLazySequenceKtImpl(impl)
}

class ConstructorLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.Constructor, ReflectConstructor<*>, HookBuilder.ConstructorMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.ConstructorKt, ReflectConstructor<*>, HookBuilderKt.ConstructorMatcherKt, HookBuilder.Constructor, HookBuilder.ConstructorMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.Constructor): HookBuilderKt.ConstructorKt =
        ConstructorKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.ConstructorMatcher): HookBuilderKt.ConstructorMatcherKt =
        ConstructorMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.Constructor, ReflectConstructor<*>, HookBuilder.ConstructorMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorKt, java.lang.reflect.Constructor<*>, HookBuilderKt.ConstructorMatcherKt> =
        ConstructorLazySequenceKtImpl(impl)
}

class StringLazySequenceKtImpl(impl: HookBuilder.LazySequence<HookBuilder.String, KtString, HookBuilder.StringMatcher>) :
    LazySequenceKtImpl<HookBuilderKt.StringKt, KtString, HookBuilderKt.StringMatcherKt, HookBuilder.String, HookBuilder.StringMatcher>(
        impl
    ) {
    override fun newImpl(impl: HookBuilder.String): HookBuilderKt.StringKt = StringKtImpl(impl)

    override fun newMatcher(impl: HookBuilder.StringMatcher): HookBuilderKt.StringMatcherKt =
        StringMatcherKtImpl(impl)

    override fun newSequence(impl: HookBuilder.LazySequence<HookBuilder.String, KtString, HookBuilder.StringMatcher>): HookBuilderKt.LazySequenceKt<HookBuilderKt.StringKt, KtString, HookBuilderKt.StringMatcherKt> =
        StringLazySequenceKtImpl(impl)
}

abstract class ExecutableMatchKtImpl<MatchKt, Reflect, Match : HookBuilder.ExecutableMatch<Match, Reflect>>(
    impl: Match
) : MemberMatchKtImpl<MatchKt, Reflect, Match>(
    impl
),
    HookBuilderKt.ExecutableMatchKt<MatchKt, Reflect> where MatchKt : HookBuilderKt.ExecutableMatchKt<MatchKt, Reflect>, Reflect : Member {
    final override val parameterTypes: HookBuilderKt.LazySequenceKt<HookBuilderKt.ParameterKt, ReflectClass<*>, HookBuilderKt.ParameterMatcherKt>
        get() = ParameterLazySequenceKtImpl(match.parameterTypes)

    @HookBuilderKt.DexAnalysis
    final override val referredStrings: HookBuilderKt.LazySequenceKt<HookBuilderKt.StringKt, KtString, HookBuilderKt.StringMatcherKt>
        get() = StringLazySequenceKtImpl(match.referredStrings)

    @HookBuilderKt.DexAnalysis
    final override val assignedFields: HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldKt, java.lang.reflect.Field, HookBuilderKt.FieldMatcherKt>
        get() = FieldLazySequenceKtImpl(match.assignedFields)

    @HookBuilderKt.DexAnalysis
    final override val accessedFields: HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldKt, java.lang.reflect.Field, HookBuilderKt.FieldMatcherKt>
        get() = FieldLazySequenceKtImpl(match.accessedFields)

    @HookBuilderKt.DexAnalysis
    final override val invokedMethods: HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodKt, java.lang.reflect.Method, HookBuilderKt.MethodMatcherKt>
        get() = MethodLazySequenceKtImpl(match.invokedMethods)

    @HookBuilderKt.DexAnalysis
    final override val invokedConstructors: HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorKt, java.lang.reflect.Constructor<*>, HookBuilderKt.ConstructorMatcherKt>
        get() = ConstructorLazySequenceKtImpl(match.invokedConstructors)
}

class MethodKtImpl(match: HookBuilder.Method) :
    ExecutableMatchKtImpl<HookBuilderKt.MethodKt, ReflectMethod, HookBuilder.Method>(
        match
    ), HookBuilderKt.MethodKt {
    override val name: HookBuilderKt.StringKt
        get() = StringKtImpl(match.name)
    override val returnType: HookBuilderKt.ClassKt
        get() = ClassKtImpl(match.returnType)

    override fun newMatchKt(match: HookBuilder.Method): HookBuilderKt.MethodKt = MethodKtImpl(match)
}

class ConstructorKtImpl(match: HookBuilder.Constructor) :
    ExecutableMatchKtImpl<HookBuilderKt.ConstructorKt, ReflectConstructor<*>, HookBuilder.Constructor>(
        match
    ), HookBuilderKt.ConstructorKt {
    override fun newMatchKt(match: HookBuilder.Constructor): HookBuilderKt.ConstructorKt =
        ConstructorKtImpl(match)
}

class FieldKtImpl(match: HookBuilder.Field) :
    MemberMatchKtImpl<HookBuilderKt.FieldKt, ReflectField, HookBuilder.Field>(
        match
    ), HookBuilderKt.FieldKt {
    override val name: HookBuilderKt.StringKt
        get() = StringKtImpl(match.name)
    override val type: HookBuilderKt.ClassKt
        get() = ClassKtImpl(match.type)

    override fun newMatchKt(match: HookBuilder.Field): HookBuilderKt.FieldKt = FieldKtImpl(match)
}

class StringKtImpl(impl: HookBuilder.String) :
    BaseMatchKtImpl<HookBuilderKt.StringKt, KtString, HookBuilder.String>(impl),
    HookBuilderKt.StringKt

internal class HookBuilderKtImpl(
    ctx: XposedInterface, classLoader: BaseDexClassLoader, sourcePath: KtString
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

    override fun methods(init: HookBuilderKt.MethodMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.MethodKt, ReflectMethod, HookBuilderKt.MethodMatcherKt> =
        MethodLazySequenceKtImpl(builder.methods {
            MethodMatcherKtImpl(it).init()
        })

    override fun firstMethod(init: HookBuilderKt.MethodMatcherKt.() -> Unit): HookBuilderKt.MethodKt =
        MethodKtImpl(builder.firstMethod {
            MethodMatcherKtImpl(it).init()
        })

    override fun classes(init: HookBuilderKt.ClassMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.ClassKt, ReflectClass<*>, HookBuilderKt.ClassMatcherKt> =
        ClassLazySequenceKtImpl(builder.classes {
            ClassMatcherKtImpl(it).init()
        })

    override fun firstClass(init: HookBuilderKt.ClassMatcherKt.() -> Unit): HookBuilderKt.ClassKt =
        ClassKtImpl(builder.firstClass {
            ClassMatcherKtImpl(it).init()
        })

    override fun fields(init: HookBuilderKt.FieldMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.FieldKt, ReflectField, HookBuilderKt.FieldMatcherKt> =
        FieldLazySequenceKtImpl(builder.fields {
            FieldMatcherKtImpl(it).init()
        })

    override fun firstField(init: HookBuilderKt.FieldMatcherKt.() -> Unit): HookBuilderKt.FieldKt =
        FieldKtImpl(builder.firstField {
            FieldMatcherKtImpl(it).init()
        })

    override fun constructors(init: HookBuilderKt.ConstructorMatcherKt.() -> Unit): HookBuilderKt.LazySequenceKt<HookBuilderKt.ConstructorKt, ReflectConstructor<*>, HookBuilderKt.ConstructorMatcherKt> =
        ConstructorLazySequenceKtImpl(builder.constructors {
            ConstructorMatcherKtImpl(it).init()
        })

    override fun firstConstructor(init: HookBuilderKt.ConstructorMatcherKt.() -> Unit): HookBuilderKt.ConstructorKt =
        ConstructorKtImpl(builder.firstConstructor {
            ConstructorMatcherKtImpl(it).init()
        })

    override fun string(init: HookBuilderKt.StringMatcherKt.() -> Unit): HookBuilderKt.StringKt =
        StringKtImpl(builder.string {
            StringMatcherKtImpl(it).init()
        })

    override val KtString.exact: HookBuilderKt.StringKt
        get() = StringKtImpl(builder.exact(this))
    override val ReflectClass<*>.exact: HookBuilderKt.ClassKt
        get() = ClassKtImpl(builder.exact(this))
    override val ReflectMethod.exact: HookBuilderKt.MethodKt
        get() = MethodKtImpl(builder.exact(this))
    override val ReflectConstructor<*>.exact: HookBuilderKt.ConstructorKt
        get() = ConstructorKtImpl(builder.exact(this))
    override val ReflectField.exact: HookBuilderKt.FieldKt
        get() = FieldKtImpl(builder.exact(this))
    override val KtString.prefix: HookBuilderKt.StringKt
        get() = StringKtImpl(builder.prefix(this))
    override val KtString.exactClass: HookBuilderKt.ClassKt
        get() = ClassKtImpl(builder.exactClass(this))

    class MatchResultKtImpl(val impl: HookBuilder.MatchResult) : HookBuilderKt.MatchResultKt {
        override val matchedClasses: Map<KtString, ReflectClass<*>>
            get() = impl.matchedClasses
        override val matchedFields: Map<KtString, ReflectField>
            get() = impl.matchedFields
        override val matchedMethods: Map<KtString, ReflectMethod>
            get() = impl.matchedMethods
        override val matchedConstructors: Map<KtString, ReflectConstructor<*>>
            get() = impl.matchedConstructors
    }

    fun build() = MatchResultKtImpl(builder.build())
}
