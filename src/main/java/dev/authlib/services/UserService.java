package dev.authlib.services;

import dev.authlib.models.User;
import dev.authlib.utils.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * User management service
 */

public class UserService {
    private final SessionFactory sessionFactory;

    public UserService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Create a new user
     */
    public User createUser(String email, String password, String firstName, String lastName) {
        // Validate
        EmailValidator.validate(email);
        PasswordValidator.validate(password);

        // Check if exists
        try (Session session = sessionFactory.openSession()) {
            User existing = findUserByEmailInSession(session, email);
            if (existing != null) {
                throw new UserAlreadyExists("User with email " + email + " already exists");
            }
        }

        // Create
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(PasswordHandler.hashPassword(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIsActive(true);
        user.setIsVerified(false);

        try (Session session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            throw new DatabaseError("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by ID
     */
    public User getUserById(int userId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null) {
                throw new UserNotFound("User with ID " + userId + " not found");
            }
            return user;
        } catch (Exception e) {
            if (e instanceof UserNotFound) throw e;
            throw new DatabaseError("Failed to get user: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            User user = findUserByEmailInSession(session, email);
            if (user == null) {
                throw new UserNotFound("User with email " + email + " not found");
            }
            return user;
        } catch (Exception e) {
            if (e instanceof UserNotFound) throw e;
            throw new DatabaseError("Failed to get user: " + e.getMessage(), e);
        }
    }

    /**
     * Update user
     */
    public User updateUser(int userId, User updates) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null) {
                throw new UserNotFound("User with ID " + userId + " not found");
            }

            if (updates.getFirstName() != null) user.setFirstName(updates.getFirstName());
            if (updates.getLastName() != null) user.setLastName(updates.getLastName());
            if (updates.getIsActive() != null) user.setIsActive(updates.getIsActive());
            if (updates.getIsVerified() != null) user.setIsVerified(updates.getIsVerified());
            if (updates.getLastLogin() != null) user.setLastLogin(updates.getLastLogin());

            var transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            if (e instanceof UserNotFound) throw e;
            throw new DatabaseError("Failed to update user: " + e.getMessage(), e);
        }
    }

    /**
     * Activate user
     */
    public User activateUser(int userId) {
        User updates = new User();
        updates.setIsActive(true);
        return updateUser(userId, updates);
    }

    /**
     * Deactivate user
     */
    public User deactivateUser(int userId) {
        User updates = new User();
        updates.setIsActive(false);
        return updateUser(userId, updates);
    }

    /**
     * Verify user
     */
    public User verifyUser(int userId) {
        User updates = new User();
        updates.setIsVerified(true);
        return updateUser(userId, updates);
    }

    /**
     * Update last login
     */
    public User updateLastLogin(int userId) {
        User updates = new User();
        updates.setLastLogin(LocalDateTime.now(ZoneId.of("UTC")));
        return updateUser(userId, updates);
    }

    private User findUserByEmailInSession(Session session, String email) {
        Query<User> query = session.createQuery(
            "FROM User WHERE email = :email", User.class
        );
        query.setParameter("email", email);
        List<User> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }
}
