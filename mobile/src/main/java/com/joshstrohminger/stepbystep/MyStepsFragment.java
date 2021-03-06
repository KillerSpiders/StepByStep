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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MyStepsFragment extends Fragment implements AdapterView.OnItemClickListener {

    private List<StepsHolder> data;

    private final int[] defaultIds = new int[]{
            R.array.steps_paper_airplane,
            R.array.steps_stand_on_one_foot
    };

    public MyStepsFragment() {
    }

    public static MyStepsFragment newInstance(String name) {
        MyStepsFragment fragment = new MyStepsFragment();
        Bundle args = new Bundle();
        args.putString(NavigationDrawerFragment.ARG_SECTION_TITLE, name);
        fragment.setArguments(args);
        return fragment;
    }

    private static class StepsHolder {
        public int id;
        public String title;
        public String author;

        private StepsHolder(int id, String title, String author) {
            this.id = id;
            this.title = title;
            this.author = author;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_steps, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listView);

        data = new ArrayList<>();

        // use default instructions
        String[] steps;
        for( int id : defaultIds) {
            steps = getResources().getStringArray(id);
            if( steps.length >= 3) {
                data.add(new StepsHolder(id, steps[0], steps[1]));
            }
        }

        ArrayAdapter<StepsHolder> adapter = new ArrayAdapter<StepsHolder>(getActivity(), android.R.layout.simple_list_item_2, android.R.id.text1, data) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                StepsHolder entry = data.get(position);
                ((TextView)view.findViewById(android.R.id.text1)).setText(entry.title);
                ((TextView)view.findViewById(android.R.id.text2)).setText(entry.author);
                return view;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        rootView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainMobileActivity)getActivity()).gotoGetSteps();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainMobileActivity) activity).onSectionAttached(getArguments().getString(NavigationDrawerFragment.ARG_SECTION_TITLE));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((MainMobileActivity)getActivity()).gotoStep(data.get(position).id);
    }
}
