import com.astratechnologies.astrasathi.SensitiveDataFilter;

public class SensitiveDataFilterTest {
    private static int passed;

    public static void main(String[] args) {
        sensitive("123456", "OTP field");
        sensitive("গোপন", "পাসওয়ার্ড লিখুন");
        sensitive("4111111111111111", "কার্ড");
        allowed("রাহুল", "সার্চ");
        protectedAction("শেয়ার কিনুন");
        System.out.println("সব " + passed + "টি SensitiveDataFilter পরীক্ষা সফল হয়েছে।");
    }

    private static void sensitive(String value, String hint) {
        if (!SensitiveDataFilter.isSensitive(value, hint)) throw new AssertionError("Sensitive value was allowed");
        passed++;
    }

    private static void allowed(String value, String hint) {
        if (SensitiveDataFilter.isSensitive(value, hint)) throw new AssertionError("Safe value was blocked");
        passed++;
    }

    private static void protectedAction(String value) {
        if (!SensitiveDataFilter.isProtectedAction(value)) throw new AssertionError("Protected action was allowed");
        passed++;
    }
}
