package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;
import com.example.service.ReservationServiceImpl;

import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;

public class ConcurrentReservationSimulator {
    public static void main(String[] args) {
        try (ReservationServiceImpl service = new ReservationServiceImpl()) {
            Reservation initial = new Reservation(
                    "Salle A",
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(2));
            initial = service.creerReservation(initial);

            // Deux détachements récupèrent la même version avant toute modification.
            Reservation tx1 = service.trouverParId(initial.getId());
            Reservation tx2 = service.trouverParId(initial.getId());
            System.out.printf("Snapshot initial : id=%d, version=%d, salle=%s%n",
                    initial.getId(), tx1.getVersion(), tx1.getSalle());

            tx1.setSalle("Salle B");
            service.modifierReservation(tx1);
            System.out.println("Transaction 1 : modification réussie (version incrémentée).");

            try {
                tx2.setSalle("Salle C");
                service.modifierReservation(tx2);
                System.out.println("Transaction 2 : modification réussie.");
            } catch (OptimisticLockException e) {
                System.out.println("Transaction 2 : conflit détecté (@Version), modification refusée.");
            }

            Reservation finalState = service.trouverParId(initial.getId());
            System.out.printf("État conservé en base : salle=%s, version=%d%n",
                    finalState.getSalle(), finalState.getVersion());
        }
    }
}
