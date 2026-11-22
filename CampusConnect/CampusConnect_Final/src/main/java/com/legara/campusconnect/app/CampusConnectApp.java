package com.legara.campusconnect.app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.legara.campusconnect.dao.ConnectionFactory;
import com.legara.campusconnect.dao.JdbcRequestStore;
import com.legara.campusconnect.exception.DataAccessException;
import com.legara.campusconnect.service.RequestService;
import com.legara.campusconnect.ui.CampusConnectFrame;

public class CampusConnectApp {

    public static void main(String[] args) {
        ConnectionFactory connections;
        try {
            connections = ConnectionFactory.fromDefaultFile();
        } catch (DataAccessException e) {
            System.err.println(e.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, e.getMessage(),
                    "CampusConnect configuration", JOptionPane.ERROR_MESSAGE));
            return;
        }
        RequestService service = new RequestService(new JdbcRequestStore(connections));
        SwingUtilities.invokeLater(() -> new CampusConnectFrame(service).setVisible(true));
    }
}
