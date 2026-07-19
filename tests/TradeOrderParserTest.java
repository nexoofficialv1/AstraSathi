import com.astratechnologies.astrasathi.TradeOrder;
import com.astratechnologies.astrasathi.TradeOrderParser;

public class TradeOrderParserTest {
    public static void main(String[] args) {
        TradeOrderParser parser = new TradeOrderParser();
        TradeOrder market = parser.parse("TCS-এর ১০টা শেয়ার মার্কেট প্রাইসে কিনো");
        check(market.isValid(), market.error);
        check("TCS".equals(market.symbol), "TCS symbol failed: " + market.symbol);
        check(market.quantity == 10 && market.side == TradeOrder.Side.BUY, "Market quantity/side failed");
        check(market.orderType == TradeOrder.OrderType.MARKET, "Market type failed");

        TradeOrder limit = parser.parse("রিলায়েন্সের ৫টি শেয়ার ২৫০০ টাকায় লিমিট দামে কিনো");
        check(limit.isValid(), limit.error);
        check(limit.quantity == 5 && limit.limitPrice == 2500, "Limit fields failed");
        check(limit.orderType == TradeOrder.OrderType.LIMIT, "Limit type failed");
        System.out.println("TradeOrderParser-এর সব পরীক্ষা সফল হয়েছে।");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
