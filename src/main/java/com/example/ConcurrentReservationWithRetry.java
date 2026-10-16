package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;
import com.example.service.ReservationServiceImpl;

import java.time.LocalDateTime;

public class ConcurrentReservationWithRetry {
    public static void main(String[] args) {
        try (ReservationServiceImpl service = new ReservationServiceImpl()) {
            ReservationService reservationService = service;
            ReservationRetryHandler retryHandler = new ReservationRetryHandler(reservationService);

            Reservation initial = new Reservation(
                    "Salle A",
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(2));
            initial = reservationService.creerReservation(initial);

            Reservation tx1 = reservationService.trouverParId(initial.getId());
            Reservation tx2 = reservationService.trouverParId(initial.getId());

            tx1.setSalle("Salle B");
            reservationService.modifierReservation(tx1);
            System.out.println("Transaction 1 : modification réussie.");

            tx2.setSalle("Salle C");
            try {
                retryHandler.modifierAvecRetry(tx2);
                System.out.println("Transaction 2 : modification réussie après retry.");
            } catch (Exception e) {
                System.out.println("Transaction 2 : échec après retries - " + e.getMessage());
            }

            Reservation finalState = reservationService.trouverParId(initial.getId());
            System.out.printf("État final : salle=%s, version=%d%n",
                    finalState.getSalle(), finalState.getVersion());
        }
    }
}
