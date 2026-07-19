package com.astratechnologies.astrasathi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CallActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        SathiInCallService service = SathiInCallService.get();
        if (service == null) return;
        if (SathiInCallService.ACTION_ANSWER.equals(intent.getAction())) service.answerCurrent();
        else if (SathiInCallService.ACTION_END.equals(intent.getAction())) service.endCurrent();
    }
}
