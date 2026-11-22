package com.legara.bookbridge.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.legara.bookbridge.model.BookStatus;
import com.legara.bookbridge.model.LibraryBook;
import com.legara.bookbridge.service.LibrarySnapshot;

public class CatalogPanel extends JPanel {
    private static final String ALL = "ALL";

    private final MainFrame frame;
    private final JTextField isbnField = new JTextField(15);
    private final JTextField titleField = new JTextField(25);
    private final JTextField keywordField = new JTextField(20);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] {ALL, BookStatus.AVAILABLE.name(), BookStatus.CHECKED_OUT.name()});
    private final DefaultTableModel model = Tables.readOnlyModel("ISBN", "Title", "Status");
    private final JTable table = Tables.singleSelectTable(model);

    public CatalogPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 6));

        JButton addButton = new JButton("Add Book");
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");
        addButton.addActionListener(e -> handleAddBook());
        searchButton.addActionListener(e -> handleSearch());
        refreshButton.addActionListener(e -> frame.reload(true));
        frame.registerAction(addButton);
        frame.registerAction(searchButton);
        frame.registerAction(refreshButton);

        FormPanel addForm = new FormPanel("Add a book");
        addForm.addField("ISBN:", isbnField);
        addForm.addField("Title:", titleField);
        addForm.addButtons(addButton);

        FormPanel searchForm = new FormPanel("Search the catalog");
        searchForm.addField("Title contains:", keywordField);
        searchForm.addField("Status:", statusFilter);
        searchForm.addButtons(searchButton, refreshButton);

        JPanel forms = new JPanel(new GridLayout(1, 2, 6, 0));
        forms.add(addForm);
        forms.add(searchForm);

        add(forms, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void show(LibrarySnapshot snapshot) {
        fill(snapshot.books());
    }

    private void fill(List<LibraryBook> books) {
        model.setRowCount(0);
        for (LibraryBook book : books) {
            model.addRow(new Object[] {book.getIsbn(), book.getTitle(), book.getStatus()});
        }
    }

    private void handleAddBook() {
        String isbn = isbnField.getText().trim();
        String title = titleField.getText().trim();
        if (isbn.isEmpty() || title.isEmpty()) {
            frame.showValidation("ISBN and title are required.");
            return;
        }
        frame.runAction("Saving book...", () -> {
            frame.service().addBook(new LibraryBook(isbn, title));
            return "Added '" + title + "' (ISBN " + isbn + ").";
        }, () -> {
            isbnField.setText("");
            titleField.setText("");
            isbnField.requestFocusInWindow();
        });
    }

    private void handleSearch() {
        String keyword = keywordField.getText();
        String status = (String) statusFilter.getSelectedItem();
        frame.runInBackground("Searching...", () -> {
            List<LibraryBook> matches = new ArrayList<>();
            for (LibraryBook book : frame.service().searchBooksByTitle(keyword)) {
                if (ALL.equals(status) || book.getStatus().name().equals(status)) {
                    matches.add(book);
                }
            }
            return matches;
        }, matches -> {
            fill(matches);
            frame.showStatus(matches.size() + " book(s) match. Press Refresh to show all.");
        });
    }
}
