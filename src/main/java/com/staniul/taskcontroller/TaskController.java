package com.staniul.taskcontroller;

import org.joda.time.DateTime;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Timer;

@Component
public class TaskController implements ApplicationContextAware {
    private ApplicationContext appContext;
    private Reflections reflections;

    private Timer timer;

    @Autowired
    public TaskController (Reflections reflections) {
        this.reflections = reflections;
        timer = new Timer("Task's Timer");
    }

    @PostConstruct
    private void init () {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(Task.class);
        for (Method method : methods) {
            Task ann = method.getAnnotation(Task.class);
            Object target = appContext.getBean(method.getDeclaringClass());

            if (target != null) {
                MethodTimerTask mtt = new MethodTimerTask(method, target);
                DateTime dateTime = getDateTimeFromTask(ann);

                timer.scheduleAtFixedRate(mtt, dateTime.toDate(), ann.delay());
            }
        }
    }

    @PreDestroy
    private void destroy() {
        timer.cancel();
    }

    private DateTime getDateTimeFromTask (Task task) {
        DateTime dateTime = DateTime.now();

        dateTime = setDay(dateTime, task.day());
        dateTime = setHour(dateTime, task.hour());
        dateTime = setMinutes(dateTime, task.minute());
        dateTime = setSeconds(dateTime, task.second());

        return dateTime;
    }

    private DateTime setDay (DateTime now, int day) {
        DateTime dateTime = now;

        if (day >= 1 && day <= 7) {
            dateTime = now.withDayOfWeek(day);
            if (dateTime.isBefore(now))
                dateTime = dateTime.plusWeeks(1);
        }

        return dateTime;
    }

    private DateTime setHour (DateTime now, int hour) {
        DateTime dateTime = now;

        if (hour >= 0 && hour <= 23) {
            dateTime = dateTime.withHourOfDay(hour);
            if (dateTime.isBefore(now))
                dateTime = dateTime.plusDays(1);
        }

        return dateTime;
    }

    private DateTime setMinutes (DateTime now, int minute) {
        DateTime dateTime = now;

        if (minute >= 0 && minute <= 59) {
            dateTime = dateTime.withMinuteOfHour(minute);
            if (dateTime.isBefore(now))
                dateTime = dateTime.plusHours(1);
        }

        return dateTime;
    }

    private DateTime setSeconds (DateTime now, int seconds) {
        DateTime dateTime = now;

        if (seconds >= 0 && seconds <= 59) {
            dateTime = dateTime.withSecondOfMinute(seconds);
            if (dateTime.isBefore(now))
                dateTime = dateTime.plusMinutes(1);
        }

        return dateTime;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }
}
