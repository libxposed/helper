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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final ConcurrentLinkedQueue<ClassMatcherImpl> rootClassMatchers = new ConcurrentLinkedQueue<>();

    @NonNull
    private final ConcurrentLinkedQueue<FieldMatcherImpl> rootFieldMatchers = new ConcurrentLinkedQueue<>();

    @NonNull
    private final ConcurrentLinkedQueue<MethodMatcherImpl> rootMethodMatchers = new ConcurrentLinkedQueue<>();

    @NonNull
    private final ConcurrentLinkedQueue<ConstructorMatcherImpl> rootConstructorMatchers = new ConcurrentLinkedQueue<>();

    @NonNull
    private final HashMap<LazyBind, AtomicInteger> binds = new HashMap<>();

    @NonNull
    private SimpleExecutor executorService = new PendingExecutor();

    @Nullable
    private SimpleExecutor userExecutorService = null;

    @NonNull
    private SimpleExecutor callbackHandler = new PendingExecutor();

    @Nullable
    private SimpleExecutor userCallbackHandler = null;

    @Nullable
    private MatchCache matchCache = null;

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
        this.userExecutorService = new SimpleExecutor() {
            @Override
            <T> Future<T> submit(Callable<T> task) {
                return executorService.submit(task);
            }
        };
        return this;
    }

    @NonNull
    @Override
    public HookBuilder setCallbackHandler(@NonNull Handler callbackHandler) {
        this.userCallbackHandler = new SimpleExecutor() {
            @Override
            public <T> Future<T> submit(Callable<T> task) {
                final var t = new FutureTask<T>(task);
                callbackHandler.post(t);
                return t;
            }
        };
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

    private abstract static class BaseMatcherImpl<Self extends BaseMatcherImpl<Self, Reflect>, Reflect> {
        protected final boolean matchFirst;

        protected BaseMatcherImpl(boolean matchFirst) {
            this.matchFirst = matchFirst;
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class ReflectMatcherImpl<Self extends ReflectMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends ReflectMatcher<Base>, Reflect, SeqImpl extends LazySequenceImpl<?, ?, Reflect, Base, ?, Self>> extends BaseMatcherImpl<Self, Reflect> implements ReflectMatcher<Base> {
        private final static int packageFlag = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;

        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?> rootMatcher;

        @Nullable
        protected String key = null;

        @Nullable
        private volatile SeqImpl lazySequence = null;

        protected int includeModifiers = 0; // (real & includeModifiers) == includeModifiers
        protected int excludeModifiers = 0; // (real & excludeModifiers) == 0

        protected volatile boolean pending = true;

        @NonNull
        protected final AtomicInteger leafCount = new AtomicInteger(1);

        @Nullable
        protected volatile Iterable<Reflect> candidates = null;

        private final Observer<?> dependencyCallback = new Observer<>() {
            @Override
            public void onMatch(@NonNull Object result) {
                if (leafCount.decrementAndGet() == 1) {
                    var c = candidates;
                    if (c != null) {
                        candidates = null;
                        doMatch(c);
                    }
                }
            }

            @Override
            public void onMiss() {
                miss();
            }
        };

        protected ReflectMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
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
                seq.matches = Collections.singletonList(exact);
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
        protected final <T extends ReflectMatchImpl<T, U, RR, ?, ?>, U extends ReflectMatch<U, RR, ?>, RR> T addDependency(@Nullable T field, @NonNull U input) {
            final var in = (T) input;
            if (field != null) {
                in.removeObserver(dependencyCallback);
            } else {
                leafCount.incrementAndGet();
            }
            in.addObserver((Observer<RR>) dependencyCallback);
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

        protected final void match(@NonNull Iterable<Reflect> matches) {
            final var lazySequence = this.lazySequence;
            if (lazySequence == null) {
                throw new IllegalStateException("Illegal state when doMatch");
            }
            leafCount.set(0);
            lazySequence.match(matches);
        }

        // do match on reflect
        protected final void doMatch(@NonNull Iterable<Reflect> candidates) {
            if (leafCount.getAndDecrement() != 1) {
                this.candidates = candidates;
                return;
            }
            final var matches = new ArrayList<Reflect>();
            for (final var candidate : candidates) {
                if (doMatch(candidate)) {
                    matches.add(candidate);
                    if (matchFirst) {
                        break;
                    }
                }
            }
            if (matches.isEmpty()) {
                miss();
            } else {
                match(matches);
            }
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

        protected final void miss() {
            final var lazySequence = this.lazySequence;
            if (lazySequence == null) {
                throw new IllegalStateException("Illegal state when miss");
            }
            leafCount.set(0);
            lazySequence.miss();
        }
    }

    private final class ClassMatcherImpl extends ReflectMatcherImpl<ClassMatcherImpl, ClassMatcher, Class<?>, ClassLazySequenceImpl> implements ClassMatcher {
        @Nullable
        private ClassMatchImpl superClass = null;

        @Nullable
        private StringMatchImpl name = null;

        @Nullable
        private ReflectSyntaxImpl<ClassMatch, ?, Class<?>> containsInterfaces = null;

        private ClassMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
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
                return superClass != null && this.superClass.match == superClass;
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

    private final class ParameterMatcherImpl extends ReflectMatcherImpl<ParameterMatcherImpl, ParameterMatcher, Parameter, ParameterLazySequenceImpl> implements ParameterMatcher {
        private int index = -1;

        @Nullable
        private ClassMatchImpl type = null;

        private ParameterMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
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
            return type == null || type.match == parameter.getType();
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
    private abstract class MemberMatcherImpl<Self extends MemberMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends MemberMatcher<Base>, Reflect extends Member, SeqImpl extends MemberLazySequenceImpl<?, ?, Reflect, Base, ?, Self>> extends ReflectMatcherImpl<Self, Base, Reflect, SeqImpl> implements MemberMatcher<Base> {
        @Nullable
        protected ClassMatchImpl declaringClass = null;

        protected boolean includeSuper = false;

        protected boolean includeInterface = false;

        protected MemberMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
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
            if (declaringClass != null) {
                final var declaringClass = this.declaringClass.match;
                return declaringClass != null && declaringClass.equals(reflect.getDeclaringClass());
            }
            return true;
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

    private final class FieldMatcherImpl extends MemberMatcherImpl<FieldMatcherImpl, FieldMatcher, Field, FieldLazySequenceImpl> implements HookBuilder.FieldMatcher {
        @Nullable
        private StringMatchImpl name = null;

        @Nullable
        private ClassMatchImpl type = null;

        private FieldMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
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
            if (type != null && type.match != field.getType()) return false;
            return name == null || name.doMatch(field.getName());
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
    private abstract class ExecutableMatcherImpl<Self extends ExecutableMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends ExecutableMatcher<Base>, Reflect extends Member, SeqImpl extends ExecutableLazySequenceImpl<?, ?, Reflect, Base, ?, Self>> extends MemberMatcherImpl<Self, Base, Reflect, SeqImpl> implements ExecutableMatcher<Base> {
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

        protected ExecutableMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
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
            this.opcodes = Arrays.copyOf(opcodes, opcodes.length);
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
                    s.miss();
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

        private MethodMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
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
            if (returnType != null && returnType.match != method.getReturnType()) return false;
            return name == null || name.doMatch(method.getName());
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
        private ConstructorMatcherImpl(@Nullable ReflectMatcherImpl<?, ?, ?, ?> rootMatcher, boolean matchFirst) {
            super(rootMatcher, matchFirst);
        }

        @NonNull
        @Override
        protected ConstructorLazySequenceImpl onBuild() {
            if (key != null) keyedConstructorMatchers.put(key, this);
            return new ConstructorLazySequenceImpl(rootMatcher);
        }
    }

    private final class StringMatcherImpl extends BaseMatcherImpl<StringMatcherImpl, String> {
        @Nullable
        private String exact = null;

        @Nullable
        private String prefix = null;

        private StringMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        private StringMatch build() {
            return new StringMatchImpl(this);
        }
    }

    private abstract class BaseSyntaxImpl<Match extends BaseMatch<Match, Reflect>, MatchImpl extends BaseMatchImpl<MatchImpl, Match, Reflect>, Reflect> implements Syntax<Match> {
        protected final class Operand {
            private @NonNull Object value;

            private Operand(@NonNull MatchImpl match) {
                this.value = match;
            }

            private Operand(@NonNull Syntax<Match> syntax) {
                this.value = syntax;
            }

            private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?>> Operand(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?> seq) {
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

        protected final @NonNull Operands operands;

        private BaseSyntaxImpl(@NonNull Syntax<Match> operand, char operator) {
            this.operands = new UnaryOperands(new Operand(operand), operator);
        }

        private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?>> BaseSyntaxImpl(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?> operand, char operator) {
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
    }


    @SuppressWarnings("unchecked")
    private final class ReflectSyntaxImpl<Match extends ReflectMatch<Match, Reflect, ?>, MatchImpl extends ReflectMatchImpl<MatchImpl, Match, Reflect, ?, ?>, Reflect> extends BaseSyntaxImpl<Match, MatchImpl, Reflect> implements Syntax<Match> {
        private ReflectSyntaxImpl(@NonNull Syntax<Match> operand, char operator) {
            super(operand, operator);
        }

        private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?>> ReflectSyntaxImpl(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?> operand, char operator) {
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
                return set.contains(((ReflectMatchImpl<?, ?, Reflect, ?, ?>) operand.value).match);
            } else if (operand.value instanceof StringMatchImpl) {
                var m = ((StringMatchImpl) operand.value);
                for (final var match : set) {
                    if (m.doMatch(match.toString())) return true;
                }
                return false;
            } else if (operand.value instanceof LazySequence) {
                final var matches = ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?>) operand.value).matches;
                if (matches == null) return false;
                if (operator == '^') {
                    for (final var match : matches) {
                        if (!set.contains(match)) return false;
                    }
                    return true;
                } else if (operator == 'v') {
                    for (final var match : matches) {
                        if (set.contains(match)) return true;
                    }
                }
                return false;
            } else {
                return ((ReflectSyntaxImpl<?, ?, Reflect>) operand.value).test(set);
            }
        }

        // TODO: instead of hash set, we should use a sorted set so that we can perform binary search
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

        private void addObserver(@NonNull Operand operand, @NonNull Observer<?> observer, @Nullable AtomicInteger count) {
            if (operand.value instanceof ReflectMatchImpl) {
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?>) operand.value).addObserver((Observer<Reflect>) observer);
                if (count != null) count.incrementAndGet();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?>) operand.value).addObserver((Observer<Iterable<Reflect>>) observer);
                if (count != null) count.incrementAndGet();
            } else {
                ((ReflectSyntaxImpl<?, ?, Reflect>) operand.value).addObserver(observer, count);
            }
        }

        void addObserver(@NonNull Observer<?> observer, @Nullable AtomicInteger count) {
            if (operands instanceof BaseSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                addObserver(binaryOperands.left, observer, count);
                addObserver(binaryOperands.right, observer, count);
            } else if (operands instanceof BaseSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                addObserver(unaryOperands.operand, observer, count);
            }
        }

        private void removeObserver(@NonNull Operand operand, @NonNull Observer<?> observer, @Nullable AtomicInteger count) {
            if (operand.value instanceof ReflectMatchImpl) {
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?>) operand.value).removeObserver(observer);
                if (count != null) count.decrementAndGet();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?>) operand.value).removeObserver((Observer<Iterable<Reflect>>) observer);
                if (count != null) count.decrementAndGet();
            } else {
                ((ReflectSyntaxImpl<?, ?, Reflect>) operand.value).removeObserver(observer, count);
            }
        }

        void removeObserver(@NonNull Observer<?> observer, @Nullable AtomicInteger count) {
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
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?>) operand.value).rootMatcher.setNonPending();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?>) operand.value).rootMatcher.setNonPending();
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

        private <M extends ReflectMatch<M, String, ?>, MI extends ReflectMatchImpl<MI, M, String, ?, ?>> StringSyntaxImpl(@NonNull LazySequenceImpl<?, M, String, ?, MI, ?> operand, char operator) {
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
    }

    @SuppressWarnings("unchecked")
    private abstract class LazySequenceImpl<Base extends LazySequence<Base, Match, Reflect, Matcher>, Match extends ReflectMatch<Match, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>, MatchImpl extends ReflectMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> implements LazySequence<Base, Match, Reflect, Matcher> {
        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?> rootMatcher;

        @Nullable
        protected volatile Iterable<Reflect> matches = null;

        @NonNull
        private final Object VALUE = new Object();

        @GuardedBy("this")
        private volatile boolean done = false;

        // specially cache `first` since it's the only one that do not need to define any callback
        @Nullable
        private volatile Match first = null;

        @GuardedBy("this")
        @NonNull
        private final Map<BaseObserver, Object> observers = new HashMap<>();

        @GuardedBy("this")
        @NonNull
        private final Queue<LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>> missReplacements = new LinkedList<>();

        protected LazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher) {
            this.rootMatcher = rootMatcher;
        }

        @NonNull
        @Override
        public final Match first() {
            var f = first;
            if (f == null) {
                final var m = newMatch();
                addObserver(new Observer<>() {
                    @Override
                    public void onMatch(@NonNull Iterable<Reflect> result) {
                        final var i = result.iterator();
                        if (i.hasNext()) m.match(i.next());
                        else onMiss();
                    }

                    @Override
                    public void onMiss() {
                        m.miss();
                    }
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    m.doMatch(result);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return (Match) m.build().first();
        }

        @NonNull
        @Override
        public final Base all(@NonNull Consumer<Matcher> consumer) {
            final var m = newMatcher(false);
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    m.doMatch(result);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return (Base) m.build();
        }

        @NonNull
        @Override
        public final Base onMatch(@NonNull Consumer<Iterable<Reflect>> consumer) {
            rootMatcher.setNonPending();
            addMatchObserver(result -> callbackHandler.submit(() -> consumer.accept(result)));
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base onMiss(@NonNull Runnable runnable) {
            addMissObserver(() -> callbackHandler.submit(runnable));
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
            missReplacements.add((LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>) substitute.get());
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base matchIfMiss(@NonNull Consumer<Matcher> consumer) {
            final var m = newMatcher(false);
            consumer.accept((Matcher) m);
            missReplacements.add((LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>) m.build());
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    final var c = binds.get(bind);
                    if (c == null) return;
                    final var old = c.decrementAndGet();
                    if (old >= 0) {
                        callbackHandler.submit(() -> consumer.accept(bind, result));
                        if (old == 0) {
                            callbackHandler.submit(bind::onMatch);
                        }
                    }
                }

                @Override
                public void onMiss() {
                    final var c = binds.get(bind);
                    if (c != null && c.getAndSet(0) > 0) callbackHandler.submit(bind::onMiss);
                }
            });
            return (Base) this;
        }

        private synchronized void performOnMatch() {
            var m = matches;
            if (m != null) {
                for (final var observer : observers.keySet()) {
                    if (observer instanceof MatchObserver) {
                        ((MatchObserver<Iterable<Reflect>>) observer).onMatch(m);
                    }
                }
            }
        }

        private synchronized void performOnMiss() {
            for (final var observer : observers.keySet()) {
                if (observer instanceof MissObserver) {
                    ((MissObserver) observer).onMiss();
                }
            }
        }

        protected final synchronized void match(@NonNull Iterable<Reflect> matches) {
            if (done) return;
            this.matches = matches;
            done = true;
            executorService.submit(this::performOnMatch);
        }

        protected final synchronized void miss() {
            if (done) return;
            final var replacement = missReplacements.poll();
            if (replacement != null) {
                replacement.rootMatcher.setNonPending();
                replacement.addObserver(new Observer<>() {
                    @Override
                    public void onMatch(@NonNull Iterable<Reflect> result) {
                        match(result);
                    }

                    @Override
                    public void onMiss() {
                        miss();
                    }
                });
            } else {
                done = true;
                executorService.submit(this::performOnMiss);
            }
        }

        @NonNull
        protected abstract MatchImpl newMatch();

        @NonNull
        protected abstract MatcherImpl newMatcher(boolean matchFirst);

        protected final synchronized void addObserver(@NonNull Observer<Iterable<Reflect>> observer) {
            observers.put(observer, VALUE);
            final var m = matches;
            if (m != null) observer.onMatch(m);
            else if (done) observer.onMiss();
        }

        protected final synchronized void addMatchObserver(@NonNull MatchObserver<Iterable<Reflect>> observer) {
            observers.put(observer, VALUE);
            final var m = matches;
            if (m != null) observer.onMatch(m);
        }

        protected final synchronized void addMissObserver(@NonNull MissObserver observer) {
            observers.put(observer, VALUE);
            final var m = matches;
            if (done && m == null) observer.onMiss();
        }

        protected final synchronized void removeObserver(@NonNull Observer<Iterable<Reflect>> observer) {
            observers.remove(observer);
        }
    }

    private class ClassLazySequenceImpl extends LazySequenceImpl<ClassLazySequence, ClassMatch, Class<?>, ClassMatcher, ClassMatchImpl, ClassMatcherImpl> implements ClassLazySequence {
        protected ClassLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Class<?>> result) {
                    m.setNonPending();
                    final var methods = new ArrayList<Method>();
                    for (final var type : result) {
                        methods.addAll(Arrays.asList(type.getDeclaredMethods()));
                    }
                    m.doMatch(methods);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
        }

        private void addConstructorsObserver(@NonNull ConstructorMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Class<?>> result) {
                    m.setNonPending();
                    final var constructors = new ArrayList<Constructor<?>>();
                    for (final var type : result) {
                        constructors.addAll(Arrays.asList(type.getDeclaredConstructors()));
                    }
                    m.doMatch(constructors);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
        }

        private void addFieldsObserver(@NonNull FieldMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Class<?>> result) {
                    m.setNonPending();
                    final var fields = new ArrayList<Field>();
                    for (final var type : result) {
                        fields.addAll(Arrays.asList(type.getDeclaredFields()));
                    }
                    m.doMatch(fields);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
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

    private final class ParameterLazySequenceImpl extends LazySequenceImpl<ParameterLazySequence, ParameterMatch, Parameter, ParameterMatcher, ParameterMatchImpl, ParameterMatcherImpl> implements ParameterLazySequence {
        private ParameterLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Parameter> result) {
                    m.setNonPending();
                    final var types = new ArrayList<Class<?>>();
                    for (final var parameter : result) {
                        types.add(parameter.getType());
                    }
                    m.doMatch(types);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
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

    private abstract class MemberLazySequenceImpl<Base extends MemberLazySequence<Base, Match, Reflect, Matcher>, Match extends MemberMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>, MatchImpl extends MemberMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl>, MatcherImpl extends MemberMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl> implements MemberLazySequence<Base, Match, Reflect, Matcher> {
        protected MemberLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addDeclaringClassesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    m.setNonPending();
                    final var declaringClasses = new ArrayList<Class<?>>();
                    for (final var type : result) {
                        declaringClasses.add(type.getDeclaringClass());
                    }
                    m.doMatch(declaringClasses);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
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

    private final class FieldLazySequenceImpl extends MemberLazySequenceImpl<FieldLazySequence, FieldMatch, Field, FieldMatcher, FieldMatchImpl, FieldMatcherImpl> implements FieldLazySequence {
        private FieldLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Field> result) {
                    m.setNonPending();
                    final var types = new ArrayList<Class<?>>();
                    for (final var type : result) {
                        types.add(type.getType());
                    }
                    m.doMatch(types);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
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

    private abstract class ExecutableLazySequenceImpl<Base extends ExecutableLazySequence<Base, Match, Reflect, Matcher>, Match extends ExecutableMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>, MatchImpl extends ExecutableMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl>, MatcherImpl extends ExecutableMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends MemberLazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl> implements ExecutableLazySequence<Base, Match, Reflect, Matcher> {
        private ExecutableLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addParametersObserver(ParameterMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
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
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
        }

        private void addParameterTypesObserver(ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
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
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
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
        private MethodLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addReturnTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Method> result) {
                    m.setNonPending();
                    final var types = new ArrayList<Class<?>>();
                    for (final var type : result) {
                        types.add(type.getReturnType());
                    }
                    m.doMatch(types);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
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
        private ConstructorLazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
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

    private abstract static class BaseMatchImpl<Self extends BaseMatchImpl<Self, Base, Reflect>, Base extends BaseMatch<Base, Reflect>, Reflect> implements BaseMatch<Base, Reflect> {
    }

    @SuppressWarnings("unchecked")
    private abstract class ReflectMatchImpl<Self extends ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>, Base extends ReflectMatch<Base, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends BaseMatchImpl<Self, Base, Reflect> implements ReflectMatch<Base, Reflect, Matcher> {
        @NonNull
        private final Object VALUE = new Object();

        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?> rootMatcher;

        @Nullable
        protected volatile String key = null;

        @Nullable
        protected volatile Reflect match = null;

        @GuardedBy("this")
        private volatile boolean done = false;

        @GuardedBy("this")
        @NonNull
        private final Map<BaseObserver, Object> observers = new HashMap<>();

        @GuardedBy("this")
        @NonNull
        private final Queue<ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>> missReplacements = new LinkedList<>();

        protected ReflectMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher) {
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
            addMatchObserver(result -> callbackHandler.submit(() -> consumer.accept(result)));
            return (Base) this;
        }

        @NonNull
        @Override
        public Base onMiss(@NonNull Runnable handler) {
            addMissObserver(() -> callbackHandler.submit(handler));
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
            missReplacements.add((ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>) m.build().first());
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Reflect result) {
                    final var c = binds.get(bind);
                    if (c == null) return;
                    final var old = c.decrementAndGet();
                    if (old >= 0) {
                        callbackHandler.submit(() -> consumer.accept(bind, result));
                        if (old == 0) {
                            callbackHandler.submit(bind::onMatch);
                        }
                    }
                }

                @Override
                public void onMiss() {
                    final var c = binds.get(bind);
                    if (c != null && c.getAndSet(0) > 0) callbackHandler.submit(bind::onMiss);
                }
            });
            return (Base) this;
        }

        protected final synchronized void addObserver(Observer<Reflect> observer) {
            observers.put(observer, VALUE);
            final var m = match;
            if (m != null) observer.onMatch(m);
            else if (done) observer.onMiss();
        }

        protected final synchronized void addMatchObserver(MatchObserver<Reflect> observer) {
            observers.put(observer, VALUE);
            final var m = match;
            if (m != null) observer.onMatch(m);
        }

        protected final synchronized void addMissObserver(MissObserver observer) {
            observers.put(observer, VALUE);
            final var m = match;
            if (done && m == null) observer.onMiss();
        }

        protected final synchronized void removeObserver(BaseObserver observer) {
            observers.remove(observer);
        }

        private synchronized void performOnMatch() {
            var m = match;
            if (m != null) {
                for (BaseObserver observer : observers.keySet()) {
                    if (observer instanceof MatchObserver)
                        ((MatchObserver<Reflect>) observer).onMatch(m);
                }
            }
        }

        private synchronized void performOnMiss() {
            for (BaseObserver observer : observers.keySet()) {
                if (observer instanceof MissObserver) ((MissObserver) observer).onMiss();
            }
        }

        protected final synchronized void match(Reflect match) {
            if (done) return;
            this.match = match;
            done = true;
            if (match instanceof AccessibleObject) {
                ((AccessibleObject) match).setAccessible(true);
            } else if (match instanceof ParameterImpl) {
                ((AccessibleObject) ((ParameterImpl) match).getDeclaringExecutable()).setAccessible(true);
            }
            executorService.submit(this::performOnMatch);
        }

        protected final synchronized void miss() {
            if (done) return;
            final var replacement = missReplacements.poll();
            if (replacement != null) {
                replacement.rootMatcher.setNonPending();
                replacement.addObserver(new Observer<>() {
                    @Override
                    public void onMatch(@NonNull Reflect result) {
                        match(result);
                    }

                    @Override
                    public void onMiss() {
                        miss();
                    }
                });
            } else {
                done = true;
                executorService.submit(this::performOnMiss);
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
    }

    private class ClassMatchImpl extends ReflectMatchImpl<ClassMatchImpl, ClassMatch, Class<?>, ClassMatcher, ClassMatcherImpl> implements ClassMatch {
        protected ClassMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    final var sup = result.getSuperclass();
                    if (sup != null) {
                        m.match(sup);
                    }
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }

        @NonNull
        @Override
        public final ClassLazySequence getInterfaces() {
            final var m = new ClassLazySequenceImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(List.of(result.getInterfaces()));
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }

        @NonNull
        @Override
        public final MethodLazySequence getDeclaredMethods() {
            var m = new MethodLazySequenceImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(List.of(result.getDeclaredMethods()));
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }

        @NonNull
        @Override
        public final ConstructorLazySequence getDeclaredConstructors() {
            final var m = new ConstructorLazySequenceImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(List.of(result.getDeclaredConstructors()));
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }

        @NonNull
        @Override
        public final FieldLazySequence getDeclaredFields() {
            final var m = new FieldLazySequenceImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(List.of(result.getDeclaredFields()));
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }

        @NonNull
        @Override
        public final ClassMatch getArrayType() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(Array.newInstance(result, 0).getClass());
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
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

    private final class ParameterMatchImpl extends ReflectMatchImpl<ParameterMatchImpl, ParameterMatch, Parameter, ParameterMatcher, ParameterMatcherImpl> implements ParameterMatch {
        int index = -1;

        private ParameterMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        protected ParameterMatcherImpl newFirstMatcher() {
            return new ParameterMatcherImpl(rootMatcher, true);
        }

        @Override
        protected void onKey(@Nullable String newKey, @Nullable String oldKey) {
            if (oldKey != null) {
                keyedParameterMatches.remove(oldKey);
            }
            if (newKey != null) {
                keyedParameterMatches.put(newKey, this);
            }
        }

        @NonNull
        @Override
        public ClassMatch getType() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Parameter result) {
                    m.match(result.getType());
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }
    }

    private abstract class MemberMatchImpl<Self extends MemberMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>, Base extends MemberMatch<Base, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl> implements MemberMatch<Base, Reflect, Matcher> {
        protected MemberMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ClassMatch getDeclaringClass() {
            final var m = new ClassMatchImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Member result) {
                    m.match(result.getDeclaringClass());
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }
    }

    private final class FieldMatchImpl extends MemberMatchImpl<FieldMatchImpl, FieldMatch, Field, FieldMatcher, FieldMatcherImpl> implements FieldMatch {
        private FieldMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Field result) {
                    m.match(result.getType());
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }
    }

    private abstract class ExecutableMatchImpl<Self extends ExecutableMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>, Base extends ExecutableMatch<Base, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>, MatcherImpl extends MemberMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends MemberMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl> implements ExecutableMatch<Base, Reflect, Matcher> {
        protected ExecutableMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ClassLazySequence getParameterTypes() {
            final var m = new ClassLazySequenceImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Reflect result) {
                    if (result instanceof Method) {
                        m.match(List.of(((Method) result).getParameterTypes()));
                    } else if (result instanceof Constructor) {
                        m.match(List.of(((Constructor<?>) result).getParameterTypes()));
                    }
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }

        @NonNull
        @Override
        public ParameterLazySequence getParameters() {
            final var m = new ParameterLazySequenceImpl(rootMatcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Reflect result) {
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
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
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
        private MethodMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
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
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Method result) {
                    m.match(result.getReturnType());
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }
    }

    private final class ConstructorMatchImpl extends ExecutableMatchImpl<ConstructorMatchImpl, ConstructorMatch, Constructor<?>, ConstructorMatcher, ConstructorMatcherImpl> implements ConstructorMatch {
        private ConstructorMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
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

        private StringMatchImpl(@NonNull StringMatcherImpl matcher) {
            this.matcher = matcher;
        }

        private boolean doMatch(@NonNull String value) {
            if (matcher.prefix != null && !value.startsWith(matcher.prefix)) return false;
            return matcher.exact == null || matcher.exact.equals(value);
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
        final var m = new StringMatcherImpl(true);
        m.exact = string;
        return m.build();
    }

    @NonNull
    @Override
    public StringMatch prefix(@NonNull String prefix) {
        final var m = new StringMatcherImpl(false);
        m.prefix = prefix;
        return m.build();
    }

    @NonNull
    @Override
    public StringMatch firstPrefix(@NonNull String prefix) {
        final var m = new StringMatcherImpl(true);
        m.prefix = prefix;
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

    public @NonNull CountDownLatch build() {
        dexAnalysis = dexAnalysis || forceDexAnalysis;
        loadMatchCache();

        var pendingTasks = ((PendingExecutor) executorService).pendingTasks;

        if (userExecutorService == null) {
            var e = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            executorService = new SimpleExecutor() {
                @Override
                <T> Future<T> submit(Callable<T> task) {
                    return e.submit(task);
                }
            };
        } else {
            executorService = userExecutorService;
        }

        var tasks = new ArrayList<Future<?>>(pendingTasks);
        for (var task : pendingTasks) {
            tasks.add(executorService.submit(task));
        }
        joinAndClearTasks(tasks);

        pendingTasks = ((PendingExecutor) callbackHandler).pendingTasks;

        if (userCallbackHandler == null) {
            var handler = new Handler(Looper.getMainLooper());
            callbackHandler = new SimpleExecutor() {
                @Override
                <T> Future<T> submit(Callable<T> task) {
                    var t = new FutureTask<>(task);
                    handler.post(t);
                    return t;
                }
            };
        } else {
            callbackHandler = userCallbackHandler;
        }
        for (var task : pendingTasks) {
            tasks.add(callbackHandler.submit(task));
        }
        joinAndClearTasks(tasks);

        if (dexAnalysis) {
            analysisDex();
        } else {
            analysisClassLoader();
        }
        CountDownLatch latch = new CountDownLatch(1);
        callbackHandler.submit(latch::countDown);
        return latch;
    }

    private void analysisDex() {
        var parsers = new ArrayList<DexParser>();
        try (var apk = new ZipFile(sourcePath)) {
            for (var i = 0; ; ++i) {
                var dex = apk.getEntry("classes" + (i == 0 ? "" : i + 1) + ".dex");
                if (dex == null) break;
                var buf = ByteBuffer.allocateDirect((int) dex.getSize());
                try (var in = apk.getInputStream(dex)) {
                    if (in.read(buf.array()) != buf.capacity()) {
                        throw new IOException("read dex failed");
                    }
                }
                parsers.add(ctx.parseDex(buf, false));
            }
        } catch (Throwable e) {
            if (exceptionHandler != null) exceptionHandler.test(e);
            return;
        }
        var tasks = new ArrayList<Future<?>>();
        for (var dex : parsers) {
            tasks.add(executorService.submit(() -> dex.visitDefinedClasses(new DexParser.ClassVisitor() {
                @Nullable
                @Override
                public DexParser.MemberVisitor visit(int clazz, int accessFlags, int superClass, @NonNull int[] interfaces, int sourceFile, @NonNull int[] staticFields, @NonNull int[] staticFieldsAccessFlags, @NonNull int[] instanceFields, @NonNull int[] instanceFieldsAccessFlags, @NonNull int[] directMethods, @NonNull int[] directMethodsAccessFlags, @NonNull int[] virtualMethods, @NonNull int[] virtualMethodsAccessFlags, @NonNull int[] annotations) {
                    // TODO
                    return null;
                }

                @Override
                public boolean stop() {
                    return false;
                }
            })));
        }
        joinAndClearTasks(tasks);
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

    private void joinAndClearTasks(List<Future<?>> tasks) {
        for (final var task : tasks) {
            try {
                task.get();
            } catch (Throwable e) {
                @NonNull Throwable throwable = e;
                if (throwable instanceof ExecutionException && throwable.getCause() != null) {
                    throwable = throwable.getCause();
                }
                if (exceptionHandler != null) {
                    exceptionHandler.test(throwable);
                }
            }
        }
        tasks.clear();
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
                if (cache.isEmpty()) hit.miss();
                else hit.match(reflector.loadClass(cache));
            } catch (Throwable ex) {
                hit.miss();
            }
        }
        for (var e : matchCache.methodCache.entrySet()) {
            var hit = keyedMethodMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                if (cache.isEmpty()) hit.miss();
                else hit.match(reflector.loadMethod(cache));
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.fieldCache.entrySet()) {
            var hit = keyedFieldMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                if (cache.isEmpty()) hit.miss();
                else hit.match(reflector.loadField(cache));
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.constructorCache.entrySet()) {
            var hit = keyedConstructorMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                if (cache.isEmpty()) hit.miss();
                else hit.match(reflector.loadConstructor(cache));
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.parameterCache.entrySet()) {
            var hit = keyedParameterMatches.get(e.getKey());
            if (hit == null) continue;
            try {
                var cache = e.getValue();
                var methodName = cache.getValue();
                var idx = cache.getKey();
                if (methodName.isEmpty()) hit.miss();
                var m = reflector.loadMethod(methodName);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    var p = m.getParameters()[idx];
                    hit.match(new ParameterImpl(idx, p.getType(), m, p.getModifiers()));
                } else {
                    var p = m.getParameterTypes()[idx];
                    hit.match(new ParameterImpl(idx, p, m, 0));
                }
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.classListCache.entrySet()) {
            var hit = keyedClassMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.miss();
                hit.match(reflector.loadClasses(value));
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.methodListCache.entrySet()) {
            var hit = keyedMethodMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.miss();
                hit.match(reflector.loadMethods(value));
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.fieldListCache.entrySet()) {
            var hit = keyedFieldMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.miss();
                hit.match(reflector.loadFields(value));
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.constructorListCache.entrySet()) {
            var hit = keyedConstructorMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.miss();
                hit.match(reflector.loadConstructors(value));
            } catch (Throwable ex) {
                hit.miss();
            }
        }

        for (var e : matchCache.parameterListCache.entrySet()) {
            var hit = keyedParameterMatchers.get(e.getKey());
            if (hit == null) continue;
            try {
                var value = e.getValue();
                if (value.isEmpty()) hit.miss();
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
            final List<Future<?>> tasks = new ArrayList<>();
            for (final var classMatcher : rootClassMatchers) {
                // not leaf
                if (classMatcher.leafCount.get() != 1) continue;
                if (classMatcher.pending) continue;
                hasMatched[0] = rootClassMatchers.remove(classMatcher) || hasMatched[0];
                final var task = executorService.submit(() -> {
                    var candidates = classMatcher.candidates;
                    if (candidates == null) {
                        TreeSetView<String> subset = classNames;
                        if (classMatcher.name != null) {
                            final var nameMatcher = classMatcher.name.matcher;
                            if (nameMatcher.exact != null) {
                                if (classNames.contains(nameMatcher.exact)) {
                                    subset = TreeSetView.ofSorted(new String[]{nameMatcher.exact});
                                } else {
                                    subset = TreeSetView.ofSorted(new String[0]);
                                }
                            } else if (nameMatcher.prefix != null) {
                                subset = classNames.subSet(nameMatcher.prefix, nameMatcher.prefix + Character.MAX_VALUE);
                            }
                        }
                        final ArrayList<Class<?>> c = new ArrayList<>(subset.size());
                        for (final var className : subset) {
                            // then check the rest conditions that need to load the class
                            final Class<?> theClass;
                            try {
                                theClass = Class.forName(className, false, classLoader);
                                c.add(theClass);
                            } catch (ClassNotFoundException e) {
                                if (exceptionHandler != null && !exceptionHandler.test(e)) {
                                    break;
                                }
                            }
                        }
                        candidates = c;
                    }
                    classMatcher.doMatch(candidates);
                });
                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (final var fieldMatcher : rootFieldMatchers) {
                // not leaf
                if (fieldMatcher.leafCount.get() != 1) continue;
                if (fieldMatcher.pending) continue;
                hasMatched[0] = rootFieldMatchers.remove(fieldMatcher) || hasMatched[0];
                final var task = executorService.submit(() -> {
                    var candidates = fieldMatcher.candidates;
                    if (candidates == null) {
                        final ArrayList<Class<?>> classList = new ArrayList<>();
                        if (fieldMatcher.declaringClass != null) {
                            var declaringClass = fieldMatcher.declaringClass.match;
                            if (declaringClass != null) classList.add(declaringClass);
                        } else {
                            if (exceptionHandler != null) {
                                exceptionHandler.test(new IllegalStateException("Match members without declaring class is not supported when not using dex analysis; set forceDexAnalysis to true to enable dex analysis."));
                            }
                            fieldMatcher.miss();
                            return;
                        }
                        final ArrayList<Field> c = new ArrayList<>();
                        for (final var theClass : classList) {
                            final var fields = theClass.getDeclaredFields();
                            // TODO: if (fieldMatcher.includeSuper)
                            c.addAll(List.of(fields));
                        }
                        candidates = c;
                    }
                    fieldMatcher.doMatch(candidates);
                });
                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (final var methodMatcher : rootMethodMatchers) {
                // not leaf
                if (methodMatcher.leafCount.get() != 1) continue;
                if (methodMatcher.pending) continue;
                hasMatched[0] = rootMethodMatchers.remove(methodMatcher) || hasMatched[0];
                final var task = executorService.submit(() -> {
                    var candidates = methodMatcher.candidates;
                    if (candidates == null) {
                        final ArrayList<Class<?>> classList = new ArrayList<>();
                        if (methodMatcher.declaringClass != null) {
                            var declaringClass = methodMatcher.declaringClass.match;
                            if (declaringClass != null) classList.add(declaringClass);
                        } else {
                            if (exceptionHandler != null) {
                                exceptionHandler.test(new IllegalStateException("Match members without declaring class is not supported when not using dex analysis; set forceDexAnalysis to true to enable dex analysis."));
                            }
                            methodMatcher.miss();
                            return;
                        }

                        var c = new ArrayList<Method>();
                        for (final var clazz : classList) {
                            final var methods = clazz.getDeclaredMethods();
                            c.addAll(List.of(methods));
                        }
                        candidates = c;
                    }
                    methodMatcher.doMatch(candidates);
                });

                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (final var constructorMatcher : rootConstructorMatchers) {
                // not leaf
                if (constructorMatcher.leafCount.get() != 1) continue;
                if (constructorMatcher.pending) continue;
                hasMatched[0] = rootConstructorMatchers.remove(constructorMatcher) || hasMatched[0];

                final var task = executorService.submit(() -> {
                    var candidates = constructorMatcher.candidates;
                    if (candidates == null) {
                        final ArrayList<Class<?>> classList = new ArrayList<>();

                        if (constructorMatcher.declaringClass != null && constructorMatcher.declaringClass.match != null) {
                            classList.add(constructorMatcher.declaringClass.match);
                        } else {
                            if (exceptionHandler != null) {
                                exceptionHandler.test(new IllegalStateException("Match members without declaring class is not supported when not using dex analysis; set forceDexAnalysis to true to enable dex analysis."));
                            }
                            constructorMatcher.miss();
                            return;
                        }

                        var c = new ArrayList<Constructor<?>>();
                        for (final var clazz : classList) {
                            final var constructors = clazz.getDeclaredConstructors();
                            c.addAll(List.of(constructors));
                        }
                        candidates = c;
                    }
                    constructorMatcher.doMatch(candidates);
                });

                tasks.add(task);
            }
            joinAndClearTasks(tasks);
        } while (hasMatched[0]);
    }

}
