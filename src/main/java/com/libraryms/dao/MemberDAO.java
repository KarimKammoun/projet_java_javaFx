package com.libraryms.dao;

import com.libraryms.models.Member;
import com.libraryms.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public Member findByPhone(String phone) throws SQLException {
        String sql = "SELECT phone, name, email, cin, type, password, admin_id FROM users WHERE phone = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Member(
                            rs.getString("phone"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("cin"),
                            rs.getString("type"),
                            rs.getString("password"),
                            rs.getObject("admin_id") == null ? null : rs.getInt("admin_id")
                    );
                }
            }
        }
        return null;
    }

    public Member findByEmailAndCin(String email, String cin) throws SQLException {
        String sql = "SELECT phone, name, email, cin, type, password, admin_id FROM users WHERE email = ? AND cin = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, cin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Member(
                            rs.getString("phone"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("cin"),
                            rs.getString("type"),
                            rs.getString("password"),
                            rs.getObject("admin_id") == null ? null : rs.getInt("admin_id")
                    );
                }
            }
        }
        return null;
    }

    public boolean existsByPhone(String phone) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE phone = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean existsByEmailExceptPhone(String email, String excludePhone) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND phone <> ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, excludePhone == null ? "" : excludePhone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public void create(Member m) throws SQLException {
        String sql = "INSERT INTO users (phone, name, email, type, cin, password, admin_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getPhone());
            ps.setString(2, m.getName());
            ps.setString(3, m.getEmail());
            ps.setString(4, m.getType());
            ps.setString(5, m.getCin());
            ps.setString(6, m.getPasswordHash());
            if (m.getAdminId() != null) ps.setInt(7, m.getAdminId()); else ps.setNull(7, java.sql.Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public void update(Member m, String originalPhone) throws SQLException {
        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

            // If phone changed, ensure not used
            if (originalPhone != null && !originalPhone.equals(m.getPhone())) {
                try (PreparedStatement chk = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE phone = ?")) {
                    chk.setString(1, m.getPhone());
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            conn.rollback();
                            throw new SQLException("Phone already in use");
                        }
                    }
                }
            }

            // Update user
            StringBuilder sb = new StringBuilder("UPDATE users SET phone = ?, name = ?, email = ?, type = ?, cin = ?");
            if (m.getPasswordHash() != null && !m.getPasswordHash().isEmpty()) sb.append(", password = ?");
            sb.append(" WHERE phone = ?");

            try (PreparedStatement upd = conn.prepareStatement(sb.toString())) {
                int idx = 1;
                upd.setString(idx++, m.getPhone());
                upd.setString(idx++, m.getName());
                upd.setString(idx++, m.getEmail());
                upd.setString(idx++, m.getType());
                upd.setString(idx++, m.getCin());
                if (m.getPasswordHash() != null && !m.getPasswordHash().isEmpty()) {
                    upd.setString(idx++, m.getPasswordHash());
                }
                upd.setString(idx, originalPhone == null ? m.getPhone() : originalPhone);
                upd.executeUpdate();
            }

            // If phone changed, update borrowings
            if (originalPhone != null && !originalPhone.equals(m.getPhone())) {
                try (PreparedStatement updBorrow = conn.prepareStatement("UPDATE borrowing SET user_phone = ? WHERE user_phone = ?")) {
                    updBorrow.setString(1, m.getPhone());
                    updBorrow.setString(2, originalPhone);
                    updBorrow.executeUpdate();
                }
            }

            conn.commit();
        }
    }

    public void delete(String phone) throws SQLException {
        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM users WHERE phone = ?")) {
                del.setString(1, phone);
                del.executeUpdate();
            }
            conn.commit();
        }
    }

    public List<Member> listByAdmin(int adminId) throws SQLException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT phone, name, email, cin, type, password, admin_id FROM users WHERE admin_id = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Member(
                            rs.getString("phone"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("cin"),
                            rs.getString("type"),
                            rs.getString("password"),
                            rs.getObject("admin_id") == null ? null : rs.getInt("admin_id")
                    ));
                }
            }
        }
        return list;
    }
}
