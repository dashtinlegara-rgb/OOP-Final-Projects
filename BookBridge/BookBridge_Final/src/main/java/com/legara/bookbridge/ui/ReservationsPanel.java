package com.legara.bookbridge.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.Reservation;
import com.legara.bookbridge.service.LibrarySnapshot;

public class ReservationsPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField memberField = new JTextField(15);
    private final JTextField isbnField = new JTextField(15);
    private final DefaultTableModel model = Tables.readOnlyModel(
            "Reservation ID", "Queue position", "Member", "Book", "Reserved on");
    private final JTable table = Tables.singleSelectTable(model);

    public ReservationsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 6));

        JButton reserveButton = new JButton("Reserve");
        JButton cancelButton = new JButton("Cancel Selected");
        JButton processButton = new JButton("Process Next");
        JButton refreshButton = new JButton("Refresh");
        reserveButton.addActionListener(e -> handleReserve());
        cancelButton.addActionListener(e -> handleCancel());
        processButton.addActionListener(e -> handleProcessNext());
        refreshButton.addActionListener(e -> frame.reload(true));
        frame.registerAction(reserveButton);
        frame.registerAction(cancelButton);
        frame.registerAction(processButton);
        frame.registerAction(refreshButton);

        FormPanel form = new FormPanel("Reserve a checked-out book");
        form.addField("Member ID:", memberField);
        form.addField("ISBN:", isbnField);
        form.addButtons(reserveButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(cancelButton);
        actions.add(processButton);
        actions.add(refreshButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    public void show(LibrarySnapshot snapshot) {
        model.setRowCount(0);
        int position = 1;
        for (Reservation r : snapshot.pendingReservations()) {
            model.addRow(new Object[] {r.getReservationId(), position, r.getMember().getMemberName(),
                    r.getBook().getTitle() + " (" + r.getBook().getIsbn() + ")", r.getReservedOn()});
            position++;
        }
    }

    private void handleReserve() {
        String memberId = memberField.getText().trim();
        String isbn = isbnField.getText().trim();
        if (memberId.isEmpty() || isbn.isEmpty()) {
            frame.showValidation("Member ID and ISBN are required.");
            return;
        }
        frame.runAction("Saving reservation...", () -> {
            Reservation r = frame.service().reserveBook(memberId, isbn);
            return "Reserved: " + r.getReservationId() + " " + r.getMember().getMemberName()
                    + " is waiting for '" + r.getBook().getTitle() + "'.";
        });
    }

    private void handleCancel() {
        String reservationId = Tables.selectedId(table);
        if (reservationId == null) {
            frame.showValidation("Select a reservation in the table first.");
            return;
        }
        if (!frame.confirm("Cancel reservation " + reservationId + "? The member loses their place in line.")) {
            return;
        }
        frame.runAction("Cancelling reservation...", () -> {
            frame.service().cancelReservation(reservationId);
            return "Cancelled reservation " + reservationId + ".";
        });
    }

    private void handleProcessNext() {
        frame.runAction("Processing the queue...", () -> {
            LoanRecord loan = frame.service().processNextReservation();
            return "Reservation served: " + loan.getLoanId() + " '" + loan.getBook().getTitle() + "' issued to "
                    + loan.getMember().getMemberName() + ", due " + loan.getDueDate() + ".";
        });
    }
}
