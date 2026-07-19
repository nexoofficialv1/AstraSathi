package com.astratechnologies.astrasathi;

public interface BrokerGateway {
    interface Callback { void onResult(boolean success, String orderId, String message); }
    String brokerName();
    boolean isConnected();
    void validate(TradeOrder order, Callback callback);
    void submitAfterUserAuthentication(TradeOrder order, String clientNonce, Callback callback);
}
