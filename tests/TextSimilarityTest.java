import com.astratechnologies.astrasathi.TextSimilarity;

public class TextSimilarityTest {
    private static int passed;

    public static void main(String[] args) {
        confident("পাঠান", "পাঠান");
        confident("সার্চ", "সার্চ করুন");
        confident("whatsap", "WhatsApp");
        rejected("সেটিংস", "পাঠান");
        rejected("হোম", "ডিলিট অ্যাকাউন্ট");
        System.out.println("সব " + passed + "টি TextSimilarity পরীক্ষা সফল হয়েছে।");
    }

    private static void confident(String query, String candidate) {
        if (!TextSimilarity.isConfident(query, candidate))
            throw new AssertionError(query + " ~ " + candidate + " score=" + TextSimilarity.score(query, candidate));
        passed++;
    }

    private static void rejected(String query, String candidate) {
        if (TextSimilarity.isConfident(query, candidate))
            throw new AssertionError(query + " unexpectedly matched " + candidate);
        passed++;
    }
}
