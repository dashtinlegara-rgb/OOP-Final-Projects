package com.legara.campusconnect.ui;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.legara.campusconnect.model.ServiceRequest;

public class RequestTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
        "Ticket", "Student ID", "Student Name", "Category", "Priority", "Est. Days", "Status", "Created", "Concern"
    };
    private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private List<ServiceRequest> rows = List.of();

    public void setRows(List<ServiceRequest> newRows) {
        rows = List.copyOf(newRows);
        fireTableDataChanged();
    }

    public ServiceRequest getRow(int index) {
        return rows.get(index);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == 4 || column == 5 ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ServiceRequest r = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> r.getTicketId();
            case 1 -> r.getStudent().getStudentId();
            case 2 -> r.getStudent().getStudentName();
            case 3 -> r.getCategory().name();
            case 4 -> r.getPriority();
            case 5 -> r.estimateDays();
            case 6 -> r.getStatus().name();
            case 7 -> r.getCreatedAt().format(CREATED_FORMAT);
            case 8 -> r.getConcern();
            default -> "";
        };
    }
}
