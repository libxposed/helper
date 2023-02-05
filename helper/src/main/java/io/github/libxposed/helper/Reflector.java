package io.github.libxposed.helper;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
final class Reflector {
    private static final Map<String, Character> abbreviationMap = Map.of("int", 'I', "boolean", 'Z', "float", 'F', "long", 'J', "short", 'S', "byte", 'B', "double", 'D', "char", 'C', "void", 'V');
    private static final Map<Character, Class<?>> primitiveClassMap = Map.of('I', int.class, 'Z', boolean.class, 'F', float.class, 'J', long.class, 'S', short.class, 'B', byte.class, 'D', double.class, 'C', char.class, 'V', void.class);
    private static final WeakReference<?> EMPTY = new WeakReference<>(null);
    private final ClassLoader classLoader;
    private final HashMap<String, WeakReference<Class<?>>> classCache = new HashMap<>();

    Reflector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @NonNull
    Class<?> loadClass(@NonNull String className) throws ClassNotFoundException {
        className = className.trim().replace('/', '.');
        if (className.startsWith("L") && className.endsWith(";")) {
            className = className.substring(1, className.length() - 1);
        } else if (className.endsWith("[]")) {
            var sb = new StringBuilder();
            while (className.endsWith("[]")) {
                className = className.substring(0, className.length() - 2);
                sb.append('[');
            }
            var abbr = abbreviationMap.get(className);
            if (abbr != null) {
                sb.append(abbr);
            } else {
                sb.append('L').append(className).append(';');
            }
            className = sb.toString();
        }
        try {
            synchronized (classCache) {
                WeakReference<Class<?>> ref = classCache.get(className);
                if (ref == EMPTY) {
                    throw new ClassNotFoundException(className);
                }
                Class<?> clazz = ref != null ? ref.get() : null;
                if (clazz == null) {
                    try {
                        clazz = Class.forName(className, false, classLoader);
                    } catch (ClassNotFoundException e) {
                        //noinspection unchecked
                        classCache.put(className, (WeakReference<Class<?>>) EMPTY);
                        throw e;
                    }
                    classCache.put(className, new WeakReference<>(clazz));
                }
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            final int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                try {
                    final String innerClassName = className.substring(0, lastDot) + '$' + className.substring(lastDot + 1);
                    return loadClass(innerClassName);
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw e;
        }
    }

    @NonNull
    Field loadField(@NonNull String fieldString) throws ClassNotFoundException, NoSuchFieldException {
        Class<?> declaringClass;
        String name;
        Class<?> type = null;
        var sep = fieldString.lastIndexOf("->");
        if (sep > 0) {
            var className = fieldString.substring(0, sep);
            var signature = fieldString.substring(sep + 2);
            declaringClass = loadClass(className);
            sep = signature.indexOf(':');
            if (sep > 0) {
                var typeName = signature.substring(sep + 1);
                type = primitiveClassMap.get(typeName.charAt(0));
                if (type == null) {
                    type = loadClass(typeName);
                }
            } else {
                sep = signature.length();
            }
            name = signature.substring(0, sep);
        } else {
            var lastSpace = fieldString.lastIndexOf(' ');
            if (lastSpace > 0) {
                var secondLastSpace = fieldString.lastIndexOf(' ', lastSpace - 1);
                var typeName = fieldString.substring(secondLastSpace + 1, lastSpace);
                type = primitiveClassMap.get(typeName.charAt(0));
                if (type == null) {
                    type = loadClass(typeName);
                }
            }
            var lastDot = fieldString.lastIndexOf('.');
            if (lastDot < 0) {
                throw new NoSuchFieldException(fieldString);
            }
            declaringClass = loadClass(fieldString.substring(lastSpace + 1, lastDot));
            name = fieldString.substring(lastDot + 1);
        }
        var f = declaringClass.getDeclaredField(name);
        if (type != null && f.getType() != type) {
            throw new NoSuchFieldException(fieldString);
        }
        return f;
    }

    @NonNull
    Method loadMethod(@NonNull String methodString) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> declaringClass;
        String name;
        ArrayList<Class<?>> parameterTypes = new ArrayList<>();
        Class<?> returnType = null;
        var sep = methodString.lastIndexOf("->");
        if (sep > 0) {
            var className = methodString.substring(0, sep);
            var signature = methodString.substring(sep + 2);
            declaringClass = loadClass(className);
            var start = signature.indexOf('(');
            var end = signature.lastIndexOf(')');
            if (start < 0 || end < 0 || end < start) {
                throw new NoSuchMethodException(methodString);
            }
            var returnTypeName = signature.substring(end + 1).trim();
            if (!returnTypeName.isEmpty()) {
                returnType = primitiveClassMap.get(returnTypeName.charAt(0));
                if (returnType == null) {
                    returnType = loadClass(returnTypeName);
                }
            }
            name = signature.substring(0, start);
            var params = signature.substring(start + 1, end);
            int idx = 0;
            while (idx < params.length()) {
                var ch = params.charAt(idx);
                String paramName;
                if (ch == 'L') {
                    var endIdx = params.indexOf(';', idx);
                    if (endIdx < 0) {
                        throw new NoSuchMethodException(methodString);
                    }
                    paramName = params.substring(idx, endIdx + 1);
                    idx = endIdx + 1;
                } else if (ch == '[') {
                    var endIdx = idx + 1;
                    while (endIdx < params.length() && params.charAt(endIdx) == '[') {
                        endIdx++;
                    }
                    if (endIdx >= params.length()) {
                        throw new NoSuchMethodException(methodString);
                    }
                    ch = params.charAt(endIdx);
                    if (ch == 'L') {
                        endIdx = params.indexOf(';', endIdx);
                    }
                    if (endIdx >= params.length()) {
                        throw new NoSuchMethodException(methodString);
                    }
                    paramName = params.substring(idx, endIdx + 1);
                    idx = endIdx + 1;
                } else {
                    parameterTypes.add(primitiveClassMap.get(ch));
                    idx++;
                    continue;
                }
                parameterTypes.add(loadClass(paramName));
            }
        } else {
            var start = methodString.indexOf('(');
            var end = methodString.lastIndexOf(')');
            if (start < 0 || end < 0 || end < start) {
                throw new NoSuchMethodException(methodString);
            }
            var lastSpace = methodString.lastIndexOf(' ', start);
            if (lastSpace > 0) {
                var secondLastSpace = methodString.lastIndexOf(' ', lastSpace - 1);
                var returnTypeName = methodString.substring(secondLastSpace + 1, lastSpace);
                var returnTypeAbbr = abbreviationMap.get(returnTypeName);
                if (returnTypeAbbr != null) {
                    returnType = primitiveClassMap.get(returnTypeAbbr);
                } else {
                    returnType = loadClass(returnTypeName);
                }
            }
            var lastDot = methodString.lastIndexOf('.', start);
            if (lastDot < 0) {
                throw new NoSuchMethodException(methodString);
            }
            name = methodString.substring(lastDot + 1, start);
            declaringClass = loadClass(methodString.substring(lastSpace + 1, lastDot));
            var params = methodString.substring(start + 1, end);
            int idx = 0;
            while (idx < params.length()) {
                var nextComma = params.indexOf(',', idx);
                if (nextComma < 0) {
                    nextComma = params.length();
                }
                var paramName = params.substring(idx, nextComma).trim();
                idx = nextComma + 1;
                var paramAbbr = abbreviationMap.get(paramName);
                if (paramAbbr != null) {
                    parameterTypes.add(primitiveClassMap.get(paramAbbr));
                } else {
                    parameterTypes.add(loadClass(paramName));
                }
            }
        }
        var m = declaringClass.getDeclaredMethod(name, parameterTypes.toArray(new Class[0]));
        if (returnType != null && m.getReturnType() != returnType) {
            throw new NoSuchMethodException(methodString);
        }
        return m;
    }

    @NonNull
    Constructor<?> loadConstructor(@NonNull String constructorString) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> declaringClass;
        ArrayList<Class<?>> parameterTypes = new ArrayList<>();
        var sep = constructorString.lastIndexOf("->");
        if (sep > 0) {
            var className = constructorString.substring(0, sep);
            var signature = constructorString.substring(sep + 2);
            declaringClass = loadClass(className);
            var start = signature.indexOf('(');
            var end = signature.lastIndexOf(')');
            if (start < 0 || end < 0 || end < start) {
                throw new NoSuchMethodException(constructorString);
            }
            var returnTypeName = signature.substring(end + 1).trim();
            if (!returnTypeName.isEmpty() && !"V".equals(returnTypeName)) {
                throw new NoSuchMethodException(constructorString);
            }
            var name = signature.substring(0, start);
            if (!"<init>".equals(name)) {
                throw new NoSuchMethodException(constructorString);
            }
            var params = signature.substring(start + 1, end);
            int idx = 0;
            while (idx < params.length()) {
                var ch = params.charAt(idx);
                String paramName;
                if (ch == 'L') {
                    var endIdx = params.indexOf(';', idx);
                    if (endIdx < 0) {
                        throw new NoSuchMethodException(constructorString);
                    }
                    paramName = params.substring(idx, endIdx + 1);
                    idx = endIdx + 1;
                } else if (ch == '[') {
                    var endIdx = idx + 1;
                    while (endIdx < params.length() && params.charAt(endIdx) == '[') {
                        endIdx++;
                    }
                    if (endIdx >= params.length()) {
                        throw new NoSuchMethodException(constructorString);
                    }
                    ch = params.charAt(endIdx);
                    if (ch == 'L') {
                        endIdx = params.indexOf(';', endIdx);
                    }
                    if (endIdx >= params.length()) {
                        throw new NoSuchMethodException(constructorString);
                    }
                    paramName = params.substring(idx, endIdx + 1);
                    idx = endIdx + 1;
                } else {
                    parameterTypes.add(primitiveClassMap.get(ch));
                    idx++;
                    continue;
                }
                parameterTypes.add(loadClass(paramName));
            }
        } else {
            var start = constructorString.indexOf('(');
            var end = constructorString.lastIndexOf(')');
            if (start < 0 || end < 0 || end < start) {
                throw new NoSuchMethodException(constructorString);
            }
            declaringClass = loadClass(constructorString.substring(0, start));
            var params = constructorString.substring(start + 1, end);
            int idx = 0;
            while (idx < params.length()) {
                var nextComma = params.indexOf(',', idx);
                if (nextComma < 0) {
                    nextComma = params.length();
                }
                var paramName = params.substring(idx, nextComma).trim();
                idx = nextComma + 1;
                var paramAbbr = abbreviationMap.get(paramName);
                if (paramAbbr != null) {
                    parameterTypes.add(primitiveClassMap.get(paramAbbr));
                } else {
                    parameterTypes.add(loadClass(paramName));
                }
            }
        }
        return declaringClass.getDeclaredConstructor(parameterTypes.toArray(new Class[0]));
    }

    @NonNull
    Iterable<Class<?>> loadClasses(Iterable<String> classNames) throws ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (String className : classNames) {
            classes.add(loadClass(className));
        }
        return classes;
    }

    @NonNull
    Iterable<Field> loadFields(Iterable<String> fieldStrings) throws ClassNotFoundException, NoSuchFieldException {
        ArrayList<Field> fields = new ArrayList<>();
        for (String fieldString : fieldStrings) {
            fields.add(loadField(fieldString));
        }
        return fields;
    }

    @NonNull
    Iterable<Method> loadMethods(Iterable<String> methodStrings) throws ClassNotFoundException, NoSuchMethodException {
        ArrayList<Method> methods = new ArrayList<>();
        for (String methodString : methodStrings) {
            methods.add(loadMethod(methodString));
        }
        return methods;
    }

    @NonNull
    Iterable<Constructor<?>> loadConstructors(Iterable<String> constructorStrings) throws ClassNotFoundException, NoSuchMethodException {
        ArrayList<Constructor<?>> constructors = new ArrayList<>();
        for (String constructorString : constructorStrings) {
            constructors.add(loadConstructor(constructorString));
        }
        return constructors;
    }
}
