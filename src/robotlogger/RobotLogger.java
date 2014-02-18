/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robotlogger;

import java.awt.EventQueue;

/**
 *
 * @author Robotics
 */
public class RobotLogger {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final Main main = new Main();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                main.setVisible(true);
            }
        });
    }
}
