package com.joshstrohminger.stepbystep;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StepFragment extends Fragment {

    private static final String ARG_STEPS_ID = "arg_steps_id";

    public StepFragment() {
    }

    public static StepFragment newInstance(String name, int stepsId) {
        StepFragment fragment = new StepFragment();
        Bundle args = new Bundle();
        args.putString(NavigationDrawerFragment.ARG_SECTION_TITLE, name);
        args.putInt(ARG_STEPS_ID, stepsId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_step, container, false);
        ((TextView)rootView.findViewById(R.id.textView)).setText("steps id: " + getArguments().getInt(ARG_STEPS_ID));
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainMobileActivity) activity).onSectionAttached(getArguments().getString(NavigationDrawerFragment.ARG_SECTION_TITLE));
    }
}
