package com.joshstrohminger.stepbystep;

import android.app.Fragment;

public class FragmentMap {
    private String fragmentTitle;
    private Class<? extends Fragment> fragmentClass;
    public FragmentMap(String fragmentTitle, Class<? extends Fragment> fragmentClass) {
        this.fragmentTitle = fragmentTitle;
        this.fragmentClass = fragmentClass;
    }
    public String getFragmentTitle() {
        return fragmentTitle;
    }
    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }
}
