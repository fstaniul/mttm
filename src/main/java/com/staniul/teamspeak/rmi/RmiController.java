package com.staniul.teamspeak.rmi;

import java.rmi.Remote;
import java.util.function.*;

public interface RmiController extends Remote {
    <R> R supply(Supplier<R> supplier);
    <T> void consume (T t, Consumer<T> consumer);
    <T, R> R doAction (T t, Function<T, R> action);
    <T, U, R> R doAction(T t, U u, BiFunction<T, U, R> action);
}
