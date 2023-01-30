package io.github.libxposed.helper;

import android.annotation.SuppressLint;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import dalvik.system.BaseDexClassLoader;
import io.github.libxposed.api.XposedInterface;


// Matcher <-> LazySequence --> List<Observer -> Result -> Observer -> Result ... >
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
final class HookBuilderImpl implements HookBuilder {
    private final @NonNull XposedInterface ctx;
    private final @NonNull BaseDexClassLoader classLoader;

    private final @NonNull String sourcePath;

    private @Nullable MatchResultImpl matchResult;

    private @Nullable Predicate<Throwable> exceptionHandler = null;

    private boolean dexAnalysis = false;

    private boolean forceDexAnalysis = false;

    private boolean includeAnnotations = false;

    private final @NonNull ArrayList<StringMatcherImpl> stringMatchers = new ArrayList<>();

    private final @NonNull ArrayList<ClassMatcherImpl> classMatchers = new ArrayList<>();

    private final @NonNull ArrayList<FieldMatcherImpl> fieldMatchers = new ArrayList<>();

    private final @NonNull ArrayList<MethodMatcherImpl> methodMatchers = new ArrayList<>();

    private final @NonNull ArrayList<ConstructorMatcherImpl> constructorMatchers = new ArrayList<>();

    private ExecutorService executorService;

    private interface Observer<T> {
        void onMatch(@NonNull T result);

        void onMiss();
    }

    private final class MatchResultImpl implements MatchResult {
        @NonNull
        @Override
        public Map<String, Class<?>> getMatchedClasses() {
            return null;
        }

        @NonNull
        @Override
        public Map<String, Field> getMatchedFields() {
            return null;
        }

        @NonNull
        @Override
        public Map<String, Method> getMatchedMethods() {
            return null;
        }

        @NonNull
        @Override
        public Map<String, Constructor<?>> getMatchedConstructors() {
            return null;
        }
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
    public HookBuilder setLastMatchResult(@NonNull MatchResult matchResult) {
        this.matchResult = (MatchResultImpl) matchResult;
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
    private abstract class ReflectMatcherImpl<Self extends ReflectMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends ReflectMatcher<Base>, Reflect, SeqImpl extends LazySequenceImpl<SeqImpl, ?, ?, Reflect, Base, ?, Self>> extends BaseMatcherImpl<Self, Reflect> implements ReflectMatcher<Base> {
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

        protected final synchronized SeqImpl build() {
            return lazySequence = onBuild();
        }

        @NonNull
        @Override
        public final Base setKey(@NonNull String key) {
            ensureNotFinalized();
            // TODO
//            pending = false;
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

        @NonNull
        protected final <T extends BaseMatchImpl<T, U, RR>, U extends BaseMatch<U, RR>, RR> T addDependency(@Nullable T field, @NonNull U input) {
            var in = (T) input;
            if (field != null) {
                in.removeObserver((Observer<RR>) dependencyCallback);
            } else {
                leafCount.incrementAndGet();
            }
            in.addObserver((Observer<RR>) dependencyCallback);
            return in;
        }

        @NonNull
        protected final <T extends ContainerSyntaxImpl<M, ?, RR>, U extends ContainerSyntax<M>, M extends BaseMatch<M, RR>, RR> T addDependencies(@Nullable T field, @NonNull U input) {
            var in = (T) input;
            // TODO
            if (field != null) {
                // ?
            }
//            input.addSupports(this);
            return in;
        }

        protected final void match(@NonNull Iterable<Reflect> matches) {
            var lazySequence = this.lazySequence;
            if (leafCount.get() != 1 || lazySequence == null) {
                throw new IllegalStateException("Illegal state when onMatch");
            }
            leafCount.decrementAndGet();
            if (!matches.iterator().hasNext()) {
                // TODO: on miss
                return;
            }
            lazySequence.match(matches);
        }

        protected final void setExact(@Nullable Reflect exact) {
            this.exact = exact;
            leafCount.compareAndSet(1, 0);
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class TypeMatcherImpl<Self extends TypeMatcherImpl<Self, Base, SeqImpl>, Base extends TypeMatcher<Base>, SeqImpl extends TypeLazySequenceImpl<SeqImpl, ?, ?, Base, ?, Self>> extends ReflectMatcherImpl<Self, Base, Class<?>, SeqImpl> implements TypeMatcher<Base> {
        @Nullable
        protected ClassMatchImpl superClass = null;

        @Nullable
        protected StringMatchImpl name = null;

        @Nullable
        protected ContainerSyntaxImpl<ClassMatch, ?, Class<?>> containsInterfaces = null;

        protected TypeMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @NonNull
        @Override
        public final Base setName(@NonNull StringMatch name) {
            ensureNotFinalized();
            this.name = addDependency(this.name, name);
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
        protected ClassLazySequenceImpl onBuild() {
            classMatchers.add(this);
            return new ClassLazySequenceImpl(this);
        }
    }

    private final class ParameterMatcherImpl extends TypeMatcherImpl<ParameterMatcherImpl, ParameterMatcher, ParameterLazySequenceImpl> implements ParameterMatcher {
        private int index = -1;

        private ParameterMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        @NonNull
        @Override
        protected ParameterLazySequenceImpl onBuild() {
            return new ParameterLazySequenceImpl(this);
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
    private abstract class MemberMatcherImpl<Self extends MemberMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends MemberMatcher<Base>, Reflect extends Member, SeqImpl extends MemberLazySequenceImpl<SeqImpl, ?, ?, Reflect, Base, ?, Self>> extends ReflectMatcherImpl<Self, Base, Reflect, SeqImpl> implements MemberMatcher<Base> {
        @Nullable
        protected ClassMatchImpl declaringClass = null;

        protected boolean includeSuper = false;

        protected boolean includeInterface = false;

        protected MemberMatcherImpl(boolean matchFirst) {
            super(matchFirst);
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

        @NonNull
        @Override
        protected FieldLazySequenceImpl onBuild() {
            fieldMatchers.add(this);
            return new FieldLazySequenceImpl(this);
        }

        @NonNull
        @Override
        public FieldMatcher setName(@NonNull StringMatch name) {
            ensureNotFinalized();
            this.name = addDependency(this.name, name);
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
    private abstract class ExecutableMatcherImpl<Self extends ExecutableMatcherImpl<Self, Base, Reflect, SeqImpl>, Base extends ExecutableMatcher<Base>, Reflect extends Member, SeqImpl extends ExecutableLazySequenceImpl<SeqImpl, ?, ?, Reflect, Base, ?, Self>> extends MemberMatcherImpl<Self, Base, Reflect, SeqImpl> implements ExecutableMatcher<Base> {
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
            this.referredStrings = addDependencies(this.referredStrings, referredStrings);
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

        @NonNull
        @Override
        protected MethodLazySequenceImpl onBuild() {
            methodMatchers.add(this);
            return new MethodLazySequenceImpl(this);
        }

        @NonNull
        @Override
        public MethodMatcher setName(@NonNull StringMatch name) {
            ensureNotFinalized();
            this.name = addDependency(this.name, name);
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
        protected ConstructorLazySequenceImpl onBuild() {
            constructorMatchers.add(this);
            return new ConstructorLazySequenceImpl(this);
        }
    }

    private final class StringMatcherImpl extends BaseMatcherImpl<StringMatcherImpl, String> {
        @Nullable
        private String prefix = null;

        private StringMatcherImpl(boolean matchFirst) {
            super(matchFirst);
        }

        private StringMatch build() {
            stringMatchers.add(this);
            return new StringMatchImpl(this);
        }
    }

    private final class ContainerSyntaxImpl<Match extends BaseMatch<Match, Reflect>, MatchImpl extends BaseMatchImpl<MatchImpl, Match, Reflect>, Reflect> implements ContainerSyntax<Match> {
        private final class Operand {
            private @NonNull Object value;

            private Operand(@NonNull MatchImpl match) {
                this.value = match;
            }

            private Operand(@NonNull ContainerSyntax<Match> syntax) {
                this.value = syntax;
            }

            private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?>> Operand(@NonNull LazySequenceImpl<?, ?, M, Reflect, ?, MI, ?> seq) {
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

        private <M extends ReflectMatch<M, Reflect, ?>, MI extends ReflectMatchImpl<MI, M, Reflect, ?>> ContainerSyntaxImpl(@NonNull LazySequenceImpl<?, ?, M, Reflect, ?, MI, ?> operand, char operator) {
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

        @SuppressWarnings("unchecked")
        private boolean operandTest(@NonNull Operand operand, @NonNull HashSet<Reflect> set, char operator) {
            if (operand.value instanceof ReflectMatchImpl) {
                return set.contains(((ReflectMatchImpl<?, ?, Reflect, ?>) operand.value).match);
            } else if (operand.value instanceof StringMatchImpl) {
                // TODO
                return false;
            } else if (operand.value instanceof LazySequence) {
                var matches = ((LazySequenceImpl<?, ?, ?, Reflect, ?, ?, ?>) operand.value).matches;
                if (matches == null) return false;
                if (operator == '^') {
                    for (var match : matches) {
                        if (!set.contains(match)) return false;
                    }
                    return true;
                } else if (operator == 'v') {
                    for (var match : matches) {
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
                var operator = binaryOperands.operator;
                boolean leftMatch = operandTest(binaryOperands.left, set, operator);
                if ((!leftMatch && operator == '&')) {
                    return false;
                } else if (leftMatch && operator == '|') {
                    return true;
                }
                return operandTest(binaryOperands.left, set, operator);
            } else if (operands instanceof ContainerSyntaxImpl.UnaryOperands) {
                UnaryOperands unaryOperands = (UnaryOperands) operands;
                var operator = unaryOperands.operator;
                boolean match = operandTest(unaryOperands.operand, set, operator);
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
    private abstract class LazySequenceImpl<Self extends LazySequenceImpl<Self, Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>, Base extends LazySequence<Base, Match, Reflect, Matcher>, Match extends ReflectMatch<Match, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>, MatchImpl extends ReflectMatchImpl<MatchImpl, Match, Reflect, Matcher>, MatcherImpl extends ReflectMatcherImpl<MatcherImpl, Matcher, Reflect, Self>> implements LazySequence<Base, Match, Reflect, Matcher> {
        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?> matcher;

        @Nullable
        protected volatile Iterable<Reflect> matches = null;

        @NonNull
        private final Object VALUE = new Object();

        @NonNull
        private final Map<Observer<Iterable<Reflect>>, Object> observers = new ConcurrentHashMap<>();

        protected LazySequenceImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            this.matcher = matcher;
        }

        @NonNull
        @Override
        public final Match first() {
            var m = newMatch();
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    m.match(result.iterator().next());
                }

                @Override
                public void onMiss() {

                }
            });
            return (Match) m;
        }

        @NonNull
        @Override
        public final Match first(@NonNull Consumer<Matcher> consumer) {
            var m = newMatcher(true);
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    m.match(result);
                }

                @Override
                public void onMiss() {

                }
            });
            return m.build().first();
        }

        @NonNull
        @Override
        public final Base all(@NonNull Consumer<Matcher> consumer) {
            var m = newMatcher(false);
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    m.match(result);
                }

                @Override
                public void onMiss() {

                }
            });
            return (Base) m.build();
        }

        @NonNull
        @Override
        public final Base onMatch(@NonNull Consumer<Iterable<Reflect>> consumer) {
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    consumer.accept(result);
                }

                @Override
                public void onMiss() {

                }
            });
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
            return null;
        }

        @NonNull
        @Override
        public final Base matchIfMiss(@NonNull Consumer<Matcher> consumer) {
            return null;
        }

        @NonNull
        @Override
        public final <Bind extends LazyBind> Base bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Iterable<Reflect>> consumer) {
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    consumer.accept(bind, result);
                }

                @Override
                public void onMiss() {

                }
            });
            return (Base) this;
        }

        protected final void match(@NonNull Iterable<Reflect> matches) {
            this.matches = matches;
            for (var observer : observers.keySet()) {
                observer.onMatch(matches);
            }
        }

        @NonNull
        protected abstract MatchImpl newMatch();

        @NonNull
        protected abstract MatcherImpl newMatcher(boolean matchFirst);

        @NonNull
        protected abstract Self newSelf();

        protected final void addObserver(@NonNull Observer<Iterable<Reflect>> observer) {
            observers.put(observer, VALUE);
            var m = matches;
            if (m != null) {
                observer.onMatch(m);
            }
        }

        protected final void removeObserver(@NonNull Observer<Iterable<Reflect>> observer) {
            observers.remove(observer);
        }
    }

    private abstract class TypeLazySequenceImpl<Self extends TypeLazySequenceImpl<Self, Base, Match, Matcher, MatchImpl, MatcherImpl>, Base extends TypeLazySequence<Base, Match, Matcher>, Match extends TypeMatch<Match, Matcher>, Matcher extends TypeMatcher<Matcher>, MatchImpl extends TypeMatchImpl<MatchImpl, Match, Matcher>, MatcherImpl extends TypeMatcherImpl<MatcherImpl, Matcher, Self>> extends LazySequenceImpl<Self, Base, Match, Class<?>, Matcher, MatchImpl, MatcherImpl> implements TypeLazySequence<Base, Match, Matcher> {
        protected TypeLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addMethodsObserver(@NonNull MethodMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Class<?>> result) {
                    var methods = new ArrayList<Method>();
                    for (var type : result) {
                        methods.addAll(Arrays.asList(type.getDeclaredMethods()));
                    }
                    m.match(methods);
                }

                @Override
                public void onMiss() {

                }
            });
        }

        private void addConstructorsObserver(@NonNull ConstructorMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Class<?>> result) {
                    var constructors = new ArrayList<Constructor<?>>();
                    for (var type : result) {
                        constructors.addAll(Arrays.asList(type.getDeclaredConstructors()));
                    }
                    m.match(constructors);
                }

                @Override
                public void onMiss() {

                }
            });
        }

        private void addFieldsObserver(@NonNull FieldMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Class<?>> result) {
                    var fields = new ArrayList<Field>();
                    for (var type : result) {
                        fields.addAll(Arrays.asList(type.getDeclaredFields()));
                    }
                    m.match(fields);
                }

                @Override
                public void onMiss() {

                }
            });
        }

        @NonNull
        @Override
        public final MethodLazySequence methods(@NonNull Consumer<MethodMatcher> matcher) {
            var m = new MethodMatcherImpl(false);
            matcher.accept(m);
            addMethodsObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher) {
            var m = new MethodMatcherImpl(true);
            matcher.accept(m);
            return m.build().first();
        }

        @NonNull
        @Override
        public final ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher) {
            var m = new ConstructorMatcherImpl(false);
            matcher.accept(m);
            addConstructorsObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher) {
            var m = new ConstructorMatcherImpl(true);
            matcher.accept(m);
            addConstructorsObserver(m);
            return m.build().first();
        }

        @NonNull
        @Override
        public final FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher) {
            var m = new FieldMatcherImpl(false);
            matcher.accept(m);
            addFieldsObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher) {
            var m = new FieldMatcherImpl(true);
            matcher.accept(m);
            addFieldsObserver(m);
            return m.build().first();
        }
    }

    private final class ClassLazySequenceImpl extends TypeLazySequenceImpl<ClassLazySequenceImpl, ClassLazySequence, ClassMatch, ClassMatcher, ClassMatchImpl, ClassMatcherImpl> implements ClassLazySequence {
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

        @NonNull
        @Override
        protected ClassLazySequenceImpl newSelf() {
            return new ClassLazySequenceImpl(matcher);
        }
    }

    private final class ParameterLazySequenceImpl extends TypeLazySequenceImpl<ParameterLazySequenceImpl, ParameterLazySequence, ParameterMatch, ParameterMatcher, ParameterMatchImpl, ParameterMatcherImpl> implements ParameterLazySequence {
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

        @NonNull
        @Override
        protected ParameterLazySequenceImpl newSelf() {
            return new ParameterLazySequenceImpl(matcher);
        }
    }

    private abstract class MemberLazySequenceImpl<Self extends MemberLazySequenceImpl<Self, Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>, Base extends MemberLazySequence<Base, Match, Reflect, Matcher>, Match extends MemberMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>, MatchImpl extends MemberMatchImpl<MatchImpl, Match, Reflect, Matcher>, MatcherImpl extends MemberMatcherImpl<MatcherImpl, Matcher, Reflect, Self>> extends LazySequenceImpl<Self, Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl> implements MemberLazySequence<Base, Match, Reflect, Matcher> {
        protected MemberLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addDeclaringClassesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    var declaringClasses = new ArrayList<Class<?>>();
                    for (var type : result) {
                        declaringClasses.add(type.getDeclaringClass());
                    }
                    m.match(declaringClasses);
                }

                @Override
                public void onMiss() {

                }
            });
        }

        @NonNull
        @Override
        public final ClassLazySequence declaringClasses(@NonNull Consumer<ClassMatcher> matcher) {
            var m = new ClassMatcherImpl(false);
            matcher.accept(m);
            addDeclaringClassesObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final ClassMatch firstDeclaringClass(@NonNull Consumer<ClassMatcher> matcher) {
            var m = new ClassMatcherImpl(true);
            matcher.accept(m);
            addDeclaringClassesObserver(m);
            return m.build().first();
        }
    }

    private final class FieldLazySequenceImpl extends MemberLazySequenceImpl<FieldLazySequenceImpl, FieldLazySequence, FieldMatch, Field, FieldMatcher, FieldMatchImpl, FieldMatcherImpl> implements FieldLazySequence {
        private FieldLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Field> result) {
                    var types = new ArrayList<Class<?>>();
                    for (var type : result) {
                        types.add(type.getType());
                    }
                    m.match(types);
                }

                @Override
                public void onMiss() {

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
        protected FieldLazySequenceImpl newSelf() {
            return new FieldLazySequenceImpl(matcher);
        }

        @NonNull
        @Override
        public ClassLazySequence types(@NonNull Consumer<ClassMatcher> matcher) {
            var m = new ClassMatcherImpl(false);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public ClassMatch firstType(@NonNull Consumer<ClassMatcher> matcher) {
            var m = new ClassMatcherImpl(true);
            matcher.accept(m);
            addTypesObserver(m);
            return m.build().first();
        }
    }

    private abstract class ExecutableLazySequenceImpl<Self extends ExecutableLazySequenceImpl<Self, Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl>, Base extends ExecutableLazySequence<Base, Match, Reflect, Matcher>, Match extends ExecutableMatch<Match, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>, MatchImpl extends ExecutableMatchImpl<MatchImpl, Match, Reflect, Matcher>, MatcherImpl extends ExecutableMatcherImpl<MatcherImpl, Matcher, Reflect, Self>> extends MemberLazySequenceImpl<Self, Base, Match, Reflect, Matcher, MatchImpl, MatcherImpl> implements ExecutableLazySequence<Base, Match, Reflect, Matcher> {
        private void addParametersObserver(ParameterMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Reflect> result) {
                    var parameters = new ArrayList<Class<?>>();
                    for (var r : result) {
                        if (r instanceof Method) {
                            parameters.addAll(Arrays.asList(((Method) r).getParameterTypes()));
                        } else if (r instanceof Constructor) {
                            parameters.addAll(Arrays.asList(((Constructor<?>) r).getParameterTypes()));
                        }
                    }
                    m.match(parameters);
                }

                @Override
                public void onMiss() {

                }
            });
        }


        private ExecutableLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ParameterLazySequence parameters(@NonNull Consumer<ParameterMatcher> matcher) {
            var m = new ParameterMatcherImpl(false);
            addParametersObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public final ParameterMatch firstParameter(@NonNull Consumer<ParameterMatcher> matcher) {
            var m = new ParameterMatcherImpl(true);
            addParametersObserver(m);
            return m.build().first();
        }
    }

    private final class MethodLazySequenceImpl extends ExecutableLazySequenceImpl<MethodLazySequenceImpl, MethodLazySequence, MethodMatch, Method, MethodMatcher, MethodMatchImpl, MethodMatcherImpl> implements MethodLazySequence {
        private MethodLazySequenceImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void addReturnTypesObserver(@NonNull ClassMatcherImpl m) {
            m.pending = true;
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Iterable<Method> result) {
                    var types = new ArrayList<Class<?>>();
                    for (var type : result) {
                        types.add(type.getReturnType());
                    }
                    m.match(types);
                }

                @Override
                public void onMiss() {

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
        protected MethodLazySequenceImpl newSelf() {
            return new MethodLazySequenceImpl(matcher);
        }

        @NonNull
        @Override
        public ClassLazySequence returnTypes(@NonNull Consumer<ClassMatcher> matcher) {
            var m = new ClassMatcherImpl(false);
            matcher.accept(m);
            addReturnTypesObserver(m);
            return m.build();
        }

        @NonNull
        @Override
        public ClassMatch firstReturnType(@NonNull Consumer<ClassMatcher> matcher) {
            var m = new ClassMatcherImpl(true);
            matcher.accept(m);
            addReturnTypesObserver(m);
            return m.build().first();
        }
    }

    private final class ConstructorLazySequenceImpl extends ExecutableLazySequenceImpl<ConstructorLazySequenceImpl, ConstructorLazySequence, ConstructorMatch, Constructor<?>, ConstructorMatcher, ConstructorMatchImpl, ConstructorMatcherImpl> implements ConstructorLazySequence {
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

        @NonNull
        @Override
        protected ConstructorLazySequenceImpl newSelf() {
            return new ConstructorLazySequenceImpl(matcher);
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class BaseMatchImpl<Self extends BaseMatchImpl<Self, Base, Reflect>, Base extends BaseMatch<Base, Reflect>, Reflect> implements BaseMatch<Base, Reflect> {
        @NonNull
        private final Object VALUE = new Object();

        @NonNull
        private final Map<Observer<Reflect>, Object> observers = new ConcurrentHashMap<>();

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

        protected final void onMatch(Reflect reflect) {
            for (Observer<Reflect> observer : observers.keySet()) {
                observer.onMatch(reflect);
            }
        }

        protected final void addObserver(Observer<Reflect> observer) {
            observers.put(observer, VALUE);
        }

        protected final void removeObserver(Observer<Reflect> observer) {
            observers.remove(observer);
        }

        @CallSuper
        protected void match(Reflect match) {
            for (Observer<Reflect> observer : observers.keySet()) {
                observer.onMatch(match);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private abstract class ReflectMatchImpl<Self extends ReflectMatchImpl<Self, Base, Reflect, Matcher>, Base extends ReflectMatch<Base, Reflect, Matcher>, Reflect, Matcher extends ReflectMatcher<Matcher>> extends BaseMatchImpl<Self, Base, Reflect> implements ReflectMatch<Base, Reflect, Matcher> {
        @NonNull
        protected final ReflectMatcherImpl<?, ?, ?, ?> matcher;

        @Nullable
        protected String key = null;

        @Nullable
        protected volatile Reflect match = null;

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
            if (key != null) {
                // TODO
//                matcher.pending = false;
            }
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base onMatch(@NonNull Consumer<Reflect> consumer) {
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Reflect result) {
                    consumer.accept(result);
                }

                @Override
                public void onMiss() {

                }
            });
            // TODO
//            matcher.pending = false;
            return (Base) this;
        }

        @NonNull
        @Override
        public final Base substituteIfMiss(@NonNull Supplier<Base> replacement) {
            // TODO: not lazy
            return null;
        }

        @NonNull
        @Override
        public final Base matchFirstIfMiss(@NonNull Consumer<Matcher> consumer) {
            return null;
        }

        @NonNull
        @Override
        public final <Bind extends LazyBind> Base bind(@NonNull Bind bind, @NonNull BiConsumer<Bind, Reflect> consumer) {
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Reflect result) {
                    consumer.accept(bind, result);
                }

                @Override
                public void onMiss() {

                }
            });
            return (Base) this;
        }

        protected final void match(@NonNull Reflect match) {
            this.match = match;
            super.match(match);
        }
    }

    private abstract class TypeMatchImpl<Self extends TypeMatchImpl<Self, Base, Matcher>, Base extends TypeMatch<Base, Matcher>, Matcher extends TypeMatcher<Matcher>> extends ReflectMatchImpl<Self, Base, Class<?>, Matcher> implements TypeMatch<Base, Matcher> {
        protected TypeMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ClassMatch getSuperClass() {
            var m = new ClassMatchImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    var sup = result.getSuperclass();
                    if (sup != null) {
                        m.match(sup);
                    }
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }

        @NonNull
        @Override
        public final ClassLazySequence getInterfaces() {
            var m = new ClassLazySequenceImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(List.of(result.getInterfaces()));
                }

                @Override
                public void onMiss() {

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

                }
            });
            return m;
        }

        @NonNull
        @Override
        public final ConstructorLazySequence getDeclaredConstructors() {
            var m = new ConstructorLazySequenceImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(List.of(result.getDeclaredConstructors()));
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }

        @NonNull
        @Override
        public final FieldLazySequence getDeclaredFields() {
            var m = new FieldLazySequenceImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(List.of(result.getDeclaredFields()));
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }

        @NonNull
        @Override
        public final ClassMatch getArrayType() {
            var m = new ClassMatchImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(Array.newInstance(result, 0).getClass());
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }
    }

    private final class ClassMatchImpl extends TypeMatchImpl<ClassMatchImpl, ClassMatch, ClassMatcher> implements ClassMatch {
        private ClassMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public ParameterMatch asParameter(int index) {
            var m = new ParameterMatchImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Class<?> result) {
                    m.match(result, index);
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }
    }

    private final class ParameterMatchImpl extends TypeMatchImpl<ParameterMatchImpl, ParameterMatch, ParameterMatcher> implements ParameterMatch {
        int index = -1;

        private ParameterMatchImpl(@NonNull ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        private void match(Class<?> type, int index) {
            this.index = index;
            super.match(type);
        }
    }

    private abstract class MemberMatchImpl<Self extends MemberMatchImpl<Self, Base, Reflect, Matcher>, Base extends MemberMatch<Base, Reflect, Matcher>, Reflect extends Member, Matcher extends MemberMatcher<Matcher>> extends ReflectMatchImpl<Self, Base, Reflect, Matcher> implements MemberMatch<Base, Reflect, Matcher> {
        protected MemberMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ClassMatch getDeclaringClass() {
            var m = new ClassMatchImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Member result) {
                    m.match(result.getDeclaringClass());
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }
    }

    private final class FieldMatchImpl extends MemberMatchImpl<FieldMatchImpl, FieldMatch, Field, FieldMatcher> implements FieldMatch {
        private FieldMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public ClassMatch getType() {
            var m = new ClassMatchImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Field result) {
                    m.match(result.getType());
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }
    }

    private abstract class ExecutableMatchImpl<Self extends ExecutableMatchImpl<Self, Base, Reflect, Matcher>, Base extends ExecutableMatch<Base, Reflect, Matcher>, Reflect extends Member, Matcher extends ExecutableMatcher<Matcher>> extends MemberMatchImpl<Self, Base, Reflect, Matcher> implements ExecutableMatch<Base, Reflect, Matcher> {
        protected ExecutableMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public final ParameterLazySequence getParameterTypes() {
            var m = new ParameterLazySequenceImpl(matcher);
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

    private final class MethodMatchImpl extends ExecutableMatchImpl<MethodMatchImpl, MethodMatch, Method, MethodMatcher> implements MethodMatch {
        private MethodMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }

        @NonNull
        @Override
        public ClassMatch getReturnType() {
            var m = new ClassMatchImpl(matcher);
            addObserver(new Observer<>() {
                @Override
                public void onMatch(@NonNull Method result) {
                    m.match(result.getReturnType());
                }

                @Override
                public void onMiss() {

                }
            });
            return m;
        }
    }

    private final class ConstructorMatchImpl extends ExecutableMatchImpl<ConstructorMatchImpl, ConstructorMatch, Constructor<?>, ConstructorMatcher> implements ConstructorMatch {
        private ConstructorMatchImpl(ReflectMatcherImpl<?, ?, ?, ?> matcher) {
            super(matcher);
        }
    }

    private final class StringMatchImpl extends BaseMatchImpl<StringMatchImpl, StringMatch, String> implements StringMatch {
        @NonNull
        private final StringMatcherImpl matcher;

        private StringMatchImpl(@NonNull StringMatcherImpl matcher) {
            this.matcher = matcher;
        }
    }

    @NonNull
    @Override
    public MethodLazySequence methods(@NonNull Consumer<MethodMatcher> matcher) {
        var m = new MethodMatcherImpl(false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public MethodMatch firstMethod(@NonNull Consumer<MethodMatcher> matcher) {
        var m = new MethodMatcherImpl(true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public ConstructorLazySequence constructors(@NonNull Consumer<ConstructorMatcher> matcher) {
        var m = new ConstructorMatcherImpl(false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public ConstructorMatch firstConstructor(@NonNull Consumer<ConstructorMatcher> matcher) {
        var m = new ConstructorMatcherImpl(true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public FieldLazySequence fields(@NonNull Consumer<FieldMatcher> matcher) {
        var m = new FieldMatcherImpl(false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public FieldMatch firstField(@NonNull Consumer<FieldMatcher> matcher) {
        var m = new FieldMatcherImpl(true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public ClassLazySequence classes(@NonNull Consumer<ClassMatcher> matcher) {
        var m = new ClassMatcherImpl(false);
        matcher.accept(m);
        return m.build();
    }

    @NonNull
    @Override
    public ClassMatch firstClass(@NonNull Consumer<ClassMatcher> matcher) {
        var m = new ClassMatcherImpl(true);
        matcher.accept(m);
        return m.build().first();
    }

    @NonNull
    @Override
    public StringMatch exact(@NonNull String string) {
        var m = new StringMatcherImpl(true);
        m.exact = string;
        return m.build();
    }

    @NonNull
    @Override
    public StringMatch prefix(@NonNull String prefix) {
        var m = new StringMatcherImpl(false);
        m.prefix = prefix;
        return m.build();
    }

    @NonNull
    @Override
    public StringMatch firstPrefix(@NonNull String prefix) {
        var m = new StringMatcherImpl(true);
        m.prefix = prefix;
        return m.build();
    }

    @NonNull
    @Override
    public ClassMatch exactClass(@NonNull String name) {
        // TODO: support binary name
        var m = new ClassMatcherImpl(true);
        try {
            m.setExact(Class.forName(name, false, classLoader));
        } catch (ClassNotFoundException e) {
            if (exceptionHandler != null) exceptionHandler.test(e);
        }
        return m.build().first();
    }

    @NonNull
    @Override
    public ClassMatch exact(@NonNull Class<?> clazz) {
        var m = new ClassMatcherImpl(true);
        m.setExact(clazz);
        return m.build().first();
    }

    @NonNull
    @Override
    public MethodMatch exactMethod(@NonNull String signature) {
        return null;
    }

    @NonNull
    @Override
    public MethodMatch exact(@NonNull Method method) {
        var m = new MethodMatcherImpl(true);
        m.setExact(method);
        return m.build().first();
    }

    @NonNull
    @Override
    public ConstructorMatch exactConstructor(@NonNull String signature) {
        return null;
    }

    @NonNull
    @Override
    public ConstructorMatch exact(@NonNull Constructor<?> constructor) {
        var m = new ConstructorMatcherImpl(true);
        m.setExact(constructor);
        return m.build().first();
    }

    @NonNull
    @Override
    public FieldMatch exactField(@NonNull String signature) {
        return null;
    }

    @NonNull
    @Override
    public FieldMatch exact(@NonNull Field field) {
        var m = new FieldMatcherImpl(true);
        m.setExact(field);
        return m.build().first();
    }

    @NonNull
    @Override
    public ParameterMatch exactParameter(@NonNull String signature, int index) {
        return null;
    }

    @NonNull
    @Override
    public ParameterMatch exact(@NonNull Class<?> clazz, int index) {
        var m = new ParameterMatcherImpl(true);
        m.setExact(clazz);
        m.index = index;
        return m.build().first();
    }

    @NonNull
    @Override
    public ContainerSyntax<ParameterMatch> exact(@NonNull Class<?>... params) {
        return null;
    }

    public @NonNull MatchResult build() {
        dexAnalysis = dexAnalysis || forceDexAnalysis;
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        if (dexAnalysis) {
            analysisDex();
        } else {
            analysisClassLoader();
        }
        return null;
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
        var pathList = pathListField.get(classLoader);
        if (pathList == null) {
            throw new IllegalStateException("pathList is null");
        }
        var dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        var dexElements = (Object[]) dexElementsField.get(pathList);
        if (dexElements == null) {
            throw new IllegalStateException("dexElements is null");
        }
        for (var dexElement : dexElements) {
            var dexFileField = dexElement.getClass().getDeclaredField("dexFile");
            dexFileField.setAccessible(true);
            var dexFile = dexFileField.get(dexElement);
            if (dexFile == null) {
                continue;
            }
            var entriesField = dexFile.getClass().getDeclaredField("entries");
            entriesField.setAccessible(true);
            @SuppressWarnings("unchecked") var entries = (Enumeration<String>) entriesField.get(dexFile);
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
        for (var task : tasks) {
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
            for (final var classMatcher : classMatchers) {
                // not leaf
                if (classMatcher.leafCount.get() != 1) continue;
                // TODO: pending
                //       if (classMatcher.pending) continue;
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
                    final ArrayList<Class<?>> matches = new ArrayList<>();
                    for (int i = low; i < high && i < classNames.size(); i++) {
                        final var className = classNames.get(i);
                        // then check the rest conditions that need to load the class
                        final Class<?> theClass;
                        try {
                            theClass = Class.forName(className, false, classLoader);
                        } catch (ClassNotFoundException e) {
                            if (exceptionHandler != null) {
                                if (exceptionHandler.test(e)) {
                                    continue;
                                } else {
                                    break;
                                }
                            }
                            continue;
                        }
                        final var modifiers = theClass.getModifiers();
                        if ((modifiers & classMatcher.includeModifiers) != classMatcher.includeModifiers)
                            continue;
                        if ((modifiers & classMatcher.excludeModifiers) != 0) continue;
                        if (classMatcher.superClass != null) {
                            final var superClass = theClass.getSuperclass();
                            if (superClass == null || classMatcher.superClass.match != superClass)
                                continue;
                        }
                        if (classMatcher.containsInterfaces != null) {
                            final var ifArray = theClass.getInterfaces();
                            final var ifs = new HashSet<Class<?>>(ifArray.length);
                            Collections.addAll(ifs, ifArray);
                            if (!classMatcher.containsInterfaces.test(ifs)) continue;
                        }
                        matches.add(theClass);
                        if (classMatcher.matchFirst) {
                            break;
                        }
                    }
                    hasMatched[0] = hasMatched[0] || !matches.isEmpty();
                    classMatcher.match(matches);
                });
                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (final var fieldMatcher : fieldMatchers) {
                // not leaf
                if (fieldMatcher.leafCount.get() != 1) continue;

                final var task = executorService.submit(() -> {
                    final ArrayList<Class<?>> classList = new ArrayList<>();
                    if (fieldMatcher.declaringClass != null && fieldMatcher.declaringClass.match != null) {
                        classList.add(fieldMatcher.declaringClass.match);
                    } else {
                        // TODO
                    }

                    final ArrayList<Field> matches = new ArrayList<>();

                    for (final var theClass : classList) {
                        final var fields = theClass.getDeclaredFields();
                        for (final var field : fields) {
                            final var modifiers = field.getModifiers();
                            if ((modifiers & fieldMatcher.includeModifiers) != fieldMatcher.includeModifiers)
                                continue;
                            if ((modifiers & fieldMatcher.excludeModifiers) != 0) continue;
                            if (fieldMatcher.type != null && fieldMatcher.type.match != field.getType())
                                continue;
                            if (fieldMatcher.name != null) {
                                final var strMatcher = fieldMatcher.name.matcher;
                                if (strMatcher.prefix != null && !field.getName().startsWith(strMatcher.prefix))
                                    continue;
                                if (strMatcher.exact != null && !field.getName().equals(strMatcher.exact))
                                    continue;
                            }
                            matches.add(field);
                            if (fieldMatcher.matchFirst) {
                                break;
                            }
                        }
                    }
                    hasMatched[0] = hasMatched[0] || !matches.isEmpty();
                    fieldMatcher.match(matches);
                });
                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (var methodMatcher : methodMatchers) {
                // not leaf
                if (methodMatcher.leafCount.get() != 1) continue;

                var task = executorService.submit(() -> {
                    final ArrayList<Class<?>> classList = new ArrayList<>();
                    if (methodMatcher.declaringClass != null && methodMatcher.declaringClass.match != null) {
                        classList.add(methodMatcher.declaringClass.match);
                    } else {
                        // TODO
                    }

                    final ArrayList<Method> matches = new ArrayList<>();

                    for (final var clazz : classList) {
                        final var methods = clazz.getDeclaredMethods();
                        for (final var method : methods) {
                            final var modifiers = method.getModifiers();
                            if ((modifiers & methodMatcher.includeModifiers) != methodMatcher.includeModifiers)
                                continue;
                            if ((modifiers & methodMatcher.excludeModifiers) != 0) continue;
                            if (methodMatcher.returnType != null && methodMatcher.returnType.match != method.getReturnType())
                                continue;
                            if (methodMatcher.name != null) {
                                final var strMatcher = methodMatcher.name.matcher;
                                if (strMatcher.prefix != null && !method.getName().startsWith(strMatcher.prefix))
                                    continue;
                                if (strMatcher.exact != null && !method.getName().equals(strMatcher.exact))
                                    continue;
                            }
                            final var typeArrays = method.getParameterTypes();
                            if (methodMatcher.parameterCount >= 0 && methodMatcher.parameterCount != typeArrays.length)
                                continue;
                            if (methodMatcher.parameterTypes != null) {
                                final var parameterTypes = new HashSet<Class<?>>(typeArrays.length);
                                Collections.addAll(parameterTypes, typeArrays);
                                if (!methodMatcher.parameterTypes.test(parameterTypes)) continue;
                            }
                            matches.add(method);
                            if (methodMatcher.matchFirst) {
                                break;
                            }
                        }
                    }
                    hasMatched[0] = hasMatched[0] || !matches.isEmpty();
                    methodMatcher.match(matches);
                });

                tasks.add(task);
            }
            joinAndClearTasks(tasks);

            for (var constructorMatcher : constructorMatchers) {
                // not leaf
                if (constructorMatcher.leafCount.get() != 1) continue;

                var task = executorService.submit(() -> {
                    final ArrayList<Class<?>> classList = new ArrayList<>();

                    if (constructorMatcher.declaringClass != null && constructorMatcher.declaringClass.match != null) {
                        classList.add(constructorMatcher.declaringClass.match);
                    } else {
                        // TODO
                    }

                    final ArrayList<Constructor<?>> matches = new ArrayList<>();

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
                            matches.add(constructor);
                            if (constructorMatcher.matchFirst) {
                                break;
                            }
                        }
                    }
                    hasMatched[0] = hasMatched[0] || !matches.isEmpty();
                    constructorMatcher.match(matches);
                });

                tasks.add(task);
            }
            joinAndClearTasks(tasks);
        } while (hasMatched[0]);
    }

}
