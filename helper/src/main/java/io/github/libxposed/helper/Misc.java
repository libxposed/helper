package io.github.libxposed.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Member;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

final class TypeOnlyParameter implements HookBuilder.Parameter {

    @NonNull
    final private Class<?> type;

    final int index;

    TypeOnlyParameter(int index, @NonNull Class<?> type) {
        this.type = type;
        this.index = index;
    }

    @NonNull
    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NonNull
    @Override
    public Member getDeclaringExecutable() {
        throw new IllegalStateException("TypeOnlyParameter does not have a declaring executable");
    }
}

final class ParameterImpl implements HookBuilder.Parameter {
    final private int modifiers;
    @NonNull
    final private Class<?> type;

    final private int index;

    @NonNull
    final private Member declaringExecutable;

    ParameterImpl(int index, @NonNull Class<?> type, @NonNull Member declaringExecutable, int modifiers) {
        this.type = type;
        this.index = index;
        this.declaringExecutable = declaringExecutable;
        this.modifiers = modifiers;
    }

    @NonNull
    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @NonNull
    @Override
    public Member getDeclaringExecutable() {
        return declaringExecutable;
    }

    int getModifiers() {
        return modifiers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, type, declaringExecutable, modifiers);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        else if (!(obj instanceof ParameterImpl)) return false;
        ParameterImpl object = (ParameterImpl) obj;
        return index == object.index && Objects.equals(type, object.type) && Objects.equals(declaringExecutable, object.declaringExecutable) && modifiers == object.modifiers;
    }
}

abstract class SimpleExecutor {
    abstract <T> Future<T> submit(Callable<T> task);

    final Future<?> submit(Runnable task) {
        return submit(() -> {
            task.run();
            return null;
        });
    }
}

final class PendingExecutor extends SimpleExecutor {
    @NonNull
    final List<FutureTask<?>> pendingTasks = new ArrayList<>();

    @Override
    <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(task);
        pendingTasks.add(futureTask);
        return futureTask;
    }
}

interface BaseObserver {
}

interface MatchObserver<T> extends BaseObserver {
    void onMatch(@NonNull T result);
}

interface MissObserver extends BaseObserver {
    void onMiss();
}

interface Observer<T> extends MatchObserver<T>, MissObserver {
}

final class MatchCache {
    @NonNull
    HashMap<String, Object> cacheInfo = new HashMap<>();

    @NonNull
    ConcurrentHashMap<String, HashSet<String>> classListCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, HashSet<String>> fieldListCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, HashSet<String>> methodListCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, HashSet<String>> constructorListCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, HashSet<AbstractMap.SimpleEntry<Integer, String>>> parameterListCache = new ConcurrentHashMap<>();

    @NonNull
    ConcurrentHashMap<String, String> classCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, String> fieldCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, String> methodCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, String> constructorCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, AbstractMap.SimpleEntry<Integer, String>> parameterCache = new ConcurrentHashMap<>();
}

final class TreeSetView<T extends Comparable<T>> implements Set<T>, SortedSet<T>, NavigableSet<T> {
    final private T[] array;
    // array[start, end);
    final private int start;
    final private int end;

    static <T extends Comparable<T>> TreeSetView<T> ofSorted(T[] array) {
        return new TreeSetView<>(array, 0, array.length);
    }

    static <T extends Comparable<T>> TreeSetView<T> ofSorted(T[] array, int start, int end) {
        return new TreeSetView<>(array, start, end);
    }

    static <T extends Comparable<T>> TreeSetView<T> ofSorted(Collection<T> c) {
        //noinspection unchecked
        return new TreeSetView<>((T[]) c.toArray(new Comparable[0]), 0, c.size());
    }


    private TreeSetView(T[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    public Comparator<? super T> comparator() {
        return null;
    }

    @Override
    public TreeSetView<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public TreeSetView<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public TreeSetView<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        return isEmpty() ? null : array[start];
    }

    @Override
    public T last() {
        return isEmpty() ? null : array[end - 1];
    }

    @Override
    public int size() {
        return end - start;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(@Nullable Object o) {
        if (o == null) return false;
        if (size() == 0) return false;
        try {
            return Arrays.binarySearch(array, start, end, o) >= 0;
        } catch (ClassCastException ignored) {
            return false;
        }
    }

    @Override
    public T lower(T t) {
        var i = Arrays.binarySearch(array, start, end, t);
        if (i >= 0) {
            i = i - 1;
        } else {
            i = -i - 2;
        }
        return i >= start ? array[i] : null;
    }

    @Override
    public T floor(T t) {
        var i = Arrays.binarySearch(array, start, end, t);
        if (i >= 0) {
            return array[i];
        } else {
            i = -i - 2;
            return i >= start ? array[i] : null;
        }
    }

    @Override
    public T ceiling(T t) {
        var i = Arrays.binarySearch(array, start, end, t);
        if (i >= 0) {
            return array[i];
        } else {
            i = -i - 1;
            return i < end ? array[i] : null;
        }
    }

    @Override
    public T higher(T t) {
        var i = Arrays.binarySearch(array, start, end, t) + 1;
        i = i >= 0 ? i : -i;
        return i < end ? array[i] : null;
    }

    @Deprecated
    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("This is a read-only view.");
    }

    @Deprecated
    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("This is a read-only view.");
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            int index = start;

            @Override
            public boolean hasNext() {
                return index < end;
            }

            @Override
            public T next() {
                return array[index++];
            }
        };
    }

    @Override
    public NavigableSet<T> descendingSet() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Iterator<T> descendingIterator() {
        return new Iterator<>() {
            int index = end - 1;

            @Override
            public boolean hasNext() {
                return index >= start;
            }

            @Override
            public T next() {
                return array[index--];
            }
        };
    }

    @Override
    public TreeSetView<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int left = Arrays.binarySearch(array, start, end, fromElement);
        if (!fromInclusive && left >= 0) {
            left = left + 1;
        } else if (left < 0) {
            left = -left - 1;
        }
        int right = Arrays.binarySearch(array, start, end, toElement);
        if (toInclusive && right >= 0) {
            right = right + 1;
        } else if (right < 0) {
            right = -right - 1;
        }
        return new TreeSetView<>(array, left, Math.max(right, left));
    }

    @Override
    public TreeSetView<T> headSet(T toElement, boolean inclusive) {
        int right = Arrays.binarySearch(array, start, end, toElement);
        if (inclusive && right >= 0) {
            right = right + 1;
        } else if (right < 0) {
            right = -right - 1;
        }
        return new TreeSetView<>(array, start, right);
    }

    @Override
    public TreeSetView<T> tailSet(T fromElement, boolean inclusive) {
        int left = Arrays.binarySearch(array, start, end, fromElement);
        if (!inclusive && left >= 0) {
            left = left + 1;
        } else if (left < 0) {
            left = -left - 1;
        }
        return new TreeSetView<>(array, left, array.length);
    }

    @NonNull
    @Override
    public Object[] toArray() {
        var arr = new Comparable[array.length];
        System.arraycopy(array, start, arr, 0, size());
        return arr;
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] a) {
        if (a.length >= array.length) {
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(array, start, a, 0, size());
            return a;
        } else {
            //noinspection unchecked
            return (T1[]) toArray();
        }
    }

    @Deprecated
    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("This is a read-only view");
    }

    @Deprecated
    @Override
    public boolean remove(@Nullable Object o) {
        throw new UnsupportedOperationException("This is a read-only view");
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        if (c instanceof Set && c.size() > size()) return false;
        if (c.size() == 0) return true;
        if (size() == 0) return false;
        if (c instanceof TreeSetView) {
            var other = (TreeSetView<?>) c;
            var s = start;
            var p = other.start;
            while (p < other.end) {
                try {
                    s = Arrays.binarySearch(array, s, end, other.array[p]);
                } catch (ClassCastException ignored) {
                    return false;
                }
                if (s < 0) return false;
                p = p + 1;
            }
        } else {
            for (var o : c) {
                if (!contains(o)) return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        throw new UnsupportedOperationException("This is a read-only view");
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("This is a read-only view");
    }

    @Deprecated
    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("This is a read-only view");
    }

    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException("This is a read-only view");
    }

    public TreeSetView<T> merge(@NonNull TreeSetView<T> other) {
        if (other.size() == 0) {
            return this;
        } else if (size() == 0) {
            return other;
        }
        // noinspection unchecked
        var res = (T[]) new Comparable[array.length + other.array.length];
        int p = 0;
        int i = start, j = other.start;
        while (i < end && j < other.end) {
            var a = array[i];
            var b = other.array[j];
            int cmp = a.compareTo(b);
            if (cmp < 0) {
                res[p++] = a;
                i++;
            } else if (cmp > 0) {
                res[p++] = b;
                j++;
            } else {
                res[p++] = a;
                i++;
                j++;
            }
        }
        while (i < end) {
            res[p++] = array[i++];
        }
        while (j < other.end) {
            res[p++] = other.array[j++];
        }
        return new TreeSetView<>(res, 0, p);
    }
}

