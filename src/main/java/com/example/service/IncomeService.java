
package com.example.service;

import java.time.YearMonth;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import com.example.model.Income;
import com.example.model.User;

public class IncomeService {
    private final SessionFactory sessionFactory;

    public IncomeService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void addIncome(Income income) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(income);
            tx.commit();
            System.out.println("Added income: " + income.getDescription() + ", Amount: " + income.getAmount());
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public void updateIncome(Income income) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(income);
            tx.commit();
            System.out.println("Updated income: " + income.getDescription() + ", New Amount: " + income.getAmount() +
                    ", New Remaining: " + income.getRemainingAmount());
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public void deductFromIncome(Income income, double amount) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Income freshIncome = session.get(Income.class, income.getId());
            if (freshIncome == null) {
                throw new IllegalStateException("Income not found: " + income.getDescription());
            }
            double newRemaining = freshIncome.getRemainingAmount() - amount;
            if (newRemaining < 0) {
                throw new IllegalStateException("Insufficient funds in " + freshIncome.getDescription() +
                        ". Requested: $" + amount + ", Remaining: $" + freshIncome.getRemainingAmount());
            }
            freshIncome.setRemainingAmount(newRemaining);
            session.merge(freshIncome);
            tx.commit();
            System.out.println("Deducted " + amount + " from income " + freshIncome.getDescription() +
                    ", new remaining: " + freshIncome.getRemainingAmount());
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public void deleteIncome(int id) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Income income = session.get(Income.class, id);
            if (income == null) {
                throw new IllegalStateException("Income not found with ID: " + id);
            }
            // Check for associated expenses
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(*) FROM Expense e WHERE e.income.id = :incomeId", Long.class);
            query.setParameter("incomeId", id);
            Long expenseCount = query.uniqueResult();
            if (expenseCount > 0) {
                throw new IllegalStateException("Cannot delete income '" + income.getDescription() +
                        "' because it is associated with " + expenseCount + " expense(s).");
            }
            session.delete(income);
            tx.commit();
            System.out.println("Deleted income: " + income.getDescription());
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public List<Income> getIncomesByUser(User user) {
        Session session = sessionFactory.openSession();
        try {
            Query<Income> query = session.createQuery("FROM Income WHERE user.id = :userId", Income.class);
            query.setParameter("userId", user.getId());
            List<Income> incomes = query.list();
            for (Income income : incomes) {
                if (income.getRemainingAmount() == null) {
                    income.setRemainingAmount(income.getAmount());
                    updateIncome(income);
                    System.out.println("Initialized null remainingAmount for income: " + income.getDescription());
                }
            }
            return incomes;
        } finally {
            session.close();
        }
    }

    public double getTotalIncome(YearMonth yearMonth, User user) {
        Session session = sessionFactory.openSession();
        try {
            Query<Double> query = session.createQuery(
                    "SELECT SUM(i.remainingAmount) FROM Income i WHERE i.user.id = :userId AND YEAR(i.date) = :year AND MONTH(i.date) = :month",
                    Double.class);
            query.setParameter("userId", user.getId());
            query.setParameter("year", yearMonth.getYear());
            query.setParameter("month", yearMonth.getMonthValue());
            Double total = query.uniqueResult();
            return total != null ? total : 0.0;
        } finally {
            session.close();
        }
    }
}
