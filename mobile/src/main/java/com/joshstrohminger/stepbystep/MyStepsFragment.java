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
import java.util.Arrays;
import java.util.List;

public class MyStepsFragment extends Fragment implements AdapterView.OnItemClickListener {

    private ListView listView;
    private ArrayAdapter<String[]> adapter;
    private List<String[]> data;
    private Toast toast;

    private final int[] defaultIds = new int[]{
            R.array.steps_paper_airplane,
            R.array.steps_stand_on_one_foot
    };

    public MyStepsFragment() {
    }

    public static MyStepsFragment newInstance(int sectionNumber) {
        MyStepsFragment fragment = new MyStepsFragment();
        Bundle args = new Bundle();
        args.putInt(NavigationDrawerFragment.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_steps, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView);

        data = new ArrayList<>();

        // use default instructions
        String[] steps;
        for( int id : defaultIds) {
            steps = getResources().getStringArray(id);
            if( steps.length >= 3) {
                data.add(Arrays.copyOf(steps, 2));
            }
        }

        adapter = new ArrayAdapter<String[]>(getActivity(), android.R.layout.simple_list_item_2, android.R.id.text1, data) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                String[] entry = data.get(position);
                ((TextView)view.findViewById(android.R.id.text1)).setText(entry[0]);
                ((TextView)view.findViewById(android.R.id.text2)).setText(entry[1]);
                return view;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        rootView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.container, NavigationDrawerFragment.PlaceholderFragment).commit();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainMobileActivity) activity).onSectionAttached(getArguments().getInt(NavigationDrawerFragment.ARG_SECTION_NUMBER));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(getActivity(), "selected position " + position, Toast.LENGTH_SHORT);
        toast.show();
    }
}
