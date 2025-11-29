package com.libraryms.controller.user;

import com.libraryms.util.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;

public class MembersController {

    @FXML private TableView<Member> membersTable;
    @FXML private TextField searchField;
    @FXML private javafx.scene.control.ChoiceBox<String> searchColumnChoice;

    private ObservableList<Member> masterData;

    @FXML
    private void initialize() {
        var cols = membersTable.getColumns();
        ((TableColumn<Member, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("phone"));
        ((TableColumn<Member, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("cin"));
        ((TableColumn<Member, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("name"));
        ((TableColumn<Member, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("email"));
        ((TableColumn<Member, String>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("type"));
        loadMembers();

        // prepare search column choice
        if (searchColumnChoice != null) {
            searchColumnChoice.getSelectionModel().select("All");
        }

        // set up filtered + sorted list for search
        FilteredList<Member> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            var lower = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(member -> {
                if (lower.isEmpty()) return true;
                String col = (searchColumnChoice != null && searchColumnChoice.getValue() != null) ? searchColumnChoice.getValue() : "All";
                switch (col) {
                    case "Phone":
                        return member.getPhone() != null && member.getPhone().toLowerCase().contains(lower);
                    case "CIN":
                        return member.getCin() != null && member.getCin().toLowerCase().contains(lower);
                    case "Name":
                        return member.getName() != null && member.getName().toLowerCase().contains(lower);
                    case "Email":
                        return member.getEmail() != null && member.getEmail().toLowerCase().contains(lower);
                    case "Type":
                        return member.getType() != null && member.getType().toLowerCase().contains(lower);
                    default:
                        if (member.getPhone() != null && member.getPhone().toLowerCase().contains(lower)) return true;
                        if (member.getName() != null && member.getName().toLowerCase().contains(lower)) return true;
                        if (member.getEmail() != null && member.getEmail().toLowerCase().contains(lower)) return true;
                        if (member.getType() != null && member.getType().toLowerCase().contains(lower)) return true;
                        if (member.getCin() != null && member.getCin().toLowerCase().contains(lower)) return true;
                        return false;
                }
            });
        });
        if (searchColumnChoice != null) {
            searchColumnChoice.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
                // re-trigger search to apply the new column filter
                if (searchField != null) searchField.setText(searchField.getText());
            });
        }
        SortedList<Member> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(membersTable.comparatorProperty());
        membersTable.setItems(sorted);
    }

    private void loadMembers() {
        var list = FXCollections.<Member>observableArrayList();
        try (var conn = DatabaseUtil.connect()) {
            Integer adminId = com.libraryms.util.Session.getAdminId();
            if (adminId == null) {
                System.err.println("No admin logged in");
                return;
            }
            var ps = conn.prepareStatement("SELECT phone, name, email, type, cin FROM users WHERE admin_id = ?");
            ps.setInt(1, adminId);
            var rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Member(
                        rs.getString("phone"),
                        rs.getString("cin"),
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
            javafx.scene.Parent root = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_member.fxml")).load();
            var scene = new javafx.scene.Scene(root);
            com.libraryms.util.SceneManager.applyGlobalStyles(scene);
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

    @FXML private Button editMemberBtn;
    @FXML private Button deleteMemberBtn;

    @FXML
    private void openEditMember() {
        // Open the add/edit member dialog, but first ask for Email + CIN in a small popup
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_member.fxml"));
            javafx.scene.Parent editRoot = loader.load();
            var editController = loader.getController();

            // Build small prompt stage asking for email and CIN
            var grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(15));
            var emailLabel = new javafx.scene.control.Label("Email:");
            var emailField = new javafx.scene.control.TextField();
            var cinLabel = new javafx.scene.control.Label("CIN:");
            var cinField = new javafx.scene.control.TextField();
            var loadBtn = new javafx.scene.control.Button("Load");
            var cancelBtn = new javafx.scene.control.Button("Cancel");

            grid.add(emailLabel, 0, 0);
            grid.add(emailField, 1, 0);
            grid.add(cinLabel, 0, 1);
            grid.add(cinField, 1, 1);
            var buttons = new javafx.scene.layout.HBox(10, loadBtn, cancelBtn);
            grid.add(buttons, 1, 2);

            var promptScene = new javafx.scene.Scene(grid);
            com.libraryms.util.SceneManager.applyGlobalStyles(promptScene);
            var promptStage = new javafx.stage.Stage();
            promptStage.setTitle("Load Member to Edit");
            promptStage.setScene(promptScene);
            promptStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            loadBtn.setOnAction(evt -> {
                String email = emailField.getText().trim();
                String cin = cinField.getText().trim();
                if (email.isEmpty() || cin.isEmpty()) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez entrer email et CIN").showAndWait();
                    return;
                }
                boolean loaded = false;
                if (editController instanceof com.libraryms.controller.pop_up.AddMemberController) {
                    loaded = ((com.libraryms.controller.pop_up.AddMemberController) editController).loadForEditByEmailAndCin(email, cin);
                }
                if (loaded) {
                    promptStage.close();
                    // show edit dialog
                    var scene = new javafx.scene.Scene(editRoot);
                    com.libraryms.util.SceneManager.applyGlobalStyles(scene);
                    var stage = new javafx.stage.Stage();
                    stage.setTitle("Edit Member - " + email);
                    stage.setScene(scene);
                    stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                    stage.showAndWait();
                    loadMembers();
                } else {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Membre non trouvé pour cet email et CIN").showAndWait();
                }
            });

            cancelBtn.setOnAction(evt -> promptStage.close());

            promptStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void deleteSelectedMember() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/delete_member.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            com.libraryms.util.SceneManager.applyGlobalStyles(scene);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Supprimer un membre");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadMembers();
        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }
    
    public static class Member {
        private final String phone, cin, name, email, type;
        public Member(String phone, String cin, String name, String email, String type) {
            this.phone = phone; this.cin = cin; this.name = name; this.email = email; this.type = type;
        }
        public String getPhone() { return phone; }
        public String getCin() { return cin; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getType() { return type; }
    }
}