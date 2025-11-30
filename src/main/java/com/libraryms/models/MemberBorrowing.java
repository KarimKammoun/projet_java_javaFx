package com.libraryms.models;

import java.time.LocalDate;

public class MemberBorrowing {
    private final String title;
    private final String copyId;
    private final String status;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private final int daysRemaining;
    private final String warning;

    public MemberBorrowing(String title, String copyId, String status, LocalDate borrowDate, LocalDate dueDate, int daysRemaining, String warning) {
        this.title = title;
        this.copyId = copyId;
        this.status = status;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.daysRemaining = daysRemaining;
        this.warning = warning;
    }

    public String getTitle() { return title; }
    public String getCopyId() { return copyId; }
    public String getStatus() { return status; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public int getDaysRemaining() { return daysRemaining; }
    public String getWarning() { return warning; }
}