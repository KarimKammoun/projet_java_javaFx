package com.libraryms.controller.user;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MemberDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label booksBorrowedValue;
    @FXML private Label limitValue;
    @FXML private Label availableValue;

    @FXML private void initialize() {
        // set welcome
        String name = Session.getName();
        if (name == null || name.isEmpty()) name = "Member";
        welcomeLabel.setText("Bienvenue, " + name + "!");

        Integer adminId = Session.getAdminId();
        String phone = Session.getPhone();

        // default limit - could be made configurable per member
        limitValue.setText("3");

        if (adminId == null) {
            booksBorrowedValue.setText("0");
            availableValue.setText("0");
            return;
        }

        try (var conn = DatabaseUtil.connect()) {
            // books borrowed by this member (in this admin/library)
            try (var pst = conn.prepareStatement("SELECT COUNT(*) FROM borrowing WHERE user_phone = ? AND admin_id = ?")) {
                pst.setString(1, phone);
                pst.setInt(2, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) booksBorrowedValue.setText(String.valueOf(rs.getInt(1)));
                }
            }

            // available copies in this library
            try (var pst = conn.prepareStatement("SELECT COUNT(*) FROM copies WHERE admin_id = ? AND status = 'Available'")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) availableValue.setText(String.valueOf(rs.getInt(1)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void goToBrowse() { SceneManager.loadScene("/fxml/user/member_dashboard.fxml"); }
    @FXML private void goToBorrowings() { SceneManager.loadScene("/fxml/user/member_borrowings.fxml"); }
}