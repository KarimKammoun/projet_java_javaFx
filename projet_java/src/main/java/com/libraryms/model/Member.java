package com.libraryms.model;

import java.time.LocalDate;

public class Member {
    private final String phone;
    private final String name;
    private final LocalDate birthdate;
    private final String type;

    public Member(String phone, String name, LocalDate birthdate, String type) {
        this.phone = phone;
        this.name = name;
        this.birthdate = birthdate;
        this.type = type;
    }

    public String getPhone() { return phone; }
    public String getName() { return name; }
    public LocalDate getBirthdate() { return birthdate; }
    public String getType() { return type; }
}