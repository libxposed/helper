package io.github.libxposed.helper;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipFile;

import dalvik.system.BaseDexClassLoader;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.utils.DexParser;


// Matcher <-> LazySequence --> List<Observer -> Result -> Observer -> Result ... >
@SuppressLint("SoonBlockedPrivateApi")
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal", "JavaReflectionMemberAccess"})
final class HookBuilderImpl implements HookBuilder {
    @NonNull
    private final XposedInterface ctx;
    @NonNull
    private final BaseDexClassLoader classLoader;
    @NonNull
    private final String sourcePath;
    @NonNull
    private final Reflector reflector;
    @NonNull
    private final ConcurrentLinkedQueue<ClassMatcherImpl> rootClassMatchers = new ConcurrentLinkedQueue<>();
    @NonNull
    private final ConcurrentLinkedQueue<FieldMatcherImpl> rootFieldMatchers = new ConcurrentLinkedQueue<>();
    @NonNull
    private final ConcurrentLinkedQueue<MethodMatcherImpl> rootMethodMatchers = new ConcurrentLinkedQueue<>();
    @NonNull
    private final ConcurrentLinkedQueue<ConstructorMatcherImpl> rootConstructorMatchers = new ConcurrentLinkedQueue<>();
    @SuppressWarnings("ComparatorCombinators")
    @NonNull
    private final SortedSet<StringMatchImpl> stringMatches = new ConcurrentSkipListSet<>((o1, o2) -> o1.matcher.pattern.compareTo(o2.matcher.pattern));
    @NonNull
    private final HashMap<LazyBind, AtomicInteger> binds = new HashMap<>();
    @NonNull
    private final HashMap<String, ClassMatcherImpl> keyedClassMatchers = new HashMap<>();
    @NonNull
    private final HashMap<String, ParameterMatcherImpl> keyedParameterMatchers = new HashMap<>();
    @NonNull
    private final HashMap<String, FieldMatcherImpl> keyedFieldMatchers = new HashMap<>();
    @NonNull
    private final HashMap<String, MethodMatcherImpl> keyedMethodMatchers = new HashMap<>();
    @NonNull
    private final HashMap<String, ConstructorMatcherImpl> keyedConstructorMatchers = new HashMap<>();
    @NonNull
    private final HashMap<String, ClassMatchImpl> keyedClassMatches = new HashMap<>();
    @NonNull
    private final HashMap<String, ParameterMatchImpl> keyedParameterMatches = new HashMap<>();
    @NonNull
    private final HashMap<String, FieldMatchImpl> keyedFieldMatches = new HashMap<>();
    @NonNull
    private final HashMap<String, MethodMatchImpl> keyedMethodMatches = new HashMap<>();
    @NonNull
    private final HashMap<String, ConstructorMatchImpl> keyedConstructorMatches = new HashMap<>();
    @Nullable
    private Predicate<Throwable> exceptionHandler = null;
    @Nullable
    private Predicate<Map<String, Object>> cacheChecker = null;
    @Nullable
    private InputStream cacheInputStream = null;
    @Nullable
    private OutputStream cacheOutputStream = null;
    private boolean dexAnalysis = false;
    private boolean forceDexAnalysis = false;
    private boolean includeAnnotations = false;
    @NonNull
    private SimpleExecutor matchExecutor = new PendingExecutor();
    @Nullable
    private ExecutorService executorService = null;
    @NonNull
    private SimpleExecutor callbackExecutor = new PendingExecutor();
    @Nullable
    private Handler callbackHandler = null;
    @Nullable
    private MatchCache matchCache = null;

    HookBuilderImpl(@NonNull XposedInterface ctx, @NonNull BaseDexClassLoader classLoader, @NonNull String sourcePath) {
        this.ctx = ctx;
        this.classLoader = classLoader;
        this.sourcePath = sourcePath;
        reflector = new Reflector(classLoader);
    }

    @DexAnalysis
    @NonNull
    @Override
    public HookBuilder setForceDexAnalysis(boolean forceDexAnalysis) {
        this.forceDexAnalysis = forceDexAnalysis;
        return this;
    }

    @NonNull
    @Override
    public HookBuilder setExecutorService(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    @NonNull
    @Override
    public HookBuilder setCallbackHandler(@NonNull Handler callbackHandler) {
        this.callbackHandler = callbackHandler;
        return this;
    }

    @NonNull
    @Override
    public HookBuilder setCacheChecker(@NonNull Predicate<Map<String, Object>> cacheChecker) {
        this.cacheChecker = cacheChecker;
        return this;
    }

    @NonNull
    @Override
    public HookBuilder setCacheInputStream(@NonNull InputStream cacheInputStream) {
        this.cacheInputStream = cacheInputStream;
        return this;
    }

    @NonNull
    @Override
    public HookBuilder setCacheOutputStream(@NonNull OutputStream cacheOutputStream) {
        this.cacheOutputStream = cacheOutputStream;
        return this;
    }

    @NonNull
    @Override
    public HookBuilder setExceptionHandler(@NonNull Predicate<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    @NonNull
    @Override
    public MethodLazySequence methods(@NonNull Consumer<MethodMatcher> matcher) {
        final var m = new MethodMatcherImpl(null, false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher) {
        final var m = new MethodMatcherImpl(null, true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher) {
        final var m = new ConstructorMatcherImpl(null, false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher) {
        final var m = new ConstructorMatcherImpl(null, true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher) {
        final var m = new FieldMatcherImpl(null, false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher) {
        final var m = new FieldMatcherImpl(null, true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public ClassLazySequence classes(@NonNull Consumer<ClassMatcher> matcher) {
        final var m = new ClassMatcherImpl(null, false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public ClassMatch firstClass(@NonNull Consumer<ClassMatcher> matcher) {
        final var m = new ClassMatcherImpl(null, true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public StringMatch exact(@NonNull String string) {
        final var m = new StringMatcherImpl(string, true, false);
        return m.build();
    }

    @NonNull
    @Override
    public StringMatch prefix(@NonNull String prefix) {
        final var m = new StringMatcherImpl(prefix, false, true);
        return m.build();
    }

    @NonNull
    @Override
    public StringMatch firstPrefix(@NonNull String prefix) {
        final var m = new StringMatcherImpl(prefix, true, true);
        return m.build();
    }

    @NonNull
    @Override
    public ClassMatch exactClass(@NonNull String name) {
        final var m = new ClassMatcherImpl(null, true);
        Class<?> exact;
        try {
            exact = reflector.loadClass(name);
        } catch (ClassNotFoundException e) {
            exact = null;
        }
        return m.build(exact).first();
    }

    @NonNull
    @Override
    public ClassMatch exact(@NonNull Class<?> clazz) {
        final var m = new ClassMatcherImpl(null, true);
        return m.build(clazz).first();
    }

    @NonNull
    @Override
    public MethodMatch exactMethod(@NonNull String signature) {
        final var m = new MethodMatcherImpl(null, true);
        @Nullable Method method;
        try {
            method = reflector.loadMethod(signature);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            method = null;
        }
        return m.build(method).first();
    }

    @NonNull
    @Override
    public MethodMatch exact(@NonNull Method method) {
        final var m = new MethodMatcherImpl(null, true);
        return m.build(method).first();
    }

    @NonNull
    @Override
    public ConstructorMatch exactConstructor(@NonNull String signature) {
        final var m = new ConstructorMatcherImpl(null, true);
        @Nullable Constructor<?> constructor;
        try {
            constructor = reflector.loadConstructor(signature);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            constructor = null;
        }
        return m.build(constructor).first();
    }

    @NonNull
    @Override
    public ConstructorMatch exact(@NonNull Constructor<?> constructor) {
        final var m = new ConstructorMatcherImpl(null, true);
        return m.build(constructor).first();
    }

    @NonNull
    @Override
    public FieldMatch exactField(@NonNull String signature) {
        final var m = new FieldMatcherImpl(null, true);
        @Nullable Field field;
        try {
            field = reflector.loadField(signature);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            field = null;
        }
        return m.build(field).first();
    }

    @NonNull
    @Override
    public FieldMatch exact(@NonNull Field field) {
        final var m = new FieldMatcherImpl(null, true);
        return m.build(field).first();
    }

    public @NonNull Future<?> build() {
        dexAnalysis = dexAnalysis || forceDexAnalysis;
        loadMatchCache();

        var pendingTasks = ((PendingExecutor) matchExecutor).pendingTasks;

        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        matchExecutor = SimpleExecutor.of(executorService);

        for (var task : pendingTasks) {
            matchExecutor.submit(task);
        }
        try {
            matchExecutor.joinAll();
        } catch (Throwable e) {
            if (exceptionHandler != null) {
                exceptionHandler.test(e);
            }
        }

        pendingTasks = ((PendingExecutor) callbackExecutor).pendingTasks;

        if (callbackHandler != null) {
            callbackExecutor = SimpleExecutor.of(callbackHandler);
            for (var task : pendingTasks) {
                callbackExecutor.submit(task);
            }
        }

        if (dexAnalysis) {
            analysisDex();
        } else {
            analysisClassLoader();
        }
        return new Future<>() {
            private volatile boolean done = false;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return done;
            }

            private void runTasks(@NonNull FutureTask<?> task) {
                try {
                    task.run();
                } catch (Throwable e) {
                    if (exceptionHandler != null) {
                        exceptionHandler.test(e);
                    }
                }
            }

            @Override
            public Object get() throws ExecutionException, InterruptedException {
                if (callbackHandler != null && callbackHandler.getLooper() == Looper.myLooper()) {
                    throw new InterruptedException("dead lock");
                }
                if (callbackExecutor instanceof PendingExecutor) {
                    var pendingTasks = ((PendingExecutor) callbackExecutor).pendingTasks;
                    for (var task : pendingTasks) {
                        runTasks(task);
                    }
                } else {
                    callbackExecutor.joinAll();
                }
                done = true;
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
                var nanos = unit.toNanos(timeout);
                if (callbackHandler != null && callbackHandler.getLooper() == Looper.myLooper()) {
                    throw new InterruptedException("dead lock");
                }
                var now = System.nanoTime();
                if (callbackExecutor instanceof PendingExecutor) {
                    var pendingTasks = ((PendingExecutor) callbackExecutor).pendingTasks;
                    for (var task : pendingTasks) {
                        var last = now;
                        now = System.nanoTime();
                        nanos -= now - last;
                        if (nanos <= 0) throw new TimeoutException();
                        runTasks(task);
                    }
                } else {
                    callbackExecutor.joinAll(unit.convert(nanos - (System.nanoTime() - now), TimeUnit.NANOSECONDS), unit);
                }
                done = true;
                return null;
            }
        };
    }

    private void analysisDex() {
        DexParser[] parsers;
        try (var apk = new ZipFile(sourcePath)) {
            var tasks = new ArrayList<Future<DexParser>>();
            for (var i = 1; ; ++i) {
                var dex = apk.getEntry("classes" + (i == 1 ? "" : i) + ".dex");
                if (dex == null) break;
                tasks.add(matchExecutor.submit(() -> {
                    var buf = ByteBuffer.allocateDirect((int) dex.getSize());
                    try (var in = apk.getInputStream(dex)) {
                        if (in.read(buf.array()) != buf.capacity()) {
                            throw new IOException("read dex failed");
                        }
                    }
                    return ctx.parseDex(buf, false);
                }));
            }
            parsers = new DexParser[tasks.size()];
            for (var i = 0; i < parsers.length; ++i) {
                parsers[i] = tasks.get(i).get();
            }
        } catch (Throwable e) {
            if (exceptionHandler != null) exceptionHandler.test(e);
            return;
        }
        // match strings first
        for (var d = 0; d < parsers.length; ++d) {
            final int dexId = d;
            final var dex = parsers[dexId];
            matchExecutor.submit(() -> {
                var stringIds = dex.getStringId();
                int length = stringIds.length;
                var strings = new String[length];
                for (var i = 0; i < length; ++i) {
                    strings[i] = stringIds[i].getString();
                }
                int left = 0;
                for (var match : stringMatches) {
                    var matcher = match.matcher;
                    if (matcher.matchPrefix) {
                        left = Arrays.binarySearch(strings, left, length, matcher.pattern);
                        if (left < 0 && -left - 1 < length && strings[-left - 1].startsWith(matcher.pattern)) {
                            left = -left - 1;
                        }
                    } else {
                        left = Arrays.binarySearch(strings, left, length, matcher.pattern);
                    }
                    int right;
                    if (left < 0) {
                        left = right = -left - 1;
                    } else {
                        if (!matcher.matchFirst && matcher.matchPrefix) {
                            right = Arrays.binarySearch(strings, left + 1, length, matcher.pattern + Character.MAX_VALUE);
                            if (right < 0) {
                                right = -right - 1;
                            }
                        } else {
                            right = left + 1;
                        }
                    }
                    var arr = new int[right - left];
                    for (var i = left; i < right; ++i) {
                        arr[i - left] = i;
                    }
                    AtomicHelper.updateIfNullAndGet(match.dexMatches, () -> new int[length][])[dexId] = arr;
                }
            });
        }

        for (var d = 0; d < parsers.length; ++d) {
            final int dexId = d;
            final var dex = parsers[dexId];
            matchExecutor.submit(() -> dex.visitDefinedClasses(new DexParser.ClassVisitor() {
                @Override
                public DexParser.MemberVisitor visit(int clazz, int accessFlags, int superClass, @NonNull int[] interfaces, int sourceFile, @NonNull int[] staticFields, @NonNull int[] staticFieldsAccessFlags, @NonNull int[] instanceFields, @NonNull int[] instanceFieldsAccessFlags, @NonNull int[] directMethods, @NonNull int[] directMethodsAccessFlags, @NonNull int[] virtualMethods, @NonNull int[] virtualMethodsAccessFlags, @NonNull int[] annotations) {
                    return new FieldAndMethodVisitor() {
                        @Override
                        public void visit(int field, int accessFlags, @NonNull int[] annotations) {

                        }

                        @Override
                        public DexParser.MethodBodyVisitor visit(int method, int accessFlags, boolean hasBody, @NonNull int[] annotations, @NonNull int[] parameterAnnotations) {
                            return (ignored1, ignored2, referredStrings, invokedMethods, accessedFields, assignedFields, opcodes) -> {
                            };
                        }

                        @Override
                        public boolean stop() {
                            return false;
                        }
                    };
                }

                @Override
                public boolean stop() {
                    return false;
                }
            }));
        }
        try {
            matchExecutor.joinAll();
        } catch (Throwable e) {
            if (exceptionHandler != null) exceptionHandler.test(e);
        }
        for (var parser : parsers) {
            try {
                parser.close();
            } catch (IOException e) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(e);
                }
            }
        }
    }

    private TreeSetView<String> getAllClassNamesFromClassLoader() throws NoSuchFieldException, IllegalAccessException {
        TreeSetView<String> res = TreeSetView.ofSorted(new String[0]);
        @SuppressWarnings("JavaReflectionMemberAccess") @SuppressLint("DiscouragedPrivateApi") var pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        final var pathList = pathListField.get(classLoader);
        if (pathList == null) {
            throw new IllegalStateException("pathList is null");
        }
        final var dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        final var dexElements = (Object[]) dexElementsField.get(pathList);
        if (dexElements == null) {
            throw new IllegalStateException("dexElements is null");
        }
        for (final var dexElement : dexElements) {
            final var dexFileField = dexElement.getClass().getDeclaredField("dexFile");
            dexFileField.setAccessible(true);
            final var dexFile = dexFileField.get(dexElement);
            if (dexFile == null) {
                continue;
            }
            final var entriesField = dexFile.getClass().getDeclaredField("entries");
            entriesField.setAccessible(true);
            @SuppressWarnings("unchecked") final var entries = (Enumeration<String>) entriesField.get(dexFile);
            if (entries == null) {
                continue;
            }
            // entries are sorted
            // perform O(N) merge so that we can have a sorted result and remove duplicates
            res = res.merge(TreeSetView.ofSorted(Collections.list(entries)));
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private void loadMatchCache() {
        if (cacheInputStream == null && cacheOutputStream == null) {
            return;
        }
        matchCache = new MatchCache();
        try {
            if (cacheInputStream != null) {
                try (var in = new ObjectInputStream(cacheInputStream)) {
                    matchCache.cacheInfo = (HashMap<String, Object>) in.readObject();
                    matchCache.classListCache = (ConcurrentHashMap<String, HashSet<String>>) in.readObject();
                    matchCache.methodListCache = (ConcurrentHashMap<String, HashSet<String>>) in.readObject();
                    matchCache.fieldListCache = (ConcurrentHashMap<String, HashSet<String>>) in.readObject();
                    matchCache.constructorListCache = (ConcurrentHashMap<String, HashSet<String>>) in.readObject();
                    matchCache.parameterListCache = (ConcurrentHashMap<String, HashSet<AbstractMap.SimpleEntry<Integer, String>>>) in.readObject();

                    matchCache.classCache = (ConcurrentHashMap<String, String>) in.readObject();
                    matchCache.methodCache = (ConcurrentHashMap<String, String>) in.readObject();
                    matchCache.fieldCache = (ConcurrentHashMap<String, String>) in.readObject();
                    matchCache.constructorCache = (ConcurrentHashMap<String, String>) in.readObject();
                    matchCache.parameterCache = (ConcurrentHashMap<String, AbstractMap.SimpleEntry<Integer, String>>) in.readObject();
                }
            }
            if (cacheChecker != null) {
                var info = matchCache.cacheInfo;
                if (!cacheChecker.test(info)) {
                    matchCache = new MatchCache();
                    matchCache.cacheInfo = info;
                }
            } else {
                var oldObj = matchCache.cacheInfo.get("lastModifyTime");
                var old = oldObj instanceof Long ? (long) oldObj : 0;
                var now = new File(sourcePath).lastModified();
                if (old != now) {
                    matchCache = new MatchCache();
                    matchCache.cacheInfo.put("lastModifyTime", now);
                }
            }
        } catch (Throwable e) {
            if (exceptionHandler != null) {
                exceptionHandler.test(e);
            }
            matchCache = new MatchCache();
        }
        for (var e : matchCache.classCache.entrySet()) {
            var hit = keyedClassMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                hit.match(cache.isEmpty() ? null : reflector.loadClass(cache));
            } catch (Throwable ex) {
                hit.match(null);
            }
        }
        for (var e : matchCache.methodCache.entrySet()) {
            var hit = keyedMethodMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                hit.match(cache.isEmpty() ? null : reflector.loadMethod(cache));
            } catch (Throwable ex) {
                hit.match(null);
            }
        }

        for (var e : matchCache.fieldCache.entrySet()) {
            var hit = keyedFieldMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                hit.match(cache.isEmpty() ? null : reflector.loadField(cache));
            } catch (Throwable ex) {
                hit.match(null);
            }
        }

        for (var e : matchCache.constructorCache.entrySet()) {
            var hit = keyedConstructorMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                hit.match(cache.isEmpty() ? null : reflector.loadConstructor(cache));
            } catch (Throwable ex) {
                hit.match(null);
            }
        }

        for (var e : matchCache.parameterCache.entrySet()) {
            var hit = keyedParameterMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                var methodName = cache.getValue();
                var idx = cache.getKey();
                if (methodName.isEmpty()) {
                    hit.match(null);
                    continue;
                }
                var m = reflector.loadMethod(methodName);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    var p = m.getParameters()[idx];
                    hit.match(new ParameterImpl(idx, p.getType(), m, p.getModifiers()));
                } else {
                    var p = m.getParameterTypes()[idx];
                    hit.match(new ParameterImpl(idx, p, m, 0));
                }
            } catch (Throwable ex) {
                hit.match(null);
            }
        }

        for (var e : matchCache.classListCache.entrySet()) {
            var hit = keyedClassMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.match(Collections.emptyList());
                hit.match(reflector.loadClasses(value));
            } catch (Throwable ex) {
                hit.match(Collections.emptyList());
            }
        }

        for (var e : matchCache.methodListCache.entrySet()) {
            var hit = keyedMethodMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.match(Collections.emptyList());
                hit.match(reflector.loadMethods(value));
            } catch (Throwable ex) {
                hit.match(Collections.emptyList());
            }
        }

        for (var e : matchCache.fieldListCache.entrySet()) {
            var hit = keyedFieldMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.match(Collections.emptyList());
                hit.match(reflector.loadFields(value));
            } catch (Throwable ex) {
                hit.match(Collections.emptyList());
            }
        }

        for (var e : matchCache.constructorListCache.entrySet()) {
            var hit = keyedConstructorMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.match(Collections.emptyList());
                hit.match(reflector.loadConstructors(value));
            } catch (Throwable ex) {
                hit.match(Collections.emptyList());
            }
        }

        for (var e : matchCache.parameterListCache.entrySet()) {
            var hit = keyedParameterMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.match(Collections.emptyList());
                var parameters = new ArrayList<Parameter>();
                for (var v : value) {
                    var methodName = v.getValue();
                    var idx = v.getKey();
                    if (methodName.isEmpty()) continue;
                    var m = reflector.loadMethod(methodName);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        var p = m.getParameters()[idx];
                        parameters.add(new ParameterImpl(idx, p.getType(), m, p.getModifiers()));
                    } else {
                        var p = m.getParameterTypes()[idx];
                        parameters.add(new ParameterImpl(idx, p, m, 0));
                    }
                }
                hit.match(parameters);
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }
    }

    private <Reflect extends Member> void memberClassLists(MemberMatcherImpl<?, ?, Reflect, ?, ?> matcher, Transformer<Class<?>, Reflect[]> transformer) {
        final ArrayList<Class<?>> classList = new ArrayList<>();
        if (matcher.declaringClass != null) {
            var match = matcher.declaringClass.match.get();
            if (match.reflect != null) classList.add(match.reflect);
        } else if (exceptionHandler != null) {
            exceptionHandler.test(new IllegalStateException("Match members without declaring class is not supported when not using dex analysis; set forceDexAnalysis to true to enable dex analysis."));
        }

        var candidates = new ArrayList<Reflect>();
        for (final var clazz : classList) {
            final var reflects = transformer.transform(clazz);
            candidates.addAll(List.of(reflects));
        }
        matcher.doMatch(candidates);
    }

    private void analysisClassLoader() {
        final TreeSetView<String> classNames;
        try {
            classNames = getAllClassNamesFromClassLoader();
        } catch (Throwable e) {
            if (exceptionHandler != null) {
                exceptionHandler.test(e);
            }
            return;
        }

        final boolean[] hasMatched = new boolean[]{false};
        do {
            // match class first
            for (final var classMatcher : rootClassMatchers) {
                // not leaf
                if (classMatcher.leafCount.get() != 1) continue;
                if (classMatcher.pending) continue;
                hasMatched[0] = rootClassMatchers.remove(classMatcher) || hasMatched[0];
                matchExecutor.submit(() -> {
                    TreeSetView<String> subset = classNames;
                    if (classMatcher.name != null) {
                        final var nameMatcher = classMatcher.name.matcher;
                        if (nameMatcher.matchPrefix) {
                            subset = classNames.subSet(nameMatcher.pattern, nameMatcher.pattern + Character.MAX_VALUE);
                        } else if (classNames.contains(nameMatcher.pattern)) {
                            subset = TreeSetView.ofSorted(new String[]{nameMatcher.pattern});
                        } else {
                            subset = TreeSetView.ofSorted(new String[0]);
                        }
                    }
                    final ArrayList<Class<?>> candidates = new ArrayList<>(subset.size());
                    for (final var className : subset) {
                        // then check the rest conditions that need to load the class
                        final Class<?> theClass;
                        try {
                            theClass = Class.forName(className, false, classLoader);
                            candidates.add(theClass);
                        } catch (ClassNotFoundException e) {
                            if (exceptionHandler != null && !exceptionHandler.test(e)) {
                                break;
                            }
                        }
                    }
                    classMatcher.doMatch(candidates);
                });
            }
            for (final var fieldMatcher : rootFieldMatchers) {
                // not leaf
                if (fieldMatcher.leafCount.get() != 1) continue;
                if (fieldMatcher.pending) continue;
                hasMatched[0] = rootFieldMatchers.remove(fieldMatcher) || hasMatched[0];
                matchExecutor.submit(() -> memberClassLists(fieldMatcher, Class::getDeclaredFields));
            }

            for (final var methodMatcher : rootMethodMatchers) {
                // not leaf
                if (methodMatcher.leafCount.get() != 1) continue;
                if (methodMatcher.pending) continue;
                hasMatched[0] = rootMethodMatchers.remove(methodMatcher) || hasMatched[0];
                matchExecutor.submit(() -> memberClassLists(methodMatcher, Class::getDeclaredMethods));
            }

            for (final var constructorMatcher : rootConstructorMatchers) {
                // not leaf
                if (constructorMatcher.leafCount.get() != 1) continue;
                if (constructorMatcher.pending) continue;
                hasMatched[0] = rootConstructorMatchers.remove(constructorMatcher) || hasMatched[0];
                matchExecutor.submit(() -> memberClassLists(constructorMatcher, Class::getDeclaredConstructors));
            }
            try {
                matchExecutor.joinAll();
            } catch (Throwable e) {
                if (exceptionHandler != null) {
                    if (!exceptionHandler.test(e)) break;
                }
            }
        } while (hasMatched[0]);
    }

    private abstract static class BaseMatcherImpl<Self extends BaseMatcherImpl<Self, Reflect, DexId>, Reflect, DexId extends DexParser.Id<DexId>> {
        protected final boolean matchFirst;

        protected BaseMatcherImpl(boolean matchFirst) {
            this.matchFirst = matchFirst;
        }
    }

    private abstract static class BaseMatchImpl<Self extends BaseMatchImpl<Self, Base, Reflect>, Base extends BaseMatch<Base, Reflect>, Reflect> implements BaseMatch<Base, Reflect> {
    }

    @SuppressWarnings("unchecked")
    private abstract class ReflectMatcherImpl<Self extends ReflectMatcherImpl<Self, Base, Reflect, DexId, SeqImpl>, Base extends ReflectMatcher<Base>, Reflect, DexId extends DexParser.Id<DexId>, SeqImpl extends LazySequenceImpl<?, ?, Reflect, Base, ?, Self, DexId>> extends BaseMatcherImpl<Self, Reflect, DexId> implements ReflectMatcher<Base> {
        private final static int packageFlag = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;

        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher;
        @NonNull
        protected final AtomicInteger leafCount = new AtomicInteger(1);
        @NonNull
        protected final AtomicReference<Collection<Reflect>> candidates = new AtomicReference<>(null);
        @NonNull
        protected final AtomicReference<Collection<DexId>[]> dexCandidates = new AtomicReference<>(null);
        @Nullable
        protected String key = null;
        protected int includeModifiers = 0; // (real & includeModifiers) == includeModifiers
        protected int excludeModifiers = 0; // (real & excludeModifiers) == 0
        protected volatile boolean pending = true;
        @Nullable
        private volatile SeqImpl lazySequence = null;
        private final BaseObserver<?> dependencyCallback = (BaseObserver<Object>) result -> {
            if (leafCount.decrementAndGet() == 0) doMatch();
        };

        protected ReflectMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(matchFirst);
            this.rootMatcher = rootMatcher == null ? this : rootMatcher;
        }

        protected final synchronized void ensureNotFinalized() {
            if (lazySequence != null) {
                throw new IllegalStateException("Cannot modify after finalized");
            }
        }

        protected final synchronized SeqImpl build() {
            final var lazySequence = onBuild();
            // specially, if matchFirst is true, propagate the key to the first match
            if (matchFirst && key != null) {
                final var f = lazySequence.first().setKey(key);
            }
            if (!pending) {
                setNonPending();
            }
            return this.lazySequence = lazySequence;
        }

        protected final synchronized SeqImpl build(@Nullable Reflect exact) {
            pending = true;
            leafCount.set(0);
            var seq = build();
            if (exact != null) {
                seq.matches.set(Collections.singletonList(exact));
            }
            return seq;
        }

        @NonNull
        @Override
        public final Base setKey(@NonNull String key) {
            ensureNotFinalized();
            pending = false;
            this.key = key;
            return (Base) this;
        }

        protected final void setModifier(boolean set, int flags) {
            ensureNotFinalized();
            if (set) {
                includeModifiers |= flags;
                excludeModifiers &= ~flags;
            } else {
                includeModifiers &= ~flags;
                excludeModifiers |= flags;
            }
        }

        @NonNull
        @Override
        public final Base setIsPublic(boolean isPublic) {
            setModifier(isPublic, Modifier.PUBLIC);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsPrivate(boolean isPrivate) {
            setModifier(isPrivate, Modifier.PRIVATE);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsProtected(boolean isProtected) {
            setModifier(isProtected, Modifier.PROTECTED);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsPackage(boolean isPackage) {
            setModifier(!isPackage, packageFlag);
            return (Base) this;
        }

        @NonNull
        protected abstract SeqImpl onBuild();

        protected abstract void setNonPending();

        @NonNull
        protected final <T extends ReflectMatchImpl<T, U, RR, ?, ?, D>, U extends ReflectMatch<U, RR, ?>, RR, D extends DexParser.Id<D>> T addDependency(@Nullable T field, @NonNull U input) {
            final var in = (T) input;
            if (field != null) {
                in.removeObserver((BaseObserver<RR>) dependencyCallback);
            } else {
                leafCount.incrementAndGet();
            }
            in.addObserver((BaseObserver<RR>) dependencyCallback);
            return in;
        }

        @NonNull
        protected final <T extends ReflectSyntaxImpl<M, ?, RR>, U extends Syntax<M>, M extends ReflectMatch<M, RR, ?>, RR> T addDependencies(@Nullable T field, @NonNull U input) {
            final var in = (T) input;
            if (field != null) {
                field.removeObserver(dependencyCallback, leafCount);
            }
            in.addObserver(dependencyCallback, leafCount);
            return in;
        }

        protected final void match(@NonNull Collection<Reflect> matches) {
            final var lazySequence = this.lazySequence;
            if (lazySequence == null) {
                throw new IllegalStateException("Illegal state when doMatch");
            }
            leafCount.set(0);
            lazySequence.match(matches);
        }

        private void doMatch() {
            final var candidates = this.candidates.getAndSet(null);
            if (candidates != null) {
                final var matches = new ArrayList<Reflect>();
                for (final var candidate : candidates) {
                    if (doMatch(candidate)) {
                        matches.add(candidate);
                        if (matchFirst) {
                            break;
                        }
                    }
                }
                match(matches);
            }
        }

        // do match on reflect
        protected final void doMatch(@NonNull Collection<Reflect> candidates) {
            var leafCount = this.leafCount.decrementAndGet();
            if (leafCount >= 0) {
                this.candidates.compareAndSet(null, candidates);
            }
            if (leafCount == 0) doMatch();
        }

        @CallSuper
        protected boolean doMatch(@NonNull Reflect reflect) {
            final int modifiers;
            if (reflect instanceof Class<?>) modifiers = ((Class<?>) reflect).getModifiers();
            else if (reflect instanceof Member) modifiers = ((Member) reflect).getModifiers();
            else if (reflect instanceof ParameterImpl)
                modifiers = ((ParameterImpl) reflect).getModifiers();
            else modifiers = 0;
            if ((modifiers & includeModifiers) != includeModifiers) return false;
            return (modifiers & excludeModifiers) == 0;
        }
    }

    private final class ClassMatcherImpl extends ReflectMatcherImpl<ClassMatcherImpl, ClassMatcher, Class<?>, DexParser.TypeId, ClassLazySequenceImpl> implements ClassMatcher {
        @Nullable
        private ClassMatchImpl superClass = null;

        @Nullable
        private StringMatchImpl name = null;

        @Nullable
        private ReflectSyntaxImpl<ClassMatch, ?, Class<?>> containsInterfaces = null;

        private ClassMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @NonNull
        @Override
        protected ClassLazySequenceImpl onBuild() {
            if (key != null) keyedClassMatchers.put(key, this);
            if (rootMatcher != this) rootClassMatchers.add(this);
            return new ClassLazySequenceImpl(rootMatcher);
        }

        @CallSuper
        @Override
        protected void setNonPending() {
            if (superClass != null) superClass.rootMatcher.setNonPending();
            if (containsInterfaces != null) containsInterfaces.setNonPending();
        }

        @Override
        protected boolean doMatch(@NonNull Class<?> theClass) {
            if (!super.doMatch(theClass)) return false;
            if (superClass != null) {
                final var superClass = theClass.getSuperclass();
                final var superClassMatch = this.superClass.match.get();
                return superClass != null && superClassMatch.reflect != null && superClass == superClassMatch.reflect;
            }
            if (containsInterfaces != null) {
                final var ifArray = theClass.getInterfaces();
                final var ifs = new HashSet<Class<?>>(ifArray.length);
                Collections.addAll(ifs, ifArray);
                return containsInterfaces.test(ifs);
            }
            return true;
        }

        @NonNull
        @Override
        public ClassMatcher setName(@NonNull StringMatch name) {
            ensureNotFinalized();
            this.name = (StringMatchImpl) name;
            return this;
        }

        @NonNull
        @Override
        public ClassMatcher setSuperClass(@NonNull ClassMatch superClassMatch) {
            ensureNotFinalized();
            this.superClass = addDependency(this.superClass, superClassMatch);
            return this;
        }

        @NonNull
        @Override
        public ClassMatcher setContainsInterfaces(@NonNull Syntax<ClassMatch> consumer) {
            ensureNotFinalized();
            this.containsInterfaces = addDependencies(this.containsInterfaces, consumer);
            return this;
        }

        @NonNull
        @Override
        public ClassMatcher setIsAbstract(boolean isAbstract) {
            setModifier(isAbstract, Modifier.ABSTRACT);
            return this;
        }

        @NonNull
        @Override
        public ClassMatcher setIsStatic(boolean isStatic) {
            setModifier(isStatic, Modifier.STATIC);
            return this;
        }

        @NonNull
        @Override
        public ClassMatcher setIsFinal(boolean isFinal) {
            setModifier(isFinal, Modifier.FINAL);
            return this;
        }

        @NonNull
        @Override
        public ClassMatcher setIsInterface(boolean isInterface) {
            setModifier(isInterface, Modifier.INTERFACE);
            return this;
        }
    }

    private final class ParameterMatcherImpl extends ReflectMatcherImpl<ParameterMatcherImpl, ParameterMatcher, Parameter, DexParser.TypeId, ParameterLazySequenceImpl> implements ParameterMatcher {
        private int index = -1;

        @Nullable
        private ClassMatchImpl type = null;

        private ParameterMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @NonNull
        @Override
        protected ParameterLazySequenceImpl onBuild() {
            if (key != null) keyedParameterMatchers.put(key, this);
//            if (rootMatcher != this) rootParameterMatchers.add(this);
            return new ParameterLazySequenceImpl(rootMatcher);
        }

        @Override
        protected void setNonPending() {
            if (type != null) type.rootMatcher.setNonPending();
        }

        @Override
        protected boolean doMatch(@NonNull Parameter parameter) {
            if (!super.doMatch(parameter)) return false;
            if (index >= 0 && index != parameter.getIndex()) return false;
            if (type == null) return true;
            var typeMatch = type.match.get();
            return typeMatch != null && typeMatch.reflect == parameter.getType();
        }

        @NonNull
        @Override
        public ParameterMatcher setIndex(int index) {
            ensureNotFinalized();
            this.index = index;
            return this;
        }

        @NonNull
        @Override
        public ParameterMatcher setType(@NonNull ClassMatch type) {
            ensureNotFinalized();
            this.type = addDependency(this.type, type);
            return this;
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        @Override
        public ParameterMatcher setIsFinal(boolean isFinal) {
            setModifier(isFinal, Modifier.FINAL);
            return this;
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        @Override
        public ParameterMatcher setIsSynthetic(boolean isSynthetic) {
            setModifier(isSynthetic, 0x00001000);
            return this;
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        @Override
        public ParameterMatcher setIsVarargs(boolean isVarargs) {
            setModifier(isVarargs, 0x00000080);
            return this;
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @NonNull
        @Override
        public ParameterMatcher setIsImplicit(boolean isImplicit) {
            setModifier(isImplicit, 0x00008000);
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class MemberMatcherImpl<Self extends MemberMatcherImpl<Self, Base, Reflect, DexId, SeqImpl>, Base extends MemberMatcher<Base>, Reflect extends Member, DexId extends DexParser.Id<DexId>, SeqImpl extends MemberLazySequenceImpl<?, ?, Reflect, Base, ?, Self, DexId>> extends ReflectMatcherImpl<Self, Base, Reflect, DexId, SeqImpl> implements MemberMatcher<Base> {
        @Nullable
        protected ClassMatchImpl declaringClass = null;

        protected boolean includeSuper = false;

        protected boolean includeInterface = false;

        protected MemberMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @CallSuper
        @Override
        protected void setNonPending() {
            if (declaringClass != null) declaringClass.rootMatcher.setNonPending();
        }

        @Override
        protected boolean doMatch(@NonNull Reflect reflect) {
            if (!super.doMatch(reflect)) return false;
            if (declaringClass == null) return true;
            final var declaringClass = this.declaringClass.match.get();
            return declaringClass != null && declaringClass.reflect == reflect.getDeclaringClass();
        }

        @NonNull
        @Override
        public final Base setDeclaringClass(@NonNull ClassMatch declaringClassMatch) {
            ensureNotFinalized();
            this.declaringClass = addDependency(this.declaringClass, declaringClassMatch);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsSynthetic(boolean isSynthetic) {
            setModifier(isSynthetic, 0x00001000);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIncludeSuper(boolean includeSuper) {
            ensureNotFinalized();
            this.includeSuper = includeSuper;
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIncludeInterface(boolean includeInterface) {
            ensureNotFinalized();
            this.includeInterface = includeInterface;
            return (Base) this;
        }
    }

    private final class FieldMatcherImpl extends MemberMatcherImpl<FieldMatcherImpl, FieldMatcher, Field, DexParser.FieldId, FieldLazySequenceImpl> implements HookBuilder.FieldMatcher {
        @Nullable
        private StringMatchImpl name = null;

        @Nullable
        private ClassMatchImpl type = null;

        private FieldMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @Override
        protected void setNonPending() {
            super.setNonPending();
            if (type != null) type.rootMatcher.setNonPending();
        }

        @NonNull
        @Override
        protected FieldLazySequenceImpl onBuild() {
            if (key != null) keyedFieldMatchers.put(key, this);
            if (rootMatcher != this) rootFieldMatchers.add(this);
            return new FieldLazySequenceImpl(rootMatcher);
        }

        @Override
        protected boolean doMatch(@NonNull Field field) {
            if (!super.doMatch(field)) return false;
            if (name != null && !name.test(field.getName())) return false;
            if (type == null) return true;
            var typeMatch = type.match.get();
            return typeMatch != null && typeMatch.reflect == field.getType();
        }

        @NonNull
        @Override
        public FieldMatcher setName(@NonNull StringMatch name) {
            ensureNotFinalized();
            this.name = (StringMatchImpl) name;
            return this;
        }

        @NonNull
        @Override
        public FieldMatcher setType(@NonNull ClassMatch type) {
            ensureNotFinalized();
            this.type = addDependency(this.type, type);
            return this;
        }

        @NonNull
        @Override
        public FieldMatcher setIsStatic(boolean isStatic) {
            setModifier(isStatic, Modifier.STATIC);
            return this;
        }

        @NonNull
        @Override
        public FieldMatcher setIsFinal(boolean isFinal) {
            setModifier(isFinal, Modifier.FINAL);
            return this;
        }

        @NonNull
        @Override
        public FieldMatcher setIsTransient(boolean isTransient) {
            setModifier(isTransient, Modifier.TRANSIENT);
            return this;
        }

        @NonNull
        @Override
        public FieldMatcher setIsVolatile(boolean isVolatile) {
            setModifier(isVolatile, Modifier.VOLATILE);
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class ExecutableMatcherImpl<Self extends ExecutableMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends ExecutableMatcher<Base>, Reflect extends Member, SeqImpl extends ExecutableLazySequenceImpl<?, ?, Reflect, Base, ?, Self>> extends MemberMatcherImpl<Self, Base, Reflect, DexParser.MethodId, SeqImpl> implements ExecutableMatcher<Base> {
        protected int parameterCount = -1;

        @Nullable
        protected ReflectSyntaxImpl<ClassMatch, ?, Class<?>> parameterTypes = null;

        @Nullable
        protected ReflectSyntaxImpl<ParameterMatch, ?, Parameter> parameters = null;

        @Nullable
        protected StringSyntaxImpl referredStrings = null;

        @Nullable
        protected ReflectSyntaxImpl<FieldMatch, ?, Field> assignedFields = null;

        @Nullable
        protected ReflectSyntaxImpl<FieldMatch, ?, Field> accessedFields = null;

        @Nullable
        protected ReflectSyntaxImpl<MethodMatch, ?, Method> invokedMethods = null;

        @Nullable
        protected ReflectSyntaxImpl<ConstructorMatch, ?, Constructor<?>> invokedConstructors = null;

        @Nullable
        protected byte[] opcodes = null;

        protected ExecutableMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @Override
        protected void setNonPending() {
            super.setNonPending();
            if (parameterTypes != null) parameterTypes.setNonPending();
            if (assignedFields != null) assignedFields.setNonPending();
            if (accessedFields != null) accessedFields.setNonPending();
            if (invokedMethods != null) invokedMethods.setNonPending();
            if (invokedConstructors != null) invokedConstructors.setNonPending();
        }

        @Override
        protected boolean doMatch(@NonNull Reflect reflect) {
            if (!super.doMatch(reflect)) return false;
            final int parameterCount;
            if (reflect instanceof Method) {
                parameterCount = ((Method) reflect).getParameterTypes().length;
            } else if (reflect instanceof Constructor) {
                parameterCount = ((Constructor<?>) reflect).getParameterTypes().length;
            } else {
                return false;
            }
            return this.parameterCount == -1 || this.parameterCount == parameterCount;
        }

        @NonNull
        @Override
        public final Base setParameterCount(int count) {
            ensureNotFinalized();
            this.parameterCount = count;
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setParameters(@NonNull Syntax<ParameterMatch> parameterTypes) {
            ensureNotFinalized();
            this.parameters = addDependencies(this.parameters, parameterTypes);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setReferredStrings(@NonNull Syntax<StringMatch> referredStrings) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.referredStrings = (StringSyntaxImpl) referredStrings;
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setAssignedFields(@NonNull Syntax<FieldMatch> assignedFields) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.assignedFields = addDependencies(this.assignedFields, assignedFields);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setAccessedFields(@NonNull Syntax<FieldMatch> accessedFields) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.accessedFields = addDependencies(this.accessedFields, accessedFields);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setInvokedMethods(@NonNull Syntax<MethodMatch> invokedMethods) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.invokedMethods = addDependencies(this.invokedMethods, invokedMethods);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setInvokedConstructors(@NonNull Syntax<ConstructorMatch> invokedConstructors) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.invokedConstructors = addDependencies(this.invokedConstructors, invokedConstructors);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setContainsOpcodes(@NonNull byte[] opcodes) {
            ensureNotFinalized();
            dexAnalysis = true;
            if (opcodes.length == 0) return (Base) this;
            // prepare for KMP
            int M = opcodes.length;
            byte[] lps = new byte[M];
            int l = 0;
            int i = 1;
            while (i < M) {
                if (opcodes[i] == opcodes[l]) {
                    l++;
                    lps[i] = (byte) l;
                    i++;
                } else {
                    if (l != 0) {
                        l = lps[l - 1];
                    } else {
                        lps[i] = 0;
                        i++;
                    }
                }
            }
            this.opcodes = new byte[2 * M];
            System.arraycopy(opcodes, 0, this.opcodes, 0, M);
            System.arraycopy(lps, 0, this.opcodes, M, M);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsVarargs(boolean isVarargs) {
            setModifier(isVarargs, 0x00000080);
            return (Base) this;
        }

        @NonNull
        @Override
        public Syntax<ParameterMatch> conjunction(@NonNull ClassMatch... types) {
            var b = new LazyBind() {
                ParameterLazySequenceImpl s = new ParameterLazySequenceImpl(rootMatcher);
                ArrayList<Parameter> p = new ArrayList<>(types.length);

                @Override
                public void onMatch() {
                    s.match(p);
                }

                @Override
                public void onMiss() {
                    s.match(Collections.emptyList());
                }
            };
            for (int i = 0; i < types.length; i++) {
                var type = types[i];
                if (type == null) continue;
                final var pi = i;
                type.bind(b, (bb, c) -> {
                    var p = new TypeOnlyParameter(pi, c);
                });
            }
            return b.s.conjunction();
        }

        @NonNull
        @Override
        public Syntax<ParameterMatch> conjunction(@NonNull Class<?>... types) {
            ParameterLazySequenceImpl s = new ParameterLazySequenceImpl(rootMatcher);
            ArrayList<Parameter> p = new ArrayList<>(types.length);
            for (int i = 0; i < types.length; i++) {
                var type = types[i];
                if (type == null) continue;
                p.add(new TypeOnlyParameter(i, type));
            }
            s.match(p);
            return s.conjunction();
        }

        @NonNull
        @Override
        public Syntax<ParameterMatch> observe(int index, @NonNull ClassMatch types) {
            final var m = new ParameterMatchImpl(rootMatcher);
            m.index = index;
            types.onMatch(c -> m.match(new TypeOnlyParameter(index, c)));
            return m.observe();
        }

        @NonNull
        @Override
        public Syntax<ParameterMatch> observe(int index, @NonNull Class<?> types) {
            final var m = new ParameterMatchImpl(rootMatcher);
            m.index = index;
            m.match(new TypeOnlyParameter(index, types));
            return m.observe();
        }

        @NonNull
        @Override
        public ParameterMatch firstParameter(@NonNull Consumer<ParameterMatcher> consumer) {
            ensureNotFinalized();
            var m = new ParameterMatcherImpl(rootMatcher, true);
            consumer.accept(m);
            return m.build().first();
        }

        @NonNull
        @Override
        public ParameterLazySequence parameters(@NonNull Consumer<ParameterMatcher> consumer) {
            ensureNotFinalized();
            var m = new ParameterMatcherImpl(rootMatcher, false);
            consumer.accept(m);
            return m.build();
        }
    }

    private final class MethodMatcherImpl extends ExecutableMatcherImpl<MethodMatcherImpl, MethodMatcher, Method, MethodLazySequenceImpl> implements MethodMatcher {
        private @Nullable StringMatchImpl name = null;

        private @Nullable ClassMatchImpl returnType = null;

        private MethodMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @Override
        protected void setNonPending() {
            super.setNonPending();
            if (returnType != null) returnType.rootMatcher.setNonPending();
        }

        @Override
        protected boolean doMatch(@NonNull Method method) {
            if (!super.doMatch(method)) return false;
            if (name != null && !name.test(method.getName())) return false;
            if (returnType == null) return true;
            var returnTypeMatch = returnType.match.get();
            return returnTypeMatch != null && returnTypeMatch.reflect == method.getReturnType();
        }

        @NonNull
        @Override
        protected MethodLazySequenceImpl onBuild() {
            if (key != null) keyedMethodMatchers.put(key, this);
            if (rootMatcher != this) rootMethodMatchers.add(this);
            return new MethodLazySequenceImpl(rootMatcher);
        }

        @NonNull
        @Override
        public MethodMatcher setName(@NonNull StringMatch name) {
            ensureNotFinalized();
            this.name = (StringMatchImpl) name;
            return this;
        }

        @NonNull
        @Override
        public MethodMatcher setReturnType(@NonNull ClassMatch returnType) {
            ensureNotFinalized();
            this.returnType = addDependency(this.returnType, returnType);
            return this;
        }

        @NonNull
        @Override
        public MethodMatcher setIsAbstract(boolean isAbstract) {
            setModifier(isAbstract, Modifier.ABSTRACT);
            return this;
        }

        @NonNull
        @Override
        public MethodMatcher setIsStatic(boolean isStatic) {
            setModifier(isStatic, Modifier.STATIC);
            return this;
        }

        @NonNull
        @Override
        public MethodMatcher setIsFinal(boolean isFinal) {
            setModifier(isFinal, Modifier.FINAL);
            return this;
        }

        @NonNull
        @Override
        public MethodMatcher setIsSynchronized(boolean isSynchronized) {
            setModifier(isSynchronized, Modifier.SYNCHRONIZED);
            return this;
        }

        @NonNull
        @Override
        public MethodMatcher setIsNative(boolean isNative) {
            setModifier(isNative, Modifier.NATIVE);
            return this;
        }
    }

    private final class ConstructorMatcherImpl extends ExecutableMatcherImpl<ConstructorMatcherImpl, ConstructorMatcher, Constructor<?>, ConstructorLazySequenceImpl> implements ConstructorMatcher {
        private ConstructorMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @NonNull
        @Override
        protected ConstructorLazySequenceImpl onBuild() {
            if (key != null) keyedConstructorMatchers.put(key, this);
            return new ConstructorLazySequenceImpl(rootMatcher);
        }
    }

    private final class StringMatcherImpl extends BaseMatcherImpl<StringMatcherImpl, String, DexParser.StringId> {
        @NonNull
        private final String pattern;

        private final boolean matchPrefix;

        private StringMatcherImpl(@NonNull String pattern, boolean matchFirst, boolean matchPrefix) {
            super(matchFirst);
            this.pattern = pattern;
            this.matchPrefix = matchPrefix;
        }

        private StringMatch build() {
            var match = new StringMatchImpl(this);
            stringMatches.add(match);
            return match;
        }
    }

    private abstract class BaseSyntaxImpl<Match extends BaseMatch<Match, Reflect>, MatchImpl extends BaseMatchImpl<MatchImpl, Match, Reflect>, Reflect> implements Syntax<Match> {
        protected final @NonNull Operands operands;

        private BaseSyntaxImpl(@NonNull Syntax<Match> operand, char operator) {
            this.operands = new UnaryOperands(new Operand(operand), operator);
        }

        private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?, D>, D extends DexParser.Id<D>> BaseSyntaxImpl(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?, D> operand, char operator) {
            this.operands = new UnaryOperands(new Operand(operand), operator);
        }

        private BaseSyntaxImpl(@NonNull MatchImpl operand, char operator) {
            this.operands = new UnaryOperands(new Operand(operand), operator);
        }

        private BaseSyntaxImpl(@NonNull Syntax<Match> left, @NonNull Syntax<Match> right, char operator) {
            this.operands = new BinaryOperands(new Operand(left), new Operand(right), operator);
        }

        abstract Syntax<Match> newSelf(@Nullable Syntax<Match> other, char operator);

        @NonNull
        @Override
        public Syntax<Match> and(@NonNull Syntax<Match> other) {
            return newSelf(other, '&');
        }

        @NonNull
        @Override
        public Syntax<Match> or(@NonNull Syntax<Match> other) {
            return newSelf(other, '|');
        }

        @NonNull
        @Override
        public Syntax<Match> not() {
            return newSelf(null, '!');
        }

        protected final class Operand {
            private @NonNull Object value;

            private Operand(@NonNull MatchImpl match) {
                this.value = match;
            }

            private Operand(@NonNull Syntax<Match> syntax) {
                this.value = syntax;
            }

            private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?, D>, D extends DexParser.Id<D>> Operand(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?, D> seq) {
                this.value = seq;
            }
        }

        protected abstract class Operands {
            protected final char operator;

            protected Operands(char operator) {
                this.operator = operator;
            }
        }

        protected final class UnaryOperands extends Operands {
            private final @NonNull Operand operand;

            private UnaryOperands(@NonNull Operand operand, char operator) {
                super(operator);
                this.operand = operand;
            }
        }

        protected final class BinaryOperands extends Operands {
            private final @NonNull Operand left;
            private final @NonNull Operand right;

            private BinaryOperands(@NonNull Operand left, @NonNull Operand right, char operator) {
                super(operator);
                this.left = left;
                this.right = right;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private final class ReflectSyntaxImpl<Match extends ReflectMatch<Match, Reflect, ?>, MatchImpl extends ReflectMatchImpl<MatchImpl, Match, Reflect, ?, ?, ?>, Reflect> extends BaseSyntaxImpl<Match, MatchImpl, Reflect> implements Syntax<Match> {
        private ReflectSyntaxImpl(@NonNull Syntax<Match> operand, char operator) {
            super(operand, operator);
        }

        private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?, D>, D extends DexParser.Id<D>> ReflectSyntaxImpl(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?, D> operand, char operator) {
            super(operand, operator);
        }

        private ReflectSyntaxImpl(@NonNull MatchImpl operand, char operator) {
            super(operand, operator);
        }

        private ReflectSyntaxImpl(@NonNull Syntax<Match> left, @NonNull Syntax<Match> right, char operator) {
            super(left, right, operator);
        }

        @Override
        Syntax<Match> newSelf(@Nullable Syntax<Match> other, char operator) {
            return other == null ? new ReflectSyntaxImpl<>(this, operator) : new ReflectSyntaxImpl<>(this, other, operator);
        }

        private boolean operandTest(@NonNull Operand operand, @NonNull HashSet<Reflect> set, char operator) {
            if (operand.value instanceof ReflectMatchImpl) {
                ReflectMatchImpl<?, ?, Reflect, ?, ?, ?>.ReflectWrapper match = ((ReflectMatchImpl<?, ?, Reflect, ?, ?, ?>) operand.value).match.get();
                if (match == null) return false;
                return set.contains(match.reflect);
            } else if (operand.value instanceof LazySequence) {
                final var matches = ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?, ?>) operand.value).matches.get();
                if (matches == null || matches.isEmpty()) return false;
                if (operator == '^') {
                    for (final var match : matches) if (!set.contains(match)) return false;
                    return true;
                } else if (operator == 'v') {
                    for (final var match : matches) if (set.contains(match)) return true;
                }
                return false;
            } else if (operand.value instanceof ReflectSyntaxImpl) {
                return ((ReflectSyntaxImpl<?, ?, Reflect>) operand.value).test(set);
            }
            return false;
        }

        private boolean test(@NonNull HashSet<Reflect> set) {
            if (operands instanceof BaseSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                final var operator = binaryOperands.operator;
                boolean leftMatch = operandTest(binaryOperands.left, set, operator);
                if ((!leftMatch && operator == '&')) {
                    return false;
                } else if (leftMatch && operator == '|') {
                    return true;
                }
                return operandTest(binaryOperands.left, set, operator);
            } else if (operands instanceof BaseSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                final var operator = unaryOperands.operator;
                boolean match = operandTest(unaryOperands.operand, set, operator);
                if (unaryOperands.operator == '!' || unaryOperands.operator == '-') {
                    return !match;
                } else if (unaryOperands.operator == '+') {
                    return match;
                }
            }
            return false;
        }

        private void addObserver(@NonNull Operand operand, @NonNull BaseObserver<?> observer, @Nullable AtomicInteger count) {
            if (operand.value instanceof ReflectMatchImpl) {
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?, ?>) operand.value).addObserver((BaseObserver<Reflect>) observer);
                if (count != null) count.incrementAndGet();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?, ?>) operand.value).addObserver((BaseObserver<Collection<Reflect>>) observer);
                if (count != null) count.incrementAndGet();
            } else {
                ((ReflectSyntaxImpl<?, ?, Reflect>) operand.value).addObserver(observer, count);
            }
        }

        void addObserver(@NonNull BaseObserver<?> observer, @Nullable AtomicInteger count) {
            if (operands instanceof BaseSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                addObserver(binaryOperands.left, observer, count);
                addObserver(binaryOperands.right, observer, count);
            } else if (operands instanceof BaseSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                addObserver(unaryOperands.operand, observer, count);
            }
        }

        private void removeObserver(@NonNull Operand operand, @NonNull BaseObserver<?> observer, @Nullable AtomicInteger count) {
            if (operand.value instanceof ReflectMatchImpl) {
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?, ?>) operand.value).removeObserver((BaseObserver<Reflect>) observer);
                if (count != null) count.decrementAndGet();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?, ?>) operand.value).removeObserver((BaseObserver<Collection<Reflect>>) observer);
                if (count != null) count.decrementAndGet();
            } else {
                ((ReflectSyntaxImpl<?, ?, Reflect>) operand.value).removeObserver(observer, count);
            }
        }

        void removeObserver(@NonNull BaseObserver<?> observer, @Nullable AtomicInteger count) {
            if (operands instanceof BaseSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                removeObserver(binaryOperands.left, observer, count);
                removeObserver(binaryOperands.right, observer, count);
            } else if (operands instanceof BaseSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                removeObserver(unaryOperands.operand, observer, count);
            }
        }

        private void setNonPending(@NonNull Operand operand) {
            if (operand.value instanceof ReflectMatchImpl) {
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?, ?>) operand.value).rootMatcher.setNonPending();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?, ?>) operand.value).rootMatcher.setNonPending();
            } else {
                ((ReflectSyntaxImpl<?, ?, Reflect>) operand.value).setNonPending();
            }
        }

        void setNonPending() {
            if (operands instanceof BaseSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                setNonPending(binaryOperands.left);
                setNonPending(binaryOperands.right);
            } else if (operands instanceof BaseSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                setNonPending(unaryOperands.operand);
            }
        }
    }

    private final class StringSyntaxImpl extends BaseSyntaxImpl<StringMatch, StringMatchImpl, String> implements Syntax<StringMatch> {
        private StringSyntaxImpl(@NonNull Syntax<StringMatch> operand, char operator) {
            super(operand, operator);
        }

        private <M extends ReflectMatch<M, String, ?>, MI extends ReflectMatchImpl<MI, M, String, ?, ?, D>, D extends DexParser.Id<D>> StringSyntaxImpl(@NonNull LazySequenceImpl<?, M, String, ?, MI, ?, D> operand, char operator) {
            super(operand, operator);
        }

        private StringSyntaxImpl(@NonNull StringMatchImpl operand, char operator) {
            super(operand, operator);
        }

        private StringSyntaxImpl(@NonNull Syntax<StringMatch> left, @NonNull Syntax<StringMatch> right, char operator) {
            super(left, right, operator);
        }

        @Override
        Syntax<StringMatch> newSelf(@Nullable Syntax<StringMatch> other, char operator) {
            return other == null ? new StringSyntaxImpl(this, operator) : new StringSyntaxImpl(this, other, operator);
        }

        private boolean operandTest(Operand operand, TreeSetView<String> set) {
            if (operand.value instanceof StringMatchImpl) {
                var matcher = ((StringMatchImpl) operand.value).matcher;
                if (matcher.matchPrefix) {
                    return !set.subSet(matcher.pattern, matcher.pattern + Character.MAX_VALUE).isEmpty();
                } else {
                    return set.contains(matcher.pattern);
                }
            } else if (operand.value instanceof StringSyntaxImpl) {
                return ((StringSyntaxImpl) operand.value).test(set);
            }
            return false;
        }

        private boolean test(TreeSetView<String> set) {
            if (operands instanceof BaseSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                char operator = binaryOperands.operator;
                boolean leftMatch = operandTest(binaryOperands.left, set);
                if ((!leftMatch && operator == '&')) {
                    return false;
                } else if (leftMatch && operator == '|') {
                    return true;
                }
                return operandTest(binaryOperands.right, set);
            } else if (operands instanceof BaseSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                boolean match = operandTest(unaryOperands.operand, set);
                if (unaryOperands.operator == '!' || unaryOperands.operator == '-') {
                    return !match;
                } else if (unaryOperands.operator == '+') {
                    return match;
                }
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class LazySequenceImpl<Base extends LazySequence<Base, Match, Reflect, Matcher>, Match extends ReflectMatch<Match, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>, MatchImpl extends ReflectMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl, DexId>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, DexId, ?>, DexId extends DexParser.Id<DexId>> implements LazySequence<Base, Match, Reflect, Matcher> {
        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher;
        @NonNull
        protected final AtomicReference<int[][]> dexMatches = new AtomicReference<>(null);
        @NonNull
        protected final AtomicReference<Collection<Reflect>> matches = new AtomicReference<>(null);
        @GuardedBy("this")
        @NonNull
        private final Set<BaseObserver<Collection<Reflect>>> observers = new HashSet<>();
        @GuardedBy("this")
        @NonNull
        private final Queue<LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl, DexId>> missReplacements = new LinkedList<>();
        // specially cache `first` since it's the only one that do not need to define any callback
        @Nullable
        private volatile Match first = null;

        protected LazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            this.rootMatcher = rootMatcher;
        }

        @NonNull
        @Override
        public final Match first() {
            var f = first;
            if (f == null) {
                final var m = newMatch();
                addObserver((ListObserver<Reflect>) result -> {
                    final var i = result.iterator();
                    if (i.hasNext()) m.match(i.next());
                    else m.match(null);
                });
                first = f = (Match) m;
            }
            return f;
        }

        @NonNull
        @Override
        public final Match first(@NonNull Consumer<Matcher> consumer) {
            final var m = newMatcher(true);
            m.pending = true;
            addObserver((ListObserver<Reflect>) m::doMatch);
            return (Match) m.build().first();
        }

        @NonNull
        @Override
        public final Base all(@NonNull Consumer<Matcher> consumer) {
            final var m = newMatcher(false);
            m.pending = true;
            addObserver((ListObserver<Reflect>) m::doMatch);
            return (Base) m.build();
        }

        @NonNull
        @Override
        public final Base onMatch(@NonNull Consumer<Iterable<Reflect>> consumer) {
            rootMatcher.setNonPending();
            addObserver(result -> {
                if (result.iterator().hasNext())
                    callbackExecutor.submit(() -> consumer.accept(result));
            });
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base onMiss(@NonNull Runnable runnable) {
            addObserver((ListObserver<Reflect>) result -> {
                if (result.isEmpty()) callbackExecutor.submit(runnable);
            });
            return (Base) this;
        }

        @NonNull
        @Override
        public final Syntax<Match> conjunction() {
            return new ReflectSyntaxImpl<>(this, '^');
        }

        @NonNull
        @Override
        public final Syntax<Match> disjunction() {
            return new ReflectSyntaxImpl<>(this, 'v');
        }

        @NonNull
        @Override
        public final Base substituteIfMiss(@NonNull Supplier<Base> substitute) {
            missReplacements.add((LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl, DexId>) substitute.get());
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base matchIfMiss(@NonNull Consumer<Matcher> consumer) {
            final var m = newMatcher(false);
            consumer.accept((Matcher) m);
            missReplacements.add((LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl, DexId>) m.build());
            return (Base) this;
        }

        @NonNull
        @Override
        public final <Bind extends LazyBind> Base bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Iterable<Reflect>> consumer) {
            if (binds.containsKey(bind)) {
                binds.put(bind, new AtomicInteger(1));
            } else {
                final var c = binds.get(bind);
                if (c != null) c.incrementAndGet();
            }
            addObserver((ListObserver<Reflect>) result -> {
                final var c = binds.get(bind);
                if (c == null) return;
                final var old = c.decrementAndGet();
                if (old >= 0) {
                    callbackExecutor.submit(() -> consumer.accept(bind, result));
                    if (old == 0) {
                        callbackExecutor.submit(bind::onMatch);
                    }
                }
            });
            return (Base) this;
        }

        protected final synchronized void match(@NonNull Collection<Reflect> matches) {
            if (!this.matches.compareAndSet(null, matches)) return;
            final Runnable runnable = () -> {
                for (final var observer : observers) {
                    observer.update(matches);
                }
            };
            if (matches.iterator().hasNext()) {
                matchExecutor.submit(runnable);
            } else {
                final var replacement = missReplacements.poll();
                if (replacement != null) {
                    replacement.rootMatcher.setNonPending();
                    replacement.addObserver((ListObserver<Reflect>) this::match);
                } else {
                    matchExecutor.submit(runnable);
                }
            }
        }

        @NonNull
        protected abstract MatchImpl newMatch();

        @NonNull
        protected abstract MatcherImpl newMatcher(boolean matchFirst);

        protected final synchronized void addObserver(@NonNull BaseObserver<Collection<Reflect>> observer) {
            observers.add(observer);
            var matches = this.matches.get();
            if (matches != null) observer.update(matches);
        }

        protected final synchronized void removeObserver(@NonNull BaseObserver<Collection<Reflect>> observer) {
            observers.remove(observer);
        }
    }

    private class ClassLazySequenceImpl extends LazySequenceImpl<ClassLazySequence, ClassMatch, Class<?>, ClassMatcher, ClassMatchImpl, ClassMatcherImpl, DexParser.TypeId> implements ClassLazySequence {
        protected ClassLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected ClassMatchImpl newMatch() {
            return new ClassMatchImpl(rootMatcher);
        }

        @NonNull
        @Override
        protected ClassMatcherImpl newMatcher(boolean matchFirst) {
            return new ClassMatcherImpl(rootMatcher, matchFirst);
        }

        private void addMethodsObserver(@NonNull MethodMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Class<?>>) result -> {
                m.setNonPending();
                final var methods = new ArrayList<Method>();
                for (final var type : result) {
                    methods.addAll(Arrays.asList(type.getDeclaredMethods()));
                }
                m.doMatch(methods);
            });
        }

        private void addConstructorsObserver(@NonNull ConstructorMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Class<?>>) result -> {
                m.setNonPending();
                final var constructors = new ArrayList<Constructor<?>>();
                for (final var type : result) {
                    constructors.addAll(Arrays.asList(type.getDeclaredConstructors()));
                }
                m.doMatch(constructors);
            });
        }

        private void addFieldsObserver(@NonNull FieldMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Class<?>>) result -> {
                m.setNonPending();
                final var fields = new ArrayList<Field>();
                for (final var type : result) {
                    fields.addAll(Arrays.asList(type.getDeclaredFields()));
                }
                m.doMatch(fields);
            });
        }

        @NonNull
        @Override
        public final MethodLazySequence methods(@NonNull Consumer<MethodMatcher> matcher) {
            final var m = new MethodMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addMethodsObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher) {
            final var m = new MethodMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addMethodsObserver(m);
            return m.build().first();
        }

        @NonNull
        @Override
        public final ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher) {
            final var m = new ConstructorMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addConstructorsObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher) {
            final var m = new ConstructorMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addConstructorsObserver(m);
            return m.build().first();
        }

        @NonNull
        @Override
        public final FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher) {
            final var m = new FieldMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addFieldsObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher) {
            final var m = new FieldMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addFieldsObserver(m);
            return m.build().first();
        }
    }

    private final class ParameterLazySequenceImpl extends LazySequenceImpl<ParameterLazySequence, ParameterMatch, Parameter, ParameterMatcher, ParameterMatchImpl, ParameterMatcherImpl, DexParser.TypeId> implements ParameterLazySequence {
        private ParameterLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected ParameterMatchImpl newMatch() {
            return new ParameterMatchImpl(rootMatcher);
        }

        @NonNull
        @Override
        protected ParameterMatcherImpl newMatcher(boolean matchFirst) {
            return new ParameterMatcherImpl(rootMatcher, matchFirst);
        }

        private void addTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Parameter>) result -> {
                m.setNonPending();
                final var types = new ArrayList<Class<?>>();
                for (final var parameter : result) {
                    types.add(parameter.getType());
                }
                m.doMatch(types);
            });
        }

        @NonNull
        @Override
        public ClassLazySequence types(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public ClassMatch firstType(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build().first();
        }
    }

    private abstract class MemberLazySequenceImpl<Base extends MemberLazySequence<Base, Match, Reflect, Matcher>, Match extends MemberMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>, MatchImpl extends MemberMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl, DexId>, MatcherImpl extends MemberMatcherImpl<MatcherImpl, Matcher, Reflect, DexId, ?>, DexId extends DexParser.Id<DexId>> extends LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl, DexId> implements MemberLazySequence<Base, Match, Reflect, Matcher> {
        protected MemberLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        private void addDeclaringClassesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Reflect>) result -> {
                m.setNonPending();
                final var declaringClasses = new ArrayList<Class<?>>();
                for (final var type : result) {
                    declaringClasses.add(type.getDeclaringClass());
                }
                m.doMatch(declaringClasses);
            });
        }

        @NonNull
        @Override
        public final ClassLazySequence declaringClasses(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addDeclaringClassesObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final ClassMatch firstDeclaringClass(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addDeclaringClassesObserver(m);
            return m.build().first();
        }
    }

    private final class FieldLazySequenceImpl extends MemberLazySequenceImpl<FieldLazySequence, FieldMatch, Field, FieldMatcher, FieldMatchImpl, FieldMatcherImpl, DexParser.FieldId> implements FieldLazySequence {
        private FieldLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        private void addTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Field>) result -> {
                m.setNonPending();
                final var types = new ArrayList<Class<?>>();
                for (final var type : result) {
                    types.add(type.getType());
                }
                m.doMatch(types);
            });
        }

        @NonNull
        @Override
        protected FieldMatchImpl newMatch() {
            return new FieldMatchImpl(rootMatcher);
        }

        @NonNull
        @Override
        protected FieldMatcherImpl newMatcher(boolean matchFirst) {
            return new FieldMatcherImpl(rootMatcher, matchFirst);
        }

        @NonNull
        @Override
        public ClassLazySequence types(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public ClassMatch firstType(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build().first();
        }
    }

    private abstract class ExecutableLazySequenceImpl<Base extends ExecutableLazySequence<Base, Match, Reflect, Matcher>, Match extends ExecutableMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>, MatchImpl extends ExecutableMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl>, MatcherImpl extends ExecutableMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends MemberLazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl, DexParser.MethodId> implements ExecutableLazySequence<Base, Match, Reflect, Matcher> {
        private ExecutableLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        private void addParametersObserver(ParameterMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Reflect>) result -> {
                m.setNonPending();
                final var parameters = new ArrayList<Parameter>();
                for (final var r : result) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        final var p = ((Executable) r).getParameters();
                        for (var i = 0; i < p.length; i++) {
                            parameters.add(new ParameterImpl(i, p[i].getType(), r, p[i].getModifiers()));
                        }
                    } else {
                        final var parameterTypes = new ArrayList<Class<?>>();
                        if (r instanceof Method) {
                            parameterTypes.addAll(Arrays.asList(((Method) r).getParameterTypes()));
                        } else if (r instanceof Constructor) {
                            parameterTypes.addAll(Arrays.asList(((Constructor<?>) r).getParameterTypes()));
                        }
                        for (int i = 0; i < parameterTypes.size(); i++) {
                            parameters.add(new ParameterImpl(i, parameterTypes.get(i), r, 0));
                        }
                    }
                }
                m.doMatch(parameters);
            });
        }

        private void addParameterTypesObserver(ClassMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Reflect>) result -> {
                m.setNonPending();
                final var types = new ArrayList<Class<?>>();
                for (final var r : result) {
                    if (r instanceof Method) {
                        types.addAll(Arrays.asList(((Method) r).getParameterTypes()));
                    } else if (r instanceof Constructor) {
                        types.addAll(Arrays.asList(((Constructor<?>) r).getParameterTypes()));
                    }
                }
                m.doMatch(types);
            });
        }

        @NonNull
        @Override
        public final ParameterLazySequence parameters(@NonNull Consumer<ParameterMatcher> matcher) {
            final var m = new ParameterMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addParametersObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final ParameterMatch firstParameter(@NonNull Consumer<ParameterMatcher> matcher) {
            final var m = new ParameterMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addParametersObserver(m);
            return m.build().first();
        }
    }

    private final class MethodLazySequenceImpl extends ExecutableLazySequenceImpl<MethodLazySequence, MethodMatch, Method, MethodMatcher, MethodMatchImpl, MethodMatcherImpl> implements MethodLazySequence {
        private MethodLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        private void addReturnTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver((ListObserver<Method>) result -> {
                m.setNonPending();
                final var types = new ArrayList<Class<?>>();
                for (final var type : result) {
                    types.add(type.getReturnType());
                }
                m.doMatch(types);
            });
        }

        @NonNull
        @Override
        protected MethodMatchImpl newMatch() {
            return new MethodMatchImpl(rootMatcher);
        }

        @NonNull
        @Override
        protected MethodMatcherImpl newMatcher(boolean matchFirst) {
            return new MethodMatcherImpl(rootMatcher, matchFirst);
        }

        @NonNull
        @Override
        public ClassLazySequence returnTypes(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, false);
            matcher.accept(m);
            addReturnTypesObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public ClassMatch firstReturnType(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(rootMatcher, true);
            matcher.accept(m);
            addReturnTypesObserver(m);
            return m.build().first();
        }
    }

    private final class ConstructorLazySequenceImpl extends ExecutableLazySequenceImpl<ConstructorLazySequence, ConstructorMatch, Constructor<?>, ConstructorMatcher, ConstructorMatchImpl, ConstructorMatcherImpl> implements ConstructorLazySequence {
        private ConstructorLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected ConstructorMatchImpl newMatch() {
            return new ConstructorMatchImpl(rootMatcher);
        }

        @NonNull
        @Override
        protected ConstructorMatcherImpl newMatcher(boolean matchFirst) {
            return new ConstructorMatcherImpl(rootMatcher, matchFirst);
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class ReflectMatchImpl<Self extends ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl, DexId>, Base extends ReflectMatch<Base, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, DexId, ?>, DexId extends DexParser.Id<DexId>> extends BaseMatchImpl<Self, Base, Reflect> implements ReflectMatch<Base, Reflect, Matcher> {
        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher;
        @NonNull
        protected final AtomicReference<int[]> dexMatch = new AtomicReference<>(null);
        @GuardedBy("this")
        @NonNull
        private final Set<BaseObserver<Reflect>> observers = new HashSet<>();
        @GuardedBy("this")
        @NonNull
        private final Queue<ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl, DexId>> missReplacements = new LinkedList<>();
        @Nullable
        protected volatile String key = null;
        @NonNull
        protected AtomicReference<ReflectWrapper> match = new AtomicReference<>(null);

        protected ReflectMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            this.rootMatcher = rootMatcher;
        }

        @Nullable
        @Override
        public final String getKey() {
            return key;
        }

        @NonNull
        @Override
        public final Base setKey(@Nullable String key) {
            this.key = key;
            rootMatcher.setNonPending();
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base onMatch(@NonNull Consumer<Reflect> consumer) {
            rootMatcher.setNonPending();
            addObserver(result -> {
                if (result != null) callbackExecutor.submit(() -> consumer.accept(result));
            });
            return (Base) this;
        }

        @NonNull
        @Override
        public Base onMiss(@NonNull Runnable handler) {
            addObserver(result -> {
                if (result == null) callbackExecutor.submit(handler);
            });
            return (Base) this;
        }

        @NonNull
        @Override
        public final synchronized Base substituteIfMiss(@NonNull Supplier<Base> replacement) {
            final var re = (Self) replacement.get();
            missReplacements.add(re);
            return (Base) this;
        }

        @NonNull
        @Override
        public final synchronized Base matchFirstIfMiss(@NonNull Consumer<Matcher> consumer) {
            MatcherImpl m = newFirstMatcher();
            consumer.accept((Matcher) m);
            m.pending = true;
            missReplacements.add((ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl, DexId>) m.build().first());
            return (Base) this;
        }

        @NonNull
        @Override
        public final <Bind extends LazyBind> Base bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Reflect> consumer) {
            if (binds.containsKey(bind)) {
                binds.put(bind, new AtomicInteger(1));
            } else {
                final var c = binds.get(bind);
                if (c != null) c.incrementAndGet();
            }
            addObserver((ItemObserver<Reflect>) result -> {
                final var c = binds.get(bind);
                if (c == null) return;
                if (result == null) {
                    if (c.getAndSet(0) > 0) callbackExecutor.submit(bind::onMiss);
                } else {
                    final var old = c.decrementAndGet();
                    if (old >= 0) {
                        callbackExecutor.submit(() -> consumer.accept(bind, result));
                        if (old == 0) {
                            callbackExecutor.submit(bind::onMatch);
                        }
                    }
                }
            });
            return (Base) this;
        }

        protected final synchronized void addObserver(BaseObserver<Reflect> observer) {
            observers.add(observer);
            final var m = match.get();
            if (m != null) observer.update(m.reflect);
        }

        protected final synchronized void removeObserver(BaseObserver<Reflect> observer) {
            observers.remove(observer);
        }

        protected final synchronized void match(@Nullable Reflect match) {
            if (!this.match.compareAndSet(null, new ReflectWrapper(match))) return;
            Runnable runnable = () -> {
                for (var observer : observers) {
                    observer.update(match);
                }
            };
            if (match != null) {
                if (match instanceof AccessibleObject) {
                    ((AccessibleObject) match).setAccessible(true);
                } else if (match instanceof ParameterImpl) {
                    ((AccessibleObject) ((ParameterImpl) match).getDeclaringExecutable()).setAccessible(true);
                }
                matchExecutor.submit(runnable);
            } else {
                final var replacement = missReplacements.poll();
                if (replacement != null) {
                    replacement.rootMatcher.setNonPending();
                    replacement.addObserver((ItemObserver<Reflect>) this::match);
                } else {
                    matchExecutor.submit(runnable);
                }
            }
        }

        @NonNull
        protected abstract MatcherImpl newFirstMatcher();

        protected abstract void onKey(@Nullable String newKey, @Nullable String oldKey);

        @NonNull
        @Override
        public final Syntax<Base> observe() {
            return new ReflectSyntaxImpl<>((Self) this, '+');
        }

        @NonNull
        @Override
        public final Syntax<Base> reverse() {
            return new ReflectSyntaxImpl<>((Self) this, '-');
        }

        private final class ReflectWrapper {
            @Nullable
            Reflect reflect;

            private ReflectWrapper(@Nullable Reflect reflect) {
                this.reflect = reflect;
            }
        }
    }

    private class ClassMatchImpl extends ReflectMatchImpl<ClassMatchImpl, ClassMatch, Class<?>, ClassMatcher, ClassMatcherImpl, DexParser.TypeId> implements ClassMatch {
        protected ClassMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected ClassMatcherImpl newFirstMatcher() {
            return new ClassMatcherImpl(rootMatcher, true);
        }

        @NonNull
        @Override
        public final ClassMatch getSuperClass() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver((ItemObserver<Class<?>>) result -> m.match(result == null ? null : result.getSuperclass()));
            return m;
        }

        @NonNull
        @Override
        public final ClassLazySequence getInterfaces() {
            final var m = new ClassLazySequenceImpl(rootMatcher);
            addObserver((ItemObserver<Class<?>>) result -> m.match(result == null ? Collections.emptyList() : List.of(result.getInterfaces())));
            return m;
        }

        @NonNull
        @Override
        public final MethodLazySequence getDeclaredMethods() {
            var m = new MethodLazySequenceImpl(rootMatcher);
            addObserver((ItemObserver<Class<?>>) result -> m.match(result == null ? Collections.emptyList() : List.of(result.getDeclaredMethods())));
            return m;
        }

        @NonNull
        @Override
        public final ConstructorLazySequence getDeclaredConstructors() {
            final var m = new ConstructorLazySequenceImpl(rootMatcher);
            addObserver((ItemObserver<Class<?>>) result -> m.match(result == null ? Collections.emptyList() : List.of(result.getDeclaredConstructors())));
            return m;
        }

        @NonNull
        @Override
        public final FieldLazySequence getDeclaredFields() {
            final var m = new FieldLazySequenceImpl(rootMatcher);
            addObserver((ItemObserver<Class<?>>) result -> m.match(result == null ? Collections.emptyList() : List.of(result.getDeclaredFields())));
            return m;
        }

        @NonNull
        @Override
        public final ClassMatch getArrayType() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver((ItemObserver<Class<?>>) result -> m.match(result == null ? null : Array.newInstance(result, 0).getClass()));
            return m;
        }

        @Override
        protected void onKey(@Nullable String newKey, @Nullable String oldKey) {
            if (oldKey != null) {
                keyedClassMatches.remove(oldKey);
            }
            if (newKey != null) {
                keyedClassMatches.put(newKey, this);
            }
        }
    }

    private final class ParameterMatchImpl extends ReflectMatchImpl<ParameterMatchImpl, ParameterMatch, Parameter, ParameterMatcher, ParameterMatcherImpl, DexParser.TypeId> implements ParameterMatch {
        int index = -1;

        private ParameterMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected ParameterMatcherImpl newFirstMatcher() {
            return new ParameterMatcherImpl(rootMatcher, true);
        }

        @Override
        protected void onKey(@Nullable String newKey, @Nullable String oldKey) {
            if (oldKey != null) keyedParameterMatches.remove(oldKey);
            if (newKey != null) keyedParameterMatches.put(newKey, this);
        }

        @NonNull
        @Override
        public ClassMatch getType() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver((ItemObserver<Parameter>) result -> m.match(result == null ? null : result.getType()));
            return m;
        }
    }

    private abstract class MemberMatchImpl<Self extends MemberMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl, DexId>, Base extends MemberMatch<Base, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, DexId, ?>, DexId extends DexParser.Id<DexId>> extends ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl, DexId> implements MemberMatch<Base, Reflect, Matcher> {
        protected MemberMatchImpl(ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        public final ClassMatch getDeclaringClass() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver((ItemObserver<Reflect>) result -> m.match(result == null ? null : result.getDeclaringClass()));
            return m;
        }
    }

    private final class FieldMatchImpl extends MemberMatchImpl<FieldMatchImpl, FieldMatch, Field, FieldMatcher, FieldMatcherImpl, DexParser.FieldId> implements FieldMatch {
        private FieldMatchImpl(ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected FieldMatcherImpl newFirstMatcher() {
            return new FieldMatcherImpl(rootMatcher, true);
        }

        @Override
        protected void onKey(@Nullable String newKey, @Nullable String oldKey) {
            if (oldKey != null) {
                keyedFieldMatches.remove(oldKey);
            }
            if (newKey != null) {
                keyedFieldMatches.put(newKey, this);
            }
        }

        @NonNull
        @Override
        public ClassMatch getType() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver((ItemObserver<Field>) result -> m.match(result == null ? null : result.getType()));
            return m;
        }
    }

    private abstract class ExecutableMatchImpl<Self extends ExecutableMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>, Base extends ExecutableMatch<Base, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>, MatcherImpl extends MemberMatcherImpl<MatcherImpl, Matcher, Reflect, DexParser.MethodId, ?>> extends MemberMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl, DexParser.MethodId> implements ExecutableMatch<Base, Reflect, Matcher> {
        protected ExecutableMatchImpl(ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        public final ClassLazySequence getParameterTypes() {
            final var m = new ClassLazySequenceImpl(rootMatcher);
            addObserver((ItemObserver<Reflect>) result -> {
                if (result instanceof Method) {
                    m.match(List.of(((Method) result).getParameterTypes()));
                } else if (result instanceof Constructor) {
                    m.match(List.of(((Constructor<?>) result).getParameterTypes()));
                } else m.match(Collections.emptyList());
            });
            return m;
        }

        @NonNull
        @Override
        public ParameterLazySequence getParameters() {
            final var m = new ParameterLazySequenceImpl(rootMatcher);
            addObserver((ItemObserver<Reflect>) result -> {
                if (result == null) {
                    m.match(Collections.emptyList());
                    return;
                }
                final var parameters = new ArrayList<Parameter>();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    final var p = ((Executable) result).getParameters();
                    for (int i = 0; i < p.length; i++) {
                        parameters.add(new ParameterImpl(i, p[i].getType(), result, p[i].getModifiers()));
                    }
                } else {
                    final var parameterTypes = new ArrayList<Class<?>>();
                    if (result instanceof Method) {
                        parameterTypes.addAll(Arrays.asList(((Method) result).getParameterTypes()));
                    } else if (result instanceof Constructor) {
                        parameterTypes.addAll(Arrays.asList(((Constructor<?>) result).getParameterTypes()));
                    }
                    for (int i = 0; i < parameterTypes.size(); i++) {
                        parameters.add(new ParameterImpl(i, parameterTypes.get(i), result, 0));
                    }
                }
                m.match(parameters);
            });
            return m;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final FieldLazySequence getAssignedFields() {
            dexAnalysis = true;
            return null;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final FieldLazySequence getAccessedFields() {
            dexAnalysis = true;
            return null;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final MethodLazySequence getInvokedMethods() {
            dexAnalysis = true;
            return null;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final ConstructorLazySequence getInvokedConstructors() {
            dexAnalysis = true;
            return null;
        }
    }

    private final class MethodMatchImpl extends ExecutableMatchImpl<MethodMatchImpl, MethodMatch, Method, MethodMatcher, MethodMatcherImpl> implements MethodMatch {
        private MethodMatchImpl(ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected MethodMatcherImpl newFirstMatcher() {
            return new MethodMatcherImpl(rootMatcher, true);
        }

        @Override
        protected void onKey(@Nullable String newKey, @Nullable String oldKey) {
            if (oldKey != null) {
                keyedMethodMatches.remove(oldKey);
            }
            if (newKey != null) {
                keyedMethodMatches.put(newKey, this);
            }
        }

        @NonNull
        @Override
        public ClassMatch getReturnType() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver((ItemObserver<Method>) result -> m.match(result == null ? null : result.getReturnType()));
            return m;
        }
    }

    private final class ConstructorMatchImpl extends ExecutableMatchImpl<ConstructorMatchImpl, ConstructorMatch, Constructor<?>, ConstructorMatcher, ConstructorMatcherImpl> implements ConstructorMatch {
        private ConstructorMatchImpl(ReflectMatcherImpl<?, ?, ?, ?, ?> rootMatcher) {
            super(rootMatcher);
        }

        @NonNull
        @Override
        protected ConstructorMatcherImpl newFirstMatcher() {
            return new ConstructorMatcherImpl(rootMatcher, true);
        }

        @Override
        protected void onKey(@Nullable String newKey, @Nullable String oldKey) {
            if (oldKey != null) {
                keyedConstructorMatches.remove(oldKey);
            }
            if (newKey != null) {
                keyedConstructorMatches.put(newKey, this);
            }
        }
    }

    private final class StringMatchImpl extends BaseMatchImpl<StringMatchImpl, StringMatch, String> implements StringMatch {
        @NonNull
        private final StringMatcherImpl matcher;

        @NonNull
        private final AtomicReference<int[][]> dexMatches = new AtomicReference<>(null);

        private StringMatchImpl(@NonNull StringMatcherImpl matcher) {
            this.matcher = matcher;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean test(@NonNull String value) {
            if (matcher.matchPrefix && !value.startsWith(matcher.pattern)) return false;
            return matcher.pattern.equals(value);
        }

        @NonNull
        @Override
        public Syntax<StringMatch> observe() {
            return new StringSyntaxImpl(this, '+');
        }

        @NonNull
        @Override
        public Syntax<StringMatch> reverse() {
            return new StringSyntaxImpl(this, '-');
        }
    }

}
