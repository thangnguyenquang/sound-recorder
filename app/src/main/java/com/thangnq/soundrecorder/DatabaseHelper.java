package com.thangnq.soundrecorder;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public Context context;

    private static OnDatabaseChangedListener onDatabaseChangedListener;

    public static final String DB_NAME = "recordings.db";
    public static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "recordings";

    public static final String ID = "id";
    public static final String RECORDING_NAME = "recording_name";
    public static final String RECORDING_FILE_PATH = "file_path";
    public static final String RECORDING_LENGTH = "length";
    public static final String TIME_ADDED = "time_added";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "CREATE TABLE " + TABLE_NAME + " ( " +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RECORDING_NAME + " TEXT, " +
                RECORDING_FILE_PATH + " TEXT, " +
                RECORDING_LENGTH + " INTEGER, " +
                TIME_ADDED + " INTEGER " + ")";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        sqLiteDatabase.execSQL(sql);
    }

    public static void setOnDatabaseChangedListener(OnDatabaseChangedListener listener) {
        onDatabaseChangedListener = listener;
    }

    @SuppressLint("Range")
    public RecordingItem getItemAt(int position) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        String[] columns = {ID, RECORDING_NAME, RECORDING_FILE_PATH, RECORDING_LENGTH, TIME_ADDED
        };
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, columns, null, null, null, null, null);
        if (cursor.moveToPosition(position)) {
            RecordingItem recordingItem = new RecordingItem();
            recordingItem.setId(cursor.getInt(0));
            recordingItem.setName(cursor.getString(1));
            recordingItem.setFilePath(cursor.getString(2));
            recordingItem.setLength(cursor.getInt(3));
            recordingItem.setTime(cursor.getLong(4));
            return recordingItem;
        }
        return null;
    }

    public List<RecordingItem> getAllRecordingItem() {
        List<RecordingItem> recordingItemArrayList = new ArrayList<>();
        String[] columns = {ID, RECORDING_NAME, RECORDING_FILE_PATH, RECORDING_LENGTH, TIME_ADDED};
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor =sqLiteDatabase.query(TABLE_NAME, columns,null, null, null, null, null);
        while (cursor.moveToNext()) {
            RecordingItem recordingItem = new RecordingItem();
            recordingItem.setId(cursor.getInt(0));
            recordingItem.setName(cursor.getString(1));
            recordingItem.setFilePath(cursor.getString(2));
            recordingItem.setLength(cursor.getInt(3));
            recordingItem.setTime(cursor.getLong(4));
            recordingItemArrayList.add(recordingItem);
        }
        return recordingItemArrayList;
    }

    public RecordingItem searchRecording(String name){
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        String[] columns = {ID, RECORDING_NAME, RECORDING_FILE_PATH, RECORDING_LENGTH, TIME_ADDED};
        String selection = " RECORDING_NAME like '%" + name + "%'";
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, columns, selection, null, null, null, null);
        if (cursor.moveToNext()) {
            RecordingItem recordingItem = new RecordingItem();
            recordingItem.setId(cursor.getInt(0));
            recordingItem.setName(cursor.getString(1));
            recordingItem.setFilePath(cursor.getString(2));
            recordingItem.setLength(cursor.getInt(3));
            recordingItem.setTime(cursor.getLong(4));
            return recordingItem;
        }
        return null;
    }

    public void removeItemWithId(int id) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.delete(TABLE_NAME, ID + " = ?", new String[]{String.valueOf(id)});
        sqLiteDatabase.close();
    }

    public int getCount() {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        String[] column = {ID};
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, column, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public Context getContext() {
        return context;
    }

    public class RecordingComparator implements Comparator<RecordingItem> {
        public int compare(RecordingItem item1, RecordingItem item2) {
            Long o1 = item1.getTime();
            Long o2 = item2.getTime();
            return o2.compareTo(o1);
        }
    }

    public void addRecording(String recordingName, String filePath, long length) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(RECORDING_NAME, recordingName);
        contentValues.put(RECORDING_FILE_PATH, filePath);
        contentValues.put(RECORDING_LENGTH, length);
        contentValues.put(TIME_ADDED, System.currentTimeMillis());
        sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        if (onDatabaseChangedListener != null) {
            onDatabaseChangedListener.onNewDatabaseEntryAdded();
        }
        sqLiteDatabase.close();
    }

    public void renameItem(RecordingItem recordingItem, String recordingName, String filePath) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(RECORDING_NAME, recordingName);
        contentValues.put(RECORDING_FILE_PATH, filePath);
        sqLiteDatabase.update(TABLE_NAME, contentValues,
                ID + "=" + recordingItem.getId(), null);

        if (onDatabaseChangedListener != null) {
            onDatabaseChangedListener.onDatabaseEntryRenamed();
        }
    }

    public long restoreRecording(RecordingItem recordingItem) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(RECORDING_NAME, recordingItem.getName());
        contentValues.put(RECORDING_FILE_PATH, recordingItem.getFilePath());
        contentValues.put(RECORDING_LENGTH, recordingItem.getLength());
        contentValues.put(TIME_ADDED, recordingItem.getTime());
        contentValues.put(ID, recordingItem.getId());
        long rowId = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
        if (onDatabaseChangedListener != null) {
            //mOnDatabaseChangedListener.onNewDatabaseEntryAdded();
        }
        return rowId;
    }
}
