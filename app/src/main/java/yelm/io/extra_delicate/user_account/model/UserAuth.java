package yelm.io.extra_delicate.user_account.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UserAuth implements Serializable {
    @SerializedName("hash")
    @Expose
    private String hash;

    @SerializedName("user")
    @Expose
    private User user;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
