package com.kirshboim.polyjam;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.EditText;

import java.lang.ref.WeakReference;


public class MainActivity extends FragmentActivity {

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        startService(new Intent(this, PolyjamService.class));
        receiver = createReceiver();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, new SplashScreenFragment())
                .commit();

        final WeakReference<MainActivity> reference = new WeakReference<>(this);
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                MainActivity activity = reference.get();
                if (activity == null || activity.isFinishing()) return;

                String name = PreferenceManager.getDefaultSharedPreferences(activity)
                        .getString("name", null);

                Fragment fragment = name == null ? new WelcomeFragment() : new RegisteringFragment();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();

            }
        }, 3000);

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, new IntentFilter(PolyjamService.Events.RegistrationCompleted));
    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (PolyjamService.Events.RegistrationCompleted.equals(action)) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container, new RegisteredFragment())
                            .commit();
                }
            }
        };
    }

    public void closeButtonClicked(View view) {
        finish();
    }

    public void aboutClicked(View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void setName(View view) {
        String name = ((EditText) findViewById(R.id.name_field)).getText().toString();
        if (name.matches("^[a-zA-Z0-9]*$")) {

            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("name", name).apply();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new RegisteringFragment())
                    .commit();

        } else {
            new AlertDialog.Builder(this).setMessage("Please use only letters or numbers for you name..")
                    .setPositiveButton("OK!", null).show();
        }
    }

}
