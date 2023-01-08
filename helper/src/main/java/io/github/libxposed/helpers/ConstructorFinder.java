package io.github.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper for quick and elegant to find constructor(s).
 */
@SuppressWarnings("unused")
public final class ConstructorFinder<C> extends BaseFinder<Constructor<C>, ConstructorFinder<C>> {
    private ConstructorFinder(@NonNull Stream<Constructor<C>> stream) {
        super(stream);
    }

    /**
     * Create ConstructorFinder with the class.
     *
     * @param clazz class
     * @return ConstructorFinder
     */
    @SuppressWarnings("unchecked")
    public static <C> ConstructorFinder<C> from(@NonNull Class<C> clazz) {
        return new ConstructorFinder<>(Arrays.stream((Constructor<C>[]) clazz.getDeclaredConstructors()));
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
    @SuppressWarnings("unchecked")
    public static <C> ConstructorFinder<C> from(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        return from((Class<C>) Class.forName(className, false, Objects.requireNonNullElse(classLoader, ClassLoader.getSystemClassLoader())));
    }

    /**
     * Create ConstructorFinder with the class name.
     * Will load the class with Class.forName.
     *
     * @param className className
     * @return ConstructorFinder
     * @throws ClassNotFoundException when the class is not found
     */
    public static <C> ConstructorFinder<C> from(@NonNull String className) throws ClassNotFoundException {
        return from(className, null);
    }

    /**
     * Create ConstructorFinder with the constructor array.
     *
     * @param constructors constructor array
     * @return ConstructorFinder
     */
    public static <C> ConstructorFinder<C> from(@NonNull Constructor<C>[] constructors) {
        return new ConstructorFinder<>(Arrays.stream(constructors));
    }

    /**
     * Create ConstructorFinder with the iterable constructors.
     *
     * @param constructors Iterable constructor
     * @return ConstructorFinder
     */
    public static <C> ConstructorFinder<C> from(@NonNull Iterable<Constructor<C>> constructors) {
        return new ConstructorFinder<>(StreamSupport.stream(constructors.spliterator(), false));
    }

    @Override
    protected Class<?>[] getParameterTypes(Constructor<C> member) {
        return member.getParameterTypes();
    }

    @NonNull
    @Override
    public <R, A> R collect(@NonNull Collector<Constructor<C>, A, R> collector) {
        onEach(method -> method.setAccessible(true));
        return stream.collect(collector);
    }
}
