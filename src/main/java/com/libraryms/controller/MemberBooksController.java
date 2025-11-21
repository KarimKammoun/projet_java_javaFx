package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class MemberBooksController {

    @FXML private TableView<BookRow> booksTable;
    @FXML private TextField searchField;

    private ObservableList<BookRow> masterData;

    @FXML
    private void initialize() {
        var cols = booksTable.getColumns();
        ((TableColumn<BookRow, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("isbn"));
        ((TableColumn<BookRow, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("title"));
        ((TableColumn<BookRow, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("author"));
        ((TableColumn<BookRow, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("category"));
        ((TableColumn<BookRow, Integer>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("available"));

        loadAvailableBooks();

        FilteredList<BookRow> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String lower = newV == null ? "" : newV.toLowerCase();
            filtered.setPredicate(b -> {
                if (lower.isEmpty()) return true;
                if (b.getIsbn() != null && b.getIsbn().toLowerCase().contains(lower)) return true;
                if (b.getTitle() != null && b.getTitle().toLowerCase().contains(lower)) return true;
                if (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(lower)) return true;
                if (b.getCategory() != null && b.getCategory().toLowerCase().contains(lower)) return true;
                return false;
            });
        });

        SortedList<BookRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(booksTable.comparatorProperty());
        booksTable.setItems(sorted);
    }

    private void loadAvailableBooks() {
        var list = FXCollections.<BookRow>observableArrayList();
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT isbn, title, author, category, available_copies FROM books WHERE available_copies > 0 ORDER BY title")) {
            while (rs.next()) {
                list.add(new BookRow(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("available_copies")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        masterData = list;
    }

    @FXML
    private void goBack() {
        // return to member home
        SceneManager.loadScene("/fxml/member_home.fxml");
    }

    public static class BookRow {
        private final String isbn, title, author, category;
        private final int available;
        public BookRow(String isbn, String title, String author, String category, int available) {
            this.isbn = isbn; this.title = title; this.author = author; this.category = category; this.available = available;
        }
        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getCategory() { return category; }
        public int getAvailable() { return available; }
    }
}
