package com.example.service;

import com.example.model.Reservation;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import java.util.List;

public class ReservationServiceImpl implements ReservationService, AutoCloseable {
    private final EntityManagerFactory emf = Persistence.createEntityManagerFactory("reservationPU");

    @Override
    public Reservation creerReservation(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(reservation);
            em.getTransaction().commit();
            return reservation;
        } catch (RuntimeException e) {
            rollbackIfActive(em);
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public Reservation modifierReservation(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            // Merge the detached snapshot so JPA can compare its @Version to the database.
            Reservation managed = em.merge(reservation);
            em.flush();
            em.getTransaction().commit();
            return managed;
        } catch (RuntimeException e) {
            rollbackIfActive(em);
            OptimisticLockException optimisticLockException = findOptimisticLockException(e);
            if (optimisticLockException != null) {
                throw optimisticLockException;
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public Reservation trouverParId(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Reservation.class, id);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Reservation> listerReservations() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Reservation> query = em.createQuery("SELECT r FROM Reservation r", Reservation.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public void close() {
        if (emf.isOpen()) {
            emf.close();
        }
    }

    private static void rollbackIfActive(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    private static OptimisticLockException findOptimisticLockException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof OptimisticLockException) {
                return (OptimisticLockException) current;
            }
            current = current.getCause();
        }
        if (error instanceof RollbackException && error.getCause() instanceof OptimisticLockException) {
            return (OptimisticLockException) error.getCause();
        }
        return null;
    }
}
