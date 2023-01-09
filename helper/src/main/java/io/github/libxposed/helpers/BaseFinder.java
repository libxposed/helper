package io.github.libxposed.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "unused"})
abstract class BaseFinder<T extends Member, S> {
    @NonNull
    protected Stream<T> stream;

    BaseFinder(@NonNull Stream<T> stream) {
        this.stream = stream;
    }

    public final S filter(@NonNull Predicate<T> predicate) {
        stream = stream.filter(predicate);
        return (S) this;
    }

    /**
     * Filter methods/constructors if they have public modifier.
     *
     * @return this
     */
    public final S filterPublic() {
        return filter(ModifierHelper::isPublic);
    }

    /**
     * Filter methods/constructors if they not have public modifier.
     *
     * @return this
     */
    public final S filterNonPublic() {
        return filter(ModifierHelper::isNotPublic);
    }

    /**
     * Filter methods/constructors if they have protected modifier.
     *
     * @return this
     */
    public final S filterProtected() {
        return filter(ModifierHelper::isProtected);
    }

    /**
     * Filter methods/constructors if they not have protected modifier.
     *
     * @return this
     */
    public final S filterNonProtected() {
        return filter(ModifierHelper::isNotProtected);
    }

    /**
     * Filter methods/constructors if they have private modifier.
     *
     * @return this
     */
    public final S filterPrivate() {
        return filter(ModifierHelper::isPrivate);
    }

    /**
     * Filter methods/constructors if they not have private modifier.
     *
     * @return this
     */
    public final S filterNonPrivate() {
        return filter(ModifierHelper::isNotPrivate);
    }

    /**
     * Filter methods/constructors if they have native modifier.
     *
     * @return this
     */
    public final S filterNative() {
        return filter(ModifierHelper::isNative);
    }

    /**
     * Filter methods/constructors if they have native modifier.
     *
     * @return this
     */
    public final S filterNonNative() {
        return filter(ModifierHelper::isNotNative);
    }

    /**
     * Filter methods/constructors if they are package private.
     *
     * @return this
     */
    public final S filterPackage() {
        return filter(ModifierHelper::isPackage);
    }

    /**
     * Filter methods/constructors if they are not package private.
     *
     * @return this
     */
    public final S filterNonPackage() {
        return filter(ModifierHelper::isNotPackage);
    }

    /**
     * Filter methods/constructors if they have vararg modifier.
     *
     * @return this
     */
    public final S filterVarargs() {
        return filter(ModifierHelper::isVarargs);
    }

    /**
     * Filter methods/constructors if they not have vararg modifier.
     *
     * @return this
     */
    public final S filterNonVarargs() {
        return filter(ModifierHelper::isNotVarargs);
    }

    /**
     * Filter methods/constructors if their modifiers include the specified modifiers.
     *
     * @param mod include modifiers
     * @return this
     */
    public final S filterIncludeModifiers(int mod) {
        return filter(m -> (m.getModifiers() & mod) != 0);
    }

    /**
     * Filter methods/constructors if their modifiers exclude the specified modifiers.
     *
     * @param mod exclude modifiers
     * @return this
     */
    public final S filterExcludeModifiers(int mod) {
        return filter(m -> (m.getModifiers() & mod) == 0);
    }

    /**
     * Filter methods/constructors by their exception types(Full-matches, cannot be null)
     *
     * @param e exception types
     * @return this
     */
    public final S filterByExceptionType(Class<?>... e) {
        var set1 = Set.of(e);

        return filter(m -> {
            var set2 = Set.of(getExceptionTypes(m));
            return set1.equals(set2);
        });
    }

    /**
     * Filter methods/constructors by parameter types, make sure length is same as target method parameter types length.
     *
     * @param parameterTypes parameter types, use null to skip check some parameters.
     * @return this
     */
    public final S filterByParameterTypes(Class<?>... parameterTypes) {
        return filter(member -> {
            var paramTypes = getParameterTypes(member);

            if (paramTypes.length != parameterTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                // ignore if null
                if (parameterTypes[i] == null)
                    continue;

                if (!paramTypes[i].equals(parameterTypes[i])) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Filter methods/constructors by parameter count.
     *
     * @param parameterCount parameter count
     * @return this
     */
    public final S filterByParameterCount(int parameterCount) {
        return filter(member -> getParameterTypes(member).length == parameterCount);
    }

    /**
     * Filter methods/constructors by parameter count with the predicate.
     *
     * @param predicate predicate
     * @return this
     */
    public final S filterByParameterCount(Predicate<Integer> predicate) {
        return filter(member -> predicate.test(getParameterTypes(member).length));
    }

    /**
     * For-each loop to consume stream.
     * <p>
     * <strong>NOTICE:</strong> This method will consume the stream, so you will not be able to continue using current MethodFinder after invoking this method.
     * </p>
     * If you want to do something with current remaining methods/constructors and use MethodFinder later, use {@link #onEach(Consumer)} instead.
     *
     * @see #onEach(Consumer)
     */
    public final void forEach(@NonNull Consumer<T> consumer) {
        stream.forEach(consumer);
    }

    /**
     * Do something with the current remaining methods.
     *
     * @param action action
     * @return this
     */
    @SuppressWarnings("UnusedReturnValue")
    public final S onEach(@NonNull Consumer<T> action) {
        stream = stream.peek(action);
        return (S) this;
    }

    /**
     * Collect the methods/constructor to a list.
     *
     * @return collected list
     */
    @NonNull
    public final List<T> toList() {
        return collect(Collectors.toList());
    }

    /**
     * Get the first method/constructor.
     *
     * @return first method/constructor
     * @throws NoSuchMethodException if the method/constructor not found
     */
    @NonNull
    public final T first() throws NoSuchMethodException {
        var m = stream.findFirst().orElse(null);
        if (m == null) {
            throw new NoSuchMethodException();
        }
        ((AccessibleObject) m).setAccessible(true);
        return m;
    }

    /**
     * Get the first method/constructor or null if not found.
     *
     * @return first method/constructor or null
     */
    @Nullable
    public final T firstOrNull() {
        var m = stream.findFirst().orElse(null);
        if (m == null) {
            return null;
        }
        ((AccessibleObject) m).setAccessible(true);
        return m;
    }

    protected abstract Class<?>[] getParameterTypes(T member);

    protected abstract Class<?>[] getExceptionTypes(T member);

    /**
     * Collect the methods/constructors.
     *
     * @param collector collector
     * @param <R>       the result type of the reduction operation
     * @param <A>       the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @return collected result
     */
    @NonNull
    public final <R, A> R collect(@NonNull Collector<T, A, R> collector) {
        onEach(member -> ((AccessibleObject) member).setAccessible(true));
        return stream.collect(collector);
    }
}
