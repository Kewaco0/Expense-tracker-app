package com.example.service;

import com.example.model.User;
import com.example.util.PasswordUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class UserService {
    private final SessionFactory sessionFactory;

    public UserService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public boolean signup(String username, String password) {
        if (username.isEmpty() || password.length() < 6) {
            return false;
        }
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            User existingUser = session.createQuery("from User where username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
            if (existingUser != null) {
                session.getTransaction().rollback();
                return false;
            }
            User user = new User(username, PasswordUtil.hashPassword(password));
            session.save(user);
            session.getTransaction().commit();
            return true;
        }
    }

    public User login(String username, String password) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.createQuery("from User where username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
            if (user != null && PasswordUtil.verifyPassword(password, user.getPassword())) {
                return user;
            }
            return null;
        }
    }
}