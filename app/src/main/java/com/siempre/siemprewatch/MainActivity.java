package com.siempre.siemprewatch;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.AlignContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nullable;

public class MainActivity extends WearableActivity {
    private static final String TAG = "MainActivity";
    public static String ACTION_FCM_TOKEN = "ACTION_FCM_TOKEN";
    public static String ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL";
    public static String INCOMING_CALL_IS_GROUP = "INCOMING_CALL_IS_GROUP";
    public static String INCOMING_CALL_ID = "INCOMING_CALL_ID";

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor prefEdit;

    public String refreshURL = "https://securetoken.googleapis.com/v1/token?key=";
    public String accessURL = "http://kumail.org:5006/accessToken";
    public static final String DEFAULT_PHOTOURL = "https://alumni.crg.eu/sites/default/files/default_images/default-picture_0_0.png";

    public String userId;
    public String refreshToken;

    private RelativeLayout parentLayout;
    private TextView mTextView;

    private int status = -1;
    private Context context;

    private RecyclerView mainList;
    private RecyclerView.Adapter mainAdapter;
    private FlexboxLayoutManager layoutManager;
    private ArrayList<DataModel> listItems = new ArrayList<>();
    private HashMap<String, Integer> friends;

    Ringtone ringtone;

    Vibrator vibrator;

    FirebaseFirestore db;
    FirebaseStorage storage;

    private boolean isReceiverRegistered;
    VoiceBroadcastReceiver voiceBroadcastReceiver;

    Timer timer;
    private LocalAudioTrack localAudioTrack;
    Room activeRoom;
    Room.Listener roomListener = roomListener();

    public static final int NUM_RINGING_SECONDS = 10;

    private static final String LOCAL_AUDIO_TRACK_NAME = "mic";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        prefEdit = sharedPreferences.edit();
        userId = sharedPreferences.getString("userId", "");
        refreshToken = sharedPreferences.getString("refreshToken", "");


        parentLayout = findViewById(R.id.parentLayout);

        mainList = findViewById(R.id.mainList);
        layoutManager = new FlexboxLayoutManager(getApplicationContext());
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        layoutManager.setAlignItems(AlignItems.CENTER);
        mainList.setLayoutManager(layoutManager);
        mainAdapter = new MainAdapter(listItems, this);
        mainList.setAdapter(mainAdapter);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        context = getApplicationContext();

        mTextView = (TextView) findViewById(R.id.text);

        Log.d(TAG, "userId: " + userId);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Found that record_audio permission not granted");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

        db.collection("users-watch-mirror").document(userId).get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    Map<String, Object> data = documentSnapshot.getData();
                    mTextView.setText((String)data.get("name"));
                    status = ((Long)data.get("status")).intValue();
                    parentLayout.setBackgroundColor(statusToColor(status));
                    mainList.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "mainlist in onClick");
                            vibrate(1);
                            changeStatus();
                        }
                    });
                    parentLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "parentlayout in onClick");
                            vibrate(1);
                            changeStatus();
                        }
                    });

                    db.collection("users-watch-mirror").document(userId)
                        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.d(TAG, "Error on listening to snapshot");
                                    return;
                                }

                                if (documentSnapshot.exists()) {
                                    Map<String, Object> data = documentSnapshot.getData();
                                    status = ((Long) data.get("status")).intValue();
                                    boolean inCall = (Boolean) data.get("inCall");
                                    if (inCall) {
                                        parentLayout.setBackgroundColor(statusToColor(3));
                                    } else {
                                        parentLayout.setBackgroundColor(statusToColor(status));
                                    }
                                }
                            }
                        });
                }
            });

        friends = new HashMap<>();
        db.collection("users-watch-mirror")
            .whereGreaterThanOrEqualTo("friends." + userId, "")
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.d(TAG, "friend update listen failed ", e);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        int friendStatus = ((Long) doc.getData().get("status")).intValue();
                        Integer index = friends.get(doc.getId());
                        if (index != null) {
                            User friend = (User) listItems.get(index);
                            friend.status = friendStatus;
                            friend.inCall = (Boolean) doc.getData().get("inCall");
                            mainAdapter.notifyItemChanged(index);
                            vibrate(0);
                        } else if (listItems.size() < 12){
                            User friend = new User();
                            String friendName = (String) doc.getData().get("name");

                            friend.id = doc.getId();
                            friend.name = friendName;
                            friend.status = friendStatus;
                            friend.photoURL = DEFAULT_PHOTOURL;
                            friend.inCall = (Boolean) doc.getData().get("inCall");
                            getPhotoURL((String) doc.getData().get("photoPath"), friend);
                            listItems.add(friend);
                            friends.put(doc.getId(), listItems.size() - 1);
                            mainAdapter.notifyItemChanged(listItems.size() - 1);
                        }
                    }
                }
            });

        Uri ringtoneURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(context, ringtoneURI);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        voiceBroadcastReceiver = new VoiceBroadcastReceiver();

        Intent intent = getIntent();
        if (intent.getAction() != null && intent.getAction().equals(ACTION_INCOMING_CALL)) {
            handleIntent(intent);
        }

        writeAccessToken();

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        registerReceiver();
        localAudioTrack = LocalAudioTrack.create(this, true, LOCAL_AUDIO_TRACK_NAME);

        db.collection("new-users").document(userId).update("watchIn", true)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess setting watchin");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, e.getMessage());
                }
            });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        unregisterReceiver();

        if (localAudioTrack != null) {
            localAudioTrack.release();
            localAudioTrack = null;
        }

        db.collection("new-users").document(userId).update("watchIn", false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void writeAccessToken() {
        String fcmToken = FirebaseInstanceId.getInstance().getToken();
        db.collection("new-users").document(userId).update("FCMTokenWatch", fcmToken).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess writing fcmtokenwatch");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure writing fcmtokenwatch");
            }
        });
    }

    private void handleIntent(Intent intent) {
        Log.d(TAG, "handleintent");
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "handleintent getaction: " + intent.getAction());
            if (intent.getAction().equals(ACTION_FCM_TOKEN)) {
                writeAccessToken();
            } else if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
                Log.d(TAG, "handleintent incoming call status: " + status);
                if (status == 0) {
                    answer(intent.getStringExtra(INCOMING_CALL_ID), intent.getBooleanExtra(INCOMING_CALL_IS_GROUP, false));
                }
            }
        }
    }

    private int statusToColor(int status) {
        return Constants.backgroundColors[status];
    }

    private void changeStatus() {
        final int newStatus = (status + 1) % 2;
        Log.d(TAG, "newstatus: " + newStatus);
        Log.d(TAG, "userId: " + userId);
        db.collection("new-users").document(userId).update("status", newStatus)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "changestatus onSuccess");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "changestatus failure, ", e);
                }
            });
    }

    public void getPhotoURL(String path, final User user) {
        StorageReference storageReference = storage.getReference().child(path);
        storageReference.getDownloadUrl()
            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    user.photoURL = uri.toString();
                    mainAdapter.notifyItemRangeChanged(0, listItems.size());
                }
            });
    }

    public void vibrate(int type) {
        if (type == 0) {
            // Small buzz
            long[] pattern = {0, 300};
            vibrator.vibrate(pattern, -1);
        } else if (type == 1) {
            // Bigger buzz
            long[] pattern = {0, 600};
            vibrator.vibrate(pattern, -1);
        } else {
            // Double buzz
            long[] pattern = {0, 500, 150, 300};
            vibrator.vibrate(pattern, -1);
        }
    }

    public void answer(String id, boolean isGroup) {
        Log.d(TAG, "answer");
        try {
            String url = String.format("%s?room=%s&call=%s&jwt=%s",
                    accessURL,
                    getRoomName(id, isGroup),
                    "",
                    getIdToken());
            String accessToken = Ion.with(this).load(url).asString().get();
            joinCall(id, accessToken, isGroup);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
    }

    public void onClickFriend(String friendID) {
        if (activeRoom != null) {
            activeRoom.disconnect();
        } else {
            makeCall(friendID);
        }
    }

    public void makeCall(String friendID) {
        try {
            String url = String.format("%s?room=%s&call=%s&jwt=%s",
                    accessURL,
                    getRoomName(friendID, false),
                    friendID,
                    getIdToken());
            String accessToken = Ion.with(this).load(url).asString().get();
            joinCall(friendID, accessToken, false);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
    }

    public void joinCall(String id, String accessToken, boolean isGroup) {
        Log.d(TAG, "in joinCall");
        ConnectOptions.Builder connectOptionsBuilder = new ConnectOptions.Builder(accessToken)
                .roomName(getRoomName(id, isGroup));
        if (localAudioTrack != null) {
            connectOptionsBuilder.audioTracks(Collections.singletonList(localAudioTrack));
        }
        Video.connect(this, connectOptionsBuilder.build(), roomListener);
    }

    private String getRoomName(String id, boolean isGroup) {
        if (isGroup) {
            return "group:" + id;
        } else {
            if (userId.compareTo(id) < 0) {
                return "private:" + userId + ":" + id;
            } else {
                return "private:" + id + ":" + userId;
            }
        }
    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_INCOMING_CALL);
            intentFilter.addAction(ACTION_FCM_TOKEN);
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    voiceBroadcastReceiver, intentFilter
            );
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(voiceBroadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    private class VoiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_INCOMING_CALL)) {
                handleIntent(intent);
            }
        }
    }

    private String getIdToken() {
        JsonObject jsonInput = new JsonObject();
        jsonInput.addProperty("grant_type", "refresh_token");
        jsonInput.addProperty("refresh_token", refreshToken);
        try {
            JsonObject refreshResponse = Ion.with(this)
                    .load(refreshURL + Constants.API_KEY)
                    .setJsonObjectBody(jsonInput)
                    .asJsonObject()
                    .get();
            if (refreshResponse.has("error")) {
                Log.d(TAG, "getIdToken" +  refreshResponse.get("error").toString());
                prefEdit.putString("refreshToken", "");
                prefEdit.commit();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return null;
            }
            refreshToken = Utilities.removeQuotes(refreshResponse.get("refresh_token").toString());
            return Utilities.removeQuotes(refreshResponse.get("id_token").toString());
        } catch (Exception e) {
            Log.d(TAG, "getIdToken" +  e.getMessage());
            prefEdit.putString("refreshToken", "");
            prefEdit.commit();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return null;
        }
    }

    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                Log.d(TAG, "Connected to " + room.getName());
                if (room.getRemoteParticipants().size() > 0) {
                } else {
                    timer = new Timer();
                    timer.schedule(new GiveupTask(), NUM_RINGING_SECONDS * 1000);
                }
                activeRoom = room;
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                Log.d(TAG, "Failed to connect ", e);
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                Log.d(TAG, "in on disconnected");
                activeRoom = null;
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant participant) {
                Log.d(TAG, "in onparticipantconnected");
            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant participant) {
                Log.d(TAG, "in onparticipantdisconnected");
                if (room.getRemoteParticipants().size() == 0) {
                    room.disconnect();
                }
            }

            @Override
            public void onRecordingStarted(Room room) {
                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    class GiveupTask extends TimerTask {
        public void run() {
            if (activeRoom != null) {
                if (activeRoom.getRemoteParticipants().size() == 0) {
                    activeRoom.disconnect();
                }
            }
            timer.cancel();
        }
    }

}
