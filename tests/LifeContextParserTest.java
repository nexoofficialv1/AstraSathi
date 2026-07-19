import com.astratechnologies.astrasathi.LifeContextIntent;
import com.astratechnologies.astrasathi.LifeContextParser;

public class LifeContextParserTest {
    private static int passed;

    public static void main(String[] args) {
        expect("তিন দিন আগে গাড়িতে ৫ লিটার তেল ভরেছি", LifeContextIntent.Type.FUEL_LOG, 5, -3);
        expect("এরপর গাড়ি ১৭৮ কিলোমিটার চালিয়েছি", LifeContextIntent.Type.DISTANCE_LOG, 178, 0);
        expect("আমি ১৭৮ কিমি রান করেছি", LifeContextIntent.Type.DISTANCE_LOG, 178, 0);
        expect("আমার গাড়ি লিটারে ১৫ কিলোমিটার মাইলেজ দেয়", LifeContextIntent.Type.MILEAGE_SET, 15, 0);
        expect("গাড়ির তেলের অবস্থা বলো", LifeContextIntent.Type.VEHICLE_STATUS, 0, 0);
        expect("আজকে গাড়িতে তেল ভরতে হবে", LifeContextIntent.Type.REFUEL_NEEDED, 0, 0);
        expect("আমি রাহুলকে বলেছি কালকে রিপোর্ট দিয়ে দেবো", LifeContextIntent.Type.COMMITMENT_SAVE, 0, 0);
        expect("কাকে কী বলেছি", LifeContextIntent.Type.COMMITMENT_LIST, 0, 0);
        expect("আজকে দুপুর ২টায় পেট ব্যথার ওষুধ খেয়েছি", LifeContextIntent.Type.MEDICINE_LOG, 0, 0);
        expect("আমার পেটে ব্যথা হচ্ছে", LifeContextIntent.Type.SYMPTOM_LOG, 0, 0);
        expect("আমার শরীরের অবস্থা কেমন", LifeContextIntent.Type.HEALTH_STATUS, 0, 0);
        expect("আমার বর্তমান পরিস্থিতি কেমন", LifeContextIntent.Type.LIFE_STATUS, 0, 0);
        LifeContextIntent phonePromise = new LifeContextParser().parse("আমি ফোনে রাহুলকে বলেছি কালকে রিপোর্ট দিয়ে দেবো");
        if (!"রাহুল".equals(phonePromise.subject)) throw new AssertionError("phone promise person: " + phonePromise.subject);
        if (new LifeContextParser().parseAll("তিন দিন আগে গাড়িতে ৫ লিটার তেল ভরেছি, এরপর গাড়ি ১৭৮ কিলোমিটার চালিয়েছি, আজ গাড়িতে তেল ভরতে হবে").size() != 3)
            throw new AssertionError("multi-event life context parse failed");
        passed += 2;
        System.out.println("সব " + passed + "টি LifeContextParser পরীক্ষা সফল হয়েছে।");
    }

    private static void expect(String input, LifeContextIntent.Type type, double amount, int dayOffset) {
        LifeContextIntent result = new LifeContextParser().parse(input);
        if (result.type != type) throw new AssertionError(input + " -> " + result.type + ", expected " + type);
        if (amount > 0 && Math.abs(result.amount - amount) > 0.001)
            throw new AssertionError(input + " -> amount " + result.amount);
        if (dayOffset != 0 && result.eventDayOffset != dayOffset)
            throw new AssertionError(input + " -> day offset " + result.eventDayOffset);
        passed++;
    }
}
