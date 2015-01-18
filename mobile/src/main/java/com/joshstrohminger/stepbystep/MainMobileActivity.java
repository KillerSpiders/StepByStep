package com.joshstrohminger.stepbystep;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.IntentSender;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


public class MainMobileActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks, DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = MainMobileActivity.class.getSimpleName();

    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String STEPS_PATH = "/steps";
    private static final String STEPS_KEY = "steps";
    public static final String POS_PATH = "/pos";
    public static final String POS_KEY = "pos";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private Handler mHandler;

    public final static FragmentMap[] SECTIONS = {
            new FragmentMap(R.string.action_home, HomeFragment.class),
            new FragmentMap(R.string.action_my_steps, MyStepsFragment.class),
            new FragmentMap(R.string.action_step, StepFragment.class),
            new FragmentMap(R.string.action_get_steps, GetStepsFragment.class),
            new FragmentMap(R.string.action_settings, NavigationDrawerFragment.PlaceholderFragment.class)
    };

    private int myStepsIndex;
    private int getStepsIndex;
    private int stepIndex;
    private int currentStepsId;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setContentView(R.layout.activity_main_mobile);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        for(int i = 0; i < SECTIONS.length; i++) {
            if(SECTIONS[i].getFragmentClass() == GetStepsFragment.class) {
                getStepsIndex = i;
            } else if(SECTIONS[i].getFragmentClass() == StepFragment.class) {
                stepIndex = i;
            } else if(SECTIONS[i].getFragmentClass() == MyStepsFragment.class) {
                myStepsIndex = i;
            }
        }
    }

    protected void gotoGetSteps() {
        mNavigationDrawerFragment.selectItem(getStepsIndex, true);
    }

    protected void gotoMySteps() {
        mNavigationDrawerFragment.selectItem(myStepsIndex, true);
    }

    protected void gotoStep(int stepsId) {
        currentStepsId = stepsId;
        mNavigationDrawerFragment.selectItem(stepIndex, true);
    }

    @Override
    public void onNavigationDrawerItemSelected(int oldPosition, int position) {
        // update the main content by replacing fragments
        Fragment fragment;
        FragmentMap map = SECTIONS[position];
        Class<? extends Fragment> type = map.getFragmentClass();
        boolean allowBack = false;

        if(type == HomeFragment.class) {
            fragment = HomeFragment.newInstance(getTitle().toString());
        } else if(type == MyStepsFragment.class) {
            fragment = MyStepsFragment.newInstance(getString(map.getFragmentName()));
            allowBack = true;
        } else if(type == GetStepsFragment.class) {
            fragment = GetStepsFragment.newInstance(getString(map.getFragmentName()));
            allowBack = true;
        } else if(type == NavigationDrawerFragment.PlaceholderFragment.class) {
            fragment = NavigationDrawerFragment.PlaceholderFragment.newInstance(getString(map.getFragmentName()));
            allowBack = true;
        } else if(type == StepFragment.class) {
            fragment = StepFragment.newInstance(getString(map.getFragmentName()), currentStepsId);
            allowBack = true;
        } else {
            Log.e(TAG, "didn't find fragment class type");
            return;
        }
        FragmentManager manager = getFragmentManager();

        FragmentTransaction transaction = manager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.container, fragment, fragment.getClass().getSimpleName());

        if(allowBack) {
            transaction.addToBackStack(String.valueOf(oldPosition));
        } else {
            // clear the back stack
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        FragmentManager manager = getFragmentManager();
        String name = null;
        if(manager.getBackStackEntryCount() > 0) {
            name = manager.getBackStackEntryAt(manager.getBackStackEntryCount()-1).getName();
        }
        if(name == null) {
            name = "0";
        }
        try {
            int position = Integer.parseInt(name);
            Log.d(TAG, "backstack to pos " + position);
            mNavigationDrawerFragment.setCurrentItemFromBackstack(position);
            if(position == 0) {
                mTitle = getTitle();
            } else {
                mTitle = getString(SECTIONS[position].getFragmentName());
            }
            restoreActionBar();
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid fragment backstack name: " + name);
        }
        super.onBackPressed();
    }

    public void onSectionAttached(String title) {
        mTitle = title;
        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main_mobile, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem wear = menu.findItem(R.id.action_wear);
        if(wear != null) {
            wear.setEnabled(mGoogleApiClient.isConnected());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_wear) {
            Toast.makeText(this, "launching wear app", Toast.LENGTH_SHORT).show();

            // Sends an RPC to start a fullscreen Activity on the wearable.
            Log.d(TAG, "Generating RPC");

            // Trigger an AsyncTask that will query for a list of connected nodes and send a
            // "start-activity" message to each connected node.
            new StartWearableActivityTask().execute();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            deleteAllDataItems();
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Google API Client was connected");
        mResolvingError = false;
        invalidateOptionsMenu();    //enable button
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
        //deleteAllDataItems();
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection to Google API client was suspended");
        invalidateOptionsMenu();    //disable button
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            invalidateOptionsMenu();    //disable button
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DataEvent event : events) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        // TODO: Nobody else should be changing data right now so should we be worried about this?
                    } else if (event.getType() == DataEvent.TYPE_DELETED) {
                        // TODO: Nobody else should be deleting data right now so should we be worried about this?
                    }
                }
            }
        });
    }

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent.getRequestId() + " " + messageEvent.getPath());
        switch(messageEvent.getPath()) {
            case POS_PATH:
                final int pos = ByteBuffer.wrap(messageEvent.getData()).getInt();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Fragment fragment = getFragmentManager().findFragmentByTag(StepFragment.class.getSimpleName());
                        if(fragment != null) {
                            ((StepFragment)fragment).updatePos(pos);
                        } else {
                            Toast.makeText(MainMobileActivity.this, "received pos but couldn't get fragment", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            default:
                Log.e(TAG, "unexpected message");
                break;
        }

    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        Log.d(TAG, "onPeerConnected: " + peer);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: don't care right know
            }
        });

    }

    @Override //NodeListener
    public void onPeerDisconnected(final Node peer) {
        Log.d(TAG, "onPeerDisconnected: " + peer);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: don't care right know
            }
        });
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    protected void sendToWearable(PutDataMapRequest dataMap, final String name) {
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if(dataItemResult.getStatus().isSuccess()) {
                            Log.d(TAG, "sent " + name);
                        } else {
                            Log.e(TAG, "failed to send " + name);
                            Toast.makeText(MainMobileActivity.this, "failed to send " + name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    protected void sendStepPositionToWearable(int pos) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(POS_PATH);
        dataMap.getDataMap().putInt(POS_KEY, pos);
        sendToWearable(dataMap, "pos");
    }

    protected void sendStepsToWearable(String[] steps) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(STEPS_PATH);
        dataMap.getDataMap().putStringArray(STEPS_KEY, steps);
        sendToWearable(dataMap, "steps");
    }

    protected void deleteAllDataItems() {
        deleteDataItem(POS_PATH);
        deleteDataItem(STEPS_PATH);
    }

    protected void deleteDataItem(final String path) {
        Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(path).build();
        Log.d(TAG, "URI for delete: " + uri);
        Wearable.DataApi.deleteDataItems(mGoogleApiClient, uri).setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
            @Override
            public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
                if(!deleteDataItemsResult.getStatus().isSuccess()) {
                    String message = "Failed to delete path " + path + " - error: " + deleteDataItemsResult.getStatus().getStatusCode() + " - " + deleteDataItemsResult.getStatus().getStatusMessage();
                    Log.e(TAG, message);
                    Toast.makeText(MainMobileActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            String message = "Failed to send message with status code: " + sendMessageResult.getStatus().getStatusCode();
                            Log.e(TAG, message);
                            Toast.makeText(MainMobileActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }
}
