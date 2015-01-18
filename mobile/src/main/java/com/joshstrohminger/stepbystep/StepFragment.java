package com.joshstrohminger.stepbystep;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;

public class StepFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String ARG_STEPS_ID = "arg_steps_id";

    private ListView listView;
    private int stepsId;
    private String title = "Please select some steps";
    private String subtitle = "so they'll show up here";

    private String[] instructions = new String[] {};

    public static StepFragment newInstance(String name, int stepsId) {
        StepFragment fragment = new StepFragment();
        Bundle args = new Bundle();
        args.putString(NavigationDrawerFragment.ARG_SECTION_TITLE, name);
        args.putInt(ARG_STEPS_ID, stepsId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_step, container, false);
        stepsId = getArguments().getInt(ARG_STEPS_ID);
        if(stepsId > 0) {
            String[] steps = getResources().getStringArray(stepsId);
            if (steps.length >= 3) {
                title = steps[0];
                subtitle = steps[1];
                instructions = Arrays.copyOfRange(steps, 2, steps.length);
                ((MainMobileActivity)getActivity()).sendStepsToWearable(steps);
            }
        } else {
            rootView.findViewById(R.id.controlPanel).setVisibility(View.INVISIBLE);
        }
        listView = (ListView) rootView.findViewById(R.id.listView);
        ((TextView) rootView.findViewById(R.id.textViewTitle)).setText(title);
        ((TextView) rootView.findViewById(R.id.textViewSubtitle)).setText(subtitle);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice, android.R.id.text1, instructions);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainMobileActivity) activity).onSectionAttached(getArguments().getString(NavigationDrawerFragment.ARG_SECTION_TITLE));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((MainMobileActivity)getActivity()).sendStepPositionToWearable(position);
    }

    @Override
    public void onStop() {
        ((MainMobileActivity)getActivity()).deleteAllDataItems();
        super.onStop();
    }

    protected void updatePos(int pos) {
        if(listView != null) {
            if(pos < 0 || pos >= instructions.length) {
                // uncheck
                pos = listView.getCheckedItemPosition();
                if(pos != AdapterView.INVALID_POSITION) {
                    listView.setItemChecked(pos, false);
                }
            } else {
                listView.setItemChecked(pos, true);
                listView.smoothScrollToPosition(pos);
            }
        }
    }
}
