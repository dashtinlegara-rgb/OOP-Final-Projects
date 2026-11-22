package com.legara.bookbridge.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FormPanel extends JPanel {
    private final GridBagConstraints c = new GridBagConstraints();
    private int row = 0;

    public FormPanel(String title) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(title));
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
    }

    public void addField(String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setLabelFor(field);
        c.gridx = 0;
        c.gridy = row;
        add(label, c);
        c.gridx = 1;
        add(field, c);
        row++;
    }

    public void addButtons(JComponent... buttons) {
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (JComponent button : buttons) {
            buttonRow.add(button);
        }
        c.gridx = 1;
        c.gridy = row;
        add(buttonRow, c);
        row++;
    }
}
