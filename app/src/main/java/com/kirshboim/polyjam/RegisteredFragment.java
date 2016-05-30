package com.kirshboim.polyjam;


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class RegisteredFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        return inflater.inflate(R.layout.fragment_registered, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        String name = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString("name", null);

        if (name != null) {
            String replace = getString(R.string.registered_text).replace("Hi!", "Hi " + name + "!");
            ((TextView) getView().findViewById(R.id.registered_text)).setText(replace);
        }
    }
}
