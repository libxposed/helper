package io.github.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper for quick and elegant to find constructor(s).
 */
@SuppressWarnings("unused")
public class ConstructorFinder {
    @NonNull
    private Stream<Constructor<?>> constructorStream;

    public ConstructorFinder(@NonNull Class<?> clazz) {
        constructorStream = Arrays.stream(clazz.getDeclaredConstructors());
    }

    public ConstructorFinder(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        this(classLoader != null ? classLoader.loadClass(className) : Class.forName(className));
    }

    public ConstructorFinder(@NonNull String className) throws ClassNotFoundException {
        this(className, null);
    }

    public ConstructorFinder(@NonNull Constructor<?>[] constructors) {
        this.constructorStream = Arrays.stream(constructors);
    }

    public ConstructorFinder(@NonNull List<Constructor<?>> constructors) {
        this.constructorStream = constructors.stream();
    }

    /**
     * Create ConstructorFinder with the class.
     *
     * @param clazz class
     * @return ConstructorFinder
     */
    public static ConstructorFinder fromClass(@NonNull Class<?> clazz) {
        return new ConstructorFinder(clazz);
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
    public static ConstructorFinder fromClassName(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        return new ConstructorFinder(className, classLoader);
    }

    /**
     * Create ConstructorFinder with the class name.
     * Will load the class with Class.forName.
     *
     * @param className className
     * @return ConstructorFinder
     * @throws ClassNotFoundException when the class is not found
     */
    public static ConstructorFinder fromClassName(@NonNull String className) throws ClassNotFoundException {
        return new ConstructorFinder(className, null);
    }

    /**
     * Create ConstructorFinder with the constructor array.
     *
     * @param constructors constructor array
     * @return ConstructorFinder
     */
    public static ConstructorFinder fromArray(@NonNull Constructor<?>[] constructors) {
        return new ConstructorFinder(constructors);
    }

    /**
     * Create ConstructorFinder with the constructor list.
     *
     * @param constructors constructor list
     * @return ConstructorFinder
     */
    public static ConstructorFinder fromList(@NonNull List<Constructor<?>> constructors) {
        return new ConstructorFinder(constructors);
    }

    /**
     * Filter the constructors with the conditions.
     *
     * @param predicate conditions
     * @return this
     */
    public ConstructorFinder filter(@NonNull Predicate<Constructor<?>> predicate) {
        constructorStream = constructorStream.filter(predicate);
        return this;
    }

    /**
     * Filter constructors if they have public modifier.
     *
     * @return this
     */
    public ConstructorFinder filterPublic() {
        return filter(ModifierHelper::isPublic);
    }

    /**
     * Filter constructors if they not have public modifier.
     *
     * @return this
     */
    public ConstructorFinder filterNonPublic() {
        return filter(ModifierHelper::isNotPublic);
    }

    /**
     * Filter constructors if they have protected modifier.
     *
     * @return this
     */
    public ConstructorFinder filterProtected() {
        return filter(ModifierHelper::isProtected);
    }

    /**
     * Filter constructors if they not have protected modifier.
     *
     * @return this
     */
    public ConstructorFinder filterNonProtected() {
        return filter(ModifierHelper::isNotProtected);
    }

    /**
     * Filter constructors if they have private modifier.
     *
     * @return this
     */
    public ConstructorFinder filterPrivate() {
        return filter(ModifierHelper::isPrivate);
    }

    /**
     * Filter constructors if they not have private modifier.
     *
     * @return this
     */
    public ConstructorFinder filterNonPrivate() {
        return filter(ModifierHelper::isNotPrivate);
    }

    /**
     * Filter constructors if they have abstract modifier.
     *
     * @return this
     */
    public ConstructorFinder filterAbstract() {
        return filter(ModifierHelper::isAbstract);
    }

    /**
     * Filter constructors if they not have abstract modifier.
     *
     * @return this
     */
    public ConstructorFinder filterNonAbstract() {
        return filter(ModifierHelper::isNotAbstract);
    }

    /**
     * Filter constructors parameter types, make sure parameter length is same as target constructors parameter types length.
     *
     * @param parameterTypes parameter types, use null to skip check some parameters.
     * @return this
     */
    public ConstructorFinder filterByParameterTypes(Class<?>... parameterTypes) {
        return filter(method -> {
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            if (methodParameterTypes.length != parameterTypes.length) {
                return false;
            }
            for (int i = 0; i < methodParameterTypes.length; i++) {
                // ignore if null
                if (parameterTypes[i] == null)
                    continue;

                if (!methodParameterTypes[i].equals(parameterTypes[i])) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Filter constructors parameter type names, make sure parameter length is same as target constructors parameter type names length.
     *
     * @param parameterTypeNames parameter type names, use null to skip check some parameters.
     * @return this
     */
    public ConstructorFinder filterByParameterTypeNames(String... parameterTypeNames) {
        return filter(method -> {
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            if (methodParameterTypes.length != parameterTypeNames.length) {
                return false;
            }
            for (int i = 0; i < methodParameterTypes.length; i++) {
                // ignore if null
                if (parameterTypeNames[i] == null)
                    continue;

                if (!methodParameterTypes[i].getName().equals(parameterTypeNames[i])) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Filter constructors by parameter count.
     *
     * @param parameterCount parameter count
     * @return this
     */
    public ConstructorFinder filterByParameterCount(int parameterCount) {
        return filter(method -> method.getParameterTypes().length == parameterCount);
    }

    /**
     * For-each loop to consume the stream.
     * <p>
     * <strong>NOTICE:</strong> This method will consume the stream, so you will not be able to continue using current ConstructorFinder after invoked this method.
     * </p>
     * If you want to do something with current remaining constructors and use ConstructorFinder later, use {@link #onEach(Consumer)} instead.
     *
     * @see #onEach(Consumer)
     */
    public void forEach(@NonNull Consumer<Constructor<?>> consumer) {
        constructorStream.forEach(consumer);
    }

    /**
     * Do something with the current remaining constructors.
     *
     * @param action action
     * @return this
     */
    @SuppressWarnings("UnusedReturnValue")
    public ConstructorFinder onEach(@NonNull Consumer<Constructor<?>> action) {
        constructorStream = constructorStream.peek(action);
        return this;
    }

    /**
     * Transform constructors to another type and collect they to a list.
     *
     * @param transform transform function
     * @param <T>       type
     * @return transformed list
     */
    public <T> List<T> mapToList(@NonNull Function<Constructor<?>, T> transform) {
        return constructorStream.map(transform).collect(Collectors.toList());
    }

    /**
     * Collect the constructors.
     *
     * @param collector collector
     * @param <R>       the result type of the reduction operation
     * @param <A>       the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @return collected result
     */
    @NonNull
    public <R, A> R collect(@NonNull Collector<? super Constructor<?>, A, R> collector) {
        onEach(method -> method.setAccessible(true));
        return constructorStream.collect(collector);
    }

    /**
     * Collect the constructors to a list.
     *
     * @return collected list
     */
    @NonNull
    public List<Constructor<?>> toList() {
        return collect(Collectors.toList());
    }

    /**
     * Get the first constructor.
     *
     * @return first constructor
     * @throws NoSuchMethodException if the constructor not found
     */
    @NonNull
    public Constructor<?> findFirst() throws NoSuchMethodException {
        Constructor<?> c = constructorStream.findFirst().orElse(null);
        if (c == null) {
            throw new NoSuchMethodException();
        }
        c.setAccessible(true);
        return c;
    }

    /**
     * Get the first constructor or null if not found.
     *
     * @return first constructor or null
     */
    @Nullable
    public Constructor<?> findFirstOrNull() {
        Constructor<?> c = constructorStream.findFirst().orElse(null);
        if (c == null) {
            return null;
        }
        c.setAccessible(true);
        return c;
    }

}
