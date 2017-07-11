package de.uzl.itm.ProjektGruppe3;

import de.dennis_boldt.RXTX;

import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

/**
 * Created by Marco Buchholz on 01.06.17.
 */
public class SerialCommunicator {
    private final int baudRate;
    private final String ports;
    private final String rxtxlib;
    private final Map<String, Observer> observers = new HashMap<String, Observer>();

    public SerialCommunicator(int baudRate, String ports, String rxtxlib) {
        this.baudRate = baudRate;
        this.ports = ports;
        this.rxtxlib = rxtxlib;
    }

    public boolean registerObserver(String name, Observer observer) throws Exception {
        return observers.putIfAbsent(name, observer) == null;
    }

    public void startAll() throws Exception{
        for (String s : observers.keySet()) {
            startCommunication(s);
        }
    }

    public void startCommunication(String name) throws Exception {
        if (!observers.containsKey(name)) {
            throw new IllegalArgumentException(String.format("Observer with name %s not found", name));
        }

        RXTX rxtx = new RXTX(baudRate);
        rxtx.start(ports, rxtxlib, observers.get(name));
    }
}
