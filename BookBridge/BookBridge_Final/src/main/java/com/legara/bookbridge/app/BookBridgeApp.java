package com.legara.bookbridge.app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.legara.bookbridge.dao.ConnectionFactory;
import com.legara.bookbridge.dao.JdbcLibraryStore;
import com.legara.bookbridge.exception.DataAccessException;
import com.legara.bookbridge.service.LibraryService;
import com.legara.bookbridge.ui.MainFrame;

public class BookBridgeApp {
    private static final String SETTINGS_FILE = "db.properties";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BookBridgeApp::start);
    }

    private static void start() {
        try {
            ConnectionFactory connections = ConnectionFactory.fromFile(SETTINGS_FILE);
            LibraryService service = new LibraryService(new JdbcLibraryStore(connections));
            MainFrame frame = new MainFrame(service);
            frame.setVisible(true);
            frame.reload(true);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Configuration error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
