package com.kirshboim.polyjam;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RegisteringFragment extends Fragment {

    ServiceConnection connection;
    private BroadcastReceiver receiver;
    private TextView status;
    private ProgressBar scroller;
    private int count = 1;
    private String name;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (PolyjamService.Events.RegistrationFailed.equals(action)) {
                    if (count < 4) {
                        status.setText("Attempt " + count + " to register failed - retrying...");
                        count++;
                        broadcastRegister();
                    } else {
                        scroller.setVisibility(View.INVISIBLE);
                        status.setText(getString(R.string.server_not_found));
                    }
                }
            }
        };

        return inflater.inflate(R.layout.fragment_registering, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();


        count = 1;
        Context context = getActivity();
        context.registerReceiver(receiver, new IntentFilter(PolyjamService.Events.RegistrationFailed));

        this.name = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("name", null);

        status = (TextView) getView().findViewById(R.id.connecting_status);
        scroller = (ProgressBar) getView().findViewById(R.id.progressBar);

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                broadcastRegister();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        getActivity().bindService(new Intent(getActivity(),
                PolyjamService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(connection);
        getActivity().unregisterReceiver(receiver);

        if (count >= 10) {
            // we failed. let's restart the whole thing (bad style to do it here though)
            getActivity().finish();
        }
    }

    private void broadcastRegister() {
        Intent intent = new Intent(PolyjamService.Events.Register);
        intent.putExtra("name", name);
        getActivity().sendBroadcast(intent);
    }

}
