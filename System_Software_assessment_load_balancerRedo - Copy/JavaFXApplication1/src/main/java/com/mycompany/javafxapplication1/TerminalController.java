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
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

public class TerminalController {

    @FXML private TextArea outputArea;
    @FXML private TextField commandInput;

    private Path currentDir = Paths.get(System.getProperty("user.dir"));

    @FXML
    public void initialize() {
        print("Simple Terminal - type 'help' for commands");
        print(currentDir.toString() + " $");
    }

    private void print(String msg) {
        outputArea.appendText(msg + "\n");
    }

    @FXML
    private void handleEnter(ActionEvent event) {
        String line = commandInput.getText().trim();
        commandInput.clear();
        if (line.isEmpty()) {
            return;
        }
        print("$ " + line);
        runCommand(line);
    }

    private void runCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0];

        try {
            switch (cmd) {
                case "ls":
                    File[] files = currentDir.toFile().listFiles();
                    if (files != null) {
                        for (File f : files) {
                            print(f.getName() + (f.isDirectory() ? "/" : ""));
                        }
                    }
                    break;
                case "pwd":
                    print(currentDir.toString());
                    break;
                case "cd":
                    if (parts.length > 1) {
                        Path newDir = currentDir.resolve(parts[1]).normalize();
                        if (newDir.toFile().isDirectory()) {
                            currentDir = newDir;
                        } else {
                            print("cd: no such directory: " + parts[1]);
                        }
                    }
                    break;
                case "mkdir":
                    if (parts.length > 1) {
                        new File(currentDir.toFile(), parts[1]).mkdir();
                        print("Created " + parts[1]);
                    }
                    break;
                case "cp":
                    if (parts.length > 2) {
                        Files.copy(currentDir.resolve(parts[1]), currentDir.resolve(parts[2]));
                        print("Copied " + parts[1] + " -> " + parts[2]);
                    }
                    break;
                case "mv":
                    if (parts.length > 2) {
                        File src = currentDir.resolve(parts[1]).toFile();
                        File dst = currentDir.resolve(parts[2]).toFile();
                        src.renameTo(dst);
                        print("Moved " + parts[1] + " -> " + parts[2]);
                    }
                    break;
                case "cat":
                    if (parts.length > 1) {
                        Path p = currentDir.resolve(parts[1]);
                        if (p.toFile().exists()) {
                            print(new String(Files.readAllBytes(p)));
                        } else {
                            print("cat: no such file: " + parts[1]);
                        }
                    }
                    break;
                case "nano":
                    if (parts.length > 1) {
                        editFile(parts[1]);
                    }
                    break;
                case "tree":
                    tree(currentDir.toFile(), "");
                    break;
                case "ps":
                    print("PID   COMMAND");
                    print("1     loadbalancer");
                    print("2     storage-server");
                    print("3     javafx-app");
                    break;
                case "whoami":
                    print(System.getProperty("user.name"));
                    break;
                case "clear":
                    outputArea.clear();
                    break;
                case "help":
                    print("Commands: ls cd pwd mkdir cp mv cat nano tree ps whoami clear help");
                    break;
                default:
                    print(cmd + ": command not found");
            }
        } catch (Exception e) {
            print("error: " + e.getMessage());
        }
    }

    private void editFile(String name) throws Exception {
        Path p = currentDir.resolve(name);
        String existing = "";
        if (p.toFile().exists()) {
            existing = new String(Files.readAllBytes(p));
        }
        TextInputDialog dialog = new TextInputDialog(existing);
        dialog.setHeaderText("Editing " + name);
        dialog.setContentText("Content:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            FileWriter fw = new FileWriter(p.toFile());
            fw.write(result.get());
            fw.close();
            print("Saved " + name);
        }
    }

    private void tree(File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            print(prefix + f.getName());
            if (f.isDirectory()) {
                tree(f, prefix + "  ");
            }
        }
    }
}