package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class MembersController {

    @FXML private TableView<Member> membersTable;
    @FXML private TextField searchField;

    private ObservableList<Member> masterData;

    @FXML
    private void initialize() {
        var cols = membersTable.getColumns();
        ((TableColumn<Member, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("phone"));
        ((TableColumn<Member, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("name"));
        ((TableColumn<Member, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("email"));
        ((TableColumn<Member, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("type"));
        loadMembers();

        // set up filtered + sorted list for search
        FilteredList<Member> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            var lower = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(member -> {
                if (lower.isEmpty()) return true;
                if (member.getPhone() != null && member.getPhone().toLowerCase().contains(lower)) return true;
                if (member.getName() != null && member.getName().toLowerCase().contains(lower)) return true;
                if (member.getEmail() != null && member.getEmail().toLowerCase().contains(lower)) return true;
                if (member.getType() != null && member.getType().toLowerCase().contains(lower)) return true;
                return false;
            });
        });
        SortedList<Member> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(membersTable.comparatorProperty());
        membersTable.setItems(sorted);
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
        // keep master data reference for filtering
        masterData = list;
        membersTable.setItems(masterData);
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