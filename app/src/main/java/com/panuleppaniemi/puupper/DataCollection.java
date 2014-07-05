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

        BluetoothDevice device = findOBDDevice(BluetoothAdapter.getDefaultAdapter().getBondedDevices());

        adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.startDiscovery();

        try {
            socket = connectToDevice(device);
        } catch (IOException exception) {
            Log.e("socket", exception.getMessage());
        }

        timeout(startReadDataThread());

        try {
            socket.getInputStream().close();
            socket.getOutputStream().close();
            socket.close();
        } catch (IOException exception) {

        }

        Log.d("app", "Finished");
    }

    public Thread startReadDataThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                EngineRPMObdCommand cmd = new EngineRPMObdCommand();

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
            if (System.currentTimeMillis() > end) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {

            }
        }
    }

    private BluetoothDevice findOBDDevice(Set<BluetoothDevice> pairedDevices) {
        for (BluetoothDevice pairedDevice : pairedDevices) { // TODO: Use Guava.
            if (pairedDevice.getName().trim().equals("OBDII")) { // OBDII
                return pairedDevice;
            }
        }

        return null;
    }

    private BluetoothSocket connectToDevice(BluetoothDevice device) throws IOException {
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUIDForApp);
        socket.connect();

        adapter.cancelDiscovery(); // Slow otherwise.

        return socket;
    }
}
