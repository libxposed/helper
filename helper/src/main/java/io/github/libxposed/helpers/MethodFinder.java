package io.github.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper for quick and elegant to find method(s).
 */
@SuppressWarnings("unused")
public final class MethodFinder extends BaseFinder<Method, MethodFinder> {
    @Nullable
    private Class<?> clazz;

    private MethodFinder(@NonNull Stream<Method> stream) {
        super(stream);
    }

    /**
     * Create MethodFinder with the class.
     *
     * @param clazz                   class
     * @param findSuperClassPredicate find super class predicate, null if not find
     * @return MethodFinder
     * @see #findSuper(Predicate)
     */
    public static MethodFinder from(@NonNull Class<?> clazz, @Nullable Predicate<Class<?>> findSuperClassPredicate) {
        var mf = new MethodFinder(Arrays.stream(clazz.getDeclaredMethods()));
        mf.clazz = clazz;
        if (findSuperClassPredicate != null) {
            mf.findSuper(findSuperClassPredicate);
        }
        return mf;
    }

    public static MethodFinder from(@NonNull Class<?> clazz) {
        return from(clazz, null);
    }

    /**
     * Create MethodFinder with the class name.
     * Will load the class with classloader, or use Class.forName if classloader is null.
     *
     * @param className               className
     * @param classLoader             classLoader
     * @param findSuperClassPredicate find super class predicate, null if not find
     * @return MethodFinder
     * @throws ClassNotFoundException when the class is not found
     * @see #findSuper(Predicate)
     */
    public static MethodFinder from(@NonNull String className, @Nullable ClassLoader classLoader, @Nullable Predicate<Class<?>> findSuperClassPredicate) throws ClassNotFoundException {
        return from(Class.forName(className, false, Objects.requireNonNullElse(classLoader, ClassLoader.getSystemClassLoader())), findSuperClassPredicate);
    }

    public static MethodFinder from(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        return from(Class.forName(className, false, Objects.requireNonNullElse(classLoader, ClassLoader.getSystemClassLoader())));
    }

    /**
     * Create MethodFinder with the class name.
     * Will load the class with Class.forName.
     *
     * @param className               className
     * @param findSuperClassPredicate find super class predicate, null if not find
     * @return MethodFinder
     * @throws ClassNotFoundException when the class is not found
     * @see #findSuper(Predicate)
     */
    public static MethodFinder from(@NonNull String className, @Nullable Predicate<Class<?>> findSuperClassPredicate) throws ClassNotFoundException {
        return from(className, null, findSuperClassPredicate);
    }

    public static MethodFinder from(@NonNull String className) throws ClassNotFoundException {
        return from(className, null, null);
    }

    /**
     * Create MethodFinder with the method array.
     *
     * @param methods method array
     * @return MethodFinder
     */
    public static MethodFinder from(@NonNull Method[] methods) {
        return new MethodFinder(Arrays.stream(methods));
    }

    /**
     * Create MethodFinder with the iterable methods.
     *
     * @param methods iterable method
     * @return MethodFinder
     */
    public static MethodFinder from(@NonNull Iterable<Method> methods) {
        return new MethodFinder(StreamSupport.stream(methods.spliterator(), false));
    }

    /**
     * Filter methods by name.
     *
     * @param name name
     * @return this
     */
    public MethodFinder filterByName(@NonNull String name) {
        stream = stream.filter(member -> member.getName().equals(name));
        return this;
    }

    /**
     * Filter methods if they have static modifier.
     *
     * @return this
     */
    public MethodFinder filterStatic() {
        return filter(ModifierHelper::isStatic);
    }

    /**
     * Filter methods if they not have static modifier.
     *
     * @return this
     */
    public MethodFinder filterNonStatic() {
        return filter(ModifierHelper::isNotStatic);
    }

    /**
     * Filter methods if they have final modifier.
     *
     * @return this
     */
    public MethodFinder filterFinal() {
        return filter(ModifierHelper::isFinal);
    }


    /**
     * Filter methods if they not have final modifier.
     *
     * @return this
     */
    public MethodFinder filterNonFinal() {
        return filter(ModifierHelper::isNotFinal);
    }

    /**
     * Filter methods if they have synchronized modifier.
     *
     * @return this
     */
    public MethodFinder filterSynchronized() {
        return filter(ModifierHelper::isSynchronized);
    }

    /**
     * Filter methods if they not have synchronized modifier.
     *
     * @return this
     */
    public MethodFinder filterNonSynchronized() {
        return filter(ModifierHelper::isNotSynchronized);
    }

    /**
     * Filter methods if they have native modifier.
     *
     * @return this
     */
    public MethodFinder filterNative() {
        return filter(ModifierHelper::isNative);
    }

    /**
     * Filter methods if they have native modifier.
     *
     * @return this
     */
    public MethodFinder filterNonNative() {
        return filter(ModifierHelper::isNotNative);
    }


    /**
     * Filter methods if they have abstract modifier.
     *
     * @return this
     */
    public MethodFinder filterAbstract() {
        return filter(ModifierHelper::isAbstract);
    }

    /**
     * Filter methods if they not have abstract modifier.
     *
     * @return this
     */
    public MethodFinder filterNonAbstract() {
        return filter(ModifierHelper::isNotAbstract);
    }

    @Override
    protected Class<?>[] getParameterTypes(Method member) {
        return member.getParameterTypes();
    }

    /**
     * Find methods in superclass.
     * Will do nothing when MethodFinder create from method array or list.
     *
     * @param classPredicate predicate to decide the last superclass, return true = last, return false = continue
     * @return this
     */
    private MethodFinder findSuper(@NotNull Predicate<Class<?>> classPredicate) {
        if (clazz == null)
            return this;

        var sc = clazz.getSuperclass();
        while (sc != null) {
            stream = Stream.concat(stream, Arrays.stream(sc.getDeclaredMethods()));
            if (classPredicate.test(sc))
                break;

            sc = sc.getSuperclass();
        }
        return this;
    }

    /**
     * Filter methods by return type.
     *
     * @param returnType return type
     * @return this
     */
    public MethodFinder filterByReturnType(@NonNull Class<?> returnType) {
        return filter(method -> method.getReturnType().equals(returnType));
    }

    /**
     * Collect the methods.
     *
     * @param collector collector
     * @param <R>       the result type of the reduction operation
     * @param <A>       the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @return collected result
     */
    @NonNull
    @Override
    public <R, A> R collect(@NonNull Collector<Method, A, R> collector) {
        onEach(method -> method.setAccessible(true));
        return stream.collect(collector);
    }
}
