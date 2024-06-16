package com.example.env_cat.enity;

public class User {
    public int id;
    public String telephone;
    public String password;

    public User(){

    }

    public User(String telephone, String password) {
        this.telephone = telephone;
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", telephone='" + telephone + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
