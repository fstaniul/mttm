package com.staniul.teamspeak.rmi;

import org.springframework.stereotype.Component;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

@Component
public class RmiServer {
    public RmiServer (RmiController controller) throws RemoteException {
        RmiController stub = (RmiController) UnicastRemoteObject.exportObject(controller, 12000);
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("RmiController", stub);
    }
}
