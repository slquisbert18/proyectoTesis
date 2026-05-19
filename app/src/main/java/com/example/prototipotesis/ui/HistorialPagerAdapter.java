package com.example.prototipotesis.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.prototipotesis.ui.fragments.FragmentCapturas;
import com.example.prototipotesis.ui.fragments.FragmentVideos;

public class HistorialPagerAdapter extends FragmentStateAdapter {

    public HistorialPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch(position){
            case 0:
                return new FragmentCapturas();
            case 1:
                return new FragmentVideos();
            default:
                return new FragmentCapturas();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}