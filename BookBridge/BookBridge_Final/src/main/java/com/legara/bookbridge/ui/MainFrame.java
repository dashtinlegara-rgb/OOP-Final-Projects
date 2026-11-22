package com.legara.bookbridge.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.legara.bookbridge.exception.DataAccessException;
import com.legara.bookbridge.exception.EntityNotFoundException;
import com.legara.bookbridge.exception.LibraryException;
import com.legara.bookbridge.service.LibraryService;
import com.legara.bookbridge.service.LibrarySnapshot;

public class MainFrame extends JFrame {
    private final LibraryService service;
    private final JLabel statusLabel = new JLabel("Starting...");
    private final JProgressBar progressBar = new JProgressBar();
    private final List<JComponent> actionControls = new ArrayList<>();

    private final CatalogPanel catalogPanel;
    private final MembersPanel membersPanel;
    private final CirculationPanel circulationPanel;
    private final ReservationsPanel reservationsPanel;
    private final ReportsPanel reportsPanel;

    public MainFrame(LibraryService service) {
        super("BookBridge Library System");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        catalogPanel = new CatalogPanel(this);
        membersPanel = new MembersPanel(this);
        circulationPanel = new CirculationPanel(this);
        reservationsPanel = new ReservationsPanel(this);
        reportsPanel = new ReportsPanel(this);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catalog", catalogPanel);
        tabs.addTab("Members", membersPanel);
        tabs.addTab("Circulation", circulationPanel);
        tabs.addTab("Reservations", reservationsPanel);
        tabs.addTab("Reports", reportsPanel);
        add(tabs, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(progressBar, BorderLayout.EAST);
        return bar;
    }

    public LibraryService service() {
        return service;
    }

    public void registerAction(JComponent control) {
        actionControls.add(control);
    }

    public void showStatus(String message) {
        statusLabel.setText(message);
    }

    public <T> void runInBackground(String busyMessage, BackgroundTask<T> task, Consumer<T> onSuccess) {
        setBusy(true, busyMessage);
        Thread worker = new Thread(() -> {
            try {
                T result = task.run();
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, null);
                    onSuccess.accept(result);
                });
            } catch (LibraryException e) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, null);
                    showError(e);
                });
            } catch (RuntimeException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, null);
                    showStatus("Unexpected error: " + e);
                    JOptionPane.showMessageDialog(this, "Unexpected error: " + e
                            + "\nDetails were printed to the Output window.", "Unexpected error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "bookbridge-worker");
        worker.start();
    }

    public void runAction(String busyMessage, BackgroundTask<String> action) {
        runAction(busyMessage, action, () -> {
        });
    }

    public void runAction(String busyMessage, BackgroundTask<String> action, Runnable afterSuccess) {
        runInBackground(busyMessage, action, message -> {
            showStatus(message);
            afterSuccess.run();
            reload(false);
        });
    }

    public void reload(boolean fromDatabase) {
        runInBackground(fromDatabase ? "Loading from database..." : null, () -> {
            if (fromDatabase) {
                service.loadFromStore();
            }
            return service.snapshot();
        }, snapshot -> {
            show(snapshot);
            if (fromDatabase) {
                showStatus("Loaded " + snapshot.books().size() + " book(s), " + snapshot.members().size()
                        + " member(s), " + snapshot.activeLoans().size() + " active loan(s) from the database.");
            }
        });
    }

    private void show(LibrarySnapshot snapshot) {
        catalogPanel.show(snapshot);
        membersPanel.show(snapshot);
        circulationPanel.show(snapshot);
        reservationsPanel.show(snapshot);
        reportsPanel.show(snapshot);
    }

    public void showValidation(String message) {
        showStatus(message);
        JOptionPane.showMessageDialog(this, message, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    public boolean confirm(String question) {
        return JOptionPane.showConfirmDialog(this, question, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void showError(LibraryException e) {
        String title;
        if (e instanceof DataAccessException) {
            title = "Database error";
            e.printStackTrace();
        } else if (e instanceof EntityNotFoundException) {
            title = "Not found";
        } else {
            title = "Request refused";
        }
        showStatus(title + ": " + e.getMessage());
        JOptionPane.showMessageDialog(this, e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void setBusy(boolean busy, String message) {
        for (JComponent control : actionControls) {
            control.setEnabled(!busy);
        }
        progressBar.setVisible(busy);
        if (message != null) {
            showStatus(message);
        }
    }
}
