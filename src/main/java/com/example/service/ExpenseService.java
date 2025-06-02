package com.example.service;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import com.example.model.Expense;
import com.example.model.User;

public class ExpenseService {
    private final SessionFactory sessionFactory;

    public ExpenseService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void addExpense(Expense expense) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(expense);
            tx.commit();
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public void updateExpense(Expense expense) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(expense);
            tx.commit();
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public void deleteExpense(int id) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Expense expense = session.get(Expense.class, id);
            if (expense != null) {
                session.delete(expense);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public List<Expense> getExpensesByUser(User user) {
        Session session = sessionFactory.openSession();
        try {
            Query<Expense> query = session.createQuery("FROM Expense WHERE user.id = :userId", Expense.class);
            query.setParameter("userId", user.getId());
            return query.list();
        } finally {
            session.close();
        }
    }

    public Map<String, Double> getSummaryByCategory(YearMonth yearMonth, User user) {
        Session session = sessionFactory.openSession();
        try {
            Query<Object[]> query = session.createQuery(
                    "SELECT e.category.name, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND YEAR(e.date) = :year AND MONTH(e.date) = :month GROUP BY e.category.name",
                    Object[].class);
            query.setParameter("userId", user.getId());
            query.setParameter("year", yearMonth.getYear());
            query.setParameter("month", yearMonth.getMonthValue());
            List<Object[]> results = query.list();
            Map<String, Double> summary = new HashMap<>();
            for (Object[] result : results) {
                summary.put((String) result[0], ((Number) result[1]).doubleValue());
            }
            return summary;
        } finally {
            session.close();
        }
    }

    public double getTotalExpenses(YearMonth yearMonth, User user) {
        Session session = sessionFactory.openSession();
        try {
            Query<Double> query = session.createQuery(
                    "SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.user.id = :userId AND YEAR(e.date) = :year AND MONTH(e.date) = :month",
                    Double.class);
            query.setParameter("userId", user.getId());
            query.setParameter("year", yearMonth.getYear());
            query.setParameter("month", yearMonth.getMonthValue());
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }
}