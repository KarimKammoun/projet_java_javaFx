package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MemberBorrowingsController {

    @FXML private Label activeCount, lateCount, completedCount;
    @FXML private VBox borrowingCards;

    private String currentUserPhone; // rempli depuis Session

    @FXML
    private void initialize() {
        loadStats();
        loadBorrowings();
    }

    private void loadStats() {
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement(
                     "SELECT " +
                             "COUNT(CASE WHEN status IN ('In Progress', 'Late') THEN 1 END) AS active, " +
                             "COUNT(CASE WHEN status = 'Late' THEN 1 END) AS late, " +
                             "COUNT(CASE WHEN status = 'Returned' THEN 1 END) AS completed " +
                             "FROM borrowing WHERE user_phone = ?")) {
            if (currentUserPhone == null) currentUserPhone = com.libraryms.util.Session.getPhone();
            if (currentUserPhone == null) {
                activeCount.setText("0");
                lateCount.setText("0");
                completedCount.setText("0");
                return;
            }
            stmt.setString(1, currentUserPhone);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                activeCount.setText(String.valueOf(rs.getInt("active")));
                lateCount.setText(String.valueOf(rs.getInt("late")));
                completedCount.setText(String.valueOf(rs.getInt("completed")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            activeCount.setText("0");
            lateCount.setText("0");
            completedCount.setText("0");
        }
    }

    private void loadBorrowings() {
        borrowingCards.getChildren().clear();
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement(
                     "SELECT b.copy_id, k.title, b.borrow_date, b.due_date, b.status " +
                             "FROM borrowing b " +
                             "JOIN copies c ON b.copy_id = c.copy_id " +
                             "JOIN books k ON c.isbn = k.isbn " +
                             "WHERE b.user_phone = ?")) {
            if (currentUserPhone == null) currentUserPhone = com.libraryms.util.Session.getPhone();
            if (currentUserPhone == null) return;
            stmt.setString(1, currentUserPhone);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                borrowingCards.getChildren().add(createBorrowingCard(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createBorrowingCard(ResultSet rs) throws Exception {
        String title = rs.getString("title");
        String copyId = rs.getString("copy_id");
        String borrowDateStr = rs.getString("borrow_date");
        String dueDateStr = rs.getString("due_date");
        LocalDate borrowDate = LocalDate.parse(borrowDateStr);
        LocalDate dueDate = LocalDate.parse(dueDateStr);
        String status = rs.getString("status");

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        String daysText = daysRemaining >= 0 ? daysRemaining + " days" : Math.abs(daysRemaining) + " days overdue";

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");

        Label copyLabel = new Label("Copy: " + copyId);
        copyLabel.setStyle("-fx-text-fill: #6c757d;");

        Label statusLabel = new Label(status);
        statusLabel.setStyle(status.equals("Late") ?
                "-fx-background-color: #f8d7da; -fx-padding: 4 8; -fx-background-radius: 20; -fx-text-fill: #721c24;" :
                "-fx-background-color: #d1ecf1; -fx-padding: 4 8; -fx-background-radius: 20; -fx-text-fill: #0c5460;");

        HBox datesBox = new HBox(20);
        datesBox.getChildren().addAll(
                new Label("Borrow Date:"), new Label(borrowDate.toString()),
                new Label("Due Date:"), new Label(dueDate.toString())
        );

        Label daysLabel = new Label("Days Remaining: " + daysText);
        daysLabel.setStyle(daysRemaining < 0 ? "-fx-text-fill: #dc3545;" : "-fx-text-fill: #ffc107;");

        if (daysRemaining < 0) {
            Label warning = new Label("Overdue! Please return this book as soon as possible.");
            warning.setStyle("-fx-background-color: #f8d7da; -fx-padding: 10; -fx-background-radius: 8; -fx-text-fill: #721c24;");
            card.getChildren().add(warning);
        }

        card.getChildren().addAll(titleLabel, copyLabel, statusLabel, new Separator(), datesBox, daysLabel);
        return card;
    }

    @FXML private void goToBrowse() { SceneManager.loadScene("/fxml/member_dashboard.fxml"); }
    @FXML private void goToBorrowings() { /* already here */ }
}