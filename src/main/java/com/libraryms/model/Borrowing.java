package com.libraryms.model;

import java.time.LocalDate;

public class Borrowing {
    private final String copyId;
    private final String memberPhone;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private final String status;

    public Borrowing(String copyId, String memberPhone, LocalDate borrowDate, LocalDate dueDate, String status) {
        this.copyId = copyId;
        this.memberPhone = memberPhone;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public String getCopyId() { return copyId; }
    public String getMemberPhone() { return memberPhone; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
}