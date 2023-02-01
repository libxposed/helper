package io.github.libxposed.helper;

import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresOptIn;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import dalvik.system.BaseDexClassLoader;
import io.github.libxposed.api.XposedInterface;

@SuppressWarnings("unused")
public interface HookBuilder {

    // replacement for java.lang.reflect.Parameter that is not available before Android O
    interface Parameter {
        @NonNull
        Class<?> getType();

        int getIndex();

        @NonNull
        Member getDeclaringExecutable();
    }

    @FunctionalInterface
    interface Supplier<T> {
        @NonNull
        T get();
    }

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
    static CountDownLatch buildHooks(@NonNull XposedInterface ctx, @NonNull BaseDexClassLoader classLoader, @NonNull String sourcePath, Consumer<HookBuilder> consumer) {
        var builder = new HookBuilderImpl(ctx, classLoader, sourcePath);
        consumer.accept(builder);
        return builder.build();
    }

    interface ReflectMatcher<Self extends ReflectMatcher<Self>> {
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
        ContainerSyntax<Match> and(@NonNull ContainerSyntax<Match> predicate);

        @NonNull
        ContainerSyntax<Match> or(@NonNull ContainerSyntax<Match> predicate);

        @NonNull
        ContainerSyntax<Match> not();
    }

    interface ClassMatcher extends ReflectMatcher<ClassMatcher> {
        @NonNull
        ClassMatcher setName(@NonNull StringMatch name);

        @NonNull
        ClassMatcher setSuperClass(@NonNull ClassMatch superClassMatch);

        @NonNull
        ClassMatcher setContainsInterfaces(@NonNull ContainerSyntax<ClassMatch> syntax);

        @NonNull
        ClassMatcher setIsAbstract(boolean isAbstract);

        @NonNull
        ClassMatcher setIsStatic(boolean isStatic);

        @NonNull
        ClassMatcher setIsFinal(boolean isFinal);

        @NonNull
        ClassMatcher setIsInterface(boolean isInterface);
    }

    interface ParameterMatcher extends ReflectMatcher<ParameterMatcher> {
        @NonNull
        ParameterMatcher setIndex(int index);

        @NonNull
        ParameterMatcher setType(@NonNull ClassMatch type);

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        ParameterMatcher setIsFinal(boolean isFinal);

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        ParameterMatcher setIsSynthetic(boolean isSynthetic);

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        ParameterMatcher setIsVarargs(boolean isVarargs);

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        ParameterMatcher setIsImplicit(boolean isImplicit);
    }

    interface MemberMatcher<Self extends MemberMatcher<Self>> extends ReflectMatcher<Self> {
        @NonNull
        Self setDeclaringClass(@NonNull ClassMatch declaringClassMatch);

        @NonNull
        Self setIsSynthetic(boolean isSynthetic);

        @NonNull
        Self setIncludeSuper(boolean includeSuper);

        @NonNull
        Self setIncludeInterface(boolean includeInterface);
    }

    interface FieldMatcher extends MemberMatcher<FieldMatcher> {
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

    interface ExecutableMatcher<Self extends ExecutableMatcher<Self>> extends MemberMatcher<Self> {
        @NonNull
        Self setParameterCount(int count);

        @NonNull
        Self setParameters(@NonNull ContainerSyntax<ParameterMatch> parameters);

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

    interface MethodMatcher extends ExecutableMatcher<MethodMatcher> {
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

    interface ConstructorMatcher extends ExecutableMatcher<ConstructorMatcher> {
    }

    interface BaseMatch<Self extends BaseMatch<Self, Reflect>, Reflect> {
        @NonNull
        ContainerSyntax<Self> observe();

        @NonNull
        ContainerSyntax<Self> reverse();
    }

    interface ReflectMatch<Self extends ReflectMatch<Self, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>> extends BaseMatch<Self, Reflect> {
        @Nullable
        String getKey();

        @NonNull
        Self setKey(@Nullable String key);

        @NonNull
        Self onMatch(@NonNull Consumer<Reflect> consumer);

        @NonNull
        Self onMiss(@NonNull Runnable handler);

        @NonNull
        Self substituteIfMiss(@NonNull Supplier<Self> substitute);

        @NonNull
        Self matchFirstIfMiss(@NonNull Consumer<Matcher> consumer);

        @NonNull
        <Bind extends LazyBind> Self bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Reflect> consumer);
    }

    interface ClassMatch extends ReflectMatch<ClassMatch, Class<?>, ClassMatcher> {
        @NonNull
        ClassMatch getSuperClass();

        @NonNull
        ClassLazySequence getInterfaces();

        @NonNull
        MethodLazySequence getDeclaredMethods();

        @NonNull
        ConstructorLazySequence getDeclaredConstructors();

        @NonNull
        FieldLazySequence getDeclaredFields();

        @NonNull
        ClassMatch getArrayType();
    }

    interface ParameterMatch extends ReflectMatch<ParameterMatch, Parameter, ParameterMatcher> {
        @NonNull
        ClassMatch getType();
    }

    interface MemberMatch<Self extends MemberMatch<Self, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>> extends ReflectMatch<Self, Reflect, Matcher> {
        @NonNull
        ClassMatch getDeclaringClass();
    }

    interface ExecutableMatch<Self extends ExecutableMatch<Self, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>> extends MemberMatch<Self, Reflect, Matcher> {
        @NonNull
        ClassLazySequence getParameterTypes();

        @NonNull
        ParameterLazySequence getParameters();

        @DexAnalysis
        @NonNull
        FieldLazySequence getAssignedFields();

        @DexAnalysis
        @NonNull
        FieldLazySequence getAccessedFields();

        @DexAnalysis
        @NonNull
        MethodLazySequence getInvokedMethods();

        @DexAnalysis
        @NonNull
        ConstructorLazySequence getInvokedConstructors();
    }

    interface MethodMatch extends ExecutableMatch<MethodMatch, Method, MethodMatcher> {
        @NonNull
        ClassMatch getReturnType();
    }

    interface ConstructorMatch extends ExecutableMatch<ConstructorMatch, Constructor<?>, ConstructorMatcher> {
    }

    interface FieldMatch extends MemberMatch<FieldMatch, Field, FieldMatcher> {
        @NonNull
        ClassMatch getType();
    }

    interface StringMatch extends BaseMatch<StringMatch, String> {

    }

    interface LazySequence<Self extends LazySequence<Self, Match, Reflect, Matcher>, Match extends ReflectMatch<Match, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>> {
        @NonNull
        Match first();

        @NonNull
        Match first(@NonNull Consumer<Matcher> consumer);

        @NonNull
        Self all(@NonNull Consumer<Matcher> consumer);

        @NonNull
        Self onMatch(@NonNull Consumer<Iterable<Reflect>> consumer);

        @NonNull
        Self onMiss(@NonNull Runnable runnable);

        @NonNull
        ContainerSyntax<Match> conjunction();

        @NonNull
        ContainerSyntax<Match> disjunction();

        @NonNull
        Self substituteIfMiss(@NonNull Supplier<Self> substitute);

        @NonNull
        Self matchIfMiss(@NonNull Consumer<Matcher> consumer);

        @NonNull
        <Bind extends LazyBind> Self bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Iterable<Reflect>> consumer);
    }

    interface ClassLazySequence extends LazySequence<ClassLazySequence, ClassMatch, Class<?>, ClassMatcher> {
        @NonNull
        MethodLazySequence methods(@NonNull Consumer<MethodMatcher> matcher);

        @NonNull
        MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher);

        @NonNull
        ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher);

        @NonNull
        ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher);

        @NonNull
        FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher);

        @NonNull
        FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher);
    }

    interface ParameterLazySequence extends LazySequence<ParameterLazySequence, ParameterMatch, Parameter, ParameterMatcher> {
        @NonNull
        ClassLazySequence types(@NonNull Consumer<ClassMatcher> matcher);

        @NonNull
        ClassMatch firstType(@NonNull Consumer<ClassMatcher> matcher);
    }

    interface MemberLazySequence<Self extends MemberLazySequence<Self, Match, Reflect, Matcher>, Match extends MemberMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>> extends LazySequence<Self, Match, Reflect, Matcher> {
        @NonNull
        ClassLazySequence declaringClasses(@NonNull Consumer<ClassMatcher> matcher);

        @NonNull
        ClassMatch firstDeclaringClass(@NonNull Consumer<ClassMatcher> matcher);
    }

    interface FieldLazySequence extends MemberLazySequence<FieldLazySequence, FieldMatch, Field, FieldMatcher> {
        @NonNull
        ClassLazySequence types(@NonNull Consumer<ClassMatcher> matcher);

        @NonNull
        ClassMatch firstType(@NonNull Consumer<ClassMatcher> matcher);
    }


    interface ExecutableLazySequence<Self extends ExecutableLazySequence<Self, Match, Reflect, Matcher>, Match extends ExecutableMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>> extends MemberLazySequence<Self, Match, Reflect, Matcher> {
        @NonNull
        ParameterLazySequence parameters(@NonNull Consumer<ParameterMatcher> matcher);

        @NonNull
        ParameterMatch firstParameter(@NonNull Consumer<ParameterMatcher> matcher);
    }

    interface MethodLazySequence extends ExecutableLazySequence<MethodLazySequence, MethodMatch, Method, MethodMatcher> {
        @NonNull
        ClassLazySequence returnTypes(@NonNull Consumer<ClassMatcher> matcher);

        @NonNull
        ClassMatch firstReturnType(@NonNull Consumer<ClassMatcher> matcher);
    }

    interface ConstructorLazySequence extends ExecutableLazySequence<ConstructorLazySequence, ConstructorMatch, Constructor<?>, ConstructorMatcher> {
    }

    interface LazyBind {
        void onMatch();

        void onMiss();
    }

    @DexAnalysis
    @NonNull
    HookBuilder setForceDexAnalysis(boolean forceDexAnalysis);

    @NonNull
    HookBuilder setExecutorService(@NonNull ExecutorService executorService);

    @NonNull
    HookBuilder setCallbackHandler(@NonNull Handler callbackHandler);

    @NonNull
    HookBuilder setCacheChecker(@NonNull Predicate<Map<String, Object>> cacheChecker);

    @NonNull
    HookBuilder setCacheInputStream(@NonNull InputStream cacheInputStream);

    @NonNull
    HookBuilder setCacheOutputStream(@NonNull OutputStream cacheOutputStream);

    @NonNull
    HookBuilder setExceptionHandler(@NonNull Predicate<Throwable> handler);

    @NonNull
    MethodLazySequence methods(@NonNull Consumer<MethodMatcher> matcher);

    @NonNull
    MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher);

    @NonNull
    ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher);

    @NonNull
    ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher);

    @NonNull
    FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher);

    @NonNull
    FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher);

    @NonNull
    ClassLazySequence classes(@NonNull Consumer<ClassMatcher> matcher);

    @NonNull
    ClassMatch firstClass(@NonNull Consumer<ClassMatcher> matcher);

    @NonNull
    StringMatch exact(@NonNull String string);

    @NonNull
    StringMatch prefix(@NonNull String prefix);

    @NonNull
    StringMatch firstPrefix(@NonNull String prefix);

    @NonNull
    ClassMatch exactClass(@NonNull String name);

    @NonNull
    ClassMatch exact(@NonNull Class<?> clazz);

    @NonNull
    MethodMatch exactMethod(@NonNull String signature);

    @NonNull
    MethodMatch exact(@NonNull Method method);

    @NonNull
    ConstructorMatch exactConstructor(@NonNull String signature);

    @NonNull
    ConstructorMatch exact(@NonNull Constructor<?> constructor);

    @NonNull
    FieldMatch exactField(@NonNull String signature);

    @NonNull
    FieldMatch exact(@NonNull Field field);
}
