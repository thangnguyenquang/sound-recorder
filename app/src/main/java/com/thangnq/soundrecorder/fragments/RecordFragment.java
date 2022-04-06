package com.thangnq.soundrecorder.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thangnq.soundrecorder.R;
import com.thangnq.soundrecorder.RecordingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecordFragment extends Fragment {
    private static int PERMISSIONS_REQUEST_RECORD_AUDIO = 10;

    private static final String ARG_POSITION = "position";

    private boolean isWritePermissionGranted = false;
    private boolean isRecordPermissionGranted = false;

    private int position;

    private Context context;

    private Button btnPause;
    private Button btnSave;
    private FloatingActionButton fabRecordButton;
    private TextView tvRecordingPrompt;
    private int recordPromptCount = 0;

    private boolean startRecording = true;
    private boolean pauseRecording = true;

    private Chronometer chronometer;
    //lưu trữ thời gian khi nhấp vào nút tạm dừng
    long timeWhenPaused = 0;

    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    if(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) != null){
                        isWritePermissionGranted = result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }

                    if(result.get(Manifest.permission.RECORD_AUDIO) != null){
                        isRecordPermissionGranted = result.get(Manifest.permission.RECORD_AUDIO);
                    }
                }
            });


    public static RecordFragment newInstance(int position) {
        RecordFragment recordFragment = new RecordFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_POSITION, position);
        recordFragment.setArguments(bundle);

        return recordFragment;
    }

    public RecordFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);

        requestPermission();
    }

    private void requestPermission() {
        isWritePermissionGranted =ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        isRecordPermissionGranted =ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequest = new ArrayList<String>();

        if(!isWritePermissionGranted) {
            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(!isRecordPermissionGranted) {
            permissionRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if(!permissionRequest.isEmpty()){
            requestPermissionLauncher.launch(permissionRequest.toArray(new String[0]));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);

        chronometer = recordView.findViewById(R.id.chronometer);
        //cập nhật văn bản nhắc ghi âm
        tvRecordingPrompt = recordView.findViewById(R.id.recording_status_text);

        fabRecordButton = recordView.findViewById(R.id.btnRecord);
        fabRecordButton.setBackgroundColor(getResources().getColor(R.color.primary));

        fabRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRecord(startRecording);
                startRecording = !startRecording;
            }
        });

        btnPause = recordView.findViewById(R.id.btnPause);
        // ẩn nút tạm dừng trước khi bắt đầu ghi
        btnPause.setVisibility(View.GONE);
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPauseRecord(pauseRecording);
                pauseRecording = !pauseRecording;
            }
        });
        return recordView;
    }


    @SuppressLint("SetTextI18n")
    private void onRecord(boolean start) {
        Intent intent = new Intent(getActivity(), RecordingService.class);

        if (start) {
            // start recording
            fabRecordButton.setImageResource(R.drawable.ic_stop);
            //pauseButton.setVisibility(View.VISIBLE);
            Toast.makeText(getActivity(), R.string.toast_recording_start, Toast.LENGTH_SHORT).show();
            File folder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder");
            if (!folder.exists()) {
                //folder /SoundRecorder doesn't exist, create the folder
                folder.mkdir();
            }

            //start Chronometer
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onChronometerTick(Chronometer chronometer) {
                    if (recordPromptCount == 0) {
                        tvRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
                    } else if (recordPromptCount == 1) {
                        tvRecordingPrompt.setText(getString(R.string.record_in_progress) + "..");
                    } else if (recordPromptCount == 2) {
                        tvRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
                        recordPromptCount = -1;
                    }
                    recordPromptCount++;
                }
            });

            //start RecordingService
            getActivity().startService(intent);
            //keep screen on while recording
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            tvRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
            recordPromptCount++;

        } else {
            //stop recording
            fabRecordButton.setImageResource(R.drawable.ic_mic);
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            timeWhenPaused = 0;
            tvRecordingPrompt.setText(getString(R.string.record_prompt));

            getActivity().stopService(intent);
            //allow the screen to turn off again once recording is finished
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onPauseRecord(boolean pause) {
        if (pause) {
            //pause recording
            btnPause.setCompoundDrawablesWithIntrinsicBounds
                    (R.drawable.ic_play, 0, 0, 0);
            tvRecordingPrompt.setText(getString(R.string.resume_recording_button).toUpperCase());
            timeWhenPaused = chronometer.getBase() - SystemClock.elapsedRealtime();
            chronometer.stop();
        } else {
            //resume recording
            btnPause.setCompoundDrawablesWithIntrinsicBounds
                    (R.drawable.ic_pause, 0, 0, 0);
            tvRecordingPrompt.setText((String) getString(R.string.pause_recording_button).toUpperCase());
            chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
            chronometer.start();
        }
    }

}
