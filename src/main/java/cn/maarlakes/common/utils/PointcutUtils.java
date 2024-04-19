package cn.maarlakes.common.utils;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

import java.lang.annotation.Annotation;

/**
 * @author linjpxc
 */
public final class PointcutUtils {
    private PointcutUtils() {
    }

    @SafeVarargs
    public static Pointcut forAnnotations(Class<? extends Annotation>... annotations) {
        ComposablePointcut pointcut = null;
        for (Class<? extends Annotation> annotation : annotations) {
            if (pointcut == null) {
                pointcut = new ComposablePointcut(classOrMethod(annotation));
            } else {
                pointcut.union(classOrMethod(annotation));
            }
        }

        return pointcut;
    }

    private static Pointcut classOrMethod(Class<? extends Annotation> annotation) {
        return Pointcuts.union(new AnnotationMatchingPointcut(null, annotation, true),
                new AnnotationMatchingPointcut(annotation, true));
    }
}
