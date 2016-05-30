package com.kirshboim.polyjam;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PolyjamHttpClient {

    private static final String TAG = PolyjamHttpClient.class.getSimpleName();

    public static PolyjamHttpClient instance = new PolyjamHttpClient();
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final HttpClient httpclient;
    private String endpoint;

    private PolyjamHttpClient() {
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 3000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 5000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        httpclient = new DefaultHttpClient(httpParameters);
    }

    public boolean register(String name) {
        try {
            String serverIp = listenForServer();
            endpoint = "http://" + serverIp + ":8080";
            return registerOnServer(name);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerOnServer(String name) {
        String url = endpoint + "/hi?name=" + name;

        HttpGet httpRequest = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpRequest);
            response.getEntity().consumeContent();
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (response != null) {
                    response.getEntity().consumeContent();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            return false;
        }
    }

    private String listenForServer() throws Exception {
        final DatagramSocket socket = new DatagramSocket(Configuration.POLYJAM_BROADCAST_PORT);
        try {
            return executor.submit(new Callable<String>() {

                @Override
                public String call() throws Exception {
                    while (true) {
                        byte[] buf = new byte[1024];
                        final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);

                        if ("hi".equals(new String(packet.getData()).trim())) {
                            return packet.getAddress().getHostAddress();
                        }
                    }
                }
            }).get(10, TimeUnit.SECONDS);
        } finally {
            socket.close();
        }
    }

    public String status() {
        String url = endpoint + "/status";

        HttpGet httpRequest = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpRequest);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            } else {
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "failed to get status - " + e.getMessage());

            try {
                if (response != null) {
                    response.getEntity().consumeContent();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            return null;
        }
    }
}
