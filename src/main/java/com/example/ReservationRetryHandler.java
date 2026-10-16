package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;

import javax.persistence.OptimisticLockException;

public class ReservationRetryHandler {
    private static final int MAX_RETRIES = 3;
    private final ReservationService service;

    public ReservationRetryHandler(ReservationService service) {
        this.service = service;
    }

    public Reservation modifierAvecRetry(Reservation reservation) {
        final String salleDemandee = reservation.getSalle();
        int tentative = 0;

        while (tentative <= MAX_RETRIES) {
            try {
                return service.modifierReservation(reservation);
            } catch (OptimisticLockException e) {
                if (tentative == MAX_RETRIES) {
                    throw new OptimisticLockException(
                            "Échec après " + MAX_RETRIES + " tentatives", e);
                }
                tentative++;
                System.out.println("Conflit détecté, tentative de retry " + tentative);
                reservation = service.trouverParId(reservation.getId());
                if (reservation == null) {
                    throw new IllegalArgumentException("Réservation non trouvée");
                }
                // Reapply the caller's requested change to the freshly loaded version.
                reservation.setSalle(salleDemandee);
            }
        }
        throw new IllegalStateException("Boucle de retry terminée de manière inattendue");
    }
}
