package com.kirshboim.polyjam;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OSCInputHandler {

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    private OSCPortOut portOut;

    public void start(String serverIp, int serverPort) {
        try {
            portOut = new OSCPortOut(InetAddress.getByName(serverIp), serverPort);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (portOut != null) {
            allFingersUp();
            portOut.close();
        }
    }

    public void allFingersUp() {
        for (int i = 0; i < Configuration.MAX_POINTER_COUNT; i++) {
            pointer(i, -1f, -1f, -1f, -1f, -1f, -1f);
        }
    }

    public void pointer(int pointerId, float x, float y, float pressure,
                        float azimuth, float pitch, float roll) {

        List<Object> args = new ArrayList<Object>(7);
        args.add(pointerId);
        args.add(x);
        args.add(y);
        args.add(pressure);
        args.add(azimuth);
        args.add(pitch);
        args.add(roll);

        send(new OSCMessage("/finger", args));
    }

    private void send(final OSCMessage message) {
        Runnable runnable =  new Runnable() {

            @Override
            public void run() {
                try {
                    portOut.send(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        executor.execute(runnable);
    }
}
