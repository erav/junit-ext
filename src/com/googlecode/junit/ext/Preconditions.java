package com.googlecode.junit.ext;

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.METHOD})
public @interface Preconditions {
    Class<? extends Precondition>[] value() default {};
}
