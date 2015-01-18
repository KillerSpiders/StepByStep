package com.joshstrohminger.stepbystep;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainWearActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private static final String TAG = MainWearActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private String title;
    private String subtitle;
    private String[] instructions;
    private View splashPanel;
    private View contentPanel;
    private GridViewPager pager;
    private SampleGridPagerAdapter adapter;
    private MyPageListener myPageListener;
    private Handler mHandler;

    Set<String> nodeIds = new HashSet<>();

    private void setAppEnabled(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                splashPanel.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
                contentPanel.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
            }
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main_wear_pager);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                splashPanel = stub.findViewById(R.id.splashPanel);
                contentPanel = stub.findViewById(R.id.contentPanel);
                pager = (GridViewPager) findViewById(R.id.pager);

                final Resources res = getResources();
                pager.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                        // Adjust page margins:
                        //   A little extra horizontal spacing between pages looks a bit
                        //   less crowded on a round display.
                        final boolean round = insets.isRound();
                        int rowMargin = res.getDimensionPixelOffset(R.dimen.page_row_margin);
                        int colMargin = res.getDimensionPixelOffset(round ? R.dimen.page_column_margin_round : R.dimen.page_column_margin);
                        pager.setPageMargins(rowMargin, colMargin);

                        // GridViewPager relies on insets to properly handle
                        // layout for round displays. They must be explicitly
                        // applied since this listener has taken them over.
                        pager.onApplyWindowInsets(insets);
                        return insets;
                    }
                });
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                getRemoteNodeIds();
                if(!nodeIds.isEmpty()) {
                    setupUris();
                }
            }
        }).start();
    }

    private Uri buildUri(String nodeId, String path) {
        return new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path(path).build();
    }

    private void setupUris() {
        getCurrentSteps();
    }

    protected void sendPosToMobile(int pos) {
        for(String nodeId : nodeIds) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, DataLayerListenerService.POS_PATH, ByteBuffer.allocate(4).putInt(pos).array())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                String message = "Failed to send pos message with status code: " + sendMessageResult.getStatus().getStatusCode();
                                Log.e(TAG, message);
                                Toast.makeText(MainWearActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

//    private String getLocalNodeId() {
//        NodeApi.GetLocalNodeResult nodeResult = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
//        return nodeResult.getNode().getId();
//    }

    private void getRemoteNodeIds() {
        NodeApi.GetConnectedNodesResult nodesResult = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        nodeIds.clear();
        for( Node node : nodesResult.getNodes()) {
            nodeIds.add(node.getId());
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        setAppEnabled(false);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
        setAppEnabled(false);
    }

    private void getCurrentSteps() {
        if(!nodeIds.isEmpty()) {
            Wearable.DataApi.getDataItem(mGoogleApiClient, buildUri(nodeIds.iterator().next(),DataLayerListenerService.STEPS_PATH)).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    DataItem dataItem = dataItemResult.getDataItem();
                    if (dataItem != null) {
                        populateSteps(DataMapItem.fromDataItem(dataItem));
                        getCurrentPos();
                    }
                }
            });
        }
    }

    private void getCurrentPos() {
        if(!nodeIds.isEmpty()) {
            Wearable.DataApi.getDataItem(mGoogleApiClient, buildUri(nodeIds.iterator().next(), DataLayerListenerService.POS_PATH)).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    DataItem dataItem = dataItemResult.getDataItem();
                    if (dataItem != null) {
                        populateStepPosition(DataMapItem.fromDataItem(dataItem), true);
                    }
                }
            });
        }
    }

    private void populateSteps(DataMapItem dataMapItem) {
        final String[] steps = dataMapItem.getDataMap().getStringArray(DataLayerListenerService.STEPS_KEY);
        if(steps.length >= 3) {
            title = steps[0];
            subtitle = steps[1];
            instructions = Arrays.copyOfRange(steps, 2, steps.length);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Populating wear steps...");
                    adapter = new SampleGridPagerAdapter(MainWearActivity.this, getFragmentManager(), pager, title, subtitle, instructions);
                    pager.setAdapter(adapter);
                    final DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
                    dotsPageIndicator.setPager(pager);
                    myPageListener = new MyPageListener(dotsPageIndicator);
                    pager.setOnPageChangeListener(myPageListener);
                }
            });
            setAppEnabled(true);
        }
    }

    private void populateStepPosition(DataMapItem dataMapItem, boolean fromRead) {
        final int pos = dataMapItem.getDataMap().getInt(DataLayerListenerService.POS_KEY);
        populateStepPosition(pos, fromRead);
    }

    private void populateStepPosition(final int pos, final boolean fromRead) {
        if(instructions != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Setting pos to " + pos);
                    int newPos = 0; // default to title page
                    if(pos == instructions.length && fromRead) {
                        // leave it as the title if getting the position from a read and not a message
                    } else if(pos >= 0 && pos <= instructions.length) {
                        // TODO: account for x, right now we're assuming it's always 0 since there is only a single column
                        newPos = pos + 1;
                    }
                    if(myPageListener != null) {
                        myPageListener.setDontReportNextSelection(true);
                    }
                    pager.setCurrentItem(newPos, 0);
                }
            });
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                DataItem dataItem = event.getDataItem();
                switch(path) {
                    case DataLayerListenerService.STEPS_PATH:
                        populateSteps(DataMapItem.fromDataItem(dataItem));
                        break;
                    case DataLayerListenerService.POS_PATH:
                        populateStepPosition(DataMapItem.fromDataItem(dataItem), false);
                        break;
                    default:
                        Log.e(TAG, "Unrecognized path: " + path);
                        break;
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem Deleted: " + event.getDataItem().toString());
                String path = event.getDataItem().getUri().getPath();
                switch(path) {
                    case DataLayerListenerService.STEPS_PATH:
                        setAppEnabled(false);
                        break;
                    case DataLayerListenerService.POS_PATH:
                        populateStepPosition(AdapterView.INVALID_POSITION, false);
                        break;
                    default:
                        Log.w(TAG, "Unhandled deleted path");
                        break;
                }
            } else {
                Log.e(TAG, "Unknown data event type: " + event.getType());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d(TAG, "onMessageReceived: " + event);
    }

    @Override
    public void onPeerConnected(final Node node) {
        Log.d(TAG, "Node Connected: " + node.getId());
        new Thread(new Runnable() {
            @Override
            public void run() {
                nodeIds.add(node.getId());
                setupUris();
            }
        }).start();
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "Node Disconnected: " + node.getId());
        nodeIds.remove(node.getId());
        if(nodeIds.isEmpty()) {
            setAppEnabled(false);
        }
    }

    public void showConfirmation() {
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        startActivity(intent);
    }

    private class MyPageListener implements GridViewPager.OnPageChangeListener {

        private boolean dontReportNextSelection;
        private DotsPageIndicator dotsPageIndicator;

        public MyPageListener(DotsPageIndicator dotsPageIndicator) {
            this.dotsPageIndicator = dotsPageIndicator;
        }

        public void setDontReportNextSelection(boolean dontReportNextSelection) {
            this.dontReportNextSelection = dontReportNextSelection;
        }

        @Override
        public void onPageScrolled(int row, int column, float rowOffset, float columnOffset, int rowOffsetPixels, int columnOffsetPixels) {
            dotsPageIndicator.onPageScrolled(row, column, rowOffset, columnOffset, rowOffsetPixels, columnOffsetPixels);
        }

        @Override
        public void onPageSelected(int row, int column) {
            dotsPageIndicator.onPageSelected(row, column);
            if(!dontReportNextSelection) {
                sendPosToMobile(row - 1);
            }
            dontReportNextSelection = false;

            // set onclick for the fragment, couldn't figure out where else to do it
            View view = adapter.getFragment(row,column).getView();
            if(view != null) {
                view.setOnClickListener(fragmentClickListener);
            }

            if(row == adapter.getRowCount() - 1) {
                showConfirmation();
            }
        }

        View.OnClickListener fragmentClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPosToMobile(pager.getCurrentItem().y - 1);
            }
        };

        @Override
        public void onPageScrollStateChanged(int state) {
            dotsPageIndicator.onPageScrollStateChanged(state);
        }
    }
}
