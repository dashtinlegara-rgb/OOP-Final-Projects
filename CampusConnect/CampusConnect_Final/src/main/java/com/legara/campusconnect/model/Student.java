package com.legara.campusconnect.model;

public class Student {
    public static final int MAX_ID_LENGTH = 20;
    public static final int MAX_NAME_LENGTH = 100;

    private final String studentId;
    private final String studentName;

    public Student(String studentId, String studentName) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be blank.");
        }
        if (studentName == null || studentName.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be blank.");
        }
        if (studentId.trim().length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("Student ID must be at most " + MAX_ID_LENGTH + " characters.");
        }
        if (studentName.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Student name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        this.studentId = studentId.trim();
        this.studentName = studentName.trim();
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    @Override
    public String toString() {
        return studentName + " (" + studentId + ")";
    }
}
