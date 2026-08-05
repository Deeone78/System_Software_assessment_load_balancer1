/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminController {

    @FXML private TableView<User> userTable;
    @FXML private TextField newUserField;
    @FXML private PasswordField newPassField;
    @FXML private TextArea statusArea;

    private DB db = new DB();

    @FXML
    public void initialize() {
        TableColumn<User, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("user"));
        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        userTable.getColumns().addAll(nameCol, roleCol);
        loadUsers();
    }

    private void loadUsers() {
        try {
            ObservableList<User> users = db.getDataFromTable();
            userTable.setItems(users);
        } catch (Exception e) {
            status("Could not load users: " + e.getMessage());
        }
    }

    private void status(String msg) {
        statusArea.appendText(msg + "\n");
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        String name = newUserField.getText();
        String pass = newPassField.getText();
        if (name.isEmpty() || pass.isEmpty()) {
            status("Enter a username and password");
            return;
        }
        try {
            db.addDataToDB(name, pass, "STANDARD");
            status("Added user " + name);
            newUserField.clear();
            newPassField.clear();
            loadUsers();
        } catch (Exception e) {
            status("Add failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status("Select a user first");
            return;
        }
        db.deleteUser(selected.getUser());
        status("Deleted user " + selected.getUser());
        loadUsers();
    }

    @FXML
    private void handlePromote(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status("Select a user first");
            return;
        }
        db.promoteToAdmin(selected.getUser());
        status("Promoted " + selected.getUser() + " to ADMIN");
        loadUsers();
    }
}
