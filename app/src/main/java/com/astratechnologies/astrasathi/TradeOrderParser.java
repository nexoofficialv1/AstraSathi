package com.astratechnologies.astrasathi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TradeOrderParser {
    public TradeOrder parse(String raw) {
        String text = BengaliText.normalize(raw);
        TradeOrder.Side side = containsAny(text, "বেচো", "বিক্রি করো", "sell")
                ? TradeOrder.Side.SELL : TradeOrder.Side.BUY;
        int quantity = readQuantity(text);
        String symbol = readSymbol(text);
        boolean market = containsAny(text, "মার্কেট প্রাইস", "মার্কেট অর্ডার", "market");
        double price = readPrice(text);
        TradeOrder.OrderType orderType = market ? TradeOrder.OrderType.MARKET : TradeOrder.OrderType.LIMIT;
        TradeOrder.Product product = containsAny(text, "ইন্ট্রাডে", "intraday", "mis")
                ? TradeOrder.Product.INTRADAY : TradeOrder.Product.DELIVERY;
        String error = "";
        if (symbol.isEmpty()) error = "কোন share/symbol তা বুঝতে পারিনি।";
        else if (quantity <= 0) error = "Quantity বুঝতে পারিনি।";
        else if (orderType == TradeOrder.OrderType.LIMIT && price <= 0)
            error = "Limit order-এর price বলুন অথবা Market order বলুন।";
        return new TradeOrder(raw, symbol, side, quantity, orderType, price, product, error);
    }

    private int readQuantity(String text) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(?:টা|টি)?\\s*(?:শেয়ার|শেয়ার|স্টক)").matcher(text);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        return -1;
    }

    private String readSymbol(String text) {
        Matcher before = Pattern.compile("^(.+?)(?:-এর|এর)?\\s+\\d+\\s*(?:টা|টি)?\\s*(?:শেয়ার|শেয়ার|স্টক)").matcher(text);
        if (before.find()) return cleanSymbol(before.group(1));
        Matcher after = Pattern.compile("\\d+\\s*(?:টা|টি)?\\s*(?:শেয়ার|শেয়ার|স্টক)\\s+(.+?)\\s+(?:কিনো|কিনে দাও|বেচো|বিক্রি করো|buy|sell)").matcher(text);
        return after.find() ? cleanSymbol(after.group(1)) : "";
    }

    private double readPrice(String text) {
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:টাকায়|টাকায়|টাকা|দামে|price|at)").matcher(text);
        double found = -1;
        while (matcher.find()) found = Double.parseDouble(matcher.group(1));
        return found;
    }

    private String cleanSymbol(String value) {
        return value.replaceAll("^(আমার|একটা|একটি)\\s+", "")
                .replaceAll("\\s*(শেয়ার|শেয়ার|স্টক)$", "").trim().toUpperCase();
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }
}
