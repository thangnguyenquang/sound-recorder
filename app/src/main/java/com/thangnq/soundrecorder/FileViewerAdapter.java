package com.thangnq.soundrecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thangnq.soundrecorder.fragments.PlayBackFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FileViewerAdapter extends RecyclerView.Adapter<FileViewerAdapter.RecordingsViewHolder> implements OnDatabaseChangedListener {

    private static final String LOG_TAG = "FileViewerAdapter";

    private DatabaseHelper databaseHelper;

    RecordingItem recordingItem;
    Context context;
    LinearLayoutManager linearLayoutManager;

    public RecordingItem getItem(int position) {
        return databaseHelper.getItemAt(position);
    }


    public FileViewerAdapter(Context context, RecordingItem recordingItem, LinearLayoutManager linearLayoutManager) {
        this.context = context;
        databaseHelper = new DatabaseHelper(context);
        databaseHelper.setOnDatabaseChangedListener(this);
        this.linearLayoutManager = linearLayoutManager;
        this.recordingItem = recordingItem;
    }

    @NonNull
    @Override
    public RecordingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);

        context = parent.getContext();

        return new RecordingsViewHolder(itemView);
    }

    public static class RecordingsViewHolder extends RecyclerView.ViewHolder {
        protected TextView tvName;
        protected TextView tvLength;
        protected TextView tvDateAdded;
        protected View cardView;

        public RecordingsViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.file_name_text);
            tvLength = view.findViewById(R.id.file_length_text);
            tvDateAdded = view.findViewById(R.id.file_date_added_text);
            cardView = view.findViewById(R.id.card_view);
        }
    }

    @Override
    public void onNewDatabaseEntryAdded() {
        //item added to top of the list
        notifyItemInserted(getItemCount() - 1);
        linearLayoutManager.scrollToPosition(getItemCount() - 1);
    }

    @Override
    //TODO
    public void onDatabaseEntryRenamed() {

    }

    public void shareFileDialog(int position) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(String.valueOf(new File(getItem(position).getFilePath()))));
        shareIntent.setType("audio/mp3");
        context.startActivity(Intent.createChooser(shareIntent, context.getText(R.string.send_to)));
    }

    public void removeOutOfApp(String filePath) {
        //người dùng xóa bản ghi đã lưu ra khỏi ứng dụng thông qua ứng dụng khác
    }


    public void rename(int position, String name) {
        //rename a file

        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath += "/SoundRecorder/" + name;
        File file = new File(filePath);

        if (file.exists() && !file.isDirectory()) {
            //tên file đã tồn tại, đổi tên file
            Toast.makeText(context, String.format(context.getString(R.string.toast_file_exists), name), Toast.LENGTH_LONG).show();

        } else {
            //Tên file là duy nhất, đổi tên file
            File oldFilePath = new File(getItem(position).getFilePath());
            oldFilePath.renameTo(file);
            databaseHelper.renameItem(getItem(position), name, filePath);
            notifyItemChanged(position);
        }
    }


    public void renameFileDialog(final int position) {
        // File rename dialog
        AlertDialog.Builder renameFileBuilder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_rename_file, null);

        final EditText input = view.findViewById(R.id.new_name);

        renameFileBuilder.setTitle(context.getString(R.string.dialog_title_rename));
        renameFileBuilder.setCancelable(true);
        renameFileBuilder.setPositiveButton(context.getString(R.string.dialog_action_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            String value = input.getText().toString().trim() + ".mp3";
                            rename(position, value);

                        } catch (Exception e) {
                            Log.e(LOG_TAG, "exception", e);
                        }

                        dialog.cancel();
                    }
                });
        renameFileBuilder.setNegativeButton(context
                        .getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        renameFileBuilder.setView(view);
        AlertDialog alertDialog = renameFileBuilder.create();
        alertDialog.show();
    }

    public void remove(int position) {
        //xóa mục khỏi cơ sở dữ liệu, recyclerview và storage

        //xóa file khỏi lưu trữ (storage)
        File file = new File(getItem(position).getFilePath());
        file.delete();

        Toast.makeText(context, String.format(context.getString(R.string.toast_file_delete), getItem(position).getName()), Toast.LENGTH_LONG).show();

        databaseHelper.removeItemWithId(getItem(position).getId());
        notifyItemRemoved(position);
    }

    public void deleteFileDialog(final int position) {
        // File delete confirm
        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(context);
        confirmDelete.setTitle(context.getString(R.string.dialog_title_delete));
        confirmDelete.setMessage(context.getString(R.string.dialog_text_delete));
        confirmDelete.setCancelable(true);
        confirmDelete.setPositiveButton(context.getString(R.string.dialog_action_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            //remove item from database, recyclerview, and storage
                            remove(position);

                        } catch (Exception e) {
                            Log.e(LOG_TAG, "exception", e);
                        }

                        dialog.cancel();
                    }
                });
        confirmDelete.setNegativeButton(context.getString(R.string.dialog_action_no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = confirmDelete.create();
        alertDialog.show();
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingsViewHolder holder, int position) {
        recordingItem = getItem(position);
        //Khoảng thời gian ghi âm
        long itemDuration = recordingItem.getLength();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration) - TimeUnit.MINUTES.toSeconds(minutes);

        holder.tvName.setText(recordingItem.getName());
        holder.tvLength.setText(String.format("%02d:%02d", minutes, seconds));
        holder.tvDateAdded.setText(DateUtils.formatDateTime(context, recordingItem.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));

        //xác định trình nghe khi nhấp để mở PlaybackFragment
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PlayBackFragment playBackFragment =
                            new PlayBackFragment().newInstance(getItem(holder.getAbsoluteAdapterPosition()));

                    FragmentTransaction transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();

                    playBackFragment.show(transaction, "dialog_playback");

                } catch (Exception e) {
                    Log.e(LOG_TAG, "exception", e);
                }
            }
        });

        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                ArrayList<String> entrys = new ArrayList<String>();
                entrys.add(context.getString(R.string.dialog_file_share));
                entrys.add(context.getString(R.string.dialog_file_rename));
                entrys.add(context.getString(R.string.dialog_file_delete));

                final CharSequence[] items = entrys.toArray(new CharSequence[entrys.size()]);

                // File delete confirm
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.dialog_title_options));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int item) {
                        if (item == 0) {
                            shareFileDialog(holder.getBindingAdapterPosition());
                        }
                        if (item == 1) {
                            renameFileDialog(holder.getBindingAdapterPosition());
                        } else if (item == 2) {
                            deleteFileDialog(holder.getBindingAdapterPosition());
                        }
                    }
                });
                builder.setCancelable(true);
                builder.setNegativeButton(context.getString(R.string.dialog_action_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int id) {
                                dialogInterface.cancel();
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                return false;
            }
        });
    }



    @Override
    public int getItemCount() {
        return databaseHelper.getCount();
    }

}
