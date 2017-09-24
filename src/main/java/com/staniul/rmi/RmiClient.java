package com.staniul.rmi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.function.Function;

public class RmiClient {
    // zignoruj, nie dziala
    static <T, R> void serialize(SerFunction<T, R> f) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream())) {
            oos.writeObject(f);
        }
    }

    public static void main(String[] args) throws NotBoundException, MalformedURLException {
        try{
            RmiController rmiController = (RmiController) Naming.lookup("rmi://localhost:12000/RmiController");
            String ala = rmiController.doAction("Ala", (Function<String, String> & Serializable) s -> s + " ma kota" );
            // zmien sobie "Ala" i "ma kota" - przy kolejnym odpaleniu zmieni sie tylko "Ala", problem z serializacja funkcji
            // trzeba zrestartowac serwer by odswiezyc "ma kota"
            //System.out.print(rmiController.echo("Ala"));
            System.out.print(ala);
        } catch (Exception e){
            System.err.println("RmiClient error:");
            e.printStackTrace();
        }
    }
}
