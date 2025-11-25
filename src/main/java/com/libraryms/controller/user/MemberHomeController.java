package com.libraryms.controller.user;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.sql.ResultSet;

public class MemberHomeController {

    @FXML private Label welcomeLabel, memberTypeLabel, borrowedCountLabel, limitLabel, availableSlotsLabel;
    @FXML private Label totalBooksLabel, totalCopiesLabel, availableCopiesLabel, borrowedCopiesLabel;
    @FXML private FlowPane booksGridBox;

    @FXML
    private void initialize() {
        loadMemberStats();
        loadLibraryStats();
        loadFeaturedBooks();
    }

    private void loadMemberStats() {
        String phone = Session.getPhone();
        if (phone == null) return;

        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement("SELECT name, type, borrow_limit FROM users WHERE phone = ?")) {
            stmt.setString(1, phone);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                int limit = rs.getInt("borrow_limit");

                welcomeLabel.setText("Welcome back, " + name.split(" ")[0] + "!");
                memberTypeLabel.setText(type);
                limitLabel.setText(String.valueOf(limit));

                // count active borrowings (scoped to member's admin/library if available)
                Integer adminId = Session.getAdminId();
                if (adminId == null) {
                    try (var stmt2 = conn.prepareStatement(
                            "SELECT COUNT(*) FROM borrowing WHERE user_phone = ? AND status IN ('In Progress', 'Late')")) {
                        stmt2.setString(1, phone);
                        var rs2 = stmt2.executeQuery();
                        if (rs2.next()) {
                            int borrowed = rs2.getInt(1);
                            borrowedCountLabel.setText(String.valueOf(borrowed));
                            availableSlotsLabel.setText(String.valueOf(Math.max(0, limit - borrowed)));
                        }
                    }
                } else {
                    try (var stmt2 = conn.prepareStatement(
                            "SELECT COUNT(*) FROM borrowing WHERE user_phone = ? AND admin_id = ? AND status IN ('In Progress', 'Late')")) {
                        stmt2.setString(1, phone);
                        stmt2.setInt(2, adminId);
                        var rs2 = stmt2.executeQuery();
                        if (rs2.next()) {
                            int borrowed = rs2.getInt(1);
                            borrowedCountLabel.setText(String.valueOf(borrowed));
                            availableSlotsLabel.setText(String.valueOf(Math.max(0, limit - borrowed)));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLibraryStats() {
        Integer adminId = Session.getAdminId();
        if (adminId == null) {
            // No admin context for this member — show zeros
            totalBooksLabel.setText("0");
            totalCopiesLabel.setText("0");
            availableCopiesLabel.setText("0");
            borrowedCopiesLabel.setText("0");
            return;
        }

        try (var conn = DatabaseUtil.connect()) {
            // Total books and copies for this admin/library
            try (var pst = conn.prepareStatement("SELECT COUNT(DISTINCT isbn) as total_books, COUNT(*) as total_copies FROM copies WHERE admin_id = ?")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) {
                        totalBooksLabel.setText(String.valueOf(rs.getInt("total_books")));
                        totalCopiesLabel.setText(String.valueOf(rs.getInt("total_copies")));
                    }
                }
            }

            // Available vs borrowed for this admin/library
            try (var pst = conn.prepareStatement(
                    "SELECT COUNT(CASE WHEN status='Available' THEN 1 END) as avail, " +
                    "COUNT(CASE WHEN status='Borrowed' THEN 1 END) as borrowed FROM copies WHERE admin_id = ?")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) {
                        availableCopiesLabel.setText(String.valueOf(rs.getInt("avail")));
                        borrowedCopiesLabel.setText(String.valueOf(rs.getInt("borrowed")));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFeaturedBooks() {
        booksGridBox.getChildren().clear();
        Integer adminId = Session.getAdminId();
        if (adminId == null) return;

        try (var conn = DatabaseUtil.connect();
             var pst = conn.prepareStatement("SELECT isbn, title, author, available_copies FROM books WHERE available_copies > 0 AND admin_id = ? ORDER BY title")) {
            pst.setInt(1, adminId);
            try (var rs = pst.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String author = rs.getString("author");
                    int available = rs.getInt("available_copies");

                    VBox card = createBookCard(title, author, available);
                    booksGridBox.getChildren().add(card);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createBookCard(String title, String author, int available) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-padding: 15; " +
            "-fx-background-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2); " +
            "-fx-min-width: 200;"
        );
        card.setPrefWidth(200);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #212529; -fx-wrap-text: true;");
        titleLabel.setWrapText(true);

        Label authorLabel = new Label("by " + author);
        authorLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11;");

        Label availLabel = new Label("Available: " + available);
        availLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745; -fx-font-size: 12;");

        card.getChildren().addAll(titleLabel, authorLabel, availLabel);
        return card;
    }

    @FXML
    private void goToBorrowingHistory() {
        SceneManager.loadScene("/fxml/user/member_borrowing_history.fxml");
    }

    @FXML private void goToBrowseBooks() {
        SceneManager.loadScene("/fxml/user/member_books.fxml");
    }
}
