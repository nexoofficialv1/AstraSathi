package com.astratechnologies.astrasathi;

/** Pure calculation boundary so a fuel estimate is never confused with a sensor reading. */
public final class FuelEstimator {
    public static final class Estimate {
        public final boolean available;
        public final double remainingLitres;
        public final boolean refuelRecommended;
        public final String missing;

        Estimate(boolean available, double remainingLitres, boolean refuelRecommended, String missing) {
            this.available = available;
            this.remainingLitres = remainingLitres;
            this.refuelRecommended = refuelRecommended;
            this.missing = missing;
        }
    }

    private FuelEstimator() { }

    public static Estimate estimate(double filledLitres, double distanceKm, double mileageKmPerLitre) {
        if (filledLitres <= 0) return new Estimate(false, 0, false, "fuel_quantity");
        if (mileageKmPerLitre <= 0) return new Estimate(false, 0, false, "mileage");
        double remaining = Math.max(0, filledLitres - Math.max(0, distanceKm) / mileageKmPerLitre);
        boolean low = remaining <= Math.max(1, filledLitres * 0.20);
        return new Estimate(true, remaining, low, "");
    }
}
