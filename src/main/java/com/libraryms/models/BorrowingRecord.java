package com.libraryms.models;

import java.time.LocalDate;

public class BorrowingRecord {
    private final int id;
    private final String copyId;
    private final String bookTitle;
    private final String memberName;
    private final String userPhone;
    private final String isbn;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private final String status;

    public BorrowingRecord(int id, String copyId, String isbn, String bookTitle, String memberName, String userPhone, LocalDate borrowDate, LocalDate dueDate, String status) {
        this.id = id;
        this.copyId = copyId;
        this.isbn = isbn;
        this.bookTitle = bookTitle;
        this.memberName = memberName;
        this.userPhone = userPhone;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public int getId() { return id; }
    public String getCopyId() { return copyId; }
    public String getBookTitle() { return bookTitle; }
    public String getMemberName() { return memberName; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public String getUserPhone() { return userPhone; }
    public String getIsbn() { return isbn; }
}
