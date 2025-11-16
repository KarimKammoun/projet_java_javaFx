package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class MembersController {

    @FXML private TableView<Member> membersTable;

    @FXML
    private void initialize() {
        var cols = membersTable.getColumns();
        ((TableColumn<Member, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("phone"));
        ((TableColumn<Member, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("name"));
        ((TableColumn<Member, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("email"));
        ((TableColumn<Member, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("type"));
        loadMembers();
    }

    private void loadMembers() {
        var list = FXCollections.<Member>observableArrayList();
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT phone, name, email, type FROM users")) {
            while (rs.next()) {
                list.add(new Member(
                        rs.getString("phone"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("type")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        membersTable.setItems(list);
    }

    @FXML
    private void openAddMember() {
        try {
            javafx.scene.Parent root = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/add_member.fxml")).load();
            var scene = new javafx.scene.Scene(root);
            var stage = new javafx.stage.Stage();
            stage.setTitle("Add Member");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadMembers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static class Member {
        private final String phone, name, email, type;
        public Member(String phone, String name, String email, String type) {
            this.phone = phone; this.name = name; this.email = email; this.type = type;
        }
        public String getPhone() { return phone; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getType() { return type; }
    }
}