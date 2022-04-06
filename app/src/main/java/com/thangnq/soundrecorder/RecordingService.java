package com.thangnq.soundrecorder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.thangnq.soundrecorder.fragments.RecordFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RecordingService extends Service {
    private static final String CHANNEL_ID = "channelID";

    private static final String LOG_TAG = "RecordingService";

    private String fileName;
    private String filePath;

    private MediaRecorder mediaRecorder = null;

    private DatabaseHelper databaseHelper;

    private long startingTimeMillis = 0;
    private long elapsedMillis = 0;
    private int elapsedSeconds = 0;
    private OnTimerChangedListener onTimerChangedListener = null;
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat timerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private Timer timer = null;
    private TimerTask incrementTimerTask = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public interface OnTimerChangedListener {
        void onTimerChanged(int seconds);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = new DatabaseHelper(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRecording();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mediaRecorder != null) {
            stopRecording();
        }
        super.onDestroy();
    }

    public void startRecording() {
        setFileNameAndPath();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setAudioChannels(1);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            startingTimeMillis = System.currentTimeMillis();

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    public void setFileNameAndPath() {
        int count = 0;
        File file;

        do {
            count++;

            fileName = getString(R.string.default_file_name)
                    + "_" + (databaseHelper.getCount() + count) + ".mp3";
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            filePath += "/SoundRecorder/" + fileName;

            file = new File(filePath);
        } while (file.exists() && !file.isDirectory());
    }

    public void stopRecording() {
        try {
            mediaRecorder.stop();
            elapsedMillis = (System.currentTimeMillis() - startingTimeMillis);
            mediaRecorder.reset();
            mediaRecorder.release();
        }catch (IllegalStateException e){
            Log.e(LOG_TAG, "exception", e);
        }

        Toast.makeText(this, getString(R.string.toast_recording_finish) + " " + filePath, Toast.LENGTH_LONG).show();

        //remove notification
        if (incrementTimerTask != null) {
            incrementTimerTask.cancel();
            incrementTimerTask = null;
        }

        mediaRecorder = null;

        try {
            databaseHelper.addRecording(fileName, filePath, elapsedMillis);

        } catch (Exception e) {
            Log.e(LOG_TAG, "exception", e);
        }
    }

    private void startTimer() {
        timer = new Timer();
        incrementTimerTask = new TimerTask() {
            @Override
            public void run() {
                elapsedSeconds++;
                if (onTimerChangedListener != null)
                    onTimerChangedListener.onTimerChanged(elapsedSeconds);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(1, createNotification());
            }
        };
        timer.scheduleAtFixedRate(incrementTimerTask, 1000, 1000);
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_mic)
                        .setContentTitle(getString(R.string.notification_recording))
                        .setContentText(timerFormat.format(elapsedSeconds * 1000))
                        .setOngoing(true);

        builder.setContentIntent(PendingIntent.getActivities(getApplicationContext(), 0,
                new Intent[]{new Intent(getApplicationContext(), MainActivity.class)}, 0));

        return builder.build();
    }
}
