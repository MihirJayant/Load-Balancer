/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javafx.beans.property.SimpleStringProperty;

public class User {
    private final SimpleStringProperty user;
    private final SimpleStringProperty pass;

    public User(String user, String pass) {
        this.user = new SimpleStringProperty(user);
        this.pass = new SimpleStringProperty(pass);
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

    public SimpleStringProperty userProperty() {
        return user;
    }

    public SimpleStringProperty passProperty() {
        return pass;
    }
}
