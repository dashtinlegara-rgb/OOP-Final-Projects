package com.legara.campusconnect.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import com.legara.campusconnect.exception.CampusConnectException;
import com.legara.campusconnect.exception.DataAccessException;
import com.legara.campusconnect.exception.DuplicateTicketException;
import com.legara.campusconnect.exception.InvalidRequestException;
import com.legara.campusconnect.exception.InvalidStatusTransitionException;
import com.legara.campusconnect.exception.RequestNotFoundException;
import com.legara.campusconnect.model.RequestStatus;
import com.legara.campusconnect.model.ServiceCategory;
import com.legara.campusconnect.model.ServiceRequest;
import com.legara.campusconnect.service.RequestService;
import com.legara.campusconnect.service.RequestSummary;

public class CampusConnectFrame extends JFrame {
    private static final String ALL = "ALL";
    private static final Color ERROR_COLOR = new Color(170, 30, 30);
    private static final Color OK_COLOR = new Color(20, 110, 40);

    private record ViewState(String message, List<ServiceRequest> rows, RequestSummary summary) {
    }

    @FunctionalInterface
    private interface BackgroundTask {
        ViewState run() throws CampusConnectException;
    }

    private final RequestService service;

    private final JTextField ticketField = new JTextField(10);
    private final JTextField studentIdField = new JTextField(12);
    private final JTextField studentNameField = new JTextField(20);
    private final JComboBox<ServiceCategory> categoryBox = new JComboBox<>(ServiceCategory.values());
    private final JSpinner prioritySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
    private final JTextField concernField = new JTextField(30);
    private final JComboBox<RequestStatus> statusBox = new JComboBox<>(RequestStatus.values());
    private final JComboBox<String> categoryFilter = new JComboBox<>(withAll(ServiceCategory.values()));
    private final JComboBox<String> statusFilter = new JComboBox<>(withAll(RequestStatus.values()));

    private final JButton addButton = new JButton("Add");
    private final JButton searchButton = new JButton("Search");
    private final JButton updateButton = new JButton("Update Status");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton processButton = new JButton("Process Next");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton clearButton = new JButton("Clear Form");
    private final JButton[] actionButtons = {addButton, searchButton, updateButton, deleteButton,
        processButton, refreshButton, clearButton};

    private final RequestTableModel tableModel = new RequestTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextArea summaryArea = new JTextArea(14, 30);
    private final JLabel statusLabel = new JLabel("Starting...");
    private final JProgressBar progressBar = new JProgressBar();

    public CampusConnectFrame(RequestService service) {
        super("CampusConnect - Student Service Requests");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildSummaryPanel(), BorderLayout.EAST);
        add(buildStatusBar(), BorderLayout.SOUTH);
        wireActions();
        setMinimumSize(new Dimension(1100, 650));
        pack();
        setLocationRelativeTo(null);
        refresh();
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Request"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        addField(form, g, 0, 0, 1, "Student ID:", studentIdField);
        addField(form, g, 2, 0, 1, "Student Name:", studentNameField);
        addField(form, g, 4, 0, 1, "Category:", categoryBox);
        addField(form, g, 0, 1, 1, "Priority (1-5):", prioritySpinner);
        addField(form, g, 2, 1, 3, "Concern:", concernField);
        addField(form, g, 0, 2, 1, "Ticket ID:", ticketField);
        addField(form, g, 2, 2, 1, "New Status:", statusBox);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filters.add(new JLabel("Filter category:"));
        filters.add(categoryFilter);
        filters.add(new JLabel("status:"));
        filters.add(statusFilter);
        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 6;
        form.add(filters, g);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (JButton b : actionButtons) {
            buttons.add(b);
        }
        g.gridy = 4;
        form.add(buttons, g);

        studentIdField.setToolTipText("Required. Also used by Search when Ticket ID is empty.");
        ticketField.setToolTipText("Used by Search, Update Status and Delete. Click a table row to fill it.");
        return form;
    }

    private static void addField(JPanel panel, GridBagConstraints g, int x, int y, int fieldWidth,
            String label, JComponent field) {
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = 1;
        g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setLabelFor(field);
        panel.add(l, g);
        g.gridx = x + 1;
        g.gridwidth = fieldWidth;
        g.weightx = 1;
        panel.add(field, g);
    }

    private JScrollPane buildTablePanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                fillFormFrom(tableModel.getRow(table.convertRowIndexToModel(table.getSelectedRow())));
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Requests (priority desc, estimated days asc, ticket ID)"));
        return scroll;
    }

    private JScrollPane buildSummaryPanel() {
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(summaryArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Summary"));
        return scroll;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        progressBar.setPreferredSize(new Dimension(160, 16));
        progressBar.setVisible(false);
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(progressBar, BorderLayout.EAST);
        return bar;
    }

    private void wireActions() {
        addButton.addActionListener(e -> onAdd());
        searchButton.addActionListener(e -> onSearch());
        updateButton.addActionListener(e -> onUpdateStatus());
        deleteButton.addActionListener(e -> onDelete());
        processButton.addActionListener(e -> onProcessNext());
        refreshButton.addActionListener(e -> refresh());
        clearButton.addActionListener(e -> clearForm());
        getRootPane().setDefaultButton(addButton);
    }

    private void onAdd() {
        String studentId = studentIdField.getText();
        String studentName = studentNameField.getText();
        ServiceCategory category = (ServiceCategory) categoryBox.getSelectedItem();
        int priority = (Integer) prioritySpinner.getValue();
        String concern = concernField.getText();
        ServiceCategory fc = selectedCategoryFilter();
        RequestStatus fs = selectedStatusFilter();
        runInBackground("Saving request...", () -> {
            ServiceRequest r = service.submit(studentId, studentName, category, priority, concern);
            return view("Added " + r.getTicketId() + " (" + r.estimateDays() + " working day(s) estimated).", fc, fs);
        }, () -> {
            ticketField.setText("");
            concernField.setText("");
        });
    }

    private void onSearch() {
        String ticketId = ticketField.getText().trim();
        String studentId = studentIdField.getText().trim();
        ServiceCategory fc = selectedCategoryFilter();
        RequestStatus fs = selectedStatusFilter();
        if (!ticketId.isEmpty()) {
            runInBackground("Searching ticket " + ticketId + "...", () -> {
                List<ServiceRequest> rows = service.lookup(ticketId);
                String message = rows.isEmpty() ? "No request with ticket ID " + ticketId + "." : "Found ticket " + ticketId + ".";
                return new ViewState(message, rows, service.summary());
            }, null);
        } else if (!studentId.isEmpty()) {
            runInBackground("Searching student " + studentId + "...", () -> {
                List<ServiceRequest> rows = service.searchByStudent(studentId);
                String message = rows.isEmpty() ? "No requests for student " + studentId + "."
                        : rows.size() + " request(s) for student " + studentId + ".";
                return new ViewState(message, rows, service.summary());
            }, null);
        } else {
            runInBackground("Filtering...", () -> view("Showing filter " + label(fc) + " / " + label(fs) + ".", fc, fs), null);
        }
    }

    private void onUpdateStatus() {
        String ticketId = ticketField.getText().trim();
        RequestStatus newStatus = (RequestStatus) statusBox.getSelectedItem();
        if (ticketId.isEmpty()) {
            showError("Validation", "Enter or select a Ticket ID first.");
            return;
        }
        ServiceCategory fc = selectedCategoryFilter();
        RequestStatus fs = selectedStatusFilter();
        runInBackground("Updating " + ticketId + "...", () -> {
            ServiceRequest r = service.updateStatus(ticketId, newStatus);
            return view(r.getTicketId() + " is now " + r.getStatus() + ".", fc, fs);
        }, null);
    }

    private void onDelete() {
        String ticketId = ticketField.getText().trim();
        if (ticketId.isEmpty()) {
            showError("Validation", "Enter or select a Ticket ID first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Permanently delete ticket " + ticketId + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            setStatus("Delete cancelled.", false);
            return;
        }
        ServiceCategory fc = selectedCategoryFilter();
        RequestStatus fs = selectedStatusFilter();
        runInBackground("Deleting " + ticketId + "...", () -> {
            ServiceRequest r = service.remove(ticketId);
            return view("Deleted " + r.getTicketId() + ".", fc, fs);
        }, () -> ticketField.setText(""));
    }

    private void onProcessNext() {
        ServiceCategory fc = selectedCategoryFilter();
        RequestStatus fs = selectedStatusFilter();
        runInBackground("Processing next request...", () -> {
            ServiceRequest r = service.processNext();
            return view("Now processing " + r.getTicketId() + " (" + r.getStudent().getStudentName()
                    + ", priority " + r.getPriority() + ").", fc, fs);
        }, null);
    }

    private void refresh() {
        ServiceCategory fc = selectedCategoryFilter();
        RequestStatus fs = selectedStatusFilter();
        runInBackground("Loading requests from the database...", () -> {
            service.reload();
            return view("Loaded " + service.size() + " request(s) from the database.", fc, fs);
        }, null);
    }

    private ViewState view(String message, ServiceCategory category, RequestStatus status) {
        return new ViewState(message, service.filter(category, status), service.summary());
    }

    private void runInBackground(String busyMessage, BackgroundTask task, Runnable afterSuccess) {
        setBusy(true, busyMessage);
        Runnable work = () -> {
            try {
                ViewState state = task.run();
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, null);
                    showState(state);
                    if (afterSuccess != null) {
                        afterSuccess.run();
                    }
                });
            } catch (CampusConnectException | RuntimeException e) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, null);
                    handleFailure(e);
                });
            }
        };
        new Thread(work, "campusconnect-background").start();
    }

    private void handleFailure(Exception failure) {
        if (failure instanceof InvalidRequestException || failure instanceof InvalidStatusTransitionException
                || failure instanceof DuplicateTicketException) {
            showError("Validation", failure.getMessage());
        } else if (failure instanceof RequestNotFoundException) {
            setStatus("Not found: " + failure.getMessage(), true);
            JOptionPane.showMessageDialog(this, failure.getMessage(), "Not found", JOptionPane.INFORMATION_MESSAGE);
        } else if (failure instanceof DataAccessException) {
            failure.printStackTrace();
            showError("Database error", failure.getMessage()
                    + "\n\nCheck that MySQL is running and db.properties is correct, then press Refresh.");
        } else if (failure instanceof CampusConnectException) {
            showError("Error", failure.getMessage());
        } else {
            failure.printStackTrace();
            showError("Unexpected error", String.valueOf(failure));
        }
    }

    private void showState(ViewState state) {
        tableModel.setRows(state.rows());
        summaryArea.setText(formatSummary(state.summary()));
        setStatus(state.message(), false);
    }

    private static String formatSummary(RequestSummary s) {
        StringBuilder text = new StringBuilder("By category\n");
        for (ServiceCategory c : ServiceCategory.values()) {
            text.append(String.format("  %-11s %3d%n", c, s.countByCategory().get(c)));
        }
        text.append("\nBy status\n");
        for (RequestStatus st : RequestStatus.values()) {
            text.append(String.format("  %-11s %3d%n", st, s.countByStatus().get(st)));
        }
        text.append(String.format("%nAverage est. days %5.2f%n", s.averageEstimatedDays()));
        text.append(String.format("Total requests    %5d%n", s.totalRequests()));
        text.append(String.format("Waiting in queue  %5d%n", s.pendingInQueue()));
        return text.toString();
    }

    private void fillFormFrom(ServiceRequest r) {
        ticketField.setText(r.getTicketId());
        studentIdField.setText(r.getStudent().getStudentId());
        studentNameField.setText(r.getStudent().getStudentName());
        categoryBox.setSelectedItem(r.getCategory());
        prioritySpinner.setValue(r.getPriority());
        concernField.setText(r.getConcern());
        statusBox.setSelectedItem(r.getStatus());
    }

    private void clearForm() {
        ticketField.setText("");
        studentIdField.setText("");
        studentNameField.setText("");
        concernField.setText("");
        categoryBox.setSelectedIndex(0);
        prioritySpinner.setValue(3);
        statusBox.setSelectedIndex(0);
        categoryFilter.setSelectedIndex(0);
        statusFilter.setSelectedIndex(0);
        table.clearSelection();
        setStatus("Form cleared.", false);
    }

    private void setBusy(boolean busy, String message) {
        for (JButton b : actionButtons) {
            b.setEnabled(!busy);
        }
        progressBar.setIndeterminate(busy);
        progressBar.setVisible(busy);
        if (busy) {
            statusLabel.setForeground(Color.DARK_GRAY);
            statusLabel.setText(message);
        }
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setForeground(error ? ERROR_COLOR : OK_COLOR);
        statusLabel.setText(message);
    }

    private void showError(String title, String message) {
        int lineEnd = message.indexOf('\n');
        String firstLine = lineEnd >= 0 ? message.substring(0, lineEnd) : message;
        setStatus(title + ": " + firstLine, true);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private ServiceCategory selectedCategoryFilter() {
        String value = (String) categoryFilter.getSelectedItem();
        return value == null || ALL.equals(value) ? null : ServiceCategory.valueOf(value);
    }

    private RequestStatus selectedStatusFilter() {
        String value = (String) statusFilter.getSelectedItem();
        return value == null || ALL.equals(value) ? null : RequestStatus.valueOf(value);
    }

    private static String label(Object filterValue) {
        return filterValue == null ? ALL : filterValue.toString();
    }

    private static String[] withAll(Object[] values) {
        String[] names = new String[values.length + 1];
        names[0] = ALL;
        for (int i = 0; i < values.length; i++) {
            names[i + 1] = values[i].toString();
        }
        return names;
    }
}
