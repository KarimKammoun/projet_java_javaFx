package com.libraryms.controller.pop_up;

import com.libraryms.dao.BookDAO;
import com.libraryms.models.Book;
import com.libraryms.util.Session;
import com.libraryms.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddBookController {

    @FXML private TextField isbnField;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField categoryField;
    @FXML private TextField copiesField;

    private boolean editMode = false;
    private String originalIsbn = null;
    private int originalTotalCopies = 0;

    private final BookDAO bookDAO = new BookDAO();

    public void loadForEdit(String isbn) {
        try {
            Book b = bookDAO.findByIsbn(isbn);
            if (b != null) {
                isbnField.setText(b.getIsbn());
                titleField.setText(b.getTitle());
                authorField.setText(b.getAuthor());
                categoryField.setText(b.getCategory());
                originalIsbn = b.getIsbn();
                originalTotalCopies = b.getTotalCopies();
                editMode = true;
                isbnField.setDisable(true);
                copiesField.setText(String.valueOf(originalTotalCopies));
                copiesField.setDisable(false);
            } else {
                new Alert(Alert.AlertType.WARNING, "Livre non trouvé pour cet ISBN.").showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void loadById() {
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer l'ISBN.").showAndWait();
            return;
        }
        loadForEdit(isbn);
    }

    @FXML
    private void saveBook() {
        String isbn = isbnField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String category = categoryField.getText().trim();
        int copies = 1;
        try {
            copies = Integer.parseInt(copiesField.getText().trim());
            if (copies < 1) copies = 1;
        } catch (Exception e) { copies = 1; }

        if (isbn.isEmpty() || title.isEmpty() || author.isEmpty() || category.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs.").showAndWait();
            return;
        }

        try {
            Integer adminId = Session.getAdminId();
            Book book = new Book(isbn, title, author, category, copies, copies, adminId);

            if (editMode && originalIsbn != null) {
                Book original = bookDAO.findByIsbn(originalIsbn);
                if (original == null) {
                    new Alert(Alert.AlertType.ERROR, "Livre original introuvable.").showAndWait();
                    return;
                }
                book.setAdminId(original.getAdminId());
                try {
                    bookDAO.update(book, copies);
                    new Alert(Alert.AlertType.INFORMATION, "Livre mis à jour.").showAndWait();
                    Stage s = (Stage) isbnField.getScene().getWindow(); s.close();
                } catch (IllegalStateException ise) {
                    new Alert(Alert.AlertType.ERROR, "Impossible de réduire le nombre de copies: " + ise.getMessage()).showAndWait();
                }
                return;
            }

            // create new book
            Book exists = bookDAO.findByIsbn(isbn);
            if (exists != null) {
                new Alert(Alert.AlertType.ERROR, "Un livre avec ce ISBN existe déjà.").showAndWait();
                return;
            }

            bookDAO.create(book, copies);
            new Alert(Alert.AlertType.INFORMATION, "Livre ajouté avec succès.").showAndWait();
            Stage s = (Stage) isbnField.getScene().getWindow(); s.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur pendant l'enregistrement: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) isbnField.getScene().getWindow(); s.close();
    }
}
