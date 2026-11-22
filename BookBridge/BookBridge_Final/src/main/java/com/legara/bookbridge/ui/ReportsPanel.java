package com.legara.bookbridge.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.MembershipType;
import com.legara.bookbridge.service.CirculationSummary;
import com.legara.bookbridge.service.LibrarySnapshot;
import com.legara.bookbridge.service.LibraryService;

public class ReportsPanel extends JPanel {
    private final MainFrame frame;
    private final JTextArea reportArea = new JTextArea();

    public ReportsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 6));

        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JButton generateButton = new JButton("Generate Circulation Report");
        generateButton.addActionListener(e -> handleGenerate());
        frame.registerAction(generateButton);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(generateButton);

        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(reportArea), BorderLayout.CENTER);
    }

    public void show(LibrarySnapshot snapshot) {
        reportArea.setText(buildReport(snapshot.today(), snapshot.summary(), snapshot.activeLoans()));
        reportArea.setCaretPosition(0);
    }

    private void handleGenerate() {
        frame.runInBackground("Generating report...", () -> {
            LibraryService service = frame.service();
            return buildReport(service.today(), service.summary(), service.listActiveLoansSorted());
        }, report -> {
            reportArea.setText(report);
            reportArea.setCaretPosition(0);
            frame.showStatus("Report generated.");
        });
    }

    private static String buildReport(LocalDate today, CirculationSummary s, List<LoanRecord> loans) {
        StringBuilder text = new StringBuilder();
        text.append("BOOKBRIDGE CIRCULATION REPORT  (").append(today).append(")\n\n");
        text.append(String.format("Available books        : %d%n", s.availableBooks()));
        text.append(String.format("Checked-out books      : %d%n", s.checkedOutBooks()));
        for (MembershipType type : MembershipType.values()) {
            text.append(String.format("Active loans %-10s: %d%n", type, s.activeLoansByType().get(type)));
        }
        text.append(String.format("Overdue loans          : %d%n", s.overdueLoans()));
        text.append(String.format("Total estimated fines  : PHP %.2f%n", s.totalEstimatedFines()));
        text.append(String.format("Pending reservations   : %d%n", s.pendingReservations()));
        text.append(String.format("Loans ever made        : %d%n%n", s.totalLoansEver()));
        text.append("ACTIVE LOANS (overdue first, then due date, then loan ID)\n");
        if (loans.isEmpty()) {
            text.append("  none\n");
        }
        for (LoanRecord loan : loans) {
            text.append(String.format("  %s  %-15s %-22s due %s %s%n", loan.getLoanId(),
                    loan.getMember().getMemberName(), loan.getBook().getTitle(), loan.getDueDate(),
                    loan.isOverdue(today) ? "OVERDUE" : ""));
        }
        return text.toString();
    }
}
