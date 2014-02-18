/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robotlogger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Robotics
 */
public class Main extends JFrame {

    private final PacketReceiver rec;

    public Main() {
        initComponents();

        final int startport = 1140;

        rec = new PacketReceiver(startport);

        portSpinner.setModel(new SpinnerNumberModel(startport, 1, 65535, 1));
        portSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateReceiver();
            }
        });

        modeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateReceiver();
            }
        });

        rec.addUniversalClient(new PacketReceiver.PacketClient() {
            @Override
            public void setQueue(FloatQueue s) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void newPackets(int k) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        rec.start();
    }

    private void updateReceiver() {
        String key = (String) modeCombo.getSelectedItem();
        int port;
        switch (key) {
            default:
            case "Only Real":
                port = (Integer) portSpinner.getValue();
                break;
            case "Log Fake":
                port = -1;
                break;
            case "Just Fake":
                port = -1;
                break;
            case "Log Real":
                port = (Integer) portSpinner.getValue();
                break;
        }
        rec.setPort(port);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        confPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jSpinner1 = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jSpinner2 = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        cutoffSpinner = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        portSpinner = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        modeCombo = new javax.swing.JComboBox();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        streamPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        tablePanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        streamTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        graphButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        streamButton = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        confPanel.setLayout(new java.awt.GridLayout(6, 2, 20, 15));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Mystery");
        confPanel.add(jLabel1);
        confPanel.add(jSpinner1);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Mystery");
        confPanel.add(jLabel2);
        confPanel.add(jSpinner2);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Textbox Cutoff");
        confPanel.add(jLabel3);
        confPanel.add(cutoffSpinner);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Port Number");
        confPanel.add(jLabel4);
        confPanel.add(portSpinner);

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Mode");
        confPanel.add(jLabel5);

        modeCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Log Real", "Log Fake", "Just Fake", "Only Real" }));
        confPanel.add(modeCombo);
        confPanel.add(filler4);
        confPanel.add(filler6);

        jTabbedPane1.addTab("Configuration", confPanel);

        streamPanel.setLayout(new java.awt.BorderLayout());

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        streamPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Stream", null, streamPanel, "");

        tablePanel.setLayout(new javax.swing.BoxLayout(tablePanel, javax.swing.BoxLayout.Y_AXIS));

        streamTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, "0"}
            },
            new String [] {
                "Key", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(streamTable);

        tablePanel.add(jScrollPane2);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));
        jPanel1.add(filler3);

        graphButton.setText("Graph");
        jPanel1.add(graphButton);
        jPanel1.add(filler1);

        streamButton.setText("Text Stream");
        jPanel1.add(streamButton);
        jPanel1.add(filler2);

        tablePanel.add(jPanel1);

        jTabbedPane1.addTab("Table", tablePanel);

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel confPanel;
    private javax.swing.JSpinner cutoffSpinner;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler6;
    private javax.swing.JButton graphButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JComboBox modeCombo;
    private javax.swing.JSpinner portSpinner;
    private javax.swing.JButton streamButton;
    private javax.swing.JPanel streamPanel;
    private javax.swing.JTable streamTable;
    private javax.swing.JPanel tablePanel;
    // End of variables declaration//GEN-END:variables
}
