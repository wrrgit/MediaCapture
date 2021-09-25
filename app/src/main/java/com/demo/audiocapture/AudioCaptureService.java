package com.demo.audiocapture;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.orhanobut.logger.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * //https://blog.csdn.net/zhangzhuo1024/article/details/100065474
 */
public class AudioCaptureService extends Service {

    private String TAG = "AudioCaptureService";
    private boolean isRun = true;

    public AudioCaptureService() {
    }

    private AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration;
    private int resultCode;
    private Intent resultData;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private int bufferSize;
    private static final int RECORD_AUDIO_BUFFER_TIMES = 1;
    private FileOutputStream fos = null;

    private String NOTIFICATION_CHANNEL_ID = "AudioCaptureService_nofity";
    private String NOTIFICATION_CHANNEL_NAME = "AudioCaptureService";
    private String NOTIFICATION_CHANNEL_DESC = "AudioCaptureService";
    private int NOTIFICATION_ID = 1000;
    private static final String NOTIFICATION_TICKER = "RecorderApp";


    @Override
    public void onCreate() {
        super.onCreate();
        createNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRun = true;
        isRun = true;
        resultCode = intent.getIntExtra("resultCode", -1);
        resultData = intent.getParcelableExtra("data");
        Log.i(TAG, "onStartCommand: " + resultCode);
        Log.i(TAG, "onStartCommand: " + resultData);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
        Log.i(TAG, "onStartCommand: " + mediaProjection);
        AudioPlaybackCaptureConfiguration.Builder builder = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection);
        builder.addMatchingUsage(AudioAttributes.USAGE_MEDIA);//多媒体
        builder.addMatchingUsage(AudioAttributes.USAGE_ALARM);//闹铃
        builder.addMatchingUsage(AudioAttributes.USAGE_GAME);//游戏
        audioPlaybackCaptureConfiguration = builder.build();
        generateAudioRecord();
        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void generateAudioRecord() {
        if (ActivityCompat.checkSelfPermission(AudioCaptureService.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);


        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(16000)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build();


        AudioRecord audioRecord = new AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(4096)
                .setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration).build();
        audioRecord.startRecording();

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    fos = new FileOutputStream("/sdcard/123.pcm");//getTempFilePath()
                    byte[] byteBuffer = new byte[bufferSize];
                    while (isRun) {
                        int end = audioRecord.read(byteBuffer, 0, byteBuffer.length);
                        Log.i(TAG, "run: end:"+end);
                        fos.write(byteBuffer, 0, end);
                        fos.flush();
                    }
                    audioRecord.stop();
                } catch (Exception e) {
                    Logger.e(e, TAG, e.getMessage());
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    /**
     * 根据当前的时间生成相应的文件名
     * 实例 audio_20210925_13_15_12
     */
    private String getTempFilePath() {
        String fileDir = String.format(Locale.getDefault(), "%s/record/", Environment.getExternalStorageDirectory().getAbsolutePath());
        if (!FileUtils.createOrExistsDir(fileDir)) {
            Logger.e(TAG, "文件夹创建失败：%s", fileDir);
        }
        String fileName = String.format(Locale.getDefault(), "audio_%s", getTimeId());
        return String.format(Locale.getDefault(), "%s%s.pcm", fileDir, fileName);
    }

    public String getTimeId() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.SIMPLIFIED_CHINESE);
        return simpleDateFormat.format(new Date(System.currentTimeMillis()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRun = false;
    }

    public void createNotification() {
        Log.i(TAG, "notification: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Call Start foreground with notification
            Intent notificationIntent = new Intent(this, AudioCaptureService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Starting Service")
                    .setContentText("Starting monitoring service")
                    .setTicker(NOTIFICATION_TICKER)
                    .setContentIntent(pendingIntent);
            Notification notification = notificationBuilder.build();
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            startForeground(NOTIFICATION_ID, notification);
            //notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
}