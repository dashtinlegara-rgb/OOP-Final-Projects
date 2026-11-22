package com.legara.bookbridge.ui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.Member;
import com.legara.bookbridge.model.MembershipType;
import com.legara.bookbridge.service.LibrarySnapshot;

public class MembersPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField idField = new JTextField(15);
    private final JTextField nameField = new JTextField(25);
    private final JComboBox<MembershipType> typeBox = new JComboBox<>(MembershipType.values());
    private final DefaultTableModel model = Tables.readOnlyModel(
            "Member ID", "Name", "Type", "Active loans", "Loan limit", "Max days", "Fine/day (PHP)");
    private final JTable table = Tables.singleSelectTable(model);

    public MembersPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 6));

        JButton registerButton = new JButton("Register Member");
        JButton refreshButton = new JButton("Refresh");
        registerButton.addActionListener(e -> handleRegister());
        refreshButton.addActionListener(e -> frame.reload(true));
        frame.registerAction(registerButton);
        frame.registerAction(refreshButton);

        FormPanel form = new FormPanel("Register a member");
        form.addField("Member ID:", idField);
        form.addField("Name:", nameField);
        form.addField("Type:", typeBox);
        form.addButtons(registerButton, refreshButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void show(LibrarySnapshot snapshot) {
        model.setRowCount(0);
        for (Member m : snapshot.members()) {
            int active = 0;
            for (LoanRecord loan : snapshot.activeLoans()) {
                if (loan.getMember().getMemberId().equals(m.getMemberId())) {
                    active++;
                }
            }
            model.addRow(new Object[] {m.getMemberId(), m.getMemberName(), m.getMembershipType(), active,
                    m.getLoanLimit(), m.getMaximumLoanDays(), String.format("%.2f", m.getDailyFine())});
        }
    }

    private void handleRegister() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        MembershipType type = (MembershipType) typeBox.getSelectedItem();
        if (id.isEmpty() || name.isEmpty()) {
            frame.showValidation("Member ID and name are required.");
            return;
        }
        frame.runAction("Saving member...", () -> {
            frame.service().registerMember(Member.create(type, id, name));
            return "Registered " + name + " (" + id + ") as " + type + ".";
        }, () -> {
            idField.setText("");
            nameField.setText("");
            idField.requestFocusInWindow();
        });
    }
}
