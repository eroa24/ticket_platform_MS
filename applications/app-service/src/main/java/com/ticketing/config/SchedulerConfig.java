package com.ticketing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.ticketing.usecase.reservation.ReleaseExpiredReservationsUseCase;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    private final ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;

    public SchedulerConfig(ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase) {
        this.releaseExpiredReservationsUseCase = releaseExpiredReservationsUseCase;
    }

    @Scheduled(fixedRateString = "${app.reservation.cleanup-rate-ms:60000}")
    public void releaseExpiredReservations() {
        log.debug("Running expired reservations cleanup...");
        releaseExpiredReservationsUseCase.execute()
                .doOnTerminate(() -> log.debug("Expired reservations cleanup complete"))
                .doOnError(e -> log.error("Error releasing expired reservations: {}", e.getMessage()))
                .subscribe();
    }
}
