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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Robotics
 */
public class PacketReceiver {

    /*
     TODO: some want float lists, others want
     string streams
     */
    public static interface SpecPacketClient {

        void setQueue(FloatQueue s);

        void newPackets(int k);
    }

    public static interface UnivPacketClient {

        void newPacket(String r);
    }

    private volatile Set<UnivPacketClient> univclients;
    private volatile Map<String, List<SpecPacketClient>> specclients;
    private int port;
    private final Thread feed;
    private volatile Map<String, FloatQueue> map;

    PacketReceiver(int default_port) {
        univclients = new HashSet<>();
        specclients = new HashMap<>();
        map = new HashMap<>();
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
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PacketReceiver.class.getName()).log(Level.SEVERE, null, ex);
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

                String o = new String(p.getData(), 0, p.getLength());
                deliverPackets(o);
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

    public static String packetKey(String p) {
        int isp = p.indexOf(":");
        return p.substring(0, isp);
    }

    public static String packetValue(String p) {
        int isp = p.indexOf(":");
        return p.substring(isp + 1);
    }

    private synchronized void deliverPackets(String packet) {
        if (packet.endsWith("\n")) {
            packet = packet.substring(0, packet.length() - 1);
        }

        for (UnivPacketClient r : univclients) {
            r.newPacket(packet);
        }

        String key = packetKey(packet);
        String val = packetValue(packet);

        List<SpecPacketClient> r = specclients.get(key);
        if (r == null) {
            r = new ArrayList<>();
            specclients.put(key, r);
        }

        FloatQueue f = map.get(key);
        if (f == null) {
            f = new FloatQueue();
            map.put(key, f);
            for (SpecPacketClient j : r) {
                j.setQueue(f);
            }
        }
        if (!f.add(val)) {
            // write failed
            return;
        }

        for (SpecPacketClient j : r) {
            j.newPackets(1);
        }
    }

    public synchronized void addSpecificClient(String key, SpecPacketClient p) {

        List<SpecPacketClient> e = specclients.get(key);
        if (e == null) {
            List<SpecPacketClient> k = new ArrayList<>(1);
            k.add(p);
            specclients.put(key, k);
        } else {
            e.add(p);
        }
        // hook in stream if it exists
        FloatQueue f = map.get(key);
        if (f != null) {
            p.setQueue(f);
        }
    }

    public synchronized void removeSpecificClient(String key, SpecPacketClient p) {
        List<SpecPacketClient> e = specclients.get(key);
        if (e != null) {
            e.remove(p);
        }
    }

    public synchronized void addUniversalClient(UnivPacketClient p) {
        univclients.add(p);
    }

    public synchronized void removeUniversalClient(UnivPacketClient p) {
        univclients.remove(p);
    }

    public synchronized Map<String, FloatQueue> getAvailable() {
        return map;
    }
}
