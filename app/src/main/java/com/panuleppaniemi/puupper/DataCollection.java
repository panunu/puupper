package com.panuleppaniemi.puupper;

import android.app.Activity;
import android.bluetooth.*;
import android.os.Bundle;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import pt.lighthouselabs.obd.commands.SpeedObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import pt.lighthouselabs.obd.commands.engine.ThrottlePositionObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelConsumptionRateObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import pt.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.ObdResetCommand;
import pt.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import pt.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import pt.lighthouselabs.obd.enums.FuelTrim;
import pt.lighthouselabs.obd.enums.ObdProtocols;

// TODO: Make it not a main activity.
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

        Log.d("socket", "Connected");

        Log.d("data", "Init");
        ensureTimeout(startInitThread());

        Log.d("data", "Start data collection");
        for (int i = 0; i < 5; i++) {
            ensureTimeout(startReadDataThread());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {

            }
        }

        closeSocket(socket);

        Log.d("app", "Finished");
    }

    public Thread startInitThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new ObdResetCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new EchoOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new TimeoutObdCommand(2000).run(socket.getInputStream(), socket.getOutputStream());
                    new SelectProtocolObdCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                } catch (Exception exception) {
                    Log.e("data", exception.getMessage());
                }
            }
        });

        thread.start();

        return thread;
    }

    public Thread startReadDataThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                EngineRPMObdCommand rpm = new EngineRPMObdCommand();
                FuelEconomyObdCommand consumption = new FuelEconomyObdCommand();
                SpeedObdCommand speed = new SpeedObdCommand();
                FuelLevelObdCommand fuelLevel = new FuelLevelObdCommand();
                FuelConsumptionRateObdCommand fuelRate = new FuelConsumptionRateObdCommand();

                try {
                    rpm.run(socket.getInputStream(), socket.getOutputStream());
                    speed.run(socket.getInputStream(), socket.getOutputStream());

                    Log.d(
                        "data",
                        rpm.getFormattedResult() + "   " +
                        speed.getFormattedResult()
                    );
                } catch (Exception exception) {
                    Log.e("data", exception.getMessage());
                }
            }
        });

        thread.start();

        return thread;
    }

    private void ensureTimeout(Thread thread) {
        long end = System.currentTimeMillis() + 15000;

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

    private void closeSocket(BluetoothSocket socket) {
        try {
            socket.getInputStream().close();
            socket.getOutputStream().close();
            socket.close();
        } catch (IOException exception) {

        }
    }
}
