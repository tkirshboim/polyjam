package com.kirshboim.polyjam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class PrePlayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preplay);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int port = getIntent().getIntExtra("port", -1);
        String speakerLetter = getSpeakerLetter(port);
        ((TextView) findViewById(R.id.speaker)).setText(speakerLetter);
    }

    private String getSpeakerLetter(int port) {
        if (port == 0) return "A";
        else if (port == 1) return "B";
        else if (port == 2) return "C";
        else if (port == 3) return "D";
        else return "?";
    }

    public void startPlaying(View view) {
        startActivity(new Intent(this, PlayActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
