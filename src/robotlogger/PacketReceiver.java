/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robotlogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Robotics
 */
public class PacketReceiver {

    private volatile Set<PacketClient> univclients;
    private volatile Map<String, List<PacketClient>> specclients;
    private int port;
    private final Thread feed;
    private volatile Map<String, FloatQueue> map;

    PacketReceiver(int default_port) {
        univclients = new TreeSet<>();
        specclients = new TreeMap<>();
        map = new TreeMap<>();
        this.port = default_port;
        feed = new Thread(new Runnable() {
            int cport;
            DatagramSocket socket;

            private void receive() {
                if (cport == -1) {
                    receiveFake();
                } else {
                    receiveReal();
                }
            }

            private double t = 0.0;

            private void receiveFake() {
                t += 3.2;
                if (t > 1000.0) {
                    t = 0.0;
                }
                deliverPackets(String.format("Foo: %f\n", (Double) Math.sin(t)));
            }

            private final byte[] buf = new byte[1 << 16];

            private void receiveReal() {
                DatagramPacket p = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(p);
                } catch (IOException e) {
                    return;
                }

                deliverPackets(new String(p.getData(), 0, p.getLength()));
            }

            private boolean init() {
                if (cport == -1) {
                    return true;
                }

                try {
                    socket = new DatagramSocket(cport);
                    socket.setSoTimeout(300);
                } catch (SocketException ex) {
                    Logger.getLogger(PacketReceiver.class.getName()).log(Level.SEVERE, null, ex);
                    socket = null;
                    return false;
                }
                return true;
            }

            private void cleanup() {
                if (port == -1) {
                    return;
                }

                socket.close();
            }

            @Override
            public void run() {
                cport = port;
                while (true) {
                    try {
                        while (cport == port) {
                            if (!init()) {
                                while (cport == port) {
                                    Thread.sleep(100);
                                }
                            } else {
                                while (cport == port) {
                                    receive();
                                }
                                cleanup();
                            }
                        }
                    } catch (InterruptedException e) {

                    }
                    cport = port;
                }
            }

        });
        feed.setDaemon(true);
    }

    void start() {
        feed.start();
    }

    private synchronized void deliverPackets(String packet) {
        System.out.print(packet);
    }

    /**
     * port = -1 signifies fake
     *
     * @param port
     */
    public synchronized void setPort(int port) {
        this.port = port;
    }

    public synchronized int getPort() {
        return port;
    }

    /*
     TODO: some want floats lists, others want
     string streams
     */
    public static interface FloatPacketClient {

        void setQueue(FloatQueue s);

        void newPackets(int k);
    }

    public static interface UnivPacketClient {

        void newPacket(String r);
    }

    public synchronized void addSpecificClient(String key, PacketClient p) {

        List<PacketClient> e = specclients.get(key);
        if (e == null) {
            List<PacketClient> k = new ArrayList<>(1);
            k.add(p);
            specclients.put(key, k);
        } else {
            e.add(p);
        }
    }

    public synchronized void removeSpecificClient(String key, PacketClient p) {
        List<PacketClient> e = specclients.get(key);
        if (e != null) {
            e.remove(p);
        }
    }

    public synchronized void addUniversalClient(PacketClient p) {
        univclients.add(p);
    }

    public synchronized void removeUniversalClient(PacketClient p) {
        univclients.remove(p);
    }

    public synchronized Map<String, FloatQueue> getAvailable() {
        return map;
    }
}
