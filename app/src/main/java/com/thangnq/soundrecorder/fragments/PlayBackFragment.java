package com.thangnq.soundrecorder.fragments;

import android.app.Dialog;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thangnq.soundrecorder.R;
import com.thangnq.soundrecorder.RecordingItem;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PlayBackFragment extends DialogFragment {
    private static final String LOG_TAG = "PlaybackFragment";

    private static final String ARG_ITEM = "recording_item";
    private RecordingItem recordingItem;

    private Handler handler = new Handler();

    private MediaPlayer mediaPlayer = null;

    private SeekBar seekBar;
    private FloatingActionButton fabPlayButton;
    private TextView tvFileName;
    private TextView tvCurrentProgress;
    private TextView tvFileLength;

    private boolean isPlaying = false;

    //Lưu trữ phút và giây của độ dài file ghi âm
    long minutes = 0;
    long seconds = 0;

    public PlayBackFragment() {
    }

    public PlayBackFragment newInstance(RecordingItem recordingItem) {
        PlayBackFragment playBackFragment = new PlayBackFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ITEM, recordingItem);
        playBackFragment.setArguments(bundle);

        return playBackFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recordingItem = getArguments().getParcelable(ARG_ITEM);

        long itemDuration = recordingItem.getLength();
        minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration) - TimeUnit.MINUTES.toSeconds(minutes);
    }

    @Override
    public void onStart() {
        super.onStart();

        //đặt nền trong suốt
        Window window = getDialog().getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);

        //vô hiệu hóa các nút từ hộp thoại diglog
        AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
        alertDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(false);
        alertDialog.getButton(Dialog.BUTTON_NEUTRAL).setEnabled(false);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            stopPlaying();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            stopPlaying();
        }
    }

    //updating mSeekBar
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {

                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);

                long minutes = TimeUnit.MILLISECONDS.toMinutes(currentPosition);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(currentPosition)
                        - TimeUnit.MINUTES.toSeconds(minutes);
                tvCurrentProgress.setText(String.format("%02d:%02d", minutes, seconds));

                updateSeekBar();
            }
        }
    };

    private void updateSeekBar() {
        handler.postDelayed(runnable, 1000);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_media_playback, null);

        tvFileName = view.findViewById(R.id.file_name_text_view);
        tvFileLength = view.findViewById(R.id.file_length_text_view);
        tvCurrentProgress = view.findViewById(R.id.current_progress_text_view);

        seekBar = view.findViewById(R.id.seekbar);
        ColorFilter colorFilter = new LightingColorFilter(getResources().getColor(R.color.primary), getResources().getColor(R.color.primary));
        seekBar.getProgressDrawable().setColorFilter(colorFilter);
        seekBar.getThumb().setColorFilter(colorFilter);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                    handler.removeCallbacks(runnable);

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition()) - TimeUnit.MINUTES.toSeconds(minutes);
                    tvCurrentProgress.setText(String.format("%02d:%02d", minutes, seconds));

                    updateSeekBar();
                } else if (mediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress);

                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    //xóa trình xử lý thông báo khỏi cập nhật thanh tiến trình
                    handler.removeCallbacks(runnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    handler.removeCallbacks(runnable);
                    mediaPlayer.seekTo(seekBar.getProgress());

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition())
                            - TimeUnit.MINUTES.toSeconds(minutes);
                    tvCurrentProgress.setText(String.format("%02d:%02d", minutes, seconds));

                    updateSeekBar();
                }
            }
        });

        fabPlayButton = view.findViewById(R.id.fab_play);
        fabPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlay(isPlaying);
                isPlaying = !isPlaying;
            }
        });

        tvFileName.setText(recordingItem.getName());
        tvFileLength.setText(String.format("%02d:%02d", minutes, seconds));

        builder.setView(view);
        //yêu cầu một cửa sổ không có tiêu đề
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return builder.create();
    }

    private void prepareMediaPlayerFromPoint(int progress) {
        //đặt mediaPlayer bắt đầu từ giữa tệp âm thanh

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(recordingItem.getFilePath());
            mediaPlayer.prepare();
            seekBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.seekTo(progress);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopPlaying();
                }
            });

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        //giữ màn hình sáng trong khi phát âm thanh
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void pausePlaying() {
        fabPlayButton.setImageResource(R.drawable.ic_play);
        handler.removeCallbacks(runnable);
        mediaPlayer.pause();
    }

    private void resumePlaying() {
        fabPlayButton.setImageResource(R.drawable.ic_pause);
        handler.removeCallbacks(runnable);
        mediaPlayer.start();

        updateSeekBar();
    }

    private void stopPlaying() {
        fabPlayButton.setImageResource(R.drawable.ic_play);
        handler.removeCallbacks(runnable);
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();

        mediaPlayer = null;

        seekBar.setProgress(seekBar.getMax());
        isPlaying = !isPlaying;

        tvCurrentProgress.setText(tvFileLength.getText());
        seekBar.setProgress(seekBar.getMax());

        //cho phép màn hình tắt lại sau khi phát xong âm thanh
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void startPlaying() {
        fabPlayButton.setImageResource(R.drawable.ic_pause);
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(recordingItem.getFilePath());
            mediaPlayer.prepare();
            seekBar.setMax(mediaPlayer.getDuration());

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed ");
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });

        updateSeekBar();
        //giữ màn hình sáng trong khi phát âm thanh
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Play start/stop
    private void onPlay(boolean isPlaying) {
        if (!isPlaying) {
            //MediaPlayer hiện tại không phát âm thanh
            if (mediaPlayer == null) {
                startPlaying(); //bắt đầu từ đầu
            } else {
                resumePlaying(); //tiếp tục MediaPlayer hiện bị tạm dừng
            }

        } else {
            //tạm dừng MediaPlayer
            pausePlaying();
        }
    }

}