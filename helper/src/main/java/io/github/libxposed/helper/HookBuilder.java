package io.github.libxposed.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresOptIn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import dalvik.system.BaseDexClassLoader;
import io.github.libxposed.api.XposedContextWrapper;

@SuppressWarnings("unused")
public interface HookBuilder {
    @FunctionalInterface
    interface Consumer<T> {
        void accept(@NonNull T t);
    }

    @FunctionalInterface
    interface BiConsumer<T, U> {
        void accept(@NonNull T t, @NonNull U u);
    }

    @FunctionalInterface
    interface Predicate<T> {
        boolean test(@NonNull T t);
    }

    @FunctionalInterface
    interface MatchConsumer<T, U> {
        @NonNull
        U accept(@NonNull T t);
    }

    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @interface DexAnalysis {
    }

    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @interface AnnotationAnalysis {
    }

    @NonNull
    static MatchResult buildHook(@NonNull XposedContextWrapper ctx, @NonNull BaseDexClassLoader classLoader, @NonNull String sourcePath, Consumer<HookBuilder> consumer) {
        var builder = new HookBuilderImpl(ctx, classLoader, sourcePath);
        consumer.accept(builder);
        return builder.build();
    }

    interface MatchResult {
        @NonNull
        Map<String, Class<?>> getMatchedClasses();

        @NonNull
        Map<String, Field> getMatchedFields();

        @NonNull
        Map<String, Method> getMatchedMethods();

        @NonNull
        Map<String, Constructor<?>> getMatchedConstructors();
    }

    interface BaseMatcher<Self extends BaseMatcher<Self, Match>, Match extends BaseMatch<Match, ?>> {
        @NonNull
        Self setMatchFirst(boolean matchFirst);

        @NonNull
        Self setMissReplacement(@NonNull Match replacement);
    }

    interface ReflectMatcher<Self extends ReflectMatcher<Self, Match>, Match extends ReflectMatch<Match, ?>> extends BaseMatcher<Self, Match> {
        @NonNull
        Self setKey(@NonNull String key);

        @NonNull
        Self setIsPublic(boolean isPublic);

        @NonNull
        Self setIsPrivate(boolean isPrivate);

        @NonNull
        Self setIsProtected(boolean isProtected);

        @NonNull
        Self setIsPackage(boolean isPackage);
    }

    interface ContainerSyntax<Match extends BaseMatch<Match, ?>> {
        @NonNull
        ContainerSyntax<Match> and(@NonNull Match element);

        @NonNull
        ContainerSyntax<Match> and(@NonNull ContainerSyntax<Match> predicate);

        @NonNull
        ContainerSyntax<Match> or(@NonNull Match element);

        @NonNull
        ContainerSyntax<Match> or(@NonNull ContainerSyntax<Match> predicate);

        @NonNull
        ContainerSyntax<Match> not();
    }

    interface TypeMatcher<Self extends TypeMatcher<Self, Match>, Match extends TypeMatch<Match>> extends ReflectMatcher<Self, Match> {
        @NonNull
        Self setName(@NonNull StringMatch name);

        @NonNull
        Self setSuperClass(@NonNull ClassMatch superClassMatch);

        @NonNull
        Self setContainsInterfaces(@NonNull ContainerSyntax<ClassMatch> syntax);

        @NonNull
        Self setIsAbstract(boolean isAbstract);

        @NonNull
        Self setIsStatic(boolean isStatic);

        @NonNull
        Self setIsFinal(boolean isFinal);
    }

    interface ClassMatcher extends TypeMatcher<ClassMatcher, ClassMatch> {
    }

    interface ParameterMatcher extends TypeMatcher<ParameterMatcher, ParameterMatch> {
        @NonNull
        ParameterMatcher setIndex(int index);
    }

    interface StringMatcher extends BaseMatcher<StringMatcher, StringMatch> {
        @NonNull
        StringMatcher setExact(@NonNull String exact);

        @NonNull
        StringMatcher setPrefix(@NonNull String prefix);
    }

    interface MemberMatcher<Self extends MemberMatcher<Self, Match>, Match extends MemberMatch<Match, ?>> extends ReflectMatcher<Self, Match> {
        @NonNull
        Self setDeclaringClass(@NonNull ClassMatch declaringClassMatch);

        @NonNull
        Self setIsSynthetic(boolean isSynthetic);
    }

    interface FieldMatcher extends MemberMatcher<FieldMatcher, FieldMatch> {
        @NonNull
        FieldMatcher setName(@NonNull StringMatch name);

        @NonNull
        FieldMatcher setType(@NonNull ClassMatch type);

        @NonNull
        FieldMatcher setIsStatic(boolean isStatic);

        @NonNull
        FieldMatcher setIsFinal(boolean isFinal);

        @NonNull
        FieldMatcher setIsTransient(boolean isTransient);

        @NonNull
        FieldMatcher setIsVolatile(boolean isVolatile);
    }

    interface ExecutableMatcher<Self extends ExecutableMatcher<Self, Match>, Match extends ExecutableMatch<Match, ?>> extends MemberMatcher<Self, Match> {
        @NonNull
        Self setParameterCount(int count);

        @NonNull
        Self setParameterTypes(@NonNull ContainerSyntax<ParameterMatch> parameterTypes);

        @DexAnalysis
        @NonNull
        Self setReferredStrings(@NonNull ContainerSyntax<StringMatch> referredStrings);

        @DexAnalysis
        @NonNull
        Self setAssignedFields(@NonNull ContainerSyntax<FieldMatch> assignedFields);

        @DexAnalysis
        @NonNull
        Self setAccessedFields(@NonNull ContainerSyntax<FieldMatch> accessedFields);

        @DexAnalysis
        @NonNull
        Self setInvokedMethods(@NonNull ContainerSyntax<MethodMatch> invokedMethods);

        @DexAnalysis
        @NonNull
        Self setInvokedConstructors(@NonNull ContainerSyntax<ConstructorMatch> invokedConstructors);

        @DexAnalysis
        @NonNull
        Self setContainsOpcodes(@NonNull byte[] opcodes);

        @NonNull
        Self setIsVarargs(boolean isVarargs);
    }

    interface MethodMatcher extends ExecutableMatcher<MethodMatcher, MethodMatch> {
        @NonNull
        MethodMatcher setName(@NonNull StringMatch name);

        @NonNull
        MethodMatcher setReturnType(@NonNull ClassMatch returnType);

        @NonNull
        MethodMatcher setIsAbstract(boolean isAbstract);

        @NonNull
        MethodMatcher setIsStatic(boolean isStatic);

        @NonNull
        MethodMatcher setIsFinal(boolean isFinal);

        @NonNull
        MethodMatcher setIsSynchronized(boolean isSynchronized);

        @NonNull
        MethodMatcher setIsNative(boolean isNative);
    }

    interface ConstructorMatcher extends ExecutableMatcher<ConstructorMatcher, ConstructorMatch> {
    }

    interface BaseMatch<Self extends BaseMatch<Self, Reflect>, Reflect> {
        @NonNull
        ContainerSyntax<Self> observe();

        @NonNull
        ContainerSyntax<Self> reverse();
    }

    interface ReflectMatch<Self extends ReflectMatch<Self, Reflect>, Reflect> extends BaseMatch<Self, Reflect> {
        @Nullable
        String getKey();

        @NonNull
        Self setKey(@Nullable String key);

        @NonNull
        Self onMatch(@NonNull Consumer<Reflect> consumer);

        @NonNull
        <Bind extends LazyBind> Self bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Reflect> consumer);
    }

    interface LazySequence<Match extends BaseMatch<Match, Reflect>, Reflect, Matcher extends BaseMatcher<Matcher, Match>> {
        @NonNull
        Match first();

        @NonNull
        Match first(@NonNull Consumer<Matcher> consumer);

        @NonNull
        LazySequence<Match, Reflect, Matcher> all(@NonNull Consumer<Matcher> consumer);

        @NonNull
        LazySequence<Match, Reflect, Matcher> onMatch(@NonNull Consumer<Iterable<Reflect>> consumer);

        @NonNull
        Match onMatch(MatchConsumer<Iterable<Reflect>, Reflect> consumer);

        @NonNull
        ContainerSyntax<Match> conjunction();

        @NonNull
        ContainerSyntax<Match> disjunction();

        @NonNull
        <Bind extends LazyBind> LazySequence<Match, Reflect, Matcher> bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Iterable<Reflect>> consumer);
    }

    interface TypeMatch<Self extends TypeMatch<Self>> extends ReflectMatch<Self, Class<?>> {
        @NonNull
        StringMatch getName();

        @NonNull
        ClassMatch getSuperClass();

        @NonNull
        LazySequence<ClassMatch, Class<?>, ClassMatcher> getInterfaces();

        @NonNull
        LazySequence<MethodMatch, Method, MethodMatcher> getDeclaredMethods();

        @NonNull
        LazySequence<ConstructorMatch, Constructor<?>, ConstructorMatcher> getDeclaredConstructors();

        @NonNull
        LazySequence<FieldMatch, Field, FieldMatcher> getDeclaredFields();

        @NonNull
        ClassMatch getArrayType();
    }

    interface ClassMatch extends TypeMatch<ClassMatch> {
        @NonNull
        ParameterMatch asParameter(int index);
    }

    interface ParameterMatch extends TypeMatch<ParameterMatch> {
    }

    interface MemberMatch<Self extends MemberMatch<Self, Reflect>, Reflect extends Member> extends ReflectMatch<Self, Reflect> {
        @NonNull
        ClassMatch getDeclaringClass();
    }

    interface ExecutableMatch<Self extends ExecutableMatch<Self, Reflect>, Reflect extends Member> extends MemberMatch<Self, Reflect> {
        @NonNull
        LazySequence<ParameterMatch, Class<?>, ParameterMatcher> getParameterTypes();

        @NonNull
        LazySequence<StringMatch, String, StringMatcher> getReferredStrings();

        @NonNull
        LazySequence<FieldMatch, Field, FieldMatcher> getAssignedFields();

        @NonNull
        LazySequence<FieldMatch, Field, FieldMatcher> getAccessedFields();

        @NonNull
        LazySequence<MethodMatch, Method, MethodMatcher> getInvokedMethods();

        @NonNull
        LazySequence<ConstructorMatch, Constructor<?>, ConstructorMatcher> getInvokedConstructors();
    }

    interface MethodMatch extends ExecutableMatch<MethodMatch, Method> {
        @NonNull
        StringMatch getName();

        @NonNull
        ClassMatch getReturnType();
    }

    interface ConstructorMatch extends ExecutableMatch<ConstructorMatch, Constructor<?>> {
    }

    interface FieldMatch extends MemberMatch<FieldMatch, Field> {
        @NonNull
        StringMatch getName();

        @NonNull
        ClassMatch getType();
    }

    interface StringMatch extends BaseMatch<StringMatch, String> {

    }

    interface LazyBind {
        void onMatch();
    }

    @DexAnalysis
    @NonNull
    HookBuilder setForceDexAnalysis(boolean forceDexAnalysis);

    @NonNull
    HookBuilder setExecutorService(@NonNull ExecutorService executorService);

    @NonNull
    HookBuilder setLastMatchResult(@NonNull MatchResult preferenceName);

    @NonNull
    HookBuilder setExceptionHandler(@NonNull Predicate<Throwable> handler);

    @NonNull
    LazySequence<MethodMatch, Method, MethodMatcher> methods(@NonNull Consumer<MethodMatcher> matcher);

    @NonNull
    MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher);

    @NonNull
    LazySequence<ConstructorMatch, Constructor<?>, ConstructorMatcher> constructors(@NonNull Consumer<ConstructorMatcher> matcher);

    @NonNull
    ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher);

    @NonNull
    LazySequence<FieldMatch, Field, FieldMatcher> fields(@NonNull Consumer<FieldMatcher> matcher);

    @NonNull
    FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher);

    @NonNull
    LazySequence<ClassMatch, Class<?>, ClassMatcher> classes(@NonNull Consumer<ClassMatcher> matcher);

    @NonNull
    ClassMatch firstClass(@NonNull Consumer<ClassMatcher> matcher);

    @NonNull
    StringMatch string(@NonNull Consumer<StringMatcher> matcher);

    @NonNull
    StringMatch exact(@NonNull String string);

    @NonNull
    StringMatch prefix(@NonNull String prefix);

    @NonNull
    ClassMatch exactClass(@NonNull String name);

    @NonNull
    ClassMatch exact(@NonNull Class<?> clazz);

    @NonNull
    MethodMatch exact(@NonNull Method method);

    @NonNull
    ConstructorMatch exact(@NonNull Constructor<?> constructor);

    @NonNull
    FieldMatch exact(@NonNull Field field);
}
