package com.libraryms.dao;

import com.libraryms.models.Admin;
import com.libraryms.util.DatabaseUtil;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {

    public Admin findByEmail(String email) throws SQLException {
        String sql = "SELECT id, email, password, name FROM admin WHERE email = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                            rs.getInt("id"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("name")
                    );
                }
            }
        }
        return null;
    }

    public boolean verifyPassword(Admin admin, String password) {
        if (admin == null || admin.getPassword() == null) return false;
        String stored = admin.getPassword();
        
        // support legacy plain-text seed and hashed passwords
        if (stored.equals(password)) {
            return true;
        }
        try {
            return BCrypt.verifyer().verify(password.toCharArray(), stored).verified;
        } catch (Exception ex) {
            return false;
        }
    }
}
