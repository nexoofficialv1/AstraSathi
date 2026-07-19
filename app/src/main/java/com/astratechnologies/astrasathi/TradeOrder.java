package com.astratechnologies.astrasathi;

public final class TradeOrder {
    public enum Side { BUY, SELL }
    public enum OrderType { MARKET, LIMIT }
    public enum Product { DELIVERY, INTRADAY }

    public final String raw;
    public final String symbol;
    public final Side side;
    public final int quantity;
    public final OrderType orderType;
    public final double limitPrice;
    public final Product product;
    public final String error;

    public TradeOrder(String raw, String symbol, Side side, int quantity, OrderType orderType,
                      double limitPrice, Product product, String error) {
        this.raw = raw; this.symbol = symbol; this.side = side; this.quantity = quantity;
        this.orderType = orderType; this.limitPrice = limitPrice; this.product = product;
        this.error = error == null ? "" : error;
    }

    public boolean isValid() { return error.isEmpty(); }
}
