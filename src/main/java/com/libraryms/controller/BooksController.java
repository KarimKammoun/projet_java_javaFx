package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class BooksController {

    @FXML private TableView<Book> booksTable;

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
    }

    @FXML
    private void openAddBook() {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/add_book.fxml"));
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

    private void loadBooks() {
        var list = FXCollections.<Book>observableArrayList();
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT isbn, title, author, category, total_copies, available_copies FROM books")) {
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
        booksTable.setItems(list);
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