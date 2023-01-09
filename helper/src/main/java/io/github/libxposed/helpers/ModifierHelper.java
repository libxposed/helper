package io.github.libxposed.helpers;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * Helper for quick check the modifier of a member/class.
 */
@SuppressWarnings("unused")
public class ModifierHelper {
    private ModifierHelper() throws RuntimeException {
        throw new RuntimeException("Do not create instance of this class.");
    }

    public static boolean isPublic(Member member) {
        return Modifier.isPublic(member.getModifiers());
    }

    public static boolean isNotPublic(Member member) {
        return !Modifier.isPublic(member.getModifiers());
    }

    public static boolean isProtected(Member member) {
        return Modifier.isProtected(member.getModifiers());
    }

    public static boolean isNotProtected(Member member) {
        return !Modifier.isProtected(member.getModifiers());
    }

    public static boolean isPrivate(Member member) {
        return Modifier.isPrivate(member.getModifiers());
    }

    public static boolean isNotPrivate(Member member) {
        return !Modifier.isPrivate(member.getModifiers());
    }

    public static boolean isStatic(Member member) {
        return Modifier.isStatic(member.getModifiers());
    }

    public static boolean isNotStatic(Member member) {
        return !Modifier.isStatic(member.getModifiers());
    }

    public static boolean isFinal(Member member) {
        return Modifier.isFinal(member.getModifiers());
    }

    public static boolean isNotFinal(Member member) {
        return !Modifier.isFinal(member.getModifiers());
    }

    public static boolean isSynchronized(Member member) {
        return Modifier.isSynchronized(member.getModifiers());
    }

    public static boolean isNotSynchronized(Member member) {
        return !Modifier.isSynchronized(member.getModifiers());
    }

    public static boolean isVolatile(Member member) {
        return Modifier.isVolatile(member.getModifiers());
    }

    public static boolean isNotVolatile(Member member) {
        return !Modifier.isVolatile(member.getModifiers());
    }

    public static boolean isTransient(Member member) {
        return Modifier.isTransient(member.getModifiers());
    }

    public static boolean isNotTransient(Member member) {
        return !Modifier.isTransient(member.getModifiers());
    }

    public static boolean isNative(Member member) {
        return Modifier.isNative(member.getModifiers());
    }

    public static boolean isNotNative(Member member) {
        return !Modifier.isNative(member.getModifiers());
    }

    public static boolean isInterface(Member member) {
        return Modifier.isInterface(member.getModifiers());
    }

    public static boolean isNotInterface(Member member) {
        return !Modifier.isInterface(member.getModifiers());
    }

    public static boolean isAbstract(Member member) {
        return Modifier.isAbstract(member.getModifiers());
    }

    public static boolean isNotAbstract(Member member) {
        return !Modifier.isAbstract(member.getModifiers());
    }

    public static boolean isStrict(Member member) {
        return Modifier.isStrict(member.getModifiers());
    }

    public static boolean isNotStrict(Member member) {
        return !Modifier.isStrict(member.getModifiers());
    }

    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isNotPublic(Class<?> clazz) {
        return !Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isProtected(Class<?> clazz) {
        return Modifier.isProtected(clazz.getModifiers());
    }

    public static boolean isNotProtected(Class<?> clazz) {
        return !Modifier.isProtected(clazz.getModifiers());
    }

    public static boolean isPrivate(Class<?> clazz) {
        return Modifier.isPrivate(clazz.getModifiers());
    }

    public static boolean isNotPrivate(Class<?> clazz) {
        return !Modifier.isPrivate(clazz.getModifiers());
    }

    public static boolean isFinal(Class<?> clazz) {
        return Modifier.isFinal(clazz.getModifiers());
    }

    public static boolean isNotFinal(Class<?> clazz) {
        return !Modifier.isFinal(clazz.getModifiers());
    }

    public static boolean isInterface(Class<?> clazz) {
        return Modifier.isInterface(clazz.getModifiers());
    }

    public static boolean isNotInterface(Class<?> clazz) {
        return !Modifier.isInterface(clazz.getModifiers());
    }

    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    public static boolean isNotAbstract(Class<?> clazz) {
        return !Modifier.isAbstract(clazz.getModifiers());
    }

    public static boolean isPackagePrivate(Member member) {
        return isNotPublic(member) && isNotProtected(member) && isNotPrivate(member);
    }

    public static boolean isNotPackagePrivate(Member member) {
        return !isPackagePrivate(member);
    }

    public static boolean isPackagePrivate(Class<?> clazz) {
        return isNotPublic(clazz) && isNotProtected(clazz) && isNotPrivate(clazz);
    }

    public static boolean isNotPackagePrivate(Class<?> clazz) {
        return !isPackagePrivate(clazz);
    }
}
