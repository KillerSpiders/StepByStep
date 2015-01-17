package com.joshstrohminger.stepbystep;

import android.app.Fragment;

public class FragmentMap {
    private int fragmentName;
    private Class<? extends Fragment> fragmentClass;
    public FragmentMap(int fragmentName, Class<? extends Fragment> fragmentClass) {
        this.fragmentName = fragmentName;
        this.fragmentClass = fragmentClass;
    }
    public int getFragmentName() {
        return fragmentName;
    }
    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }
}
