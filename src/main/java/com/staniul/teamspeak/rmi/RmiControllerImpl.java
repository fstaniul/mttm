package com.staniul.teamspeak.rmi;

import org.springframework.stereotype.Component;

import java.util.function.*;

@Component
public class RmiControllerImpl implements RmiController {
    @Override
    public <R> R supply (Supplier<R> supplier) {
        return supplier.get();
    }

    @Override
    public <T> void consume(T t, Consumer<T> consumer) {
        consumer.accept(t);
    }

    @Override
    public <T, R> R doAction(T t, Function<T, R> action) {
        return action.apply(t);
    }

    @Override
    public <T, U, R> R doAction (T t, U u, BiFunction<T, U, R> action) {
        return action.apply(t, u);
    }
}
