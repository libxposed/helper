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

@Hooker
class HookBuilderKt (
    ctx: XposedInterface, classLoader: BaseDexClassLoader, sourcePath: String
) {
    @RequiresOptIn(message = "Dex analysis is time-consuming, please use it carefully.")
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
    annotation class DexAnalysis

    @RequiresOptIn(message = "Annotation analysis is time-consuming, please use it carefully.")
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
    annotation class AnnotationAnalysis
    class WOException : UnsupportedOperationException("Write-only property")

    @Matcher
    sealed class BaseMatcherKt<Match, Matcher>(val matcher: Matcher) where Matcher : BaseMatcher<Matcher, Match>, Match : BaseMatch<Match, *> {
        var matchFirst: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            set(value) {
                matcher.setMatchFirst(value)
            }
    }

    sealed class ReflectMatcherKt<Match, Matcher>(
        matcher: Matcher
    ) : BaseMatcherKt<Match, Matcher>(matcher) where Matcher : ReflectMatcher<Matcher, Match>, Match : ReflectMatch<Match, *> {
        var key: String
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setKey(value)
            }
        var isPublic: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsPublic(value)
            }
        var isPrivate: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsPrivate(value)
            }
        var isProtected: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            set(value) {
                matcher.setIsProtected(value)
            }
        var isPackage: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            set(value) {
                matcher.setIsPackage(value)
            }
    }

    class ContainerSyntaxKt<MatchKt, Match> (val syntax: ContainerSyntax<Match>) where MatchKt : BaseMatchKt<MatchKt, Match, *>, Match : BaseMatch<Match, *> {
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

    sealed class TypeMatcherKt<Match, Matcher>(matcher: Matcher) : ReflectMatcherKt<Match, Matcher>(
        matcher
    ) where Matcher : TypeMatcher<Matcher, Match>, Match : TypeMatch<Match> {
        var name: StringMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setName(value.match)
            }
        var superClass: ClassMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setSuperClass(value.match)
            }
        var containsInterfaces: ContainerSyntaxKt<ClassMatchKt, ClassMatch>
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setContainsInterfaces(value.syntax)
            }
        var isAbstract: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsAbstract(value)
            }
        var isStatic: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsStatic(value)
            }
        var isFinal: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsFinal(value)
            }
        var isInterface: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsInterface(value)
            }
    }

    class ClassMatcherKt (matcher: ClassMatcher) :
        TypeMatcherKt<ClassMatch, ClassMatcher>(
            matcher
        ) {
        var missReplacement: ClassMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setMissReplacement(value.match)
            }
    }

    class ParameterMatcherKt (matcher: ParameterMatcher) :
        TypeMatcherKt<ParameterMatch, ParameterMatcher>(
            matcher
        ) {
        var index: Int
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIndex(value)
            }
        var missReplacement: ParameterMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setMissReplacement(value.match)
            }
    }

    class StringMatcherKt (matcher: StringMatcher) :
        BaseMatcherKt<StringMatch, StringMatcher>(
            matcher
        ) {
        var exact: String
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setExact(value)
            }
        var prefix: String
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setPrefix(value)
            }
        var missReplacement: StringMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setMissReplacement(value.match)
            }
    }

    sealed class MemberMatcherKt<Match, Matcher>(
        matcher: Matcher
    ) : ReflectMatcherKt<Match, Matcher>(matcher) where Matcher : MemberMatcher<Matcher, Match>, Match : MemberMatch<Match, *> {
        var declaringClass: ClassMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setDeclaringClass(value.match)
            }
        var isSynthetic: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsSynthetic(value)
            }
    }

    class FieldMatcherKt (matcher: FieldMatcher) :
        MemberMatcherKt<FieldMatch, FieldMatcher>(
            matcher
        ) {
        var name: StringMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setName(value.match)
            }
        var type: ClassMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setType(value.match)
            }
        var isStatic: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsStatic(value)
            }
        var isFinal: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsFinal(value)
            }
        var isTransient: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsTransient(value)
            }
        var isVolatile: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsVolatile(value)
            }
        var missReplacement: FieldMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setMissReplacement(value.match)
            }
    }

    sealed class ExecutableMatcherKt<Match, Matcher>(
        matcher: Matcher
    ) : MemberMatcherKt<Match, Matcher>(matcher) where Matcher : ExecutableMatcher<Matcher, Match>, Match : ExecutableMatch<Match, *> {
        var parameterCounts: Int
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setParameterCount(value)
            }
        var parameterTypes: ContainerSyntaxKt<ParameterMatchKt, ParameterMatch>
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setParameterTypes(value.syntax)
            }

        @DexAnalysis
        var referredStrings: ContainerSyntaxKt<StringMatchKt, StringMatch>
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setReferredStrings(value.syntax)
            }

        @DexAnalysis
        var assignedFields: ContainerSyntaxKt<FieldMatchKt, FieldMatch>
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setAssignedFields(value.syntax)
            }

        @DexAnalysis
        var accessedFields: ContainerSyntaxKt<FieldMatchKt, FieldMatch>
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setAccessedFields(value.syntax)
            }

        @DexAnalysis
        var invokedMethods: ContainerSyntaxKt<MethodMatchKt, MethodMatch>
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setInvokedMethods(value.syntax)
            }

        @DexAnalysis
        var invokedConstructor: ContainerSyntaxKt<ConstructorMatchKt, ConstructorMatch>
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setInvokedConstructors(value.syntax)
            }

        @DexAnalysis
        var containsOpcodes: ByteArray
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setContainsOpcodes(value)
            }

        var isVarargs: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsVarargs(value)
            }
    }

    class MethodMatcherKt (matcher: MethodMatcher) :
        ExecutableMatcherKt<MethodMatch, MethodMatcher>(
            matcher
        ) {
        var name: StringMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setName(value.match)
            }
        var returnType: ClassMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setReturnType(value.match)
            }
        var isAbstract: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsAbstract(value)
            }
        var isStatic: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsStatic(value)
            }
        var isFinal: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsFinal(value)
            }
        var isSynchronized: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsSynchronized(value)
            }
        var isNative: Boolean
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setIsNative(value)
            }
        var missReplacement: MethodMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setMissReplacement(value.match)
            }
    }

    class ConstructorMatcherKt (matcher: ConstructorMatcher) :
        ExecutableMatcherKt<ConstructorMatch, ConstructorMatcher>(
            matcher
        ) {
        var missReplacement: ConstructorMatchKt
            @Deprecated(
                "Write only", level = DeprecationLevel.HIDDEN
            ) inline get() = throw WOException()
            inline set(value) {
                matcher.setMissReplacement(value.match)
            }
    }

    @Hooker
    object DummyHooker

    @Hooker
    sealed class BaseMatchKt<Self, Match, Reflect>(
        val match: Match
    ) where Self : BaseMatchKt<Self, Match, Reflect>, Match : BaseMatch<Match, Reflect> {
        operator fun unaryPlus() = ContainerSyntaxKt<Self, Match>(match.observe())

        operator fun unaryMinus() = ContainerSyntaxKt<Self, Match>(match.reverse())
    }

    @Hooker
    abstract class LazyBind {
        val bind = LazyBind {
            onMatch()
        }

        abstract fun onMatch()
    }

    sealed class ReflectMatchKt<Self, Match, Reflect>(
        match: Match
    ) : BaseMatchKt<Self, Match, Reflect>(match) where Self : ReflectMatchKt<Self, Match, Reflect>, Match : ReflectMatch<Match, Reflect> {
        var key: String?
            inline get() = match.key
            inline set(value) {
                match.key = value
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

        inline fun onMatch(crossinline handler: DummyHooker.(Reflect) -> Unit): Self =
            newSelf(match.onMatch {
                DummyHooker.handler(it)
            })

        abstract fun newSelf(match: Match): Self
    }

    @Hooker
    @OptIn(ExperimentalTypeInference::class)
    sealed class LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(
        val seq: Seq
    ) where Self : LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>, MatchKt : BaseMatchKt<MatchKt, Match, Reflect>, Reflect : Any, MatcherKt : BaseMatcherKt<Match, Matcher>, Match : BaseMatch<Match, Reflect>, Matcher : BaseMatcher<Matcher, Match>, Seq : LazySequence<Seq, Match, Reflect, Matcher> {
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

        abstract fun newMatch(impl: Match): MatchKt

        abstract fun newMatcher(impl: Matcher): MatcherKt

        abstract fun newSelf(impl: Seq): Self
    }

    sealed class TypeMatchKt<Self, Match>(
        match: Match
    ) : ReflectMatchKt<Self, Match, Class<*>>(match) where Self : TypeMatchKt<Self, Match>, Match : TypeMatch<Match> {
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

    sealed class TypeLazySequenceKt<Base, MatchKt, MatcherKt, Match, Matcher, Seq>(
        seq: Seq
    ) : LazySequenceKt<Base, MatchKt, Class<*>, MatcherKt, Match, Matcher, Seq>(seq) where Base : TypeLazySequenceKt<Base, MatchKt, MatcherKt, Match, Matcher, Seq>, MatchKt : TypeMatchKt<MatchKt, Match>, MatcherKt : TypeMatcherKt<Match, Matcher>, Match : TypeMatch<Match>, Matcher : TypeMatcher<Matcher, Match>, Seq : TypeLazySequence<Seq, Match, Matcher> {
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

    sealed class MemberLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(
        seq: Seq
    ) : LazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(seq) where Self : MemberLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>, MatchKt : MemberMatchKt<MatchKt, Match, Reflect>, Reflect : Member, MatcherKt : MemberMatcherKt<Match, Matcher>, Match : MemberMatch<Match, Reflect>, Matcher : MemberMatcher<Matcher, Match>, Seq : MemberLazySequence<Seq, Match, Reflect, Matcher> {
        inline fun declaringClasses(crossinline init: ClassMatcherKt.() -> Unit) =
            ClassLazySequenceKt(seq.declaringClasses {
                ClassMatcherKt(it).init()
            })

        inline fun firstDeclaringClass(crossinline init: ClassMatcherKt.() -> Unit) =
            ClassMatchKt(seq.firstDeclaringClass {
                ClassMatcherKt(it).init()
            })

    }

    sealed class ExecutableLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(
        seq: Seq
    ) : MemberLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>(seq) where Self : ExecutableLazySequenceKt<Self, MatchKt, Reflect, MatcherKt, Match, Matcher, Seq>, MatchKt : ExecutableMatchKt<MatchKt, Match, Reflect>, Reflect : Member, MatcherKt : ExecutableMatcherKt<Match, Matcher>, Match : ExecutableMatch<Match, Reflect>, Matcher : ExecutableMatcher<Matcher, Match>, Seq : ExecutableLazySequence<Seq, Match, Reflect, Matcher> {
        inline fun parameters(crossinline init: ParameterMatcherKt.() -> Unit) =
            ParameterLazySequenceKt(seq.parameters {
                ParameterMatcherKt(it).init()
            })

        inline fun firstParameter(crossinline init: ParameterMatcherKt.() -> Unit) =
            ParameterMatchKt(seq.firstParameter {
                ParameterMatcherKt(it).init()
            })
    }

    class ClassMatchKt (match: ClassMatch) :
        TypeMatchKt<ClassMatchKt, ClassMatch>(match) {
        override fun newSelf(match: ClassMatch) = ClassMatchKt(match)

        operator fun get(index: Int): ParameterMatchKt = ParameterMatchKt(match.asParameter(index))
    }

    class ParameterMatchKt (match: ParameterMatch) :
        TypeMatchKt<ParameterMatchKt, ParameterMatch>(match) {

        override fun newSelf(match: ParameterMatch) = ParameterMatchKt(match)
    }

    sealed class MemberMatchKt<Self, Match, Reflect>(
        match: Match
    ) : ReflectMatchKt<Self, Match, Reflect>(match) where Self : MemberMatchKt<Self, Match, Reflect>, Match : MemberMatch<Match, Reflect>, Reflect : Member {
        val declaringClass: ClassMatchKt
            inline get() = ClassMatchKt(match.declaringClass)
    }


    class ClassLazySequenceKt (impl: ClassLazySequence) :
        TypeLazySequenceKt<ClassLazySequenceKt, ClassMatchKt, ClassMatcherKt, ClassMatch, ClassMatcher, ClassLazySequence>(
            impl
        ) {
        override fun newMatch(impl: ClassMatch) = ClassMatchKt(impl)

        override fun newMatcher(impl: ClassMatcher) = ClassMatcherKt(impl)

        override fun newSelf(impl: ClassLazySequence) = ClassLazySequenceKt(impl)
    }

    class ParameterLazySequenceKt (impl: ParameterLazySequence) :
        TypeLazySequenceKt<ParameterLazySequenceKt, ParameterMatchKt, ParameterMatcherKt, ParameterMatch, ParameterMatcher, ParameterLazySequence>(
            impl
        ) {
        override fun newMatch(impl: ParameterMatch) = ParameterMatchKt(impl)

        override fun newMatcher(impl: ParameterMatcher) = ParameterMatcherKt(impl)

        override fun newSelf(impl: ParameterLazySequence) = ParameterLazySequenceKt(impl)
    }

    class FieldLazySequenceKt (impl: FieldLazySequence) :
        MemberLazySequenceKt<FieldLazySequenceKt, FieldMatchKt, Field, FieldMatcherKt, FieldMatch, FieldMatcher, FieldLazySequence>(
            impl
        ) {
        override fun newMatch(impl: FieldMatch) = FieldMatchKt(impl)

        override fun newMatcher(impl: FieldMatcher) = FieldMatcherKt(impl)

        override fun newSelf(impl: FieldLazySequence) = FieldLazySequenceKt(impl)
        inline fun types(crossinline init: ClassMatcherKt.() -> Unit) =
            ClassLazySequenceKt(seq.types {
                ClassMatcherKt(it).init()
            })

        inline fun firstType(crossinline init: ClassMatcherKt.() -> Unit) =
            ClassMatchKt(seq.firstType {
                ClassMatcherKt(it).init()
            })
    }

    class MethodLazySequenceKt (impl: MethodLazySequence) :
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

    class ConstructorLazySequenceKt (seq: ConstructorLazySequence) :
        ExecutableLazySequenceKt<ConstructorLazySequenceKt, ConstructorMatchKt, Constructor<*>, ConstructorMatcherKt, ConstructorMatch, ConstructorMatcher, ConstructorLazySequence>(
            seq
        ) {
        override fun newMatch(impl: ConstructorMatch) = ConstructorMatchKt(impl)

        override fun newMatcher(impl: ConstructorMatcher) = ConstructorMatcherKt(impl)

        override fun newSelf(impl: ConstructorLazySequence) = ConstructorLazySequenceKt(impl)
    }

    sealed class ExecutableMatchKt<Self, Match, Reflect>(
        match: Match
    ) : MemberMatchKt<Self, Match, Reflect>(
        match
    ) where Self : ExecutableMatchKt<Self, Match, Reflect>, Match : ExecutableMatch<Match, Reflect>, Reflect : Member {
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

    class MethodMatchKt (match: MethodMatch) :
        ExecutableMatchKt<MethodMatchKt, MethodMatch, Method>(
            match
        ) {
        val name: StringMatchKt
            inline get() = StringMatchKt(match.name)
        val returnType: ClassMatchKt
            inline get() = ClassMatchKt(match.returnType)

        override fun newSelf(match: MethodMatch): MethodMatchKt = MethodMatchKt(match)
    }

    class ConstructorMatchKt (match: ConstructorMatch) :
        ExecutableMatchKt<ConstructorMatchKt, ConstructorMatch, Constructor<*>>(
            match
        ) {
        override fun newSelf(match: ConstructorMatch): ConstructorMatchKt =
            ConstructorMatchKt(match)
    }

    class FieldMatchKt (match: FieldMatch) :
        MemberMatchKt<FieldMatchKt, FieldMatch, Field>(
            match
        ) {
        val name: StringMatchKt
            inline get() = StringMatchKt(match.name)
        val type: ClassMatchKt
            inline get() = ClassMatchKt(match.type)

        override fun newSelf(match: FieldMatch): FieldMatchKt = FieldMatchKt(match)
    }

    class StringMatchKt(match: StringMatch) :
        BaseMatchKt<StringMatchKt, StringMatch, String>(match)

    val builder = HookBuilderImpl(ctx, classLoader, sourcePath)

    var lastMatchResult: MatchResultKt
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = throw WOException()
        inline set(value) {
            builder.setLastMatchResult((value.result))
        }

    var exceptionHandler: (Throwable) -> Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = throw WOException()
        set(value) {
            builder.setExceptionHandler(value)
        }

    @DexAnalysis
    var forceDexAnalysis: Boolean
        @Deprecated(
            "Write only", level = DeprecationLevel.HIDDEN
        ) inline get() = throw WOException()
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

    class MatchResultKt(val result: MatchResult) {
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

fun XposedInterface.buildHooks(
    classLoader: BaseDexClassLoader, sourcePath: String, init: HookBuilderKt.() -> Unit
): HookBuilderKt.MatchResultKt {
    val builder = HookBuilderKt(this, classLoader, sourcePath)
    builder.init()
    return builder.build()
}
