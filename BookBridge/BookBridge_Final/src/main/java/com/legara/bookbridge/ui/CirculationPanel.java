package com.legara.bookbridge.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.service.LibrarySnapshot;

public class CirculationPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField memberField = new JTextField(15);
    private final JTextField isbnField = new JTextField(15);
    private final JTextField daysField = new JTextField("7", 5);
    private final JTextField renewDaysField = new JTextField("2", 5);
    private final DefaultTableModel model = Tables.readOnlyModel(
            "Loan ID", "Member", "Type", "Book", "Loan date", "Due date", "Overdue", "Fine so far (PHP)");
    private final JTable table = Tables.singleSelectTable(model);

    public CirculationPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 6));

        JButton issueButton = new JButton("Issue Loan");
        JButton returnButton = new JButton("Return Selected");
        JButton renewButton = new JButton("Renew Selected");
        JButton refreshButton = new JButton("Refresh");
        issueButton.addActionListener(e -> handleIssue());
        returnButton.addActionListener(e -> handleReturn());
        renewButton.addActionListener(e -> handleRenew());
        refreshButton.addActionListener(e -> frame.reload(true));
        frame.registerAction(issueButton);
        frame.registerAction(returnButton);
        frame.registerAction(renewButton);
        frame.registerAction(refreshButton);

        FormPanel form = new FormPanel("Issue a loan");
        form.addField("Member ID:", memberField);
        form.addField("ISBN:", isbnField);
        form.addField("Loan days:", daysField);
        form.addButtons(issueButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(returnButton);
        actions.add(new JLabel("Renew by days:"));
        actions.add(renewDaysField);
        actions.add(renewButton);
        actions.add(refreshButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    public void show(LibrarySnapshot snapshot) {
        model.setRowCount(0);
        for (LoanRecord loan : snapshot.activeLoans()) {
            model.addRow(new Object[] {loan.getLoanId(), loan.getMember().getMemberName(),
                    loan.getMember().getMembershipType(), loan.getBook().getTitle(), loan.getLoanDate(),
                    loan.getDueDate(), loan.isOverdue(snapshot.today()) ? "YES" : "no",
                    String.format("%.2f", loan.estimatedFine(snapshot.today()))});
        }
    }

    private void handleIssue() {
        String memberId = memberField.getText().trim();
        String isbn = isbnField.getText().trim();
        Integer days = parseDays(daysField, "Loan days");
        if (memberId.isEmpty() || isbn.isEmpty()) {
            frame.showValidation("Member ID and ISBN are required.");
            return;
        }
        if (days == null) {
            return;
        }
        frame.runAction("Issuing loan...", () -> {
            LoanRecord loan = frame.service().issueLoan(memberId, isbn, days);
            return "Issued " + loan.getLoanId() + ": '" + loan.getBook().getTitle() + "' to "
                    + loan.getMember().getMemberName() + ", due " + loan.getDueDate() + ".";
        });
    }

    private void handleReturn() {
        String loanId = Tables.selectedId(table);
        if (loanId == null) {
            frame.showValidation("Select a loan in the table first.");
            return;
        }
        if (!frame.confirm("Return loan " + loanId + "? This cannot be undone.")) {
            return;
        }
        frame.runAction("Returning book...", () -> {
            LoanRecord loan = frame.service().returnBook(loanId);
            return String.format("Returned '%s' (%s): %d day(s) late, fine PHP %.2f.", loan.getBook().getTitle(),
                    loanId, loan.daysLate(frame.service().today()), loan.estimatedFine(frame.service().today()));
        });
    }

    private void handleRenew() {
        String loanId = Tables.selectedId(table);
        if (loanId == null) {
            frame.showValidation("Select a loan in the table first.");
            return;
        }
        Integer days = parseDays(renewDaysField, "Renew days");
        if (days == null) {
            return;
        }
        frame.runAction("Renewing loan...", () -> {
            LoanRecord loan = frame.service().renewLoan(loanId, days);
            return "Renewed " + loanId + ": now due " + loan.getDueDate() + ".";
        });
    }

    private Integer parseDays(JTextField field, String label) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value < 1 || value > 30) {
                frame.showValidation(label + " must be from 1 to 30.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            frame.showValidation(label + " must be a whole number.");
            return null;
        }
    }
}
