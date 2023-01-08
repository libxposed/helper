package org.lsposed.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper for quick and elegant to find method(s).
 */
@SuppressWarnings("unused")
public final class MethodFinder {
    @Nullable
    private Class<?> clazz;

    @NonNull
    private Stream<Method> methodStream;

    public MethodFinder(@NonNull Class<?> clazz) {
        this.clazz = clazz;
        methodStream = Arrays.stream(clazz.getDeclaredMethods());
    }

    public MethodFinder(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        this(classLoader != null ? classLoader.loadClass(className) : Class.forName(className));
    }

    public MethodFinder(@NonNull String className) throws ClassNotFoundException {
        this(className, null);
    }

    public MethodFinder(@NonNull Method[] methods) {
        this.methodStream = Arrays.stream(methods);
    }

    public MethodFinder(@NonNull List<Method> methods) {
        this.methodStream = methods.stream();
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
     * Create MethodFinder with the method list.
     *
     * @param methods method list
     * @return MethodFinder
     */
    public static MethodFinder fromList(@NonNull List<Method> methods) {
        return new MethodFinder(methods);
    }

    /**
     * Find methods in superclass.
     * Will do nothing when MethodFinder create from method array or list.
     *
     * @param superclass Until to the superclass, null if no limit.
     * @return this
     * @throws IllegalArgumentException If the superclass parameter is not a superclass of the class.
     */
    public MethodFinder findSuper(@Nullable Class<?> superclass) throws IllegalArgumentException {
        if (clazz == null)
            return this;

        if (superclass != null) {
            if (!superclass.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(superclass.getName() + " is not a superclass of " + clazz.getName());
            }
        }

        Class<?> superclz = clazz.getSuperclass();
        while (superclz != null) {
            methodStream = Stream.concat(methodStream, Arrays.stream(superclz.getDeclaredMethods()));
            if (superclass != null && superclass.equals(superclz))
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
     * Filter methods by conditions.
     *
     * @param predicate conditions
     * @return this
     */
    public MethodFinder filter(@NonNull Predicate<Method> predicate) {
        methodStream = methodStream.filter(predicate);
        return this;
    }

    public MethodFinder filterByName(@NonNull String name) {
        return filter(method -> method.getName().equals(name));
    }

    /**
     * Filter methods if they have public modifier.
     *
     * @return this
     */
    public MethodFinder filterPublic() {
        return filter(ModifierHelper::isPublic);
    }

    /**
     * Filter methods if they not have public modifier.
     *
     * @return this
     */
    public MethodFinder filterNonPublic() {
        return filter(ModifierHelper::isNotPublic);
    }

    /**
     * Filter methods if they have protected modifier.
     *
     * @return this
     */
    public MethodFinder filterProtected() {
        return filter(ModifierHelper::isProtected);
    }

    /**
     * Filter methods if they not have protected modifier.
     *
     * @return this
     */
    public MethodFinder filterNonProtected() {
        return filter(ModifierHelper::isNotProtected);
    }

    /**
     * Filter methods if they have private modifier.
     *
     * @return this
     */
    public MethodFinder filterPrivate() {
        return filter(ModifierHelper::isPrivate);
    }

    /**
     * Filter methods if they not have private modifier.
     *
     * @return this
     */
    public MethodFinder filterNonPrivate() {
        return filter(ModifierHelper::isNotPrivate);
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
     * Filter methods by return type name.
     *
     * @param returnTypeName name of return type
     * @return this
     */
    public MethodFinder filterByReturnTypeName(@NonNull String returnTypeName) {
        return filter(method -> method.getReturnType().getName().equals(returnTypeName));
    }

    /**
     * Filter methods by parameter types, make sure length is same as target method parameter types length.
     *
     * @param parameterTypes parameter types, use null to skip check some parameters.
     * @return this
     */
    public MethodFinder filterByParameterTypes(Class<?>... parameterTypes) {
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
     * Filter methods by parameter type names, make sure parameter length is same as target method parameter types length.
     *
     * @param parameterTypeNames parameter type names, use null to skip check some parameters.
     * @return this
     */
    public MethodFinder filterByParameterTypeNames(String... parameterTypeNames) {
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
     * Filter methods by parameter count.
     *
     * @param parameterCount parameter count
     * @return this
     */
    public MethodFinder filterByParameterCount(int parameterCount) {
        return filter(method -> method.getParameterTypes().length == parameterCount);
    }

    /**
     * For-each loop to consume stream.
     * <p>
     * <strong>NOTICE:</strong> This method will consume the stream, so you will not be able to continue using current MethodFinder after invoking this method.
     * </p>
     * If you want to do something with current remaining methods and use MethodFinder later, use {@link #onEach(Consumer)} instead.
     *
     * @see #onEach(Consumer)
     */
    public void forEach(@NonNull Consumer<Method> consumer) {
        methodStream.forEach(consumer);
    }

    /**
     * Do something with the current remaining methods.
     *
     * @param action action
     * @return this
     */
    @SuppressWarnings("UnusedReturnValue")
    public MethodFinder onEach(@NonNull Consumer<Method> action) {
        methodStream = methodStream.peek(action);
        return this;
    }

    /**
     * Transform methods to another type and collect they to a list.
     *
     * @param transform transform function
     * @param <T>       type
     * @return transformed list
     */
    public <T> List<T> mapToList(@NonNull Function<Method, T> transform) {
        return methodStream.map(transform).collect(Collectors.toList());
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
    public <R, A> R collect(@NonNull Collector<? super Method, A, R> collector) {
        onEach(method -> method.setAccessible(true));
        return methodStream.collect(collector);
    }

    /**
     * Collect the methods to a list.
     *
     * @return collected list
     */
    @NonNull
    public List<Method> toList() {
        return collect(Collectors.toList());
    }

    /**
     * Get the first method.
     *
     * @return first method
     * @throws NoSuchMethodException if the method not found
     */
    @NonNull
    public Method findFirst() throws NoSuchMethodException {
        Method m = methodStream.findFirst().orElse(null);
        if (m == null) {
            throw new NoSuchMethodException();
        }
        m.setAccessible(true);
        return m;
    }

    /**
     * Get the first method or null if not found.
     *
     * @return first method or null
     */
    @Nullable
    public Method findFirstOrNull() {
        Method m = methodStream.findFirst().orElse(null);
        if (m == null) {
            return null;
        }
        m.setAccessible(true);
        return m;
    }
}

