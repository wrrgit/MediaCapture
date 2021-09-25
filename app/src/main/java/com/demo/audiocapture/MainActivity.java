package com.demo.audiocapture;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.demo.audiocapture.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * https://blog.csdn.net/qq_38376757/article/details/105449458
 * https://blog.csdn.net/qq_35928566/article/details/84935994
 *  https://www.jianshu.com/p/66728b95baaa
 *  https://blog.csdn.net/zhangzhuo1024/article/details/100065474
 *  https://www.jianshu.com/p/8e3026bb69d2
 *  https://blog.csdn.net/qq_35928566/article/details/84858924
 *  https://blog.csdn.net/u011046184/article/details/90543916
 *  https://developer.android.google.cn/reference/kotlin/android/media/AudioPlaybackCaptureConfiguration
 */
public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private static final int AUDIO_REQUEST_CODE = 101;
    private static final int RECORD_REQUEST_CODE = 102;
    private MediaProjectionManager mediaProjectionManager;
    private static final int BUFFER_SIZE = 4096;
    private byte[] mBuffer;
    AudioTrack audioTrack;
    /**
     * RECORD_AUDIO     音频权限
     * WRITE_EXTERNAL_STORAGE 写入权限
     * CAMERA        相机权限
     */
    public void checkPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
        };
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 200 + i);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //关键代码
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View rootView = binding.getRoot();
        setContentView(rootView);
        checkPermissions();

        mBuffer = new byte[BUFFER_SIZE];

        binding.btnStartCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                //开启录屏请求intent
                Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, AUDIO_REQUEST_CODE);
            }
        });


        binding.btnStopCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this, AudioCaptureService.class));
            }
        });

        binding.btnStartPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        File file = new File("/sdcard/123.pcm");
                        playAudio(file);
                    }
                });
            }
        });

        binding.btnStopPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioTrack.release();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.i(TAG, "onActivityResult: resultCode:"+resultCode);
        Log.i(TAG, "onActivityResult: SDK_INT:"+Build.VERSION.SDK_INT);
        if (requestCode == AUDIO_REQUEST_CODE && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(MainActivity.this,AudioCaptureService.class);
                intent.putExtra("resultCode", resultCode);
                intent.putExtra("data", data);
                startForegroundService(intent);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * https://blog.csdn.net/qq_38376757/article/details/105449458
     * @param audioFile
     */

    private void playAudio(File audioFile) {
        Log.d(TAG, "play audio");
        int streamType = AudioManager.STREAM_MUSIC;
        int simpleRate = 16000;//44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int encodingPcm16bit = AudioFormat.ENCODING_PCM_16BIT;
        int audioMode = AudioTrack.MODE_STREAM;

        int minBufferSize = AudioTrack.getMinBufferSize(simpleRate, channelConfig, encodingPcm16bit);

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(16000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build();

//        audioTrack = new AudioTrack(streamType, simpleRate, channelConfig, encodingPcm16bit,
//                Math.max(minBufferSize, BUFFER_SIZE), audioMode);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();


        audioTrack = new AudioTrack.Builder()
                .setAudioFormat(audioFormat)
                .setAudioAttributes(audioAttributes)
                .setTransferMode(audioMode)
                .setBufferSizeInBytes(Math.max(minBufferSize, BUFFER_SIZE))
                .setSessionId(1)
                .build();


        audioTrack.play();
        Log.d(TAG, minBufferSize + " is the min buffer size , " + BUFFER_SIZE + " is the read buffer size");

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(audioFile);
            int read;
            while ((read = inputStream.read(mBuffer)) > 0) {
                Log.d("MainActivity", "lu yin kaishi11111");
                audioTrack.write(mBuffer, 0, read);
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioTrack.release();
        binding = null;
    }
}