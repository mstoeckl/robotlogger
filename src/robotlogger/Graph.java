/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robotlogger;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

/**
 *
 * @author Robotics
 */
public class Graph extends JFrame implements PacketReceiver.FloatPacketClient {

    private final JPanel substrate;
    private FloatQueue queue;
    private float ymin;
    private float ymax;
    private float hscale;

    private volatile static Color[] colors = new Color[0];
    private int[][] xbuffer;
    private int[][] ybuffer;
    private int[] pcounts;
    private volatile boolean ignore;

    /**
     * Creates new form Graph
     */
    public Graph() {
        initComponents();

        ignore = false;
        repaint_scheduled = false;

        xbuffer = new int[1][1];
        ybuffer = new int[1][1];
        pcounts = new int[1];

        ChangeListener e = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateSettings();
            }
        };

        configJSpinner(spinYMax, e);
        configJSpinner(spinYMin, e);
        configJSpinner(spinHScale, e);
        configJSpinner(spinHLength, e);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                updateSettings();
            }
        });

        ignoreButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                ignore = ignoreButton.isSelected();
                if (!ignore) {
                    scheduleRepaint();
                }
            }
        });

        substrate = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                draw((Graphics2D) g);
            }
        };
        substrate.setPreferredSize(new Dimension(500, 300));

        jScrollPane1.setViewportView(substrate);
        updateSettings();
    }

    @Override
    public void setQueue(FloatQueue s) {
        queue = s;
    }

    @Override
    public void newPackets(int k) {
        if (!ignore) {
            scheduleRepaint();
        }
    }

    /**
     * Rescale, repaint, reconfigure the underlying panel
     */
    private void updateSettings() {
        System.out.println("Updating settings");

        int width = (Integer) spinHLength.getValue();
        hscale = ((Double) spinHScale.getValue()).floatValue();
        ymin = ((Double) spinYMin.getValue()).floatValue();
        ymax = ((Double) spinYMax.getValue()).floatValue();

        int height = substrate.getHeight();
        substrate.setPreferredSize(new Dimension(width, height));

        scheduleRepaint();
    }

    private volatile boolean repaint_scheduled;

    private void scheduleRepaint() {
        if (!repaint_scheduled) {
            repaint_scheduled = false;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            repaint(500);
                            repaint_scheduled = false;
                        }
                    });
                }
            }).start();

        }
    }

    public static void configJSpinner(JSpinner j, ChangeListener e) {
        ((DefaultFormatter) ((JFormattedTextField) j.getEditor()
                .getComponent(0)).getFormatter()).setCommitsOnValidEdit(true);
        j.addChangeListener(e);
    }

    private void draw(Graphics2D g) {
        int width = substrate.getWidth();
        int height = substrate.getHeight();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, width, height);

        if (hscale == 0 || queue == null) {
            return;
        }
        int ticks = (int) (width / hscale);
        int len = queue.size();
        int scanback;
        if (len > ticks) {
            scanback = ticks;
        } else {
            scanback = len;
        }

        rescaleBuffers(scanback + 2);

        int last_span = 0;
        for (float[] values : queue) {
            int span = values.length;
            for (int i = 0; i < span; i++) {
                // add values to set
                pcounts[i] += 1;
                int pos = pcounts[i];
                xbuffer[i][pos] = (int) (scanback * hscale);
                ybuffer[i][pos] = (int) linearScale(values[i], ymin, ymax, (float) height, 0.0f);
            }

            for (int i = span; i < last_span; i++) {
                // terminate set
                int cutoff = pcounts[i];
                if (cutoff > 0) {
                    System.out.format("c %d\n", cutoff);
                    g.setColor(colors[i]);
                    g.drawPolyline(xbuffer[i], ybuffer[i], cutoff + 1);
                }
                pcounts[i] = -1;
            }

            last_span = span;
            scanback--;
            if (scanback < 0) {
                break;
            }
        }
        // finalize all tracks making it to the end
        for (int i = 0; i < last_span; i++) {
            g.setColor(colors[i]);
            g.drawPolyline(xbuffer[i], ybuffer[i], pcounts[i] + 1);
        }
    }

    private static float linearScale(float v, float imin, float imax, float omin, float omax) {
        return (v - imin) / (imax - imin) * (omax - omin) + omin;
    }

    private void rescaleBuffers(int length) {
        int width = 0;
        int cc = 0;
        for (float[] values : queue) {
            if (values.length > width) {
                width = values.length;
            }
            cc++;
            if (cc > length) {
                break;
            }
        }
        if (width == 0) {
            return;
        }

        if (width > pcounts.length) {
            pcounts = new int[width];

            int[][] tx = xbuffer;
            xbuffer = new int[width][];
            System.arraycopy(tx, 0, xbuffer, 0, tx.length);

            int[][] ty = ybuffer;
            ybuffer = new int[width][];
            System.arraycopy(ty, 0, ybuffer, 0, ty.length);

            if (length > tx[0].length) {
                for (int i = 0; i < width; i++) {
                    xbuffer[i] = new int[length];
                    ybuffer[i] = new int[length];
                }
            } else {
                for (int i = tx.length; i < width; i++) {
                    xbuffer[i] = new int[length];
                    ybuffer[i] = new int[length];
                }
            }
        } else if (length > xbuffer[0].length) {
            for (int i = 0; i < xbuffer.length; i++) {
                xbuffer[i] = new int[length];
                ybuffer[i] = new int[length];
            }
        }
        Arrays.fill(pcounts, -1);

        if (colors.length < pcounts.length) {
            colors = new Color[pcounts.length];
            for (int ii = 0; ii < pcounts.length; ii++) {
                colors[ii] = Color.getHSBColor(ii * 0.17f, 1.0f, 0.7f);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        spinYMin = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        spinYMax = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        spinHScale = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        spinHLength = new javax.swing.JSpinner();
        ignoreButton = new javax.swing.JToggleButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane1.setMinimumSize(new java.awt.Dimension(60, 100));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 437, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 260, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(jPanel3);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.X_AXIS));

        jLabel1.setText("Y Min");
        jPanel2.add(jLabel1);

        spinYMin.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(-1.0d), null, null, Double.valueOf(1.0d)));
        jPanel2.add(spinYMin);

        jLabel2.setText("Y Max");
        jPanel2.add(jLabel2);

        spinYMax.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(1.0d), null, null, Double.valueOf(1.0d)));
        jPanel2.add(spinYMax);

        jLabel3.setText("Horizontal Scale");
        jPanel2.add(jLabel3);

        spinHScale.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(1.0d), Double.valueOf(0.0d), null, Double.valueOf(1.0d)));
        jPanel2.add(spinHScale);

        jLabel4.setText("Graph Width");
        jPanel2.add(jLabel4);

        spinHLength.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1000), null, null, Integer.valueOf(10)));
        jPanel2.add(spinHLength);

        ignoreButton.setText("Ignore");
        jPanel2.add(ignoreButton);

        getContentPane().add(jPanel2);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton ignoreButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner spinHLength;
    private javax.swing.JSpinner spinHScale;
    private javax.swing.JSpinner spinYMax;
    private javax.swing.JSpinner spinYMin;
    // End of variables declaration//GEN-END:variables
}
