/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robotlogger;

import java.util.Map;
import java.util.TreeMap;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Robotics
 */
public class StreamTableModel extends DefaultTableModel implements PacketReceiver.UnivPacketClient {

    private final Map<String, Integer> map;

    public StreamTableModel() {
        super(new Object[][]{{"", ""}},
                new Object[]{"Key", "Value"});
        map = new TreeMap<>();
        map.put("", 0);
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public void newPacket(String r) {
        String key = PacketReceiver.packetKey(r);
        String val = PacketReceiver.packetValue(r);

        Integer loc = map.get(key);
        if (loc == null) {
            this.addRow(new Object[]{key, val});
            map.put(key, this.getRowCount() - 1);
        } else {
            this.setValueAt(val, loc, 1);
        }
    }
}
