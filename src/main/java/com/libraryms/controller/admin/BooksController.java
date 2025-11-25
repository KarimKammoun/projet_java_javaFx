package com.libraryms.controller.admin;

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
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;

public class BooksController {

    @FXML private TableView<Book> booksTable;
    @FXML private TextField searchField;
    @FXML private javafx.scene.control.ChoiceBox<String> searchColumnChoice;

    private ObservableList<Book> masterData;

    @FXML
    private void initialize() {
        var cols = booksTable.getColumns();
        ((TableColumn<Book, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("isbn"));
        ((TableColumn<Book, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("title"));
        ((TableColumn<Book, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("author"));
        ((TableColumn<Book, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("category"));
        ((TableColumn<Book, Integer>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        ((TableColumn<Book, Integer>) cols.get(5)).setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        loadBooks();

        if (searchColumnChoice != null) searchColumnChoice.getSelectionModel().select("All");

        FilteredList<Book> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            var lower = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(book -> {
                if (lower.isEmpty()) return true;
                String col = (searchColumnChoice != null && searchColumnChoice.getValue() != null) ? searchColumnChoice.getValue() : "All";
                switch (col) {
                    case "ISBN":
                        return book.getIsbn() != null && book.getIsbn().toLowerCase().contains(lower);
                    case "Title":
                        return book.getTitle() != null && book.getTitle().toLowerCase().contains(lower);
                    case "Author":
                        return book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lower);
                    case "Category":
                        return book.getCategory() != null && book.getCategory().toLowerCase().contains(lower);
                    case "Copies":
                        return String.valueOf(book.getTotalCopies()).contains(lower);
                    case "Available":
                        return String.valueOf(book.getAvailableCopies()).contains(lower);
                    default:
                        if (book.getIsbn() != null && book.getIsbn().toLowerCase().contains(lower)) return true;
                        if (book.getTitle() != null && book.getTitle().toLowerCase().contains(lower)) return true;
                        if (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lower)) return true;
                        if (book.getCategory() != null && book.getCategory().toLowerCase().contains(lower)) return true;
                        if (String.valueOf(book.getTotalCopies()).contains(lower)) return true;
                        if (String.valueOf(book.getAvailableCopies()).contains(lower)) return true;
                        return false;
                }
            });
        });
        if (searchColumnChoice != null) {
            searchColumnChoice.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
                if (searchField != null) searchField.setText(searchField.getText());
            });
        }
        SortedList<Book> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(booksTable.comparatorProperty());
        booksTable.setItems(sorted);
    }

    @FXML
    private void openAddBook() {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_book.fxml"));
            javafx.scene.Parent root = loader.load();
            var scene = new javafx.scene.Scene(root);
            var stage = new javafx.stage.Stage();
            stage.setTitle("Add Book");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            // refresh list after dialog closed
            loadBooks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private Button editBookBtn;
    @FXML private Button deleteBookBtn;

    @FXML
    private void openEditBook() {
        var dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Edit Book");
        dialog.setHeaderText("Enter ISBN to load for editing");
        dialog.setContentText("ISBN:");
        var res = dialog.showAndWait();
        if (res.isPresent() && !res.get().trim().isEmpty()) {
            String isbn = res.get().trim();
            try {
                var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_book.fxml"));
                javafx.scene.Parent root = loader.load();
                var controller = loader.getController();
                if (controller instanceof com.libraryms.controller.pop_up.AddBookController) {
                    ((com.libraryms.controller.pop_up.AddBookController) controller).loadForEdit(isbn);
                }
                var scene = new javafx.scene.Scene(root);
                var stage = new javafx.stage.Stage();
                stage.setTitle("Edit Book - " + isbn);
                stage.setScene(scene);
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
                loadBooks();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void deleteSelectedBook() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/delete_book.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Supprimer un livre");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadBooks();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    private void loadBooks() {
        var list = FXCollections.<Book>observableArrayList();
        try (var conn = DatabaseUtil.connect()) {
            Integer adminId = com.libraryms.util.Session.getAdminId();
            if (adminId == null) {
                System.err.println("No admin logged in");
                return;
            }
            var ps = conn.prepareStatement("SELECT isbn, title, author, category, total_copies, available_copies FROM books WHERE admin_id = ?");
            ps.setInt(1, adminId);
            var rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Book(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("total_copies"),
                        rs.getInt("available_copies")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        masterData = list;
        booksTable.setItems(masterData);
    }

    public static class Book {
        private final String isbn, title, author, category;
        private final int totalCopies, availableCopies;
        public Book(String isbn, String title, String author, String category, int totalCopies, int availableCopies) {
            this.isbn = isbn; this.title = title; this.author = author; this.category = category;
            this.totalCopies = totalCopies; this.availableCopies = availableCopies;
        }
        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getCategory() { return category; }
        public int getTotalCopies() { return totalCopies; }
        public int getAvailableCopies() { return availableCopies; }
    }
}