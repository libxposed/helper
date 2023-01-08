package io.github.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

/**
 * Helper for quick and elegant to find constructor(s).
 */
@SuppressWarnings("unused")
public class ConstructorFinder<C> extends BaseFinder<Constructor<C>, ConstructorFinder<C>> {
    @SuppressWarnings("unchecked")
    private ConstructorFinder(@NonNull Class<C> clazz) {
        super(Arrays.stream((Constructor<C>[]) clazz.getDeclaredConstructors()));
    }

    @SuppressWarnings("unchecked")
    private ConstructorFinder(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        this((Class<C>) Class.forName(className, false, Objects.requireNonNullElse(classLoader, ClassLoader.getSystemClassLoader())));
    }

    private ConstructorFinder(@NonNull String className) throws ClassNotFoundException {
        this(className, null);
    }

    private ConstructorFinder(@NonNull Constructor<C>[] constructors) {
        super(Arrays.stream(constructors));
    }

    private ConstructorFinder(@NonNull Iterable<Constructor<C>> constructors) {
        super(StreamSupport.stream(constructors.spliterator(), false));
    }

    /**
     * Create ConstructorFinder with the class.
     *
     * @param clazz class
     * @return ConstructorFinder
     */
    public static <C> ConstructorFinder<C> fromClass(@NonNull Class<C> clazz) {
        return new ConstructorFinder<>(clazz);
    }

    /**
     * Create ConstructorFinder with the class name.
     * Will load the class with classloader, or use Class.forName if classloader is null.
     *
     * @param className   className
     * @param classLoader classLoader
     * @return ConstructorFinder
     * @throws ClassNotFoundException when the class is not found
     */
    public static <C> ConstructorFinder<C> fromClassName(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        return new ConstructorFinder<>(className, classLoader);
    }

    /**
     * Create ConstructorFinder with the class name.
     * Will load the class with Class.forName.
     *
     * @param className className
     * @return ConstructorFinder
     * @throws ClassNotFoundException when the class is not found
     */
    public static <C> ConstructorFinder<C> fromClassName(@NonNull String className) throws ClassNotFoundException {
        return new ConstructorFinder<>(className, null);
    }

    /**
     * Create ConstructorFinder with the constructor array.
     *
     * @param constructors constructor array
     * @return ConstructorFinder
     */
    public static <C> ConstructorFinder<C> fromArray(@NonNull Constructor<C>[] constructors) {
        return new ConstructorFinder<>(constructors);
    }

    /**
     * Create ConstructorFinder with the iterable constructors.
     *
     * @param constructors Iterable constructor
     * @return ConstructorFinder
     */
    public static <C> ConstructorFinder<C> fromIterable(@NonNull Iterable<Constructor<C>> constructors) {
        return new ConstructorFinder<>(constructors);
    }

    @NonNull
    @Override
    public <R, A> R collect(@NonNull Collector<Constructor<C>, A, R> collector) {
        onEach(method -> method.setAccessible(true));
        return stream.collect(collector);
    }
}
