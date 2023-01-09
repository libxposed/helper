package io.github.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper for quick and elegant to find method(s).
 */
@SuppressWarnings("unused")
public final class MethodFinder extends BaseFinder<Method, MethodFinder> {

    private MethodFinder(@NonNull Stream<Method> stream) {
        super(stream);
    }

    /**
     * Create MethodFinder with the class.
     *
     * @param clazz                   class
     * @param findSuperClassPredicate find super class predicate(return true = break, false = continue), null if don't find methods in superclass
     * @return MethodFinder
     */
    public static MethodFinder from(@NonNull Class<?> clazz, @Nullable Predicate<Class<?>> findSuperClassPredicate) {
        Stream<Method> stream = Stream.empty();

        do {
            stream = Stream.concat(stream, Arrays.stream(clazz.getDeclaredMethods()));
            stream = Stream.concat(stream, Arrays.stream(clazz.getInterfaces())
                    .flatMap(c -> Arrays.stream(c.getDeclaredMethods())));
        } while (findSuperClassPredicate != null &&
                !findSuperClassPredicate.test(clazz) &&
                (clazz = clazz.getSuperclass()) != Object.class &&
                clazz != null);

        return new MethodFinder(stream);
    }

    /**
     * Create MethodFinder with the class name.
     * Will load the class with classloader, or use Class.forName if classloader is null.
     *
     * @param className               className
     * @param classLoader             classLoader
     * @param findSuperClassPredicate find super class predicate(return true = break, false = continue), null if don't find methods in superclass
     * @return MethodFinder
     * @throws ClassNotFoundException when the class is not found
     */
    public static MethodFinder from(@NonNull String className, @Nullable ClassLoader classLoader, @Nullable Predicate<Class<?>> findSuperClassPredicate) throws ClassNotFoundException {
        return from(className, classLoader, findSuperClassPredicate);
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
     * Filter methods if they are default method.
     *
     * @return this
     */
    public MethodFinder filterDefault() {
        return filter(Method::isDefault);
    }

    /**
     * Filter methods if they are not default method.
     *
     * @return this
     */
    public MethodFinder filterNonDefault() {
        return filter(m -> !m.isDefault());
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

    @Override
    protected Class<?>[] getParameterTypes(Method member) {
        return member.getParameterTypes();
    }

    @Override
    protected Class<?>[] getExceptionTypes(Method member) {
        return member.getExceptionTypes();
    }
}
