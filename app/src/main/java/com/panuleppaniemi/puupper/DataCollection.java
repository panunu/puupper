package com.panuleppaniemi.puupper;

import android.app.Activity;
import android.bluetooth.*;
import android.os.Bundle;
import android.util.Log;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;

public class DataCollection extends Activity {
    private static final UUID UUIDForApp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected BluetoothAdapter adapter;
    protected BluetoothSocket socket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);

        Log.d("onCreate", "Initialized view");

        BluetoothDevice device = findOBDDevice(BluetoothAdapter.getDefaultAdapter().getBondedDevices());

        adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.startDiscovery();

        Log.d("adapter", Integer.toString(adapter.getState()));

        try {
            Log.d("socket", "Connecting to " + device.getName());
            socket = connectToDevice(device);

            Log.d("adapter", Integer.toString(adapter.getState()));
        } catch (IOException exception) {
            Log.e("socket", exception.getMessage());
        }

        Log.d("socket", socket.getRemoteDevice().toString());
        Log.d("socket", "Connected: " + Boolean.toString(socket.isConnected()));

        timeout(startReadDataThread());

        try {
            socket.getInputStream().close();
            socket.getOutputStream().close();
            socket.close();
            Log.d("adapter", Integer.toString(adapter.getState()));
            Log.d("socket", "Closed connection");
        } catch (IOException exception) {

        }

        Log.d("socket", "Is connected: " + socket.isConnected());

        Log.d("finish", "Done");
    }

    public Thread startReadDataThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                EngineRPMObdCommand cmd = new EngineRPMObdCommand();

                Log.d("data", "Trying to read RPM");

                try {
                    cmd.run(socket.getInputStream(), socket.getOutputStream());
                    Log.d("data", cmd.getFormattedResult());
                } catch (Exception exception) {
                    Log.e("data", exception.getMessage());
                }
            }
        });

        thread.start();

        return thread;
    }

    private void timeout(Thread thread) {
        long end = System.currentTimeMillis() + 10000;

        while (thread.isAlive()) {
            if (System.currentTimeMillis() >= end) {
                break;
            }

            try {
                Thread.sleep(1000);
                Log.d("thread", "Trying...");
            } catch (InterruptedException exception) {

            }
        }
    }

    private BluetoothDevice findOBDDevice(Set<BluetoothDevice> pairedDevices) {
        for (BluetoothDevice pairedDevice : pairedDevices) { // TODO: Use Guava.
            Log.d("findOBDDevice", pairedDevice.getName());

            if (pairedDevice.getName().trim().equals("OBDII")) { // OBDII
                Log.d("findOBDDevice", "Found OBDII device");

                return pairedDevice;
            }
        }

        return null;
    }

    private BluetoothSocket connectToDevice(BluetoothDevice device) throws IOException {
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUIDForApp);
        socket.connect();

        adapter.cancelDiscovery();

        Log.d("connectToDevice", "Connected to socket " + socket.getRemoteDevice().toString());

        return socket;
    }
}
