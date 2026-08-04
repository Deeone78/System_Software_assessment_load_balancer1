/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Button logoutBtn;
    @FXML private Button uploadBtn;
    @FXML private Button downloadBtn;
    @FXML private Button deleteBtn;
    @FXML private Button shareBtn;
    @FXML private ComboBox<String> algorithmCombo;
    @FXML private Button terminalBtn;
    @FXML private Button adminBtn;
    @FXML private TableView<FileItem> fileTable;
    @FXML private TextArea logArea;

    private String currentUser;
    private String currentRole;

    private DB db = new DB();
    private FilePartitioner partitioner = new FilePartitioner();
    private HttpClient client = HttpClient.newHttpClient();

    private String loadBalancer = "http://localhost:8080";

    public void initialise(String username, String role) {
        currentUser = username;
        currentRole = role;
        welcomeLabel.setText("Welcome, " + username);
        roleLabel.setText("Role: " + role);

        algorithmCombo.getItems().addAll("ROUND_ROBIN", "FCFS", "SJN");
        algorithmCombo.setValue("ROUND_ROBIN");

        adminBtn.setVisible(role.equals("ADMIN"));

        TableColumn<FileItem, String> nameCol = new TableColumn<>("Filename");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        TableColumn<FileItem, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setCellValueFactory(new PropertyValueFactory<>("owner"));
        TableColumn<FileItem, Long> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        fileTable.getColumns().addAll(nameCol, ownerCol, sizeCol);

        refreshFiles();
        log("Logged in as " + username);
    }

    private void refreshFiles() {
        ObservableList<FileItem> files = db.getFilesForUser(currentUser);
        fileTable.setItems(files);
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    @FXML
    private void handleUpload(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(uploadBtn.getScene().getWindow());
        if (file == null) {
            return;
        }
        log("Uploading " + file.getName() + " ...");
        new Thread(() -> {
            try {
                int chunks = partitioner.uploadFile(file, currentUser);
                db.saveFile(file.getName(), currentUser, file.length(), chunks, "lbc_storage_01");
                db.syncFileToMySQL(file.getName(), currentUser, file.length(), chunks, "lbc_storage_01");
                log("Upload done: " + file.getName() + " (" + chunks + " chunks)");
                Platform.runLater(this::refreshFiles);
            } catch (Exception e) {
                log("Upload failed: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleDownload(ActionEvent event) {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            log("Please select a file to download");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(selected.getFilename());
        File dest = chooser.showSaveDialog(downloadBtn.getScene().getWindow());
        if (dest == null) {
            return;
        }
        log("Downloading " + selected.getFilename() + " ...");
        new Thread(() -> {
            try {
                byte[] data = partitioner.downloadFile(selected.getFilename(), selected.getChunks());
                partitioner.saveTo(data, dest);
                log("Download done: " + dest.getName());
            } catch (Exception e) {
                log("Download failed: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            log("Please select a file to delete");
            return;
        }
        db.deleteFile(selected.getFilename());
        new Thread(() -> {
            try {
                for (int i = 0; i < selected.getChunks(); i++) {
                    String chunkName = selected.getFilename() + ".part" + i;
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(loadBalancer + "/delete?filename=" + chunkName))
                            .DELETE().build();
                    client.send(req, HttpResponse.BodyHandlers.ofString());
                }
            } catch (Exception e) {
                log("Delete on storage failed: " + e.getMessage());
            }
        }).start();
        log("Deleted " + selected.getFilename());
        refreshFiles();
    }

    @FXML
    private void handleShare(ActionEvent event) {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            log("Please select a file to share");
            return;
        }
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setHeaderText("Share file: " + selected.getFilename());
        nameDialog.setContentText("Share with username:");
        Optional<String> name = nameDialog.showAndWait();
        if (!name.isPresent() || name.get().trim().isEmpty()) {
            return;
        }
        ChoiceDialog<String> permDialog = new ChoiceDialog<>("READ", "READ", "WRITE");
        permDialog.setHeaderText("Permission");
        permDialog.setContentText("Choose permission:");
        Optional<String> perm = permDialog.showAndWait();
        if (!perm.isPresent()) {
            return;
        }
        db.setPermission(selected.getFilename(), name.get().trim(), perm.get());
        log("Shared " + selected.getFilename() + " with " + name.get() + " (" + perm.get() + ")");
    }

    @FXML
    private void handleAlgorithm(ActionEvent event) {
        String algo = algorithmCombo.getValue();
        log("Switching algorithm to " + algo);
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(loadBalancer + "/algorithm?name=" + algo))
                        .GET().build();
                client.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                log("Could not switch algorithm: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleTerminal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("terminal.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 450));
            stage.setTitle("Terminal");
            stage.show();
        } catch (Exception e) {
            log("Could not open terminal: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdmin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("admin.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 500, 450));
            stage.setTitle("Admin Panel");
            stage.show();
        } catch (Exception e) {
            log("Could not open admin panel: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            log("Logout failed: " + e.getMessage());
        }
    }
}
