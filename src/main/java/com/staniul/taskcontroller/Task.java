package com.staniul.taskcontroller;

import java.lang.annotation.*;

/**
 * Methods annotated by this annotation are treated as task and will be invoked depending on context of this annotation.
 * Tasks must be declared inside classes that are Spring beans otherwise they will be omitted during adding creating timed tasks.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Task {
    /**
     * Delay between executions
     */
    long delay ();

    /**
     * Hour at which execution should begin.
     */
    int hour() default -1;

    /**
     * Minute in hour at which execution should begin.
     */
    int minute() default -1;

    /**
     * Second in minute at which execution should begin.
     */
    int second() default -1;

    /**
     * Day of the week in which the execution should begin.
     */
    int day() default -1;
}
