package io.github.libxposed.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
        return index == object.index &&
                Objects.equals(type, object.type) &&
                Objects.equals(declaringExecutable, object.declaringExecutable) &&
                modifiers == object.modifiers;
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
    ConcurrentHashMap<String, HashSet<String>> parameterListCache = new ConcurrentHashMap<>();

    @NonNull
    ConcurrentHashMap<String, String> classCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, String> fieldCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, String> methodCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, String> constructorCache = new ConcurrentHashMap<>();
    @NonNull
    ConcurrentHashMap<String, String> parameterCache = new ConcurrentHashMap<>();
}


