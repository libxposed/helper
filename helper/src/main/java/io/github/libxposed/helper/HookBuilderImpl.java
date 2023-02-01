package io.github.libxposed.helper;

import android.annotation.SuppressLint;
import android.os.Handler;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import dalvik.system.BaseDexClassLoader;
import io.github.libxposed.api.XposedInterface;


// Matcher <-> LazySequence --> List<Observer -> Result -> Observer -> Result ... >
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
final class HookBuilderImpl implements HookBuilder {
    private final @NonNull XposedInterface ctx;
    private final @NonNull BaseDexClassLoader classLoader;

    private final @NonNull String sourcePath;

    private @Nullable Predicate<Throwable> exceptionHandler = null;

    private @Nullable Predicate<Map<String, Object>> cacheChecker = null;

    private @Nullable InputStream cacheInputStream = null;

    private @Nullable OutputStream cacheOutputStream = null;

    private boolean dexAnalysis = false;

    private boolean forceDexAnalysis = false;

    private boolean includeAnnotations = false;

    private final @NonNull ArrayList<ClassMatcherImpl> rootClassMatchers = new ArrayList<>();

    private final @NonNull ArrayList<FieldMatcherImpl> rootFieldMatchers = new ArrayList<>();

    private final @NonNull ArrayList<MethodMatcherImpl> rootMethodMatchers = new ArrayList<>();

    private final @NonNull ArrayList<ConstructorMatcherImpl> rootConstructorMatchers = new ArrayList<>();

    private final @NonNull HashMap<LazyBind, AtomicInteger> binds = new HashMap<>();

    private ExecutorService executorService;

    private Handler callbackHandler;

    @Nullable
    private MatchCache matchCache = null;

    private HashMap<String, TypeMatcherImpl<?, ?, ?>> keyedClassMatchers = new HashMap<>();

    private HashMap<String, FieldMatcherImpl> keyedFieldMatchers = new HashMap<>();

    private HashMap<String, MethodMatcherImpl> keyedMethodMatchers = new HashMap<>();

    private HashMap<String, ConstructorMatcherImpl> keyedConstructorMatchers = new HashMap<>();

    private HashMap<String, TypeMatchImpl<?, ?, ?, ?>> keyedClassMatches = new HashMap<>();

    private HashMap<String, FieldMatchImpl> keyedFieldMatches = new HashMap<>();

    private HashMap<String, MethodMatchImpl> keyedMethodMatches = new HashMap<>();

    private HashMap<String, ConstructorMatchImpl> keyedConstructorMatches = new HashMap<>();

    private static class MatchCache {
        HashMap<String, Object> cacheInfo = new HashMap<>();

        ConcurrentHashMap<String, HashSet<String>> classListCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, HashSet<String>> fieldListCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, HashSet<String>> methodListCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, HashSet<String>> constructorListCache = new ConcurrentHashMap<>();

        ConcurrentHashMap<String, String> classCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> fieldCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> methodCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> constructorCache = new ConcurrentHashMap<>();
    }

    private interface BaseObserver<T> {
    }

    private interface MatchObserver<T> extends BaseObserver<T> {
        void onMatch(@NonNull T result);
    }

    private interface MissObserver<T> extends BaseObserver<T> {
        void onMiss();
    }

    private interface Observer<T> extends MatchObserver<T>, MissObserver<T> {
    }

    HookBuilderImpl(@NonNull XposedInterface ctx, @NonNull BaseDexClassLoader classLoader, @NonNull String sourcePath) {
        this.ctx = ctx;
        this.classLoader = classLoader;
        this.sourcePath = sourcePath;
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

    private abstract static class BaseMatcherImpl<Self extends BaseMatcherImpl<Self, Reflect>, Reflect> {
        @Nullable
        protected Reflect exact = null;

        protected final boolean matchFirst;

        protected BaseMatcherImpl(boolean matchFirst) {
            this.matchFirst = matchFirst;
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class ReflectMatcherImpl<Self extends ReflectMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends ReflectMatcher<Base>, Reflect, SeqImpl extends LazySequenceImpl<?, ?, Reflect, Base, ?, Self>> extends BaseMatcherImpl<Self, Reflect> implements ReflectMatcher<Base> {
        private final static int packageFlag = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;

        @Nullable
        protected String key = null;

        @Nullable
        private volatile SeqImpl lazySequence = null;

        protected int includeModifiers = 0; // (real & includeModifiers) == includeModifiers
        protected int excludeModifiers = 0; // (real & excludeModifiers) == 0

        protected volatile boolean pending = true;

        @NonNull
        protected final AtomicInteger leafCount = new AtomicInteger(1);

        private final Observer<?> dependencyCallback = new Observer<>() {
            @Override
            public void onMatch(@NonNull Object result) {
                leafCount.decrementAndGet();
            }

            @Override
            public void onMiss() {
                miss();
            }
        };

        protected ReflectMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        protected final synchronized void ensureNotFinalized() {
            if (lazySequence != null) {
                throw new IllegalStateException("Cannot modify after finalized");
            }
        }

        protected final synchronized SeqImpl build(@NonNull ReflectMatcherImpl<?, ?, ?, ?> indirectMatcher, @Nullable Reflect exact) {
            this.exact = exact;
            final var hasExact = exact != null;
            final var lazySequence = onBuild(indirectMatcher);
            if (hasExact) {
                pending = true;
                leafCount.compareAndSet(1, 0);
                lazySequence.matches = Collections.singletonList(exact);
            }
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
            return build(this, exact);
        }

        protected final synchronized SeqImpl build() {
            return build(this, null);
        }

        protected final synchronized SeqImpl build(@NonNull ReflectMatcherImpl<?, ?, ?, ?> indirectMatcher) {
            return build(indirectMatcher, null);
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
        protected abstract SeqImpl onBuild(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher);

        protected abstract void setNonPending();

        @NonNull
        protected final <T extends ReflectMatchImpl<T, U, RR, ?, ?>, U extends ReflectMatch<U, RR, ?>, RR> T addDependency(@Nullable T field, @NonNull U input) {
            final var in = (T) input;
            if (field != null) {
                in.removeObserver((Observer<RR>) dependencyCallback);
            } else {
                leafCount.incrementAndGet();
            }
            in.addObserver((Observer<RR>) dependencyCallback);
            return in;
        }

        @NonNull
        protected final <T extends ContainerSyntaxImpl<M, ?, RR>, U extends ContainerSyntax<M>, M extends ReflectMatch<M, RR, ?>, RR> T addDependencies(@Nullable T field, @NonNull U input) {
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
        protected final boolean doMatch(@NonNull Iterable<Reflect> candidates) {
            if (leafCount.getAndDecrement() != 1) return false;
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
                return false;
            }
            match(matches);
            return true;
        }

        @CallSuper
        protected boolean doMatch(@NonNull Reflect reflect) {
            final int modifiers;
            if (reflect instanceof Class<?>) modifiers = ((Class<?>) reflect).getModifiers();
            else if (reflect instanceof Member) modifiers = ((Member) reflect).getModifiers();
            else return false;
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

    @SuppressWarnings("unchecked")
    private abstract class TypeMatcherImpl<Self extends TypeMatcherImpl<Self, Base, SeqImpl>, Base extends TypeMatcher<Base>, SeqImpl extends TypeLazySequenceImpl<?, ?, Base, ?, Self>> extends ReflectMatcherImpl<Self, Base, Class<?>, SeqImpl> implements TypeMatcher<Base> {
        @Nullable
        protected ClassMatchImpl superClass = null;

        @Nullable
        protected StringMatchImpl name = null;

        @Nullable
        protected ContainerSyntaxImpl<ClassMatch, ?, Class<?>> containsInterfaces = null;

        protected TypeMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @CallSuper
        @Override
        protected void setNonPending() {
            if (superClass != null) superClass.matcher.setNonPending();
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
        public final Base setName(@NonNull StringMatch name) {
            ensureNotFinalized();
            this.name = (StringMatchImpl) name;
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setSuperClass(@NonNull ClassMatch superClassMatch) {
            ensureNotFinalized();
            this.superClass = addDependency(this.superClass, superClassMatch);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setContainsInterfaces(@NonNull ContainerSyntax<ClassMatch> consumer) {
            ensureNotFinalized();
            this.containsInterfaces = addDependencies(this.containsInterfaces, consumer);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsAbstract(boolean isAbstract) {
            setModifier(isAbstract, Modifier.ABSTRACT);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsStatic(boolean isStatic) {
            setModifier(isStatic, Modifier.STATIC);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsFinal(boolean isFinal) {
            setModifier(isFinal, Modifier.FINAL);
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base setIsInterface(boolean isInterface) {
            setModifier(isInterface, Modifier.INTERFACE);
            return (Base) this;
        }
    }

    private final class ClassMatcherImpl extends TypeMatcherImpl<ClassMatcherImpl, ClassMatcher, ClassLazySequenceImpl> implements ClassMatcher {
        private ClassMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @NonNull
        @Override
        protected ClassLazySequenceImpl onBuild(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher) {
            if (key != null) keyedClassMatchers.put(key, this);
            return new ClassLazySequenceImpl(rootMatcher);
        }
    }

    private final class ParameterMatcherImpl extends TypeMatcherImpl<ParameterMatcherImpl, ParameterMatcher, ParameterLazySequenceImpl> implements ParameterMatcher {
        private int index = -1;

        private ParameterMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @NonNull
        @Override
        protected ParameterLazySequenceImpl onBuild(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher) {
            if (key != null) keyedClassMatchers.put(key, this);
            return new ParameterLazySequenceImpl(rootMatcher);
        }

        @NonNull
        @Override
        public ParameterMatcher setIndex(int index) {
            ensureNotFinalized();
            this.index = index;
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class MemberMatcherImpl<Self extends MemberMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends MemberMatcher<Base>, Reflect extends Member, SeqImpl extends MemberLazySequenceImpl<?, ?, Reflect, Base, ?, Self>> extends ReflectMatcherImpl<Self, Base, Reflect, SeqImpl> implements MemberMatcher<Base> {
        @Nullable
        protected ClassMatchImpl declaringClass = null;

        protected boolean includeSuper = false;

        protected boolean includeInterface = false;

        protected MemberMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @CallSuper
        @Override
        protected void setNonPending() {
            if (declaringClass != null) declaringClass.matcher.setNonPending();
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

        private FieldMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @Override
        protected void setNonPending() {
            super.setNonPending();
            if (type != null) type.matcher.setNonPending();
        }

        @NonNull
        @Override
        protected FieldLazySequenceImpl onBuild(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher) {
            if (key != null) keyedFieldMatchers.put(key, this);
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
        protected ContainerSyntaxImpl<ParameterMatch, ?, Class<?>> parameterTypes = null;

        @Nullable
        protected ContainerSyntaxImpl<StringMatch, ?, String> referredStrings = null;

        @Nullable
        protected ContainerSyntaxImpl<FieldMatch, ?, Field> assignedFields = null;

        @Nullable
        protected ContainerSyntaxImpl<FieldMatch, ?, Field> accessedFields = null;

        @Nullable
        protected ContainerSyntaxImpl<MethodMatch, ?, Method> invokedMethods = null;

        @Nullable
        protected ContainerSyntaxImpl<ConstructorMatch, ?, Constructor<?>> invokedConstructors = null;

        @Nullable
        protected byte[] opcodes = null;

        protected ExecutableMatcherImpl(boolean matchFirst) {
            super(matchFirst);
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
        public final Base setParameterTypes(@NonNull ContainerSyntax<ParameterMatch> parameterTypes) {
            ensureNotFinalized();
            this.parameterTypes = addDependencies(this.parameterTypes, parameterTypes);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setReferredStrings(@NonNull ContainerSyntax<StringMatch> referredStrings) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.referredStrings = (ContainerSyntaxImpl<StringMatch, ?, String>) referredStrings;
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setAssignedFields(@NonNull ContainerSyntax<FieldMatch> assignedFields) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.assignedFields = addDependencies(this.assignedFields, assignedFields);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setAccessedFields(@NonNull ContainerSyntax<FieldMatch> accessedFields) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.accessedFields = addDependencies(this.accessedFields, accessedFields);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setInvokedMethods(@NonNull ContainerSyntax<MethodMatch> invokedMethods) {
            ensureNotFinalized();
            dexAnalysis = true;
            this.invokedMethods = addDependencies(this.invokedMethods, invokedMethods);
            return (Base) this;
        }

        @DexAnalysis
        @NonNull
        @Override
        public final Base setInvokedConstructors(@NonNull ContainerSyntax<ConstructorMatch> invokedConstructors) {
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
    }

    private final class MethodMatcherImpl extends ExecutableMatcherImpl<MethodMatcherImpl, MethodMatcher, Method, MethodLazySequenceImpl> implements MethodMatcher {
        private @Nullable StringMatchImpl name = null;

        private @Nullable ClassMatchImpl returnType = null;

        private MethodMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @Override
        protected void setNonPending() {
            super.setNonPending();
            if (returnType != null) returnType.matcher.setNonPending();
        }

        @Override
        protected boolean doMatch(@NonNull Method method) {
            if (!super.doMatch(method)) return false;
            if (returnType != null && returnType.match != method.getReturnType()) return false;
            return name == null || name.doMatch(method.getName());
        }

        @NonNull
        @Override
        protected MethodLazySequenceImpl onBuild(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher) {
            if (key != null) keyedMethodMatchers.put(key, this);
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
        private ConstructorMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @NonNull
        @Override
        protected ConstructorLazySequenceImpl onBuild(@NonNull ReflectMatcherImpl<?, ?, ?, ?> rootMatcher) {
            if (key != null) keyedConstructorMatchers.put(key, this);
            return new ConstructorLazySequenceImpl(rootMatcher);
        }
    }

    private final class StringMatcherImpl extends BaseMatcherImpl<StringMatcherImpl, String> {
        @Nullable
        private String prefix = null;

        private StringMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        private StringMatch build() {
            return new StringMatchImpl(this);
        }
    }

    @SuppressWarnings("unchecked")
    private final class ContainerSyntaxImpl<Match extends BaseMatch<Match, Reflect>, MatchImpl extends BaseMatchImpl<MatchImpl, Match, Reflect>, Reflect> implements ContainerSyntax<Match> {
        private final class Operand {
            private @NonNull Object value;

            private Operand(@NonNull MatchImpl match) {
                this.value = match;
            }

            private Operand(@NonNull ContainerSyntax<Match> syntax) {
                this.value = syntax;
            }

            private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?>> Operand(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?> seq) {
                this.value = seq;
            }
        }

        private abstract class Operands {
            protected final char operator;

            protected Operands(char operator) {
                this.operator = operator;
            }
        }

        private final class UnaryOperands extends Operands {
            private final @NonNull Operand operand;

            private UnaryOperands(@NonNull Operand operand, char operator) {
                super(operator);
                this.operand = operand;
            }
        }

        private final class BinaryOperands extends Operands {
            private final @NonNull Operand left;
            private final @NonNull Operand right;

            private BinaryOperands(@NonNull Operand left, @NonNull Operand right, char operator) {
                super(operator);
                this.left = left;
                this.right = right;
            }
        }

        private final @NonNull Operands operands;

        private ContainerSyntaxImpl(@NonNull ContainerSyntax<Match> operand, char operator) {
            this.operands = new UnaryOperands(new Operand(operand), operator);
        }

        private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?, ?>> ContainerSyntaxImpl(@NonNull LazySequenceImpl<?, M, Reflect, ?, MI, ?> operand, char operator) {
            this.operands = new UnaryOperands(new Operand(operand), operator);
        }

        private ContainerSyntaxImpl(@NonNull MatchImpl operand, char operator) {
            this.operands = new UnaryOperands(new Operand(operand), operator);
        }

        private ContainerSyntaxImpl(@NonNull ContainerSyntax<Match> left, @NonNull ContainerSyntax<Match> right, char operator) {
            this.operands = new BinaryOperands(new Operand(left), new Operand(right), operator);
        }

        @NonNull
        @Override
        public ContainerSyntax<Match> and(@NonNull ContainerSyntax<Match> predicate) {
            return new ContainerSyntaxImpl<>(this, predicate, '&');
        }

        @NonNull
        @Override
        public ContainerSyntax<Match> or(@NonNull ContainerSyntax<Match> predicate) {
            return new ContainerSyntaxImpl<>(this, predicate, '|');
        }

        @NonNull
        @Override
        public ContainerSyntax<Match> not() {
            return new ContainerSyntaxImpl<>(this, '!');
        }

        private boolean operandTest(@NonNull Operand operand, @NonNull HashSet<Reflect> set, char operator) {
            if (operand.value instanceof ReflectMatchImpl) {
                return set.contains(((ReflectMatchImpl<?, ?, Reflect, ?, ?>) operand.value).match);
            } else if (operand.value instanceof StringMatchImpl) {
                // TODO
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
                return ((ContainerSyntaxImpl<?, ?, Reflect>) operand.value).test(set);
            }
        }

        private boolean test(@NonNull HashSet<Reflect> set) {
            if (operands instanceof ContainerSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                final var operator = binaryOperands.operator;
                boolean leftMatch = operandTest(binaryOperands.left, set, operator);
                if ((!leftMatch && operator == '&')) {
                    return false;
                } else if (leftMatch && operator == '|') {
                    return true;
                }
                return operandTest(binaryOperands.left, set, operator);
            } else if (operands instanceof ContainerSyntaxImpl.UnaryOperands) {
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
                ((ContainerSyntaxImpl<?, ?, Reflect>) operand.value).addObserver(observer, count);
            }
        }

        void addObserver(@NonNull Observer<?> observer, @Nullable AtomicInteger count) {
            if (operands instanceof ContainerSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                addObserver(binaryOperands.left, observer, count);
                addObserver(binaryOperands.right, observer, count);
            } else if (operands instanceof ContainerSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                addObserver(unaryOperands.operand, observer, count);
            }
        }

        private void removeObserver(@NonNull Operand operand, @NonNull Observer<?> observer, @Nullable AtomicInteger count) {
            if (operand.value instanceof ReflectMatchImpl) {
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?>) operand.value).removeObserver((Observer<Reflect>) observer);
                if (count != null) count.decrementAndGet();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?>) operand.value).removeObserver((Observer<Iterable<Reflect>>) observer);
                if (count != null) count.decrementAndGet();
            } else {
                ((ContainerSyntaxImpl<?, ?, Reflect>) operand.value).removeObserver(observer, count);
            }
        }

        void removeObserver(@NonNull Observer<?> observer, @Nullable AtomicInteger count) {
            if (operands instanceof ContainerSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                removeObserver(binaryOperands.left, observer, count);
                removeObserver(binaryOperands.right, observer, count);
            } else if (operands instanceof ContainerSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                removeObserver(unaryOperands.operand, observer, count);
            }
        }

        private void setNonPending(@NonNull Operand operand) {
            if (operand.value instanceof ReflectMatchImpl) {
                ((ReflectMatchImpl<?, ?, Reflect, ?, ?>) operand.value).matcher.setNonPending();
            } else if (operand.value instanceof LazySequenceImpl) {
                ((LazySequenceImpl<?, ?, Reflect, ?, ?, ?>) operand.value).matcher.setNonPending();
            } else {
                ((ContainerSyntaxImpl<?, ?, Reflect>) operand.value).setNonPending();
            }
        }

        void setNonPending() {
            if (operands instanceof ContainerSyntaxImpl.BinaryOperands) {
                BinaryOperands binaryOperands = (BinaryOperands) operands;
                setNonPending(binaryOperands.left);
                setNonPending(binaryOperands.right);
            } else if (operands instanceof ContainerSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                setNonPending(unaryOperands.operand);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class LazySequenceImpl<Base extends LazySequence<Base, Match, Reflect, Matcher>, Match extends ReflectMatch<Match, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>, MatchImpl extends ReflectMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> implements LazySequence<Base, Match, Reflect, Matcher> {
        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?> matcher;

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
        private final Map<BaseObserver<Iterable<Reflect>>, Object> observers = new HashMap<>();

        @GuardedBy("this")
        @NonNull
        private final Queue<LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>> missReplacements = new LinkedList<>();

        protected LazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            this.matcher = matcher;
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
            return (Match) m.build(this.matcher).first();
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
            return (Base) m.build(this.matcher);
        }

        @NonNull
        @Override
        public final Base onMatch(@NonNull Consumer<Iterable<Reflect>> consumer) {
            matcher.setNonPending();
            addMatchObserver(result -> callbackHandler.post(() -> consumer.accept(result)));
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base onMiss(@NonNull Runnable runnable) {
            addMissObserver(() -> callbackHandler.post(runnable));
            return (Base) this;
        }

        @NonNull
        @Override
        public final ContainerSyntax<Match> conjunction() {
            return new ContainerSyntaxImpl<>(this, '^');
        }

        @NonNull
        @Override
        public final ContainerSyntax<Match> disjunction() {
            return new ContainerSyntaxImpl<>(this, 'v');
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
            missReplacements.add((LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>) m.build(this.matcher));
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
                        callbackHandler.post(() -> consumer.accept(bind, result));
                        if (old == 0) {
                            callbackHandler.post(bind::onMatch);
                        }
                    }
                }

                @Override
                public void onMiss() {
                    final var c = binds.get(bind);
                    if (c != null && c.getAndSet(0) > 0) callbackHandler.post(bind::onMiss);
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
                    ((MissObserver<Iterable<Reflect>>) observer).onMiss();
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
                replacement.matcher.setNonPending();
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

        protected final synchronized void addMissObserver(@NonNull MissObserver<Iterable<Reflect>> observer) {
            observers.put(observer, VALUE);
            final var m = matches;
            if (done && m == null) observer.onMiss();
        }

        protected final synchronized void removeObserver(@NonNull Observer<Iterable<Reflect>> observer) {
            observers.remove(observer);
        }
    }

    private abstract class TypeLazySequenceImpl<Base extends TypeLazySequence<Base, Match, Matcher>, Match extends TypeMatch<Match, Matcher>, Matcher extends TypeMatcher<Matcher>, MatchImpl extends TypeMatchImpl<MatchImpl, Match, Matcher, MatcherImpl>, MatcherImpl extends TypeMatcherImpl<MatcherImpl, Matcher, ?>> extends LazySequenceImpl<Base, Match, Class<?>, Matcher, MatchImpl, MatcherImpl> implements TypeLazySequence<Base, Match, Matcher> {
        protected TypeLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addMethodsObserver(@NonNull MethodMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Class<?>> result) {
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
            final var m = new MethodMatcherImpl(false);
            matcher.accept(m);
            addMethodsObserver(m);
            return m.build(this.matcher);
        }

        @NonNull
        @Override
        public final MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher) {
            final var m = new MethodMatcherImpl(true);
            matcher.accept(m);
            addMethodsObserver(m);
            return m.build(this.matcher).first();
        }

        @NonNull
        @Override
        public final ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher) {
            final var m = new ConstructorMatcherImpl(false);
            matcher.accept(m);
            addConstructorsObserver(m);
            return m.build(this.matcher);
        }

        @NonNull
        @Override
        public final ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher) {
            final var m = new ConstructorMatcherImpl(true);
            matcher.accept(m);
            addConstructorsObserver(m);
            return m.build(this.matcher).first();
        }

        @NonNull
        @Override
        public final FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher) {
            final var m = new FieldMatcherImpl(false);
            matcher.accept(m);
            addFieldsObserver(m);
            return m.build(this.matcher);
        }

        @NonNull
        @Override
        public final FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher) {
            final var m = new FieldMatcherImpl(true);
            matcher.accept(m);
            addFieldsObserver(m);
            return m.build(this.matcher).first();
        }
    }

    private final class ClassLazySequenceImpl extends TypeLazySequenceImpl<ClassLazySequence, ClassMatch, ClassMatcher, ClassMatchImpl, ClassMatcherImpl> implements ClassLazySequence {
        private ClassLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        protected ClassMatchImpl newMatch() {
            return new ClassMatchImpl(matcher);
        }

        @NonNull
        @Override
        protected ClassMatcherImpl newMatcher(boolean matchFirst) {
            return new ClassMatcherImpl(matchFirst);
        }
    }

    private final class ParameterLazySequenceImpl extends TypeLazySequenceImpl<ParameterLazySequence, ParameterMatch, ParameterMatcher, ParameterMatchImpl, ParameterMatcherImpl> implements ParameterLazySequence {
        private ParameterLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        protected ParameterMatchImpl newMatch() {
            return new ParameterMatchImpl(matcher);
        }

        @NonNull
        @Override
        protected ParameterMatcherImpl newMatcher(boolean matchFirst) {
            return new ParameterMatcherImpl(matchFirst);
        }
    }

    private abstract class MemberLazySequenceImpl<Base extends MemberLazySequence<Base, Match, Reflect, Matcher>, Match extends MemberMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>, MatchImpl extends MemberMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl>, MatcherImpl extends MemberMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends LazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl> implements MemberLazySequence<Base, Match, Reflect, Matcher> {
        protected MemberLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addDeclaringClassesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
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
            final var m = new ClassMatcherImpl(false);
            matcher.accept(m);
            addDeclaringClassesObserver(m);
            return m.build(this.matcher);
        }

        @NonNull
        @Override
        public final ClassMatch firstDeclaringClass(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(true);
            matcher.accept(m);
            addDeclaringClassesObserver(m);
            return m.build(this.matcher).first();
        }
    }

    private final class FieldLazySequenceImpl extends MemberLazySequenceImpl<FieldLazySequence, FieldMatch, Field, FieldMatcher, FieldMatchImpl, FieldMatcherImpl> implements FieldLazySequence {
        private FieldLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Field> result) {
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
            return new FieldMatchImpl(matcher);
        }

        @NonNull
        @Override
        protected FieldMatcherImpl newMatcher(boolean matchFirst) {
            return new FieldMatcherImpl(matchFirst);
        }

        @NonNull
        @Override
        public ClassLazySequence types(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(false);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build(this.matcher);
        }

        @NonNull
        @Override
        public ClassMatch firstType(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(true);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build(this.matcher).first();
        }
    }

    private abstract class ExecutableLazySequenceImpl<Base extends ExecutableLazySequence<Base, Match, Reflect, Matcher>, Match extends ExecutableMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>, MatchImpl extends ExecutableMatchImpl<MatchImpl, Match, Reflect, Matcher, MatcherImpl>, MatcherImpl extends ExecutableMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends MemberLazySequenceImpl<Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl> implements ExecutableLazySequence<Base, Match, Reflect, Matcher> {
        private void addParametersObserver(ParameterMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    final var parameters = new ArrayList<Class<?>>();
                    final var idx = m.index;
                    if (idx == -1) {
                        for (final var r : result) {
                            if (r instanceof Method) {
                                parameters.addAll(Arrays.asList(((Method) r).getParameterTypes()));
                            } else if (r instanceof Constructor) {
                                parameters.addAll(Arrays.asList(((Constructor<?>) r).getParameterTypes()));
                            }
                        }
                    } else {
                        for (final var r : result) {
                            if (r instanceof Method) {
                                var params = ((Method) r).getParameterTypes();
                                if (idx < params.length) {
                                    parameters.add(params[idx]);
                                }
                            } else if (r instanceof Constructor) {
                                var params = ((Constructor<?>) r).getParameterTypes();
                                if (idx < params.length) {
                                    parameters.add(params[idx]);
                                }
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


        private ExecutableLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ParameterLazySequence parameters(@NonNull Consumer<ParameterMatcher> matcher) {
            final var m = new ParameterMatcherImpl(false);
            addParametersObserver(m);
            return m.build(this.matcher);
        }

        @NonNull
        @Override
        public final ParameterMatch firstParameter(@NonNull Consumer<ParameterMatcher> matcher) {
            final var m = new ParameterMatcherImpl(true);
            addParametersObserver(m);
            return m.build(this.matcher).first();
        }
    }

    private final class MethodLazySequenceImpl extends ExecutableLazySequenceImpl<MethodLazySequence, MethodMatch, Method, MethodMatcher, MethodMatchImpl, MethodMatcherImpl> implements MethodLazySequence {
        private MethodLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addReturnTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Method> result) {
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
            return new MethodMatchImpl(matcher);
        }

        @NonNull
        @Override
        protected MethodMatcherImpl newMatcher(boolean matchFirst) {
            return new MethodMatcherImpl(matchFirst);
        }

        @NonNull
        @Override
        public ClassLazySequence returnTypes(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(false);
            matcher.accept(m);
            addReturnTypesObserver(m);
            return m.build(this.matcher);
        }

        @NonNull
        @Override
        public ClassMatch firstReturnType(@NonNull Consumer<ClassMatcher> matcher) {
            final var m = new ClassMatcherImpl(true);
            matcher.accept(m);
            addReturnTypesObserver(m);
            return m.build(this.matcher).first();
        }
    }

    private final class ConstructorLazySequenceImpl extends ExecutableLazySequenceImpl<ConstructorLazySequence, ConstructorMatch, Constructor<?>, ConstructorMatcher, ConstructorMatchImpl, ConstructorMatcherImpl> implements ConstructorLazySequence {
        private ConstructorLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        protected ConstructorMatchImpl newMatch() {
            return new ConstructorMatchImpl(matcher);
        }

        @NonNull
        @Override
        protected ConstructorMatcherImpl newMatcher(boolean matchFirst) {
            return new ConstructorMatcherImpl(matchFirst);
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class BaseMatchImpl<Self extends BaseMatchImpl<Self, Base, Reflect>, Base extends BaseMatch<Base, Reflect>, Reflect> implements BaseMatch<Base, Reflect> {
        protected BaseMatchImpl() {
        }

        @NonNull
        @Override
        public final ContainerSyntax<Base> observe() {
            return new ContainerSyntaxImpl<>((Self) this, '+');
        }

        @NonNull
        @Override
        public final ContainerSyntax<Base> reverse() {
            return new ContainerSyntaxImpl<>((Self) this, '-');
        }

    }

    @SuppressWarnings("unchecked")
    private abstract class ReflectMatchImpl<Self extends ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>, Base extends ReflectMatch<Base, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends BaseMatchImpl<Self, Base, Reflect> implements ReflectMatch<Base, Reflect, Matcher> {
        @NonNull
        private final Object VALUE = new Object();

        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?> matcher;

        @Nullable
        protected volatile String key = null;

        @Nullable
        protected volatile Reflect match = null;

        @GuardedBy("this")
        private volatile boolean done = false;

        @GuardedBy("this")
        @NonNull
        private final Map<BaseObserver<Reflect>, Object> observers = new HashMap<>();

        @GuardedBy("this")
        @NonNull
        private final Queue<ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>> missReplacements = new LinkedList<>();

        protected ReflectMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            this.matcher = matcher;
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
            matcher.setNonPending();
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base onMatch(@NonNull Consumer<Reflect> consumer) {
            matcher.setNonPending();
            addMatchObserver(result -> callbackHandler.post(() -> consumer.accept(result)));
            return (Base) this;
        }

        @NonNull
        @Override
        public Base onMiss(@NonNull Runnable handler) {
            addMissObserver(() -> callbackHandler.post(handler));
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
                        callbackHandler.post(() -> consumer.accept(bind, result));
                        if (old == 0) {
                            callbackHandler.post(bind::onMatch);
                        }
                    }
                }

                @Override
                public void onMiss() {
                    final var c = binds.get(bind);
                    if (c != null && c.getAndSet(0) > 0) callbackHandler.post(bind::onMiss);
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

        protected final synchronized void addMissObserver(MissObserver<Reflect> observer) {
            observers.put(observer, VALUE);
            final var m = match;
            if (done && m == null) observer.onMiss();
        }

        protected final synchronized void removeObserver(BaseObserver<Reflect> observer) {
            observers.remove(observer);
        }

        private synchronized void performOnMatch() {
            var m = match;
            if (m != null) {
                for (BaseObserver<Reflect> observer : observers.keySet()) {
                    if (observer instanceof MatchObserver)
                        ((MatchObserver<Reflect>) observer).onMatch(m);
                }
            }
        }

        private synchronized void performOnMiss() {
            for (BaseObserver<Reflect> observer : observers.keySet()) {
                if (observer instanceof MissObserver)
                    ((MissObserver<Reflect>) observer).onMiss();
            }
        }

        protected final synchronized void match(Reflect match) {
            if (done) return;
            this.match = match;
            done = true;
            executorService.submit(this::performOnMatch);
        }

        protected final synchronized void miss() {
            if (done) return;
            final var replacement = missReplacements.poll();
            if (replacement != null) {
                replacement.matcher.setNonPending();
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
    }

    private abstract class TypeMatchImpl<Self extends TypeMatchImpl<Self, Base, Matcher, MatcherImpl>, Base extends TypeMatch<Base, Matcher>, Matcher extends TypeMatcher<Matcher>, MatcherImpl extends TypeMatcherImpl<MatcherImpl, Matcher, ?>> extends ReflectMatchImpl<Self, Base, Class<?>, Matcher, MatcherImpl> implements TypeMatch<Base, Matcher> {
        protected TypeMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ClassMatch getSuperClass() {
            final var m = new ClassMatchImpl(matcher);
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
            final var m = new ClassLazySequenceImpl(matcher);
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
            var m = new MethodLazySequenceImpl(matcher);
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
            final var m = new ConstructorLazySequenceImpl(matcher);
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
            final var m = new FieldLazySequenceImpl(matcher);
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
            final var m = new ClassMatchImpl(matcher);
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

    private final class ClassMatchImpl extends TypeMatchImpl<ClassMatchImpl, ClassMatch, ClassMatcher, ClassMatcherImpl> implements ClassMatch {
        private ClassMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        protected ClassMatcherImpl newFirstMatcher() {
            return new ClassMatcherImpl(true);
        }

        @NonNull
        @Override
        public ParameterMatch asParameter(int index) {
            final var m = new ParameterMatchImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(result, index);
                }

                @Override
                public void onMiss() {
                    m.miss();
                }
            });
            return m;
        }
    }

    private final class ParameterMatchImpl extends TypeMatchImpl<ParameterMatchImpl, ParameterMatch, ParameterMatcher, ParameterMatcherImpl> implements ParameterMatch {
        int index = -1;

        private ParameterMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        protected ParameterMatcherImpl newFirstMatcher() {
            return new ParameterMatcherImpl(true);
        }

        private void match(Class<?> type, int index) {
            this.index = index;
            super.match(type);
        }
    }

    private abstract class MemberMatchImpl<Self extends MemberMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl>, Base extends MemberMatch<Base, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, ?>> extends ReflectMatchImpl<Self, Base, Reflect, Matcher, MatcherImpl> implements MemberMatch<Base, Reflect, Matcher> {
        protected MemberMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ClassMatch getDeclaringClass() {
            final var m = new ClassMatchImpl(matcher);
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
            return new FieldMatcherImpl(true);
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
            final var m = new ClassMatchImpl(matcher);
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
        public final ParameterLazySequence getParameterTypes() {
            final var m = new ParameterLazySequenceImpl(matcher);
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
            return new MethodMatcherImpl(true);
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
            final var m = new ClassMatchImpl(matcher);
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
            return new ConstructorMatcherImpl(true);
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
    }

    @NonNull
    @Override
    public MethodLazySequence methods(@NonNull Consumer<MethodMatcher> matcher) {
        final var m = new MethodMatcherImpl(false);
        rootMethodMatchers.add(m);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher) {
        final var m = new MethodMatcherImpl(true);
        rootMethodMatchers.add(m);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher) {
        final var m = new ConstructorMatcherImpl(false);
        rootConstructorMatchers.add(m);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher) {
        final var m = new ConstructorMatcherImpl(true);
        rootConstructorMatchers.add(m);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher) {
        final var m = new FieldMatcherImpl(false);
        rootFieldMatchers.add(m);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher) {
        final var m = new FieldMatcherImpl(true);
        rootFieldMatchers.add(m);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public ClassLazySequence classes(@NonNull Consumer<ClassMatcher> matcher) {
        final var m = new ClassMatcherImpl(false);
        rootClassMatchers.add(m);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public ClassMatch firstClass(@NonNull Consumer<ClassMatcher> matcher) {
        final var m = new ClassMatcherImpl(true);
        rootClassMatchers.add(m);
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
        // TODO: support binary name
        final var m = new ClassMatcherImpl(true);
        Class<?> exact = null;
        try {
            exact = Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            if (exceptionHandler != null) exceptionHandler.test(e);
        }
        return m.build(exact).first();
    }

    @NonNull
    @Override
    public ClassMatch exact(@NonNull Class<?> clazz) {
        final var m = new ClassMatcherImpl(true);
        return m.build(clazz).first();
    }

    @NonNull
    @Override
    public MethodMatch exactMethod(@NonNull String signature) {
        final var m = new MethodMatcherImpl(true);
        // TODO
        return m.build().first();
    }

    @NonNull
    @Override
    public MethodMatch exact(@NonNull Method method) {
        final var m = new MethodMatcherImpl(true);
        return m.build(method).first();
    }

    @NonNull
    @Override
    public ConstructorMatch exactConstructor(@NonNull String signature) {
        final var m = new ConstructorMatcherImpl(true);
        // TODO
        return m.build().first();
    }

    @NonNull
    @Override
    public ConstructorMatch exact(@NonNull Constructor<?> constructor) {
        final var m = new ConstructorMatcherImpl(true);
        return m.build(constructor).first();
    }

    @NonNull
    @Override
    public FieldMatch exactField(@NonNull String signature) {
        final var m = new FieldMatcherImpl(true);
        // TODO
        return m.build().first();
    }

    @NonNull
    @Override
    public FieldMatch exact(@NonNull Field field) {
        final var m = new FieldMatcherImpl(true);
        return m.build(field).first();
    }

    @NonNull
    @Override
    public ParameterMatch exactParameter(@NonNull String signature, int index) {
        return null;
    }

    @NonNull
    @Override
    public ParameterMatch exact(@NonNull Class<?> clazz, int index) {
        final var m = new ParameterMatcherImpl(true);
        m.index = index;
        return m.build(clazz).first();
    }

    @NonNull
    @Override
    public ContainerSyntax<ParameterMatch> exact(@NonNull Class<?>... params) {
        return null;
    }

    public @NonNull CountDownLatch build() {
        dexAnalysis = dexAnalysis || forceDexAnalysis;
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        if (dexAnalysis) {
            analysisDex();
        } else {
            analysisClassLoader();
        }
        CountDownLatch latch = new CountDownLatch(1);
        callbackHandler.post(latch::countDown);
        return latch;
    }

    private void analysisDex() {

    }

    // return first element that is greater than or equal to key
    private static <T extends Comparable<T>> int binarySearchLowerBound(final List<T> list, T key) {
        int low = 0, high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = list.get(mid).compareTo(key);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return low;
    }

    private static <T extends Comparable<T>> ArrayList<T> merge(final List<T> a, final List<T> b) {
        ArrayList<T> res = new ArrayList<>(a.size() + b.size());
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            int cmp = a.get(i).compareTo(b.get(j));
            if (cmp < 0) res.add(a.get(i++));
            else if (cmp > 0) res.add(b.get(j++));
            else {
                res.add(a.get(i++));
                j++;
            }
        }
        res.addAll(0, a);
        while (i < a.size()) res.add(a.get(i++));
        while (j < b.size()) res.add(b.get(j++));
        return res;
    }

    private List<String> getAllClassNamesFromClassLoader() throws NoSuchFieldException, IllegalAccessException {
        List<String> res = new ArrayList<>();
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
            res = merge(res, Collections.list(entries));
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

                    matchCache.classCache = (ConcurrentHashMap<String, String>) in.readObject();
                    matchCache.methodCache = (ConcurrentHashMap<String, String>) in.readObject();
                    matchCache.fieldCache = (ConcurrentHashMap<String, String>) in.readObject();
                    matchCache.constructorCache = (ConcurrentHashMap<String, String>) in.readObject();
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
        var bkExecutorService = executorService;
        ArrayList<Runnable> pendingTasks = new ArrayList<>();
        executorService = new ExecutorService() {
            @Override
            public void shutdown() {
            }

            @Override
            public List<Runnable> shutdownNow() {
                return null;
            }

            @Override
            public boolean isShutdown() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isTerminated() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Future<?> submit(Runnable task) {
                pendingTasks.add(task);
                return null;
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void execute(Runnable command) {
                throw new UnsupportedOperationException();
            }
        };
        for (var e : matchCache.classCache.entrySet()) {
            try {
                TypeMatchImpl<?, ?, ?, ?> hit = keyedClassMatches.get(e.getKey());
                if (hit == null) continue;
                var c = Class.forName(e.getValue(), false, classLoader);
                hit.match(c);
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }
        for (var e : matchCache.methodCache.entrySet()) {
            try {
                var hit = keyedMethodMatches.get(e.getKey());
                if (hit == null) continue;
                // TODO
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }

        for (var e : matchCache.fieldCache.entrySet()) {
            try {
                var hit = keyedFieldMatches.get(e.getKey());
                if (hit == null) continue;
                // TODO
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }

        for (var e : matchCache.constructorCache.entrySet()) {
            try {
                var hit = keyedConstructorMatches.get(e.getKey());
                if (hit == null) continue;
                // TODO
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }

        for (var e : matchCache.classListCache.entrySet()) {
            try {
                TypeMatcherImpl<?, ?, ?> hit = keyedClassMatchers.get(e.getKey());
                if (hit == null) continue;
                var value = e.getValue();
                var cs = new HashSet<Class<?>>(e.getValue().size());
                for (var v : value) {
                    cs.add(Class.forName(v, false, classLoader));
                }
                hit.match(cs);
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }

        for (var e : matchCache.methodListCache.entrySet()) {
            try {
                var hit = keyedMethodMatchers.get(e.getKey());
                if (hit == null) continue;
                // TODO
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }

        for (var e : matchCache.fieldListCache.entrySet()) {
            try {
                var hit = keyedFieldMatchers.get(e.getKey());
                if (hit == null) continue;
                // TODO
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }

        for (var e : matchCache.constructorListCache.entrySet()) {
            try {
                var hit = keyedConstructorMatchers.get(e.getKey());
                if (hit == null) continue;
                // TODO
            } catch (Throwable ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.test(ex);
                }
            }
        }

        executorService = bkExecutorService;
        var tasks = new ArrayList<Future<?>>(pendingTasks.size());
        for (var task : pendingTasks) {
            tasks.add(executorService.submit(task));
        }
        joinAndClearTasks(tasks);
    }

    private void analysisClassLoader() {
        final List<String> classNames;
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
                final var task = executorService.submit(() -> {
                    int low = 0, high = classNames.size() - 1;
                    if (classMatcher.name != null) {
                        final var nameMatcher = classMatcher.name.matcher;
                        if (nameMatcher.prefix != null) {
                            low = binarySearchLowerBound(classNames, nameMatcher.prefix);
                            high = nameMatcher.matchFirst ? low + 1 : binarySearchLowerBound(classNames, nameMatcher.prefix + Character.MAX_VALUE);
                        }
                        if (nameMatcher.exact != null) {
                            low = binarySearchLowerBound(classNames, nameMatcher.exact);
                            if (low < classNames.size() && classNames.get(low).equals(nameMatcher.exact)) {
                                high = low + 1;
                            } else {
                                low = high + 1;
                            }
                        }
                    }
                    final ArrayList<Class<?>> candidates = new ArrayList<>(high - low);
                    for (int i = low; i < high && i < classNames.size(); i++) {
                        final var className = classNames.get(i);
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
                    hasMatched[0] = classMatcher.doMatch(candidates) || hasMatched[0];
                });
                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (final var fieldMatcher : rootFieldMatchers) {
                // not leaf
                if (fieldMatcher.leafCount.get() != 1) continue;
                if (fieldMatcher.pending) continue;

                final var task = executorService.submit(() -> {
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
                    final ArrayList<Field> candidates = new ArrayList<>();

                    for (final var theClass : classList) {
                        final var fields = theClass.getDeclaredFields();
                        // TODO: if (fieldMatcher.includeSuper)
                        candidates.addAll(List.of(fields));
                    }
                    hasMatched[0] = fieldMatcher.doMatch(candidates) || hasMatched[0];
                });
                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (final var methodMatcher : rootMethodMatchers) {
                // not leaf
                if (methodMatcher.leafCount.get() != 1) continue;
                if (methodMatcher.pending) continue;

                final var task = executorService.submit(() -> {
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

                    final ArrayList<Method> candidates = new ArrayList<>();

                    for (final var clazz : classList) {
                        final var methods = clazz.getDeclaredMethods();
                        candidates.addAll(List.of(methods));
                    }
                    hasMatched[0] = methodMatcher.doMatch(candidates) || hasMatched[0];
                });

                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (final var constructorMatcher : rootConstructorMatchers) {
                // not leaf
                if (constructorMatcher.leafCount.get() != 1) continue;
                if (constructorMatcher.pending) continue;

                final var task = executorService.submit(() -> {
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

                    final ArrayList<Constructor<?>> nameMatched = new ArrayList<>();

                    for (final var clazz : classList) {
                        final var constructors = clazz.getDeclaredConstructors();
                        for (final var constructor : constructors) {
                            final var modifiers = constructor.getModifiers();
                            if ((modifiers & constructorMatcher.includeModifiers) != constructorMatcher.includeModifiers)
                                continue;
                            if ((modifiers & constructorMatcher.excludeModifiers) != 0) continue;
                            final var typeArrays = constructor.getParameterTypes();
                            if (constructorMatcher.parameterCount >= 0 && constructorMatcher.parameterCount != typeArrays.length)
                                continue;
                            if (constructorMatcher.parameterTypes != null) {
                                final var parameterTypes = new HashSet<Class<?>>(typeArrays.length);
                                Collections.addAll(parameterTypes, typeArrays);
                                if (!constructorMatcher.parameterTypes.test(parameterTypes))
                                    continue;
                            }
                            nameMatched.add(constructor);
                            if (constructorMatcher.matchFirst) {
                                break;
                            }
                        }
                    }
                    hasMatched[0] = constructorMatcher.doMatch(nameMatched) || hasMatched[0];
                });

                tasks.add(task);
            }
            joinAndClearTasks(tasks);
        } while (hasMatched[0]);
    }

}
