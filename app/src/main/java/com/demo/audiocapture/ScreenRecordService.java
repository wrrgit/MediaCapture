package com.demo.audiocapture;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class ScreenRecordService extends Service {

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;

    private String videoPath = "";

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
    }

    public class ScreenRecordBinder extends Binder {
        public ScreenRecordService getScreenRecordService(){
            return ScreenRecordService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ScreenRecordBinder();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }
        initRecorder();
        createVirtualDisplay();
        try {
            mediaRecorder.start();
            running = true;
            return true;
        }catch (IllegalStateException e){
            e.printStackTrace();
            Toast.makeText(this,"start ????????????????????????",Toast.LENGTH_SHORT).show();
            running = false;
            return false;
        }
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        try{
            mediaRecorder.stop();
            mediaRecorder.reset();
            virtualDisplay.release();
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(ScreenRecordService.this, "????????????,????????????", Toast.LENGTH_SHORT).show();
            return false;
        }
        Toast.makeText(ScreenRecordService.this, "???????????????????????????", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void createVirtualDisplay() {
        try{
            virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
        }catch (Exception e){
            Toast.makeText(this,"virtualDisplay ???????????????",Toast.LENGTH_SHORT).show();
        }
    }

    private void initRecorder() {
        mediaRecorder = new MediaRecorder();
        //??????????????????
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //??????????????????
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //??????????????????
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //????????????????????????
        videoPath = getSaveDirectory() + System.currentTimeMillis() + ".mp4";
        mediaRecorder.setOutputFile(videoPath);
        //??????????????????
        mediaRecorder.setVideoSize(width, height);
        //??????????????????
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //??????????????????
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        //????????????
        mediaRecorder.setVideoEncodingBitRate(2 * 1920 * 1080);
        mediaRecorder.setVideoFrameRate(18);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,"prepare????????????????????????",Toast.LENGTH_SHORT).show();
        }
    }

    public String getSaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + "??????????????????/" + "/";
            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }
            return rootDir;
        } else {
            return null;
        }
    }

}
