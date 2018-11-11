package com.trinity.aayushman.passwordmanager;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PassEntry {

    public String username;
    public String password;
    public String description;
    public Long uniquen;

    public PassEntry(String username, String password, String description, Long uniquen){
        this.username = username;
        this.password = password;
        this.description = description;
        this.uniquen = uniquen;
    }

    public  PassEntry(){
        //Def Constructor
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUniquen() {
        return uniquen;
    }

    public void setUniquen(long uniquen) {
        this.uniquen = uniquen;
    }
}
