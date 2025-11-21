package com.libraryms.util;

public final class Session {
    private static boolean admin = false;
    private static String email;
    private static String name;
    private static String phone;

    private Session() {}

    public static boolean isAdmin() { return admin; }
    public static void setAdmin(boolean a) { admin = a; }

    public static String getEmail() { return email; }
    public static void setEmail(String e) { email = e; }

    public static String getName() { return name; }
    public static void setName(String n) { name = n; }

    public static String getPhone() { return phone; }
    public static void setPhone(String p) { phone = p; }

    public static boolean isLoggedIn() {
        return (admin && email != null) || (!admin && phone != null);
    }

    public static void logout() {
        admin = false;
        email = null;
        name = null;
        phone = null;
    }
}
