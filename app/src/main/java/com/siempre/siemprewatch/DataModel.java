package com.siempre.siemprewatch;

public class DataModel {
    public int type; // 0 is user, 1 is group
    public String id;
    public String name;
}

class User extends DataModel {
    public User() {
        this.type = 0;
    }

    public int status;
    public String photoURL;
    public boolean inCall;
}
