package com.example.service;

import com.example.model.Category;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.List;

public class CategoryService {
    private final SessionFactory sessionFactory;

    public CategoryService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void initializeCategories() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            List<Category> categories = session.createQuery("from Category", Category.class).list();
            if (categories.isEmpty()) {
                String[] defaultCategories = {"Food", "Transport", "Entertainment", "Bills", "Other"};
                for (String name : defaultCategories) {
                    session.save(new Category(name));
                }
                session.getTransaction().commit();
            }
        }
    }

    public List<Category> getAllCategories() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Category", Category.class).list();
        }
    }
}