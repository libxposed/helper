package io.github.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
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

    private MethodFinder(@NonNull Class<?> clazz) {
        super(Arrays.stream(clazz.getDeclaredMethods()));
        this.clazz = clazz;
    }

    private MethodFinder(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        this(Class.forName(className, false, Objects.requireNonNullElse(classLoader, ClassLoader.getSystemClassLoader())));
    }

    private MethodFinder(@NonNull String className) throws ClassNotFoundException {
        this(className, null);
    }

    private MethodFinder(@NonNull Method[] methods) {
        super(Arrays.stream(methods));
    }

    private MethodFinder(@NonNull Iterable<Method> methods) {
        super(StreamSupport.stream(methods.spliterator(), false));
    }

    /**
     * Create MethodFinder with the class.
     *
     * @param clazz class
     * @return MethodFinder
     */
    public static MethodFinder fromClass(@NonNull Class<?> clazz) {
        return new MethodFinder(clazz);
    }

    /**
     * Create MethodFinder with the class name.
     * Will load the class with classloader, or use Class.forName if classloader is null.
     *
     * @param className   className
     * @param classLoader classLoader
     * @return MethodFinder
     * @throws ClassNotFoundException when the class is not found
     */
    public static MethodFinder fromClassName(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        return new MethodFinder(className, classLoader);
    }

    /**
     * Create MethodFinder with the class name.
     * Will load the class with Class.forName.
     *
     * @param className className
     * @return MethodFinder
     * @throws ClassNotFoundException when the class is not found
     */
    public static MethodFinder fromClassName(@NonNull String className) throws ClassNotFoundException {
        return new MethodFinder(className, null);
    }

    /**
     * Create MethodFinder with the method array.
     *
     * @param methods method array
     * @return MethodFinder
     */
    public static MethodFinder fromArray(@NonNull Method[] methods) {
        return new MethodFinder(methods);
    }

    /**
     * Create MethodFinder with the iterable methods.
     *
     * @param methods iterable method
     * @return MethodFinder
     */
    public static MethodFinder fromIterable(@NonNull Iterable<Method> methods) {
        return new MethodFinder(methods);
    }

    /**
     * Find methods in superclass.
     * Will do nothing when MethodFinder create from method array or list.
     *
     * @param untilSuperClass Until to the superclass, null if no limit.
     * @return this
     * @throws IllegalArgumentException If the superclass parameter is not a superclass of the class.
     */
    public MethodFinder findSuper(@Nullable Class<?> untilSuperClass) throws IllegalArgumentException {
        if (clazz == null)
            return this;

        if (untilSuperClass != null) {
            if (!untilSuperClass.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(untilSuperClass.getName() + " is not a superclass of " + clazz.getName());
            }
        }

        var superclz = clazz.getSuperclass();
        while (superclz != null) {
            stream = Stream.concat(stream, Arrays.stream(superclz.getDeclaredMethods()));
            if (untilSuperClass != null && untilSuperClass.equals(superclz))
                break;

            superclz = superclz.getSuperclass();
        }
        return this;
    }

    /**
     * Find methods in superclasses.
     * Will do nothing when MethodFinder create from method array or list.
     *
     * @return this
     */
    public MethodFinder findSuper() {
        return findSuper(null);
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
