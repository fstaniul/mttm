package com.staniul.teamspeak.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

//@Component
public class RmiServer {
    public RmiServer (RmiController controller) throws RemoteException {
        RmiController stub = (RmiController) UnicastRemoteObject.exportObject(controller, 0);
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("RmiController", stub);
    }
}
