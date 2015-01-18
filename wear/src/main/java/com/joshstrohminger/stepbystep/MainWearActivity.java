package com.joshstrohminger.stepbystep;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
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
import java.util.Arrays;
import java.util.List;

public class MainWearActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private static final String TAG = MainWearActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private ListView listView;
    private TextView titleTextView;
    private TextView subtitleTextView;
    private String title;
    private String subtitle;
    private String[] instructions;
    private View splashPanel;
    private View contentPanel;
    private Handler mHandler;

    private Uri stepsUri;
    private Uri posUri;
    String nodeId;

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
        setContentView(R.layout.activity_main_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                listView = (ListView) stub.findViewById(R.id.dataItem_list);
                splashPanel = stub.findViewById(R.id.splashPanel);
                contentPanel = stub.findViewById(R.id.contentPanel);
                titleTextView = (TextView) stub.findViewById(R.id.textViewTitle);
                subtitleTextView = (TextView) stub.findViewById(R.id.textViewSubtitle);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        sendPosToMobile(position);
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
                nodeId = getRemoteNodeId();
                if(nodeId != null) {
                    setupUris();
                }
            }
        }).start();
    }

    private void setupUris() {
        stepsUri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path(DataLayerListenerService.STEPS_PATH).build();
        posUri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path(DataLayerListenerService.POS_PATH).build();
        getCurrentSteps();
    }

    private void sendPosToMobile(int pos) {
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

    private String getLocalNodeId() {
        NodeApi.GetLocalNodeResult nodeResult = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
        return nodeResult.getNode().getId();
    }

    private String getRemoteNodeId() {
        NodeApi.GetConnectedNodesResult nodesResult = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        List<Node> nodes = nodesResult.getNodes();
        if (nodes.size() > 0) {
            return nodes.get(0).getId();
        }
        return null;
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
        Wearable.DataApi.getDataItem(mGoogleApiClient, stepsUri).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                DataItem dataItem = dataItemResult.getDataItem();
                if(dataItem != null) {
                    populateSteps(DataMapItem.fromDataItem(dataItem));
                    getCurrentPos();
                }
            }
        });
    }

    private void getCurrentPos() {
        Wearable.DataApi.getDataItem(mGoogleApiClient, posUri).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                DataItem dataItem = dataItemResult.getDataItem();
                if(dataItem != null) {
                    populateStepPosition(DataMapItem.fromDataItem(dataItem));
                }
            }
        });
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
                    titleTextView.setText(title);
                    subtitleTextView.setText(subtitle);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainWearActivity.this, android.R.layout.simple_list_item_single_choice, android.R.id.text1, instructions);
                    listView.setAdapter(adapter);
                }
            });
            setAppEnabled(true);
        }
    }

    private void populateStepPosition(DataMapItem dataMapItem) {
        final int pos = dataMapItem.getDataMap().getInt(DataLayerListenerService.POS_KEY);
        populateStepPosition(pos);
    }

    private void populateStepPosition(final int pos) {
        if(instructions != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Setting pos to " + pos);
                    if(pos < 0 || pos >= instructions.length) {
                        // uncheck
                        int oldPos = listView.getCheckedItemPosition();
                        if(oldPos != AdapterView.INVALID_POSITION) {
                            listView.setItemChecked(oldPos, false);
                        }
                    } else {
                        listView.setItemChecked(pos, true);
                        listView.smoothScrollToPosition(pos);
                    }
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
                        populateStepPosition(DataMapItem.fromDataItem(dataItem));
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
                        populateStepPosition(AdapterView.INVALID_POSITION);
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
                nodeId = node.getId();
                setupUris();
            }
        }).start();
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "Node Disconnected: " + node.getId());
        setAppEnabled(false);
    }

    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {

            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }
}
