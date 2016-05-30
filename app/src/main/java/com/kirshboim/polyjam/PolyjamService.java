package com.kirshboim.polyjam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PolyjamService extends Service {

    public interface Events {
        String Register = PolyjamService.class.getSimpleName() + "#Register";
        String RegistrationCompleted = PolyjamService.class.getSimpleName() + "#RegistrationCompleted";
        String RegistrationFailed = PolyjamService.class.getSimpleName() + "#RegistrationFailed";
        String PlayData = PolyjamService.class.getSimpleName() + "#PlayData";
        String GetPlayData = PolyjamService.class.getSimpleName() + "#GetPlayData";
    }

    private PolyjamHttpClient client = PolyjamHttpClient.instance;
    private int failedStatusCounter = 0;
    private ExecutorService executorRegister = Executors.newSingleThreadExecutor();
    private ExecutorService executorStatus = Executors.newSingleThreadExecutor();
    private Future<?> statusPollingTask = null;
    private BroadcastReceiver receiver;
    private String serverIp = null;
    private int port = -1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onCreate() {

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (Events.Register.equals(action)) {
                    final String name = intent.getStringExtra("name");

                    register(name);

                } else if (Events.GetPlayData.equals(action)) {

                    Intent intentPlayData = new Intent(Events.PlayData);
                    intentPlayData.putExtra("port", port);
                    intentPlayData.putExtra("ip", serverIp);
                    sendBroadcast(intentPlayData);
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(Events.Register));
        registerReceiver(receiver, new IntentFilter(Events.GetPlayData));
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);

        stopStatusPolling();
        shutdown(executorStatus);
        shutdown(executorRegister);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    private void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void register(final String name) {
        executorRegister.submit(new Runnable() {
            @Override
            public void run() {
                boolean success = client.register(name);
                if (success) {
                    PolyjamService.this.sendBroadcast(new Intent(Events.RegistrationCompleted));
                    startStatusPolling();
                    if (port != -1) {
                        port = -1;
                        serverIp = null;
                    }

                } else {
                    PolyjamService.this.sendBroadcast(new Intent(Events.RegistrationFailed));
                    stopStatusPolling();
                }
            }
        });
    }

    private void startStatusPolling() {
        if (statusPollingTask != null) {
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(PolyjamService.this)
                        .setSmallIcon(R.drawable.logo_hand)
                        .setContentTitle("Polyjam: running");

        builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
        Notification notification = builder.build();
        startForeground(1223, notification);

        statusPollingTask = executorStatus.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String status = client.status();

                    if (status == null) {
                        // something went wrong - try 3 times and give up
                        if (failedStatusCounter++ >= 3) {
                            failedStatusCounter = 0;
                            status = "wait";
                        } else {
                            resubmit();
                            return;
                        }
                    }

                    boolean waitResponse = StatusParser.isWait(status);
                    boolean startPlayActivity = (waitResponse == false) && (port == -1);

                    if (waitResponse) {
                        port = -1;
                        serverIp = null;

                    } else {
                        port = StatusParser.parsePort(status);
                        serverIp = StatusParser.parseIp(status);
                    }

                    if (startPlayActivity) {
                        NotificationCompat.Builder builder =
                                new NotificationCompat.Builder(PolyjamService.this)
                                        .setSmallIcon(R.drawable.logo_hand)
                                        .setContentTitle("Polyjam: It's your turn!");

                        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        builder.setSound(alarmSound);
                        builder.setVibrate(new long[]{0, 500, 500, 500, 500});

                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                        Notification notification = builder.build();
                        notificationManager.notify(0, notification);

                        Intent intent = new Intent(PolyjamService.this, PrePlayActivity.class);
                        intent.putExtra("port", port - 5550);
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                resubmit();
            }

            private void resubmit() {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                executorStatus.submit(this);
            }
        });
    }

    private void stopStatusPolling() {
        stopForeground(true);

        if (statusPollingTask != null) {
            statusPollingTask.cancel(true);
            statusPollingTask = null;
        }
    }

    private static class StatusParser {

        public static boolean isWait(String status) {
            return status.equals("wait");
        }

        public static int parsePort(String status) {
            return Integer.parseInt(status.substring(0, 4));
        }

        public static String parseIp(String status) {
            return status.substring(5).trim();
        }
    }
}
