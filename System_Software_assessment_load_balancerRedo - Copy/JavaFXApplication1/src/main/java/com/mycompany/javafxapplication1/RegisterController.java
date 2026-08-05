package com.mycompany.javafxapplication1;

import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private Button registerBtn;
    @FXML private Button backLoginBtn;
    @FXML private PasswordField passPasswordField;
    @FXML private PasswordField rePassPasswordField;
    @FXML private TextField userTextField;

    private void dialogue(String headerMsg, String contentMsg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        alert.showAndWait();
    }

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            DB myObj = new DB();
            if (passPasswordField.getText().equals(rePassPasswordField.getText())) {
                String username = userTextField.getText();
                myObj.addDataToDB(username, passPasswordField.getText(), "STANDARD");
                dialogue("Success", "Account created!");
                loader.setLocation(getClass().getResource("dashboard.fxml"));
                Parent root = loader.load();
                secondaryStage.setScene(new Scene(root, 800, 550));
                DashboardController controller = loader.getController();
                secondaryStage.setTitle("Dashboard");
                controller.initialise(username, "STANDARD");
            } else {
                dialogue("Error", "Passwords do not match. Please try again.");
                loader.setLocation(getClass().getResource("register.fxml"));
                Parent root = loader.load();
                secondaryStage.setScene(new Scene(root, 640, 480));
                secondaryStage.setTitle("Register a new User");
            }
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) backLoginBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            secondaryStage.setScene(new Scene(root, 640, 480));
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}