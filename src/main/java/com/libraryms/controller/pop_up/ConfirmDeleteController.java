package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ConfirmDeleteController {

    @FXML
    private Label messageLabel;

    private String entityType; // "user", "book", "borrowing"
    private String identifier; // phone for user, isbn for book, id for borrowing

    public void setContext(String entityType, String identifier, String message) {
        this.entityType = entityType;
        this.identifier = identifier;
        messageLabel.setText(message);
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    @FXML
    private void onConfirm() {
        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            if ("user".equals(entityType)) {
                // delete user by phone
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE phone = ?")) {
                    ps.setString(1, identifier);
                    ps.executeUpdate();
                }
            } else if ("book".equals(entityType)) {
                // delete book by isbn, also delete copies and borrowings via foreign keys or explicit deletes
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE isbn = ?")) {
                    ps.setString(1, identifier);
                    ps.executeUpdate();
                }
            } else if ("borrowing".equals(entityType)) {
                // borrowing id -> mark copy available and delete borrowing
                // get copy_id for borrowing
                String copyId = null;
                try (PreparedStatement ps = conn.prepareStatement("SELECT copy_id FROM borrowing WHERE id = ?")) {
                    ps.setInt(1, Integer.parseInt(identifier));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) copyId = rs.getString("copy_id");
                    }
                }

                if (copyId != null) {
                    // delete borrowing
                    try (PreparedStatement del = conn.prepareStatement("DELETE FROM borrowing WHERE id = ?")) {
                        del.setInt(1, Integer.parseInt(identifier));
                        del.executeUpdate();
                    }
                    // mark copy available
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE copies SET status = 'Available' WHERE copy_id = ?")) {
                        ps.setString(1, copyId);
                        ps.executeUpdate();
                    }
                    // increment book available_copies
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE isbn = (SELECT isbn FROM copies WHERE copy_id = ?)") ) {
                        ps.setString(1, copyId);
                        ps.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        closeWindow();
    }

    private void closeWindow() {
        Stage s = (Stage) messageLabel.getScene().getWindow();
        s.close();
    }
}
