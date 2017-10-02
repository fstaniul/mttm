package com.staniul.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RmiServer {
    public static void main(String[] args) throws RemoteException { // debug only
        try{
            RmiController controller =  new RmiControllerImpl();
            Registry registry = LocateRegistry.createRegistry(12000);
            RmiController RemObj =
                    (RmiController) UnicastRemoteObject.exportObject(controller,12000);  // port 0 here has no meaning since it will be taken over by the registry
            registry.rebind("RmiController", RemObj);
            System.out.println("RmiServer started");
        } catch (Exception e){
            System.err.println("RmiServer exception:");
            e.printStackTrace();
        }
    }
}
