package pl.climbbetter.training.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Calculation settings are snapshotted with every new assessed entry. */
@Component
record RecencyLoadSettings(int horizonDays, boolean includeTopRope, boolean weightSmallSample) {
    RecencyLoadSettings(
            @Value("${climbbetter.load.recency.horizon-days:730}") int horizonDays,
            @Value("${climbbetter.load.recency.include-top-rope:false}") boolean includeTopRope,
            @Value("${climbbetter.load.recency.weight-small-sample:false}") boolean weightSmallSample
    ) {
        if (horizonDays <= 0) throw new IllegalArgumentException("Recency horizon must be positive");
        this.horizonDays = horizonDays;
        this.includeTopRope = includeTopRope;
        this.weightSmallSample = weightSmallSample;
    }
}
