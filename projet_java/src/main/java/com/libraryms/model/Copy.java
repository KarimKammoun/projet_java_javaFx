package com.libraryms.model;

public class Copy {
    private final String copyId;
    private final String isbn;
    private final String status;
    private final String location;

    public Copy(String copyId, String isbn, String status, String location) {
        this.copyId = copyId;
        this.isbn = isbn;
        this.status = status;
        this.location = location;
    }

    public String getCopyId() { return copyId; }
    public String getIsbn() { return isbn; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
}