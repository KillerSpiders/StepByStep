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

    Button buttonSendNotification;
    int count;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(int sectionNumber) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putInt(NavigationDrawerFragment.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        buttonSendNotification = (Button) root.findViewById(R.id.buttonSendNotification);
        count = 1;
        buttonSendNotification.setText("Send Note " + count);
        buttonSendNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //NotificationCompat.Action action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Play", null).build();

                Notification note = new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentTitle("Step By Step")
                        .setContentText("This is a longer note " + count)
                        .extend(new NotificationCompat.WearableExtender()
                                        //.addAction(action)
                                        .setContentIcon(R.drawable.logo_simple)
                                                //.setContentAction(0)
                                        .setHintHideIcon(true)
                                        .setContentIconGravity(count % 2 == 0 ? Gravity.START : Gravity.NO_GRAVITY)
                        ).build();
                NotificationManagerCompat.from(getActivity()).notify(count, note);
                buttonSendNotification.setText("Send Note " + ++count);
            }
        });
        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainMobileActivity) activity).onSectionAttached(getArguments().getInt(NavigationDrawerFragment.ARG_SECTION_NUMBER));
    }
}
