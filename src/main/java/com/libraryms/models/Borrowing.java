package com.libraryms.models;

import java.time.LocalDate;

public class Borrowing {
    private final String copyId;
    private final String memberPhone;
    private final String memberName;
    private final String bookTitle;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private final String status;

    // Backwards-compatible constructor (no book title)
    public Borrowing(String copyId, String memberPhone, LocalDate borrowDate, LocalDate dueDate, String status) {
        this(copyId, memberPhone, null, null, borrowDate, dueDate, status);
    }

    public Borrowing(String copyId, String memberPhone, String memberName, String bookTitle, LocalDate borrowDate, LocalDate dueDate, String status) {
        this.copyId = copyId;
        this.memberPhone = memberPhone;
        this.memberName = memberName;
        this.bookTitle = bookTitle;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public String getCopyId() { return copyId; }
    public String getMemberPhone() { return memberPhone; }
    public String getBookTitle() { return bookTitle; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public String getMemberName() { return memberName; }
}