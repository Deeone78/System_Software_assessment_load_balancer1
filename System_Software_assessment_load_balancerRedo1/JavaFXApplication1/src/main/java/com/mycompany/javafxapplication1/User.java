/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */

import javafx.beans.property.SimpleStringProperty;

public class User {
    private SimpleStringProperty user;
    private SimpleStringProperty pass;
    private String role;

    public User(String user, String pass) {
        this.user = new SimpleStringProperty(user);
        this.pass = new SimpleStringProperty(pass);
        this.role = "STANDARD";
    }

    public User(String user, String pass, String role) {
        this.user = new SimpleStringProperty(user);
        this.pass = new SimpleStringProperty(pass);
        this.role = role;
    }

    public String getUser() {
        return user.get();
    }

    public void setUser(String user) {
        this.user.set(user);
    }

    public String getPass() {
        return pass.get();
    }

    public void setPass(String pass) {
        this.pass.set(pass);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
