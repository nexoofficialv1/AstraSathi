package com.astratechnologies.astrasathi;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Locale;

public class FinancialActionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financial_action);
        String raw = getIntent().getStringExtra("trade_command");
        if (raw == null) raw = "";
        TradeOrder order = new TradeOrderParser().parse(raw);
        TextView validation = findViewById(R.id.tradeValidationText);
        TextView summary = findViewById(R.id.tradeSummaryText);
        TextView broker = findViewById(R.id.brokerStatusText);
        Button approve = findViewById(R.id.approveTradeButton);

        if (!order.isValid()) {
            validation.setText("● Order অসম্পূর্ণ: " + order.error);
            validation.setTextColor(getColor(R.color.danger));
        } else {
            validation.setText("● Voice order-এর প্রয়োজনীয় fields পাওয়া গেছে");
            validation.setTextColor(getColor(R.color.forest));
        }
        String price = order.orderType == TradeOrder.OrderType.MARKET ? "Market price"
                : NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(order.limitPrice);
        summary.setText("Symbol/Share: " + empty(order.symbol) + "\n"
                + "Action: " + order.side + "\nQuantity: " + (order.quantity > 0 ? order.quantity : "—") + "\n"
                + "Order type: " + order.orderType + "\nPrice: " + price + "\nProduct: " + order.product + "\n\n"
                + "Original voice order:\n“" + raw + "”");
        BrokerGateway gateway = BrokerRegistry.connectedGateway();
        if (gateway == null || !gateway.isConnected()) {
            broker.setText("○ কোনো official broker API adapter connected নয়। Real order পাঠানো বন্ধ আছে।");
            broker.setTextColor(getColor(R.color.danger));
            approve.setEnabled(false);
            approve.setAlpha(0.5f);
        } else {
            broker.setText("● Connected broker: " + gateway.brokerName());
            broker.setTextColor(getColor(R.color.forest));
            approve.setEnabled(order.isValid());
        }
        findViewById(R.id.connectBrokerButton).setOnClickListener(v -> Toast.makeText(this,
                "নির্দিষ্ট broker-এর official API documentation ও credentials দিয়ে adapter যোগ করতে হবে।",
                Toast.LENGTH_LONG).show());
        approve.setOnClickListener(v -> Toast.makeText(this,
                "Submission-এর আগে device credential/biometric confirmation বাধ্যতামূলক হবে।",
                Toast.LENGTH_LONG).show());
        findViewById(R.id.closeFinancialButton).setOnClickListener(v -> finish());
    }

    private String empty(String value) { return value == null || value.isEmpty() ? "—" : value; }
}
