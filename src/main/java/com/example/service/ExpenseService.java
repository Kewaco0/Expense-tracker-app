package com.example.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.example.model.Expense;
import com.example.model.User;

// Listener interface for expense events
interface ExpenseListener {
    void onExpenseUpdated(Expense expense);
}

public class ExpenseService {
    private final SessionFactory sessionFactory;
    private final List<ExpenseListener> listeners = new ArrayList<>();

    public ExpenseService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    // Method to add a listener
    public void addListener(ExpenseListener listener) {
        listeners.add(listener);
    }

    // Method to remove a listener
    public void removeListener(ExpenseListener listener) {
        listeners.remove(listener);
    }

    // Notify all listeners of an expense update
    private void notifyExpenseUpdated(Expense expense) {
        for (ExpenseListener listener : listeners) {
            listener.onExpenseUpdated(expense);
        }
    }

    public void addExpense(Expense expense) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.save(expense);
            session.getTransaction().commit();
        }
    }

    public List<Expense> getExpensesByUser(User user) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Expense where user.id = :userId", Expense.class)
                    .setParameter("userId", user.getId())
                    .list();
        }
    }

    public void updateExpense(Expense expense) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            // Verify that the expense exists
            Expense existingExpense = session.get(Expense.class, expense.getId());
            if (existingExpense == null) {
                session.getTransaction().rollback();
                throw new IllegalArgumentException("Expense with ID " + expense.getId() + " does not exist.");
            }
            // Merge the detached expense into the session
            session.merge(expense);
            session.getTransaction().commit();
            // Notify listeners (optional)
            System.out.println("Expense updated: " + expense);
            notifyExpenseUpdated(expense);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update expense: " + e.getMessage());
        }
    }

    public void deleteExpense(int expenseId) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Expense expense = session.get(Expense.class, expenseId);
            if (expense != null) {
                session.delete(expense);
            }
            session.getTransaction().commit();
        }
    }

    public Map<String, Double> getSummaryByCategory(YearMonth yearMonth, User user) {
        try (Session session = sessionFactory.openSession()) {
            List<Expense> expenses = session
                    .createQuery("from Expense where year(date) = :year and month(date) = :month and user.id = :userId",
                            Expense.class)
                    .setParameter("year", yearMonth.getYear())
                    .setParameter("month", yearMonth.getMonthValue())
                    .setParameter("userId", user.getId())
                    .list();
            return expenses.stream()
                    .collect(Collectors.groupingBy(
                            expense -> expense.getCategory().getName(),
                            Collectors.summingDouble(Expense::getAmount)));
        }
    }
}