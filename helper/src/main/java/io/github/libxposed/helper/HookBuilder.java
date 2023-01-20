package io.github.libxposed.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.libxposed.XposedContextWrapper;
import io.github.libxposed.XposedModuleInterface;

@SuppressWarnings("unused")
public interface HookBuilder {
    @NonNull
    static MatchResult buildHook(@NonNull XposedContextWrapper ctx, @NonNull XposedModuleInterface.PackageLoadedParam param, Consumer<HookBuilder> consumer) {
        var builder = new HookBuilderImpl(ctx, param);
        consumer.accept(builder);
        return builder.build();
    }

    interface MatchResult extends Serializable, Cloneable {
        @NonNull
        Map<java.lang.String, java.lang.Class<?>> getMatchedClasses();

        @NonNull
        Map<java.lang.String, java.lang.reflect.Field> getMatchedFields();

        @NonNull
        Map<java.lang.String, java.lang.reflect.Method> getMatchedMethods();

        @NonNull
        Map<java.lang.String, java.lang.reflect.Constructor<?>> getMatchedConstructors();
    }

    interface BaseMatcher<T, U> {
        T setMatchFirst(boolean matchFirst);

        T setMissReplacement(U replacement);
    }

    interface ReflectMatcher<T, U> extends BaseMatcher<T, U> {
        T setKey(@NonNull java.lang.String key);

        T setIsPublic(boolean isPublic);

        T setIsPrivate(boolean isPrivate);

        T setIsProtected(boolean isProtected);

        T setIsPackage(boolean isPackage);
    }

    interface ContainerSyntax<T> {

    }

    interface ClassMatcher extends ReflectMatcher<ClassMatcher, Class> {
        @NonNull
        ClassMatcher setName(@NonNull String name);

        @NonNull
        ClassMatcher setSuperClass(@NonNull Class superClass);

        @NonNull
        ClassMatcher setContainsMethods(@NonNull Consumer<ContainerSyntax<Method>> consumer);

        @NonNull
        ClassMatcher setContainsConstructors(@NonNull Consumer<ContainerSyntax<Constructor>> consumer);

        @NonNull
        ClassMatcher setContainsFields(@NonNull Consumer<ContainerSyntax<Field>> consumer);

        @NonNull
        ClassMatcher setInterfaces(@NonNull Consumer<ContainerSyntax<Class>> consumer);

        @NonNull
        ClassMatcher setIsAbstract(boolean isAbstract);

        @NonNull
        ClassMatcher setIsStatic(boolean isStatic);

        @NonNull
        ClassMatcher setIsFinal(boolean isFinal);
    }

    interface StringMatcher extends BaseMatcher<StringMatcher, String> {
        @NonNull
        StringMatcher setExact(@NonNull java.lang.String exact);

        @NonNull
        StringMatcher setPrefix(@NonNull java.lang.String prefix);
    }

    interface MemberMatcher<T, U> extends ReflectMatcher<T, U> {
        @NonNull
        T setDeclaringClass(@NonNull Class declaringClass);

        @NonNull
        T setIsSynthetic(boolean isSynthetic);
    }

    interface FieldMatcher extends MemberMatcher<FieldMatcher, Field> {
        @NonNull
        FieldMatcher setName(@NonNull String name);

        @NonNull
        FieldMatcher setType(@NonNull Class type);

        @NonNull
        FieldMatcher setIsStatic(boolean isStatic);

        @NonNull
        FieldMatcher setIsFinal(boolean isFinal);

        @NonNull
        FieldMatcher setIsTransient(boolean isTransient);

        @NonNull
        FieldMatcher setIsVolatile(boolean isVolatile);
    }

    interface ExecutableMatcher<T, U> extends MemberMatcher<T, U> {
        @NonNull
        T setParameterCount(int count);

        @NonNull
        T setParameterTypes(@NonNull ContainerSyntax<Class> parameterTypes);

        @NonNull
        T setReferredStrings(@NonNull ContainerSyntax<String> referredStrings);

        @NonNull
        T setAssignedFields(@NonNull ContainerSyntax<Field> assignedFields);

        @NonNull
        T setInvokedMethods(@NonNull ContainerSyntax<Method> invokedMethods);

        @NonNull
        T setInvokedConstructors(@NonNull ContainerSyntax<Constructor> invokedConstructors);

        @NonNull
        T setContainsOpcodes(@NonNull Byte[] opcodes);

        @NonNull
        T setIsVarargs(boolean isVarargs);
    }

    interface MethodMatcher extends ExecutableMatcher<MethodMatcher, Method> {
        @NonNull
        MethodMatcher setName(@NonNull String name);

        @NonNull
        MethodMatcher setReturnType(@NonNull Class returnType);

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

    interface ConstructorMatcher extends ExecutableMatcher<ConstructorMatcher, Constructor> {
    }

    interface BaseMatch<T, U> {
        @Nullable
        java.lang.String getKey();

        @NonNull
        T onMatch(@NonNull Consumer<U> consumer);
    }

    interface LazySequence<T, U, V extends BaseMatcher<V, T>> {
        @NonNull
        T first();

        @NonNull
        T first(@NonNull Consumer<V> consumer);

        @NonNull
        LazySequence<T, U, V> all(@NonNull Consumer<V> consumer);

        @NonNull
        LazySequence<T, U, V> onMatch(@NonNull Consumer<Iterable<U>> consumer);

        @NonNull
        T onMatch(Function<Iterable<U>, U> consumer);

        @NonNull
        ContainerSyntax<T> conjunction();

        @NonNull
        ContainerSyntax<T> disjunction();
    }

    interface Class extends BaseMatch<Class, java.lang.Class<?>> {
        @NonNull
        String getName();

        @NonNull
        Class getSuperClass();

        @NonNull
        LazySequence<Class, java.lang.Class<?>, ClassMatcher> getInterfaces();

        @NonNull
        LazySequence<Method, java.lang.reflect.Method, MethodMatcher> getDeclaredMethods();

        @NonNull
        LazySequence<Constructor, java.lang.reflect.Constructor<?>, ConstructorMatcher> getDeclaredConstructors();

        @NonNull
        LazySequence<Field, java.lang.reflect.Field, FieldMatcher> getDeclaredFields();

        @NonNull
        Class getArrayType();
    }

    interface MemberMatch<T, U> extends BaseMatch<T, U> {
        @NonNull
        Class getDeclaringClass();
    }

    interface ExecutableMatch<T, U> extends MemberMatch<T, U> {
        @NonNull
        LazySequence<Class, java.lang.Class<?>, ClassMatcher> getParameterTypes();

        @NonNull
        LazySequence<String, java.lang.String, StringMatcher> getReferredStrings();

        @NonNull
        LazySequence<Field, java.lang.reflect.Field, FieldMatcher> getAssignedFields();

        @NonNull
        LazySequence<Field, java.lang.reflect.Field, FieldMatcher> getAccessedFields();

        @NonNull
        LazySequence<Method, java.lang.reflect.Method, MethodMatcher> getInvokedMethods();

        @NonNull
        LazySequence<Constructor, java.lang.reflect.Constructor<?>, ConstructorMatcher> getInvokedConstructors();
    }

    interface Method extends ExecutableMatch<Method, java.lang.reflect.Method> {
        @NonNull
        String getName();

        @NonNull
        Class getReturnType();
    }

    interface Constructor extends ExecutableMatch<Constructor, java.lang.reflect.Constructor<?>> {
    }

    interface Field extends MemberMatch<Field, java.lang.reflect.Field> {
        @NonNull
        String getName();

        @NonNull
        Class getType();
    }

    interface String {

    }

    @NonNull
    HookBuilder setLastMatchResult(@NonNull MatchResult preferenceName);

    @NonNull
    HookBuilder setExceptionHandler(@NonNull Predicate<Throwable> handler);

    LazySequence<Method, java.lang.reflect.Method, MethodMatcher> methods(Consumer<MethodMatcher> matcher);

    Method firstMethod(Consumer<MethodMatcher> matcher);

    LazySequence<Constructor, java.lang.reflect.Constructor<?>, ConstructorMatcher> constructors(Consumer<ConstructorMatcher> matcher);

    Constructor firstConstructor(Consumer<ConstructorMatcher> matcher);

    LazySequence<Field, java.lang.reflect.Field, FieldMatcher> fields(Consumer<FieldMatcher> matcher);

    Field firstField(Consumer<FieldMatcher> matcher);

    LazySequence<Class, java.lang.Class<?>, ClassMatcher> classes(Consumer<ClassMatcher> matcher);

    Class firstClass(Consumer<ClassMatcher> matcher);

    LazySequence<String, java.lang.String, StringMatcher> strings(Consumer<StringMatcher> matcher);

    String firstString(Consumer<StringMatcher> matcher);

    @NonNull
    String exact(@NonNull java.lang.String string);

    @NonNull
    LazySequence<String, java.lang.String, StringMatcher> prefix(@NonNull java.lang.String prefix);

    @NonNull
    Class exactClass(@NonNull java.lang.String name);

    @NonNull
    Class exact(@NonNull java.lang.Class<?> clazz);

    @NonNull
    Method exact(@NonNull java.lang.reflect.Method method);

    @NonNull
    Constructor exact(@NonNull java.lang.reflect.Constructor<?> constructor);

    @NonNull
    Field exact(@NonNull java.lang.reflect.Field field);
}
