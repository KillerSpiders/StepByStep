package com.joshstrohminger.stepbystep;

import android.app.Fragment;

public class FragmentMap {
    private boolean useNameAsTitle;
    private String fragmentName;
    private Class<? extends Fragment> fragmentClass;
    public FragmentMap(String fragmentName, Class<? extends Fragment> fragmentClass) {
        this(fragmentName, fragmentClass, true);
    }
    public FragmentMap(String fragmentName, Class<? extends Fragment> fragmentClass, boolean useNameAsTitle) {
        this.fragmentName = fragmentName;
        this.fragmentClass = fragmentClass;
        this.useNameAsTitle = useNameAsTitle;
    }
    public String getFragmentName() {
        return fragmentName;
    }
    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }
    public boolean shouldUseNameAsTitle() { return useNameAsTitle; }
}
