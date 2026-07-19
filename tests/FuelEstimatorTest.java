import com.astratechnologies.astrasathi.FuelEstimator;

public class FuelEstimatorTest {
    public static void main(String[] args) {
        FuelEstimator.Estimate normal = FuelEstimator.estimate(20, 180, 15);
        if (!normal.available || Math.abs(normal.remainingLitres - 8) > 0.001 || normal.refuelRecommended)
            throw new AssertionError("normal fuel estimate failed");
        FuelEstimator.Estimate low = FuelEstimator.estimate(5, 70, 15);
        if (!low.available || !low.refuelRecommended) throw new AssertionError("low fuel warning failed");
        if (FuelEstimator.estimate(0, 10, 15).available) throw new AssertionError("missing fuel must not estimate");
        if (FuelEstimator.estimate(5, 10, 0).available) throw new AssertionError("missing mileage must not estimate");
        System.out.println("FuelEstimator-এর সব ৪টি পরীক্ষা সফল হয়েছে।");
    }
}
