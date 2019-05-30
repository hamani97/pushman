package com.hamani.suntech.pushman.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.hamani.suntech.pushman.MainActivity;
import com.hamani.suntech.pushman.NoticeDetailActivity;
import com.hamani.suntech.pushman.R;
import com.hamani.suntech.pushman.common.DeviceUuidFactory;
import com.hamani.suntech.pushman.common.Globals;
import com.hamani.suntech.pushman.common.HttpSuntechService;
import com.hamani.suntech.pushman.common.SharedData;
import com.hamani.suntech.pushman.data.RestData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    protected SharedData sd;
    DeviceUuidFactory factory;

    private static int noti_id = 0;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        //Log.e(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //Log.d(TAG, "From: " + remoteMessage.getFrom());

        int reqID = (int) System.currentTimeMillis();

        String idx = "0";
        String kind = "DEVICE";       // DEVICE : from device, NOTICE : from web admin
        String title = "FCM";
        String ndate = "1900-01-01"; //new SimpleDateFormat("yyyy-MM-dd").format(new Date(reqID));
        String ntime = "12:00:00"; //new SimpleDateFormat("HH:mm").format(new Date(reqID));
        String factory_idx = "";
        String line_idx = "";
        String mc_model = "";
        String text = "";
        String msg = "Body";

        if (remoteMessage.getData().size() > 0) {
            //Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            idx = remoteMessage.getData().get("idx");
            kind = remoteMessage.getData().get("kind");
            title = remoteMessage.getData().get("title");
            ndate = remoteMessage.getData().get("ndate");
            ntime = remoteMessage.getData().get("ntime");
            factory_idx = remoteMessage.getData().get("factory_idx");
            line_idx = remoteMessage.getData().get("line_idx");
            mc_model = remoteMessage.getData().get("mc_model");
            text = remoteMessage.getData().get("text");
        }

        if (remoteMessage.getNotification() != null) {
            //Log.d(TAG, "message Notification Body: " + remoteMessage.getNotification().getBody());
            msg = remoteMessage.getNotification().getBody();
        }

        Intent intent = new Intent(this, MainActivity.class);
        Bundle extra = new Bundle();
        extra.putString("idx", idx);

        if (kind != null && kind.equals("NOTICE")) {
            extra.putString("kind", "NOTICE");
            extra.putString("title", title);
        } else {
            extra.putString("kind", "DEVICE");
            extra.putString("ndate", ndate);
            extra.putString("ntime", ntime);
            extra.putString("factory_idx", factory_idx);
            extra.putString("line_idx", line_idx);
            extra.putString("mc_model", mc_model);
            extra.putString("text", text);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtras(extra);

        PendingIntent contentIntent = PendingIntent.getActivity(this, reqID, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(msg)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{1, 1000});

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(noti_id, mBuilder.build());
        noti_id++;

        mBuilder.setContentIntent(contentIntent);
    }

    private void sendRegistrationToServer(String token) {

        sd = new SharedData(getApplicationContext());
        factory = new DeviceUuidFactory(getApplicationContext());

        String device_id = factory.getDeviceUuid().toString();

        // 디바이스 아이디를 생성할 수 없음 (사용할 수 없는 기기)
        if (device_id == null) {
            return;
        }

        // 토큰을 SD 에 저장
        sd.setString(Globals.PREFS_PUSH_TOKEN, token);

        String addr = sd.getString(Globals.PREFS_SERVER_ADDR);
        String port = sd.getString(Globals.PREFS_SERVER_PORT);

        // 서버 주소가 설정되지 않았음. 디바이스를 등록할 수 없음.
        if (addr == null || addr.equals("")) {
            return;
            //addr = Globals.DEFAULT_SERVER_URL;
        }
        if (port != null && !port.equals("") && !port.equals("80")) {
            addr = addr + ":" + port;
        }
        addr = "http://" + addr + "/";

        String brand = Build.BRAND;
        String model = Build.MODEL;
        String version = Build.VERSION.RELEASE;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(addr)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        HttpSuntechService service = retrofit.create(HttpSuntechService.class);

        try {
            final Call<RestData> call = service.addDevice(Globals.PUSH_USER_ADD, device_id, token, brand, model, version, "", "");
            call.enqueue(new Callback<RestData>() {
                @Override
                public void onResponse(Call<RestData> call, Response<RestData> response) {
                    if (response.isSuccessful()) {
                        //Log.e(TAG, response.body().getMsg());
                    } else {
                        //Log.e(TAG, "fail");
                    }
                }

                @Override
                public void onFailure(Call<RestData> call, Throwable t) {
                    t.printStackTrace();
                    //Log.e(TAG, t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
