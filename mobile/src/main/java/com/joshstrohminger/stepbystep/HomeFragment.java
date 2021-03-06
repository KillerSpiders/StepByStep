package com.joshstrohminger.stepbystep;


import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String name) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(NavigationDrawerFragment.ARG_SECTION_TITLE, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_home, container, false);
//                //NotificationCompat.Action action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Play", null).build();
//
//                Notification note = new NotificationCompat.Builder(getActivity())
//                        .setSmallIcon(R.drawable.ic_launcher)
//                        .setPriority(NotificationCompat.PRIORITY_MAX)
//                        .setContentTitle("Step By Step")
//                        .setContentText("This is a longer note " + count)
//                        .extend(new NotificationCompat.WearableExtender()
//                                        //.addAction(action)
//                                        .setContentIcon(R.drawable.logo_simple)
//                                                //.setContentAction(0)
//                                        .setHintHideIcon(true)
//                                        .setContentIconGravity(count % 2 == 0 ? Gravity.START : Gravity.NO_GRAVITY)
//                        ).build();
//                NotificationManagerCompat.from(getActivity()).notify(count, note);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainMobileActivity) getActivity()).deleteAllDataItems();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainMobileActivity) activity).onSectionAttached(getArguments().getString(NavigationDrawerFragment.ARG_SECTION_TITLE));
    }
}
