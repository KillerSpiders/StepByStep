package com.joshstrohminger.stepbystep;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Locale;

public class StepFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String ARG_STEPS_INDEX = "arg_steps_id";
    private static final String TAG = StepFragment.class.getSimpleName();
    private static final String UTTERANCE_ID_CLIP = "CLIP";

    boolean ready = false;
    TextToSpeech speaker;
    TextView statusTextView;
    ImageButton playPauseButton;
    ImageButton skipButton;
    private ListView listView;
    private String[] steps;
    private int stepsIndex;
    private int stepsId;

    private String[] instructions = new String[] {};

    public static StepFragment newInstance(String name, int stepsIndex) {
        StepFragment fragment = new StepFragment();
        Bundle args = new Bundle();
        args.putString(NavigationDrawerFragment.ARG_SECTION_TITLE, name);
        args.putInt(ARG_STEPS_INDEX, stepsIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_step, container, false);
        Button button = (Button) rootView.findViewById(R.id.button);
        TextView titleTextView = (TextView) rootView.findViewById(R.id.textViewTitle);
        TextView subtitleTextView = (TextView) rootView.findViewById(R.id.textViewSubtitle);
        stepsIndex = getArguments().getInt(ARG_STEPS_INDEX, -1);
        if(stepsIndex >= 0) {
            MainMobileActivity.DefaultHolder holder = ((MainMobileActivity)getActivity()).DEFAULTS[stepsIndex];
            stepsId = holder.stepsId;
            steps = getResources().getStringArray(stepsId);
            if (steps.length >= 3) {
                titleTextView.setText(steps[0]);
                subtitleTextView.setText(steps[1]);
                instructions = Arrays.copyOfRange(steps, 2, steps.length);
                button.setVisibility(View.INVISIBLE);
            }
        } else {
            rootView.findViewById(R.id.controlPanel).setVisibility(View.INVISIBLE);
            titleTextView.setVisibility(View.INVISIBLE);
            subtitleTextView.setVisibility(View.INVISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainMobileActivity)getActivity()).gotoMySteps();
                }
            });
        }
        statusTextView = (TextView) rootView.findViewById(R.id.statusTextView);
        playPauseButton = (ImageButton) rootView.findViewById(R.id.playPauseButton);
        skipButton = (ImageButton) rootView.findViewById(R.id.skipButton);
        listView = (ListView) rootView.findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice, android.R.id.text1, instructions);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        statusTextView.setText("Starting...");
        playPauseButton.setOnClickListener(this);
        skipButton.setOnClickListener(this);

        if(button.getVisibility() == View.INVISIBLE) {
            speaker = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        speaker.setLanguage(Locale.US);
                        statusTextView.setText("Ready");
                        ready = true;
                        speaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(final String utteranceId) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusTextView.setText("Reading");
                                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                                    }
                                });
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //listView.setEnabled(true);
                                        statusTextView.setText("Paused");
                                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                                    }
                                });
                            }

                            @Override
                            public void onError(String utteranceId) {
                                Log.e(TAG, "Old error");
                            }

                            @Override
                            public void onError(String utteranceId, int errorCode) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusTextView.setText("Error");
                                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                                    }
                                });
                                super.onError(utteranceId, errorCode);
                            }
                        });
                    } else {
                        statusTextView.setText("Forgot how to read");
                    }
                }
            });
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(steps != null) {
            ((MainMobileActivity) getActivity()).sendStepsToWearable(steps);
        }
        int pos = listView.getCheckedItemPosition();
        if(pos != AdapterView.INVALID_POSITION) {
            ((MainMobileActivity) getActivity()).sendStepPositionToWearable(pos);
        }
        //listView.setEnabled(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainMobileActivity) activity).onSectionAttached(getArguments().getString(NavigationDrawerFragment.ARG_SECTION_TITLE));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((MainMobileActivity)getActivity()).sendStepPositionToWearable(position);
        if(speaker != null && speaker.isSpeaking()) {
            speaker.stop();
        }
        play();
    }

    @Override
    public void onPause() {
        if(speaker != null) {
            speaker.stop();
        }
        ((MainMobileActivity)getActivity()).deleteAllDataItems();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if(speaker != null) {
            speaker.shutdown();
        }
        super.onDestroyView();
    }

    protected void updatePos(int pos) {
        if(listView != null) {
            if(speaker != null && speaker.isSpeaking()) {
                speaker.stop();
            }
            if(pos < 0 || pos >= instructions.length) {
                if(pos == instructions.length) {
                    play("That was the last step");
                    statusTextView.setText("Done");
                } else {
                    statusTextView.setText("Ready");
                }

                // uncheck
                pos = listView.getCheckedItemPosition();
                if (pos != AdapterView.INVALID_POSITION) {
                    listView.setItemChecked(pos, false);
                }
            } else {
                listView.setItemChecked(pos, true);
                listView.smoothScrollToPosition(pos);
                play();
            }
        }
    }

    private void play(String text) {
        //listView.setEnabled(false);
        speaker.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID_CLIP);
    }

    private void play() {
        int pos = listView.getCheckedItemPosition();
        if(pos == AdapterView.INVALID_POSITION) {
            pos = 0;
            listView.setItemChecked(pos, true);
        }
        play(instructions[pos]);
    }

    // skip to the next item and share with wear
    private void skip() {

        if(speaker.isSpeaking()) {
            speaker.stop();
        }

        int pos = listView.getCheckedItemPosition();
        if( pos == AdapterView.INVALID_POSITION) {
            pos = 0;
        } else {
            ++pos;
        }
        updatePos(pos);
        if(pos == instructions.length) {
            ((MainMobileActivity) getActivity()).sendStepPositionToWearable(pos);
        } else {
            ((MainMobileActivity) getActivity()).sendStepPositionToWearable(listView.getCheckedItemPosition());
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.playPauseButton:
                if(ready) {
                    if(speaker.isSpeaking()) {
                        //listView.setEnabled(true);
                        speaker.stop();
                    } else {
                        play();
                    }
                }
                break;
            case R.id.skipButton:
                skip();
                break;
            default:
                Log.e(TAG, "Failed to handle click");
                break;
        }
    }
}
