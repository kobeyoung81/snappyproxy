/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kobeyoung81.snappyvpn;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import de.jarnbjo.jsnappy.Buffer;
import de.jarnbjo.jsnappy.SnappyCompressor;
import de.jarnbjo.jsnappy.SnappyDecompressor;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

public class SnappyVPNService extends VpnService implements Handler.Callback, Runnable {
    private static final String TAG = "SnappyVPNService";

    private String mServerAddress;
    private String mServerPort;
    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThread;

    private ParcelFileDescriptor mInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }

        // Extract information from the intent.
        String prefix = getPackageName();
        mServerAddress = intent.getStringExtra(prefix + ".ADDRESS");
        mServerPort = intent.getStringExtra(prefix + ".PORT");

        // Start a new session by creating a new thread.
        mThread = new Thread(this, "SnappyVPNThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");

            // If anything needs to be obtained using the network, get it now.
            // This greatly reduces the complexity of seamless handover, which
            // tries to recreate the tunnel without shutting down everything.
            // In this demo, all we need to know is the server address.
            InetSocketAddress server = new InetSocketAddress(
                    mServerAddress, Integer.parseInt(mServerPort));

            // We try to create the tunnel for several times. The better way
            // is to work with ConnectivityManager, such as trying only when
            // the network is avaiable. Here we just use a counter to keep
            // things simple.
            for (int attempt = 0; attempt < 10; ++attempt) {
                mHandler.sendEmptyMessage(R.string.connecting);

                // Reset the counter if we were connected.
                if (run(server)) {
                    attempt = 0;
                }

                // Sleep for a while. This also checks if we got interrupted.
                Thread.sleep(3000);
            }
            Log.i(TAG, "Giving up");
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }
            mInterface = null;

            mHandler.sendEmptyMessage(R.string.disconnected);
            Log.i(TAG, "Exiting");
        }
    }

    private boolean run(InetSocketAddress server) throws Exception {
        DatagramChannel tunnel = null;
        boolean connected = false;
        try {            
            tunnel = DatagramChannel.open();

            // Protect the tunnel before connecting to avoid loopback.
            if (!protect(tunnel.socket())) {
                throw new IllegalStateException("Cannot protect the tunnel");
            }

            // Connect to the server.
            tunnel.connect(server);

            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            tunnel.configureBlocking(false);

            // Authenticate and configure the virtual network interface.
            //handshake(tunnel);
            configure();

            // Now we are connected. Set the flag and show the message.
            connected = true;
            mHandler.sendEmptyMessage(R.string.connected);

            // Packets to be sent are queued in this input stream.
            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());

            // Packets received need to be written to this output stream.
            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());

            // Allocate the buffer for a single packet.
            ByteBuffer packet = ByteBuffer.allocate(32767);

            // We use a timer to determine the status of the tunnel. It
            // works on both sides. A positive value means sending, and
            // any other means receiving. We start with receiving.
            int timer = 0;

            // We keep forwarding packets till something goes wrong.
            while (true) {
                // Assume that we did not make any progress in this iteration.
                boolean idle = true;

                // Read the outgoing packet from the input stream.
                int length = in.read(packet.array());
                if (length > 0) {
                    // Write the outgoing packet to the tunnel.
                    packet.limit(length);
                    //Snappy compression
                    Buffer compressedResult = SnappyCompressor.compress(packet.array(), 0, length);
                    
                    tunnel.write(ByteBuffer.wrap((compressedResult.getData())));
                    
                    packet.clear();

                    // There might be more outgoing packets.
                    idle = false;

                    // If we were receiving, switch to sending.
                    if (timer < 1) {
                        timer = 1;
                    }
                }

                // Read the incoming packet from the tunnel.
                length = tunnel.read(packet);
                if (length > 0) {
                    // Ignore control messages, which start with zero.
                    //if (packet.get(0) != 0) {
                    // Write the incoming packet to the output stream.
                	
                	//Snappy decompresssion
                	Buffer result = SnappyDecompressor.decompress(packet.array(), 0, length);
                    out.write(result.getData());
                    //}
                    packet.clear();

                    // There might be more incoming packets.
                    idle = false;

                    // If we were sending, switch to receiving.
                    if (timer > 0) {
                        timer = 0;
                    }
                }

                // If we are idle or waiting for the network, sleep for a
                // fraction of time to avoid busy looping.
                if (idle) {
                    Thread.sleep(100);

                    // Increase the timer. This is inaccurate but good enough,
                    // since everything is operated in non-blocking mode.
                    timer += (timer > 0) ? 100 : -100;

                    // We are receiving for a long time but not sending.
                    if (timer < -15000) {
                        // Switch to sending.
                        timer = 1;
                    }

                    // We are sending for a long time but not receiving.
                    if (timer > 20000) {
                        throw new IllegalStateException("Timed out");
                    }
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                tunnel.close();
            } catch (Exception e) {
                // ignore
            }
        }
        return connected;
    }

    private void configure() throws Exception {
        // If the old interface has exactly the same parameters, use it!
        if (mInterface != null) {
            Log.i(TAG, "Using the previous interface");
            return;
        }

        // Configure a builder while parsing the parameters.
        Builder builder = new Builder();

        // Create a new interface using the builder and save the parameters.
        mInterface = builder.setSession(mServerAddress)
        		.addAddress("192.168.77.7", 24)
        		.addDnsServer("8.8.8.8")
        		.addRoute("0.0.0.0", 0)
                .setConfigureIntent(mConfigureIntent)
                .establish();
        Log.i(TAG, "New interface established. ");
    }
}
