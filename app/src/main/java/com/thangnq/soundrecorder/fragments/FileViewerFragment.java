package com.thangnq.soundrecorder.fragments;

import android.app.SearchManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thangnq.soundrecorder.DatabaseHelper;
import com.thangnq.soundrecorder.FileViewerAdapter;
import com.thangnq.soundrecorder.R;
import com.thangnq.soundrecorder.RecordingItem;

import java.util.ArrayList;
import java.util.List;

public class FileViewerFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = "FileViewerFragment";

    private int position;
    private FileViewerAdapter fileViewerAdapter;
    LinearLayoutManager linearLayoutManager;
    RecyclerView recyclerView;

    DatabaseHelper databaseHelper;

    RecordingItem recordingItem;

    //List<RecordingItem> listRecordingItem = new ArrayList<>();

    public static FileViewerFragment newInstance(int position) {
        FileViewerFragment fileViewerFragment = new FileViewerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_POSITION, position);
        fileViewerFragment.setArguments(bundle);

        return fileViewerFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
        observer.startWatching();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_file_viewer, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        //Thứ tự mới nhất đến cũ nhất (cơ sở dữ liệu lưu trữ từ cũ nhất đến mới nhất)
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        databaseHelper =new DatabaseHelper(getActivity());
        //listRecordingItem = databaseHelper.getAllRecordingItem();

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        fileViewerAdapter = new FileViewerAdapter(getActivity(), recordingItem, linearLayoutManager);
        recyclerView.setAdapter(fileViewerAdapter);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.searchView);

        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                searchRecording(text);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchRecording(newText);
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void searchRecording(String keyword) {
        databaseHelper = new DatabaseHelper(getActivity());
        recordingItem = databaseHelper.searchRecording(keyword);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        fileViewerAdapter = new FileViewerAdapter(getActivity(), recordingItem, linearLayoutManager);
        recyclerView.setAdapter(fileViewerAdapter);
    }

    FileObserver observer = new FileObserver(Environment.getExternalStorageDirectory().toString() + "/SoundRecorder") {
        //thiết lập một trình quan sát tệp để xem thư mục này trên thẻ sd
        @Override
        public void onEvent(int event, @Nullable String file) {
            if(event == FileObserver.DELETE){

                String filePath = android.os.Environment.getExternalStorageDirectory().toString()
                        + "/SoundRecorder" + file + "]";

                Log.d(LOG_TAG, "File deleted ["
                        + android.os.Environment.getExternalStorageDirectory().toString()
                        + "/SoundRecorder" + file + "]");

                // xóa file khỏi database và recyclerview
                fileViewerAdapter.removeOutOfApp(filePath);
            }
        }
    };

}
