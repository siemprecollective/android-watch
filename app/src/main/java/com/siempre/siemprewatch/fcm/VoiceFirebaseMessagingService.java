package com.siempre.siemprewatch.fcm;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.siempre.siemprewatch.MainActivity;

import java.util.Map;

public class VoiceFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoiceFCMService";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received onMessageReceived()");
        Log.d(TAG, "Bundle data: " + remoteMessage.getData());
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            final Map<String, String> data = remoteMessage.getData();
            if (data.get("type") != null && data.get("type").equals("status_change")) {
                String status = data.get("status");
                String name = data.get("name");
                sendStatusChangeToService(status, name);
            } else if (data.get("type").equals("incoming_call")) {
                Log.d(TAG, "Incoming video call from fcm");
                sendIncomingCallToActivity(data.get("id"), data.get("group_id"));
            }
        }
    }

    private void sendStatusChangeToService(String status, String name) {
        // Do nothing for now
    }

    private void sendIncomingCallToActivity(String id, String groupId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_INCOMING_CALL);
        intent.putExtra(MainActivity.INCOMING_CALL_IS_GROUP, !groupId.equals(""));
        if (!groupId.equals("")) {
            intent.putExtra(MainActivity.INCOMING_CALL_ID, groupId);
        } else {
            intent.putExtra(MainActivity.INCOMING_CALL_ID, id);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
