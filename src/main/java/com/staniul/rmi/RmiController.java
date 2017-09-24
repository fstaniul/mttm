package com.staniul.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface RmiController extends Remote {
    <R> R supply(Supplier<R> supplier) throws RemoteException;
    <T> void consume (T t, Consumer<T> consumer) throws RemoteException;
    <T, R> R doAction (T t, Function<T, R> action) throws RemoteException;
    <T, U, R> R doAction(T t, U u, BiFunction<T, U, R> action) throws RemoteException;
    public String echo(String str) throws  RemoteException;
}
