package com.thangnq.soundrecorder;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.thangnq.soundrecorder.fragments.FileViewerFragment;
import com.thangnq.soundrecorder.fragments.RecordFragment;

public class ViewPagerFragmentAdapter extends FragmentStateAdapter {
    private int[] titles = new int[]{R.string.tab_title_record, R.string.tab_title_saved_recordings};


    public ViewPagerFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: {
                return RecordFragment.newInstance(position);
            }
            case 1: {
                return FileViewerFragment.newInstance(position);
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }
}
