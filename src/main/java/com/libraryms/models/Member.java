package com.libraryms.models;

public class Member {
    private String phone;
    private String name;
    private String email;
    private String cin;
    private String type;
    private String passwordHash;
    private Integer adminId;

    public Member(String phone, String name, String email, String cin, String type, String passwordHash, Integer adminId) {
        this.phone = phone;
        this.name = name;
        this.email = email;
        this.cin = cin;
        this.type = type;
        this.passwordHash = passwordHash;
        this.adminId = adminId;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }
}
