package com.prymo.transactionservice.infrastructure.scheduler;

import com.prymo.transactionservice.application.usecase.ExpireHoldsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpirationScheduler {

    private final ExpireHoldsUseCase expireHoldsUseCase;

    public HoldExpirationScheduler(ExpireHoldsUseCase expireHoldsUseCase) {
        this.expireHoldsUseCase = expireHoldsUseCase;
    }

    // Run every 30 seconds for responsive escrow cleanup
    @Scheduled(fixedDelay = 30000)
    public void checkExpiredHolds() {
        expireHoldsUseCase.execute();
    }
}
