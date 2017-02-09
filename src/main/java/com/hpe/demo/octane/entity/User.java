package com.hpe.demo.octane.entity;

import com.hpe.demo.octane.excel.OctaneEntityIterator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by panuska on 11/15/12.
 */
public class User {

    private String id;
    private String login;
    private String password;

    public User(String id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    static private Map<String, User> users = new HashMap<>();

    public static void addUser(User user) {
        users.put(user.getId(), user);
        OctaneEntityIterator.putReference("users." + user.getId(), user.getLogin());
    }

    public static User getUser(String userId) {
        return users.get(userId);
    }
}
