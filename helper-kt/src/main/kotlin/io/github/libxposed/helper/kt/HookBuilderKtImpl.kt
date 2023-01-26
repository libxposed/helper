@file:Suppress("OverridingDeprecatedMember")

package io.github.libxposed.helper.kt

import dalvik.system.BaseDexClassLoader
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.helper.HookBuilder.*
import io.github.libxposed.helper.HookBuilderImpl
import io.github.libxposed.helper.kt.HookBuilderKt.*
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method

class WOException : UnsupportedOperationException("Write-only property")

val wo: Nothing
    @Throws(WOException::class) inline get() = throw WOException()

abstract class BaseMatcherKtImpl<Match, MatchKt, Matcher>
    (
    internal val matcher: Matcher
) : BaseMatcherKt<MatchKt> where Matcher : BaseMatcher<Matcher, Match>, MatchKt : BaseMatchKt<MatchKt, *>, Match : BaseMatch<Match, *> {
    final override var matchFirst: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setMatchFirst(value)
        }
}

abstract class ReflectMatcherKtImpl<Match, MatchKt, Matcher>(
    matcher: Matcher
) : BaseMatcherKtImpl<Match, MatchKt, Matcher>(matcher),
    ReflectMatcherKt<MatchKt> where Matcher : ReflectMatcher<Matcher, Match>, MatchKt : ReflectMatchKt<MatchKt, *>, Match : ReflectMatch<Match, *> {
    final override var key: String
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setKey(value)
        }
    final override var isPublic: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsPublic(value)
        }
    final override var isPrivate: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsPrivate(value)
        }
    final override var isProtected: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsProtected(value)
        }
    final override var isPackage: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsPackage(value)
        }
}

open class ContainerSyntaxKtImpl<MatchKt, Match>(internal val syntax: ContainerSyntax<Match>) :
    ContainerSyntaxKt<MatchKt> where MatchKt : BaseMatchKt<MatchKt, *>, Match : BaseMatch<Match, *> {
    override fun and(element: MatchKt): ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun and(element: ContainerSyntaxKt<MatchKt>): ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun or(element: MatchKt): ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun or(element: ContainerSyntaxKt<MatchKt>): ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun not(): ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }
}

@Suppress("UNCHECKED_CAST")
abstract class TypeMatcherKtImpl<Match, MatchKt, Matcher>(matcher: Matcher) :
    ReflectMatcherKtImpl<Match, MatchKt, Matcher>(
        matcher
    ),
    TypeMatcherKt<MatchKt> where Matcher : TypeMatcher<Matcher, Match>, MatchKt : TypeMatchKt<MatchKt>, Match : TypeMatch<Match> {
    override var name: StringMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setName((value as StringMatchKtImpl).match)
        }
    override var superClass: ClassMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setSuperClass((value as ClassMatchKtImpl).match)
        }
    override var containsInterfaces: ContainerSyntaxKt<ClassMatchKt>
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setContainsInterfaces((value as ContainerSyntaxKtImpl<ClassMatchKt, ClassMatch>).syntax)
        }
    override var isAbstract: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsAbstract(value)
        }
    override var isStatic: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsStatic(value)
        }
    override var isFinal: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsFinal(value)
        }
    override var isInterface: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsInterface(value)
        }
}

class ClassMatcherKtImpl(matcher: ClassMatcher) :
    TypeMatcherKtImpl<ClassMatch, ClassMatchKt, ClassMatcher>(
        matcher
    ), ClassMatcherKt {
    override var missReplacement: ClassMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setMissReplacement((value as ClassMatchKtImpl).match)
        }
}

class ParameterMatcherKtImpl(matcher: ParameterMatcher) :
    TypeMatcherKtImpl<ParameterMatch, ParameterMatchKt, ParameterMatcher>(
        matcher
    ), ParameterMatcherKt {
    override var index: Int
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIndex(value)
        }
    override var missReplacement: ParameterMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setMissReplacement((value as ParameterMatchKtImpl).match)
        }
}

class StringMatcherKtImpl(matcher: StringMatcher) :
    BaseMatcherKtImpl<StringMatch, StringMatchKt, StringMatcher>(
        matcher
    ), StringMatcherKt {
    override var exact: String
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setExact(value)
        }
    override var prefix: String
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setPrefix(value)
        }
    override var missReplacement: StringMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setMissReplacement((value as StringMatchKtImpl).match)
        }
}

abstract class MemberMatcherKtImpl<Match, MatchKt, Matcher>(
    matcher: Matcher
) : ReflectMatcherKtImpl<Match, MatchKt, Matcher>(matcher),
    MemberMatcherKt<MatchKt> where Matcher : MemberMatcher<Matcher, Match>, MatchKt : MemberMatchKt<MatchKt, *>, Match : MemberMatch<Match, *> {
    final override var declaringClass: ClassMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setDeclaringClass((value as ClassMatchKtImpl).match)
        }
    final override var isSynthetic: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsSynthetic(value)
        }
}

class FieldMatcherKtImpl(matcher: FieldMatcher) :
    MemberMatcherKtImpl<FieldMatch, FieldMatchKt, FieldMatcher>(
        matcher
    ), FieldMatcherKt {
    override var name: StringMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setName((value as StringMatchKtImpl).match)
        }
    override var type: ClassMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setType((value as ClassMatchKtImpl).match)
        }
    override var isStatic: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsStatic(value)
        }
    override var isFinal: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsFinal(value)
        }
    override var isTransient: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsTransient(value)
        }
    override var isVolatile: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsVolatile(value)
        }
    override var missReplacement: FieldMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setMissReplacement((value as FieldMatchKtImpl).match)
        }
}

@Suppress("UNCHECKED_CAST")
abstract class ExecutableMatcherKtImpl<Match, MatchKt, Matcher>(
    matcher: Matcher
) : MemberMatcherKtImpl<Match, MatchKt, Matcher>(matcher),
    ExecutableMatcherKt<MatchKt> where Matcher : ExecutableMatcher<Matcher, Match>, MatchKt : ExecutableMatchKt<MatchKt, *>, Match : ExecutableMatch<Match, *> {
    final override var parameterCounts: Int
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setParameterCount(value)
        }
    final override var parameterTypes: ContainerSyntaxKt<ParameterMatchKt>
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setParameterTypes((value as ContainerSyntaxKtImpl<ParameterMatchKt, ParameterMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var referredStrings: ContainerSyntaxKt<StringMatchKt>
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setReferredStrings((value as ContainerSyntaxKtImpl<StringMatchKt, StringMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var assignedFields: ContainerSyntaxKt<FieldMatchKt>
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setAssignedFields((value as ContainerSyntaxKtImpl<FieldMatchKt, FieldMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var accessedFields: ContainerSyntaxKt<FieldMatchKt>
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setAccessedFields((value as ContainerSyntaxKtImpl<FieldMatchKt, FieldMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var invokedMethods: ContainerSyntaxKt<MethodMatchKt>
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setInvokedMethods((value as ContainerSyntaxKtImpl<MethodMatchKt, MethodMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var invokedConstructor: ContainerSyntaxKt<ConstructorMatchKt>
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setInvokedConstructors((value as ContainerSyntaxKtImpl<ConstructorMatchKt, ConstructorMatch>).syntax)
        }

    @HookBuilderKt.DexAnalysis
    final override var containsOpcodes: ByteArray
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setContainsOpcodes(value)
        }
    final override var isVarargs: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsVarargs(value)
        }
}

class MethodMatcherKtImpl(matcher: MethodMatcher) :
    ExecutableMatcherKtImpl<MethodMatch, MethodMatchKt, MethodMatcher>(
        matcher
    ), MethodMatcherKt {
    override var name: StringMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setName((value as StringMatchKtImpl).match)
        }
    override var returnType: ClassMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setReturnType((value as ClassMatchKtImpl).match)
        }
    override var isAbstract: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsAbstract(value)
        }
    override var isStatic: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsStatic(value)
        }
    override var isFinal: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsFinal(value)
        }
    override var isSynchronized: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsSynchronized(value)
        }
    override var isNative: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setIsNative(value)
        }
    override var missReplacement: MethodMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setMissReplacement((value as MethodMatchKtImpl).match)
        }
}

class ConstructorMatcherKtImpl(matcher: ConstructorMatcher) :
    ExecutableMatcherKtImpl<ConstructorMatch, ConstructorMatchKt, ConstructorMatcher>(
        matcher
    ), ConstructorMatcherKt {
    override var missReplacement: ConstructorMatchKt
        @Throws(WOException::class) get() = wo
        set(value) {
            matcher.setMissReplacement((value as ConstructorMatchKtImpl).match)
        }
}

object DummyHookerImpl : DummyHooker

abstract class BaseMatchKtImpl<MatchKt, Reflect, Match>(
    internal val match: Match
) : BaseMatchKt<MatchKt, Reflect> where MatchKt : BaseMatchKt<MatchKt, Reflect>, Match : BaseMatch<Match, Reflect> {
    override fun unaryPlus(): ContainerSyntaxKt<MatchKt> = ContainerSyntaxKtImpl(match.observe())

    override fun unaryMinus(): ContainerSyntaxKt<MatchKt> = ContainerSyntaxKtImpl(match.reverse())
}

abstract class ReflectMatchKtImpl<MatchKt, Reflect, Match>(
    impl: Match
) : BaseMatchKtImpl<MatchKt, Reflect, Match>(impl),
    ReflectMatchKt<MatchKt, Reflect> where MatchKt : ReflectMatchKt<MatchKt, Reflect>, Match : ReflectMatch<Match, Reflect> {
    final override var key: String?
        get() = match.key
        set(value) {
            match.key = value
        }

    override fun <Bind : HookBuilderKt.LazyBind> bind(
        bind: Bind, handler: Bind.(Reflect) -> Unit
    ): MatchKt {
        match.bind(bind.impl) { _, r ->
            bind.handler(r)
        }
        @Suppress("UNCHECKED_CAST") return this as MatchKt
    }

    final override fun onMatch(handler: DummyHooker.(Reflect) -> Unit): MatchKt =
        newMatchKt(match.onMatch {
            DummyHookerImpl.handler(it)
        })

    abstract fun newMatchKt(match: Match): MatchKt
}

abstract class LazySequenceKtImpl<Base, MatchKt, Reflect, MatcherKt, Match, Matcher, Impl>(
    private val impl: Impl
) : LazySequenceKt<Base, MatchKt, Reflect, MatcherKt> where Base : LazySequenceKt<Base, MatchKt, Reflect, MatcherKt>, MatchKt : BaseMatchKt<MatchKt, Reflect>, Reflect : Any, MatcherKt : BaseMatcherKt<MatchKt>, Match : BaseMatch<Match, Reflect>, Matcher : BaseMatcher<Matcher, Match>, Impl : LazySequence<Impl, Match, Reflect, Matcher> {
    override fun first(): MatchKt = newImpl(impl.first())
    override fun unaryPlus(): ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    override fun unaryMinus(): ContainerSyntaxKt<MatchKt> {
        TODO("Not yet implemented")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Bind : HookBuilderKt.LazyBind> bind(
        bind: Bind, handler: Bind.(Sequence<Reflect>) -> Unit
    ): Base {
        impl.bind(bind.impl) { _, r ->
            bind.handler(r.asSequence())
        }
        return this as Base
    }

    override fun onMatch(handler: DummyHooker.(Sequence<Reflect>) -> Reflect): MatchKt =
        newImpl(impl.onMatch(MatchConsumer<Iterable<Reflect>, Reflect> {
            handler(
                DummyHookerImpl, it.asSequence()
            )
        }))

    override fun onMatch(handler: DummyHooker.(Sequence<Reflect>) -> Unit): Base =
        newSequence(impl.onMatch(Consumer<Iterable<Reflect>> { t ->
            DummyHookerImpl.handler(
                t.asSequence()
            )
        }))

    override fun all(init: MatcherKt.() -> Unit): Base = newSequence(impl.all {
        newMatcher(it).init()
    })

    override fun first(init: MatcherKt.() -> Unit): MatchKt = newImpl(impl.first {
        newMatcher(it).init()
    })

    abstract fun newImpl(impl: Match): MatchKt

    abstract fun newMatcher(impl: Matcher): MatcherKt

    abstract fun newSequence(impl: Impl): Base
}

abstract class TypeMatchKtImpl<MatchKt, Match>(
    match: Match
) : ReflectMatchKtImpl<MatchKt, Class<*>, Match>(match),
    TypeMatchKt<MatchKt> where MatchKt : TypeMatchKt<MatchKt>, Match : TypeMatch<Match> {
    override val name: StringMatchKt
        get() = StringMatchKtImpl(match.name)
    override val superClass: ClassMatchKt
        get() = ClassMatchKtImpl(match.superClass)
    override val interfaces: ClassLazySequenceKt
        get() = ClassLazySequenceKtImpl(match.interfaces)
    override val declaredMethods: MethodLazySequenceKt
        get() = MethodLazySequenceKtImpl(match.declaredMethods)
    override val declaredConstructors: ConstructorLazySequenceKt
        get() = ConstructorLazySequenceKtImpl(match.declaredConstructors)
    override val declaredFields: FieldLazySequenceKt
        get() = FieldLazySequenceKtImpl(match.declaredFields)
    override val arrayType: ClassMatchKt
        get() = ClassMatchKtImpl(match.arrayType)
}

abstract class TypeLazySequenceKtImpl<Base, MatchKt, MatcherKt, Match, Matcher, Impl>(
    impl: Impl
) : LazySequenceKtImpl<Base, MatchKt, Class<*>, MatcherKt, Match, Matcher, Impl>(impl),
    TypeLazySequenceKt<Base, MatchKt, MatcherKt> where Base : TypeLazySequenceKt<Base, MatchKt, MatcherKt>, MatchKt : TypeMatchKt<MatchKt>, MatcherKt : TypeMatcherKt<MatchKt>, Match : TypeMatch<Match>, Matcher : TypeMatcher<Matcher, Match>, Impl : TypeLazySequence<Impl, Match, Matcher> {
    override fun methods(init: MethodMatcherKt.() -> Unit): MethodLazySequenceKt {
        TODO("Not yet implemented")
    }

    override fun firstMethod(init: MethodMatcherKt.() -> Unit): MethodMatchKt {
        TODO("Not yet implemented")
    }

    override fun fields(init: FieldMatcherKt.() -> Unit): FieldLazySequenceKt {
        TODO("Not yet implemented")
    }

    override fun firstField(init: FieldMatcherKt.() -> Unit): FieldMatchKt {
        TODO("Not yet implemented")
    }

    override fun constructors(init: ConstructorMatcherKt.() -> Unit): ConstructorLazySequenceKt {
        TODO("Not yet implemented")
    }

    override fun firstConstructor(init: ConstructorMatcherKt.() -> Unit): ConstructorMatchKt {
        TODO("Not yet implemented")
    }
}

abstract class MemberLazySequenceKtImpl<Base, MatchKt, Reflect, MatcherKt, Match, Matcher, Impl>(
    impl: Impl
) : LazySequenceKtImpl<Base, MatchKt, Reflect, MatcherKt, Match, Matcher, Impl>(impl),
    MemberLazySequenceKt<Base, MatchKt, Reflect, MatcherKt> where Base : MemberLazySequenceKt<Base, MatchKt, Reflect, MatcherKt>, MatchKt : MemberMatchKt<MatchKt, Reflect>, Reflect : Member, MatcherKt : MemberMatcherKt<MatchKt>, Match : MemberMatch<Match, Reflect>, Matcher : MemberMatcher<Matcher, Match>, Impl : MemberLazySequence<Impl, Match, Reflect, Matcher> {
    override fun declaringClasses(init: ClassMatcherKt.() -> Unit): ClassLazySequenceKt {
        TODO("Not yet implemented")
    }

    override fun firstDeclaringClass(init: ClassMatcherKt.() -> Unit): ClassMatchKt {
        TODO("Not yet implemented")
    }

}

abstract class ExecutableLazySequenceKtImpl<Base, MatchKt, Reflect, MatcherKt, Match, Matcher, Impl>(
    impl: Impl
) : MemberLazySequenceKtImpl<Base, MatchKt, Reflect, MatcherKt, Match, Matcher, Impl>(impl),
    ExecutableLazySequenceKt<Base, MatchKt, Reflect, MatcherKt> where Base : ExecutableLazySequenceKt<Base, MatchKt, Reflect, MatcherKt>, MatchKt : ExecutableMatchKt<MatchKt, Reflect>, Reflect : Member, MatcherKt : ExecutableMatcherKt<MatchKt>, Match : ExecutableMatch<Match, Reflect>, Matcher : ExecutableMatcher<Matcher, Match>, Impl : ExecutableLazySequence<Impl, Match, Reflect, Matcher> {
    override fun parameters(init: ParameterMatcherKt.() -> Unit): ParameterLazySequenceKt {
        TODO("Not yet implemented")
    }

    override fun firstParameter(init: ParameterMatcherKt.() -> Unit): ParameterMatchKt {
        TODO("Not yet implemented")
    }
}

class ClassMatchKtImpl(match: ClassMatch) : TypeMatchKtImpl<ClassMatchKt, ClassMatch>(match),
    ClassMatchKt {

    override fun newMatchKt(match: ClassMatch) = ClassMatchKtImpl(match)

    override fun get(index: Int): ParameterMatchKt {
        TODO("Not yet implemented")
    }
}

class ParameterMatchKtImpl(match: ParameterMatch) :
    TypeMatchKtImpl<ParameterMatchKt, ParameterMatch>(match), ParameterMatchKt {

    override fun newMatchKt(match: ParameterMatch): ParameterMatchKt = ParameterMatchKtImpl(match)
}

abstract class MemberMatchKtImpl<Base, Reflect, Impl : MemberMatch<Impl, Reflect>>(
    impl: Impl
) : ReflectMatchKtImpl<Base, Reflect, Impl>(impl),
    MemberMatchKt<Base, Reflect> where Base : MemberMatchKt<Base, Reflect>, Reflect : Member {
    final override val declaringClass: ClassMatchKt
        get() = ClassMatchKtImpl(match.declaringClass)
}


class ClassLazySequenceKtImpl(impl: ClassLazySequence) :
    TypeLazySequenceKtImpl<ClassLazySequenceKt, ClassMatchKt, ClassMatcherKt, ClassMatch, ClassMatcher, ClassLazySequence>(
        impl
    ), ClassLazySequenceKt {
    override fun newImpl(impl: ClassMatch) = ClassMatchKtImpl(impl)

    override fun newMatcher(impl: ClassMatcher) = ClassMatcherKtImpl(impl)

    override fun newSequence(impl: ClassLazySequence) = ClassLazySequenceKtImpl(impl)
}

class ParameterLazySequenceKtImpl(impl: ParameterLazySequence) :
    TypeLazySequenceKtImpl<ParameterLazySequenceKt, ParameterMatchKt, ParameterMatcherKt, ParameterMatch, ParameterMatcher, ParameterLazySequence>(
        impl
    ), ParameterLazySequenceKt {
    override fun newImpl(impl: ParameterMatch) = ParameterMatchKtImpl(impl)

    override fun newMatcher(impl: ParameterMatcher) = ParameterMatcherKtImpl(impl)

    override fun newSequence(impl: ParameterLazySequence) = ParameterLazySequenceKtImpl(impl)
}

class FieldLazySequenceKtImpl(impl: FieldLazySequence) :
    MemberLazySequenceKtImpl<FieldLazySequenceKt, FieldMatchKt, Field, FieldMatcherKt, FieldMatch, FieldMatcher, FieldLazySequence>(
        impl
    ), FieldLazySequenceKt {
    override fun newImpl(impl: FieldMatch) = FieldMatchKtImpl(impl)

    override fun newMatcher(impl: FieldMatcher) = FieldMatcherKtImpl(impl)

    override fun newSequence(impl: FieldLazySequence) = FieldLazySequenceKtImpl(impl)
    override fun types(init: ClassMatcherKt.() -> Unit): ClassLazySequenceKt {
        TODO("Not yet implemented")
    }

    override fun firstType(init: ClassMatcherKt.() -> Unit): ClassMatchKt {
        TODO("Not yet implemented")
    }
}

class MethodLazySequenceKtImpl(impl: MethodLazySequence) :
    ExecutableLazySequenceKtImpl<MethodLazySequenceKt, MethodMatchKt, Method, MethodMatcherKt, MethodMatch, MethodMatcher, MethodLazySequence>(
        impl
    ), MethodLazySequenceKt {
    override fun newImpl(impl: MethodMatch) = MethodMatchKtImpl(impl)

    override fun newMatcher(impl: MethodMatcher) = MethodMatcherKtImpl(impl)

    override fun newSequence(impl: MethodLazySequence) = MethodLazySequenceKtImpl(impl)
    override fun returnTypes(init: ClassMatcherKt.() -> Unit): ClassLazySequenceKt {
        TODO("Not yet implemented")
    }

    override fun firstReturnType(init: ClassMatcherKt.() -> Unit): ClassMatchKt {
        TODO("Not yet implemented")
    }
}

class ConstructorLazySequenceKtImpl(impl: ConstructorLazySequence) :
    ExecutableLazySequenceKtImpl<ConstructorLazySequenceKt, ConstructorMatchKt, Constructor<*>, ConstructorMatcherKt, ConstructorMatch, ConstructorMatcher, ConstructorLazySequence>(
        impl
    ), ConstructorLazySequenceKt {
    override fun newImpl(impl: ConstructorMatch) = ConstructorMatchKtImpl(impl)

    override fun newMatcher(impl: ConstructorMatcher) = ConstructorMatcherKtImpl(impl)

    override fun newSequence(impl: ConstructorLazySequence) = ConstructorLazySequenceKtImpl(impl)
}

abstract class ExecutableMatchKtImpl<MatchKt, Reflect, Match : ExecutableMatch<Match, Reflect>>(
    impl: Match
) : MemberMatchKtImpl<MatchKt, Reflect, Match>(
    impl
),
    ExecutableMatchKt<MatchKt, Reflect> where MatchKt : ExecutableMatchKt<MatchKt, Reflect>, Reflect : Member {
    final override val parameterTypes: ParameterLazySequenceKt
        get() = ParameterLazySequenceKtImpl(match.parameterTypes)

    @HookBuilderKt.DexAnalysis
    final override val assignedFields: FieldLazySequenceKt
        get() = FieldLazySequenceKtImpl(match.assignedFields)

    @HookBuilderKt.DexAnalysis
    final override val accessedFields: FieldLazySequenceKt
        get() = FieldLazySequenceKtImpl(match.accessedFields)

    @HookBuilderKt.DexAnalysis
    final override val invokedMethods: MethodLazySequenceKt
        get() = MethodLazySequenceKtImpl(match.invokedMethods)

    @HookBuilderKt.DexAnalysis
    final override val invokedConstructors: ConstructorLazySequenceKt
        get() = ConstructorLazySequenceKtImpl(match.invokedConstructors)
}

class MethodMatchKtImpl(match: MethodMatch) :
    ExecutableMatchKtImpl<MethodMatchKt, Method, MethodMatch>(
        match
    ), MethodMatchKt {
    override val name: StringMatchKt
        get() = StringMatchKtImpl(match.name)
    override val returnType: ClassMatchKt
        get() = ClassMatchKtImpl(match.returnType)

    override fun newMatchKt(match: MethodMatch): MethodMatchKt = MethodMatchKtImpl(match)
}

class ConstructorMatchKtImpl(match: ConstructorMatch) :
    ExecutableMatchKtImpl<ConstructorMatchKt, Constructor<*>, ConstructorMatch>(
        match
    ), ConstructorMatchKt {
    override fun newMatchKt(match: ConstructorMatch): ConstructorMatchKt =
        ConstructorMatchKtImpl(match)
}

class FieldMatchKtImpl(match: FieldMatch) : MemberMatchKtImpl<FieldMatchKt, Field, FieldMatch>(
    match
), FieldMatchKt {
    override val name: StringMatchKt
        get() = StringMatchKtImpl(match.name)
    override val type: ClassMatchKt
        get() = ClassMatchKtImpl(match.type)

    override fun newMatchKt(match: FieldMatch): FieldMatchKt = FieldMatchKtImpl(match)
}

class StringMatchKtImpl(impl: StringMatch) :
    BaseMatchKtImpl<StringMatchKt, String, StringMatch>(impl), StringMatchKt

internal class HookBuilderKtImpl(
    ctx: XposedInterface, classLoader: BaseDexClassLoader, sourcePath: String
) : HookBuilderKt {
    private val builder = HookBuilderImpl(ctx, classLoader, sourcePath)

    override var lastMatchResult: MatchResultKt
        @Throws(WOException::class) get() = wo
        set(value) {
            builder.setLastMatchResult((value as MatchResultKtImpl).impl)
        }

    override var exceptionHandler: (Throwable) -> Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            builder.setExceptionHandler(value)
        }

    @HookBuilderKt.DexAnalysis
    override var forceDexAnalysis: Boolean
        @Throws(WOException::class) get() = wo
        set(value) {
            builder.setForceDexAnalysis(value)
        }

    override fun methods(init: MethodMatcherKt.() -> Unit) =
        MethodLazySequenceKtImpl(builder.methods {
            MethodMatcherKtImpl(it).init()
        })

    override fun firstMethod(init: MethodMatcherKt.() -> Unit) =
        MethodMatchKtImpl(builder.firstMethod {
            MethodMatcherKtImpl(it).init()
        })

    override fun classes(init: ClassMatcherKt.() -> Unit) =
        ClassLazySequenceKtImpl(builder.classes {
            ClassMatcherKtImpl(it).init()
        })

    override fun firstClass(init: ClassMatcherKt.() -> Unit) = ClassMatchKtImpl(builder.firstClass {
        ClassMatcherKtImpl(it).init()
    })

    override fun fields(init: FieldMatcherKt.() -> Unit) = FieldLazySequenceKtImpl(builder.fields {
        FieldMatcherKtImpl(it).init()
    })

    override fun firstField(init: FieldMatcherKt.() -> Unit) = FieldMatchKtImpl(builder.firstField {
        FieldMatcherKtImpl(it).init()
    })

    override fun constructors(init: ConstructorMatcherKt.() -> Unit) =
        ConstructorLazySequenceKtImpl(builder.constructors {
            ConstructorMatcherKtImpl(it).init()
        })

    override fun firstConstructor(init: ConstructorMatcherKt.() -> Unit) =
        ConstructorMatchKtImpl(builder.firstConstructor {
            ConstructorMatcherKtImpl(it).init()
        })

    override fun string(init: StringMatcherKt.() -> Unit) = StringMatchKtImpl(builder.string {
        StringMatcherKtImpl(it).init()
    })

    override val String.exact: StringMatchKt
        get() = StringMatchKtImpl(builder.exact(this))
    override val Class<*>.exact: ClassMatchKt
        get() = ClassMatchKtImpl(builder.exact(this))
    override val Method.exact: MethodMatchKt
        get() = MethodMatchKtImpl(builder.exact(this))
    override val Constructor<*>.exact: ConstructorMatchKt
        get() = ConstructorMatchKtImpl(builder.exact(this))
    override val Field.exact: FieldMatchKt
        get() = FieldMatchKtImpl(builder.exact(this))
    override val String.prefix: StringMatchKt
        get() = StringMatchKtImpl(builder.prefix(this))
    override val String.exactClass: ClassMatchKt
        get() = ClassMatchKtImpl(builder.exactClass(this))

    class MatchResultKtImpl(val impl: MatchResult) : MatchResultKt {
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
