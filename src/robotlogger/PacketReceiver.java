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

    public static String packetKey(String p) {
        int isp = p.indexOf(":");
        return p.substring(0, isp);
    }

    public static String packetValue(String p) {
        int isp = p.indexOf(":");
        return p.substring(isp + 1);
    }
    private volatile Map<String, List<FloatPacketClient>> fclients;
    private final Thread feed;
    private volatile Map<String, FloatQueue> fmap;
    private int port;
    private double rpm;
    private volatile Map<String, List<StringPacketClient>> sclients;
    private volatile Map<String, List<String>> smap;

    private volatile Set<UnivPacketClient> uclients;

    PacketReceiver(int default_port, double default_rpm) {
        uclients = new HashSet<>();
        fclients = new HashMap<>();
        sclients = new HashMap<>();
        fmap = new HashMap<>();
        smap = new HashMap<>();
        rpm = default_rpm;

        this.port = default_port;
        feed = new Thread(new UDPPuller(this));
        feed.setDaemon(true);
    }

    public synchronized void addSpecificClient(String key, FloatPacketClient p) {

        List<FloatPacketClient> e = fclients.get(key);
        if (e == null) {
            List<FloatPacketClient> k = new ArrayList<>(1);
            k.add(p);
            fclients.put(key, k);
        } else {
            e.add(p);
        }
        // hook in stream if it exists
        FloatQueue f = fmap.get(key);
        if (f != null) {
            p.setQueue(f);
        }
    }

    public synchronized void addSpecificClient(String key, StringPacketClient p) {

        List<StringPacketClient> e = sclients.get(key);
        if (e == null) {
            List<StringPacketClient> k = new ArrayList<>(1);
            k.add(p);
            sclients.put(key, k);
        } else {
            e.add(p);
        }
        // hook in stream if it exists
        List<String> f = smap.get(key);
        if (f != null) {
            p.setQueue(f);
        }
    }

    public synchronized void addUniversalClient(UnivPacketClient p) {
        uclients.add(p);
    }

    public synchronized Map<String, FloatQueue> getAvailable() {
        return fmap;
    }

    public synchronized int getPort() {
        return port;
    }

    public synchronized double getRPM() {
        return rpm;
    }

    public synchronized void removeSpecificClient(String key, FloatPacketClient p) {
        List<FloatPacketClient> e = fclients.get(key);
        if (e != null) {
            e.remove(p);
        }
    }

    public synchronized void removeSpecificClient(String key, StringPacketClient p) {
        List<StringPacketClient> e = sclients.get(key);
        if (e != null) {
            e.remove(p);
        }
    }

    public synchronized void removeUniversalClient(UnivPacketClient p) {
        uclients.remove(p);
    }

    /**
     * port = -1 signifies fake
     *
     * @param port
     */
    public synchronized void setPort(int port) {
        this.port = port;
    }

    public synchronized void setRPM(double rpm) {
        this.rpm = rpm;
    }

    private void deliverFloat(String key, String val) {
        List<FloatPacketClient> r = fclients.get(key);
        if (r == null) {
            r = new ArrayList<>();
            fclients.put(key, r);
        }

        FloatQueue f = fmap.get(key);
        if (f == null) {
            f = new FloatQueue();
            fmap.put(key, f);
            for (FloatPacketClient j : r) {
                j.setQueue(f);
            }
        }
        if (!f.add(val)) {
            // write failed
            return;
        }

        for (FloatPacketClient j : r) {
            j.newPackets(1);
        }
    }

    private synchronized void deliverPackets(String packet) {
        if (packet.endsWith("\n")) {
            packet = packet.substring(0, packet.length() - 1);
        }

        for (UnivPacketClient r : uclients) {
            r.newPacket(packet);
        }

        String key = packetKey(packet);
        String val = packetValue(packet);
        deliverFloat(key, val);
        deliverString(key, val);
    }

    private void deliverString(String key, String val) {
        List<StringPacketClient> r = sclients.get(key);
        if (r == null) {
            r = new ArrayList<>();
            sclients.put(key, r);
        }

        List<String> f = smap.get(key);
        if (f == null) {
            f = new ArrayList<>();
            smap.put(key, f);
            for (StringPacketClient j : r) {
                j.setQueue(f);
            }
        }
        if (!f.add(val)) {
            // write failed
            return;
        }

        for (StringPacketClient j : r) {
            j.newPackets(1);
        }
    }

    void start() {
        feed.start();
    }

    /*
     TODO: some want float lists, others want
     string streams
     */
    public static interface FloatPacketClient {

        void setQueue(FloatQueue s);

        void newPackets(int k);
    }

    public static interface StringPacketClient {

        void setQueue(List<String> s);

        void newPackets(int k);
    }

    public static interface UnivPacketClient {

        void newPacket(String r);
    }

    private static class UDPPuller implements Runnable {

        private final byte[] buf = new byte[1 << 16];

        private double t = 0.0;

        int cport;
        PacketReceiver rec;

        DatagramSocket socket;

        public UDPPuller(PacketReceiver r) {
            rec = r;
        }

        @Override
        public void run() {
            cport = rec.getPort();
            while (true) {
                try {
                    while (cport == rec.getPort()) {
                        if (!init()) {
                            while (cport == rec.getPort()) {
                                Thread.sleep(100);
                            }
                        } else {
                            while (cport == rec.getPort()) {
                                receive();
                            }
                            cleanup();
                        }
                    }
                } catch (InterruptedException e) {

                }
                cport = rec.getPort();
            }
        }

        private void cleanup() {
            if (cport == -1) {
                return;
            }

            socket.close();
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

        private void receive() {
            if (cport == -1) {
                receiveFake();
            } else {
                receiveReal();
            }
        }

        private void receiveFake() {
            t += 3.2;
            if (t > 1000.0) {
                t = 0.0;
            }
            try {
                Thread.sleep((int) ((60.0 / rec.getRPM()) * 1000.0));
            } catch (InterruptedException ex) {
                Logger.getLogger(PacketReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }

            rec.deliverPackets(String.format("Foo: %f\n", (Double) Math.sin(t)));
        }

        private void receiveReal() {
            DatagramPacket p = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(p);
            } catch (IOException e) {
                return;
            }

            String o = new String(p.getData(), 0, p.getLength());
            rec.deliverPackets(o);
        }
    }
}
