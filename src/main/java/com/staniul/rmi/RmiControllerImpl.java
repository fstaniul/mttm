package com.staniul.rmi;

import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

//@Component
public class RmiControllerImpl implements RmiController {

    @Override
    public <R> R supply (Supplier<R> supplier) throws RemoteException {
        return supplier.get();
    }

    @Override
    public <T> void consume(T t, Consumer<T> consumer) throws RemoteException {
        consumer.accept(t);
    }

    @Override
    public <T, R> R doAction(T t, Function<T, R> action) throws RemoteException {
        return action.apply(t);
    }

    @Override
    public <T, U, R> R doAction (T t, U u, BiFunction<T, U, R> action) throws RemoteException {
        return action.apply(t, u);
    }

    @Override
    public String echo(String str){
        return "Response: " + str;
    }
}