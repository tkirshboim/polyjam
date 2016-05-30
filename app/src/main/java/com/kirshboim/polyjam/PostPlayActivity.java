package com.kirshboim.polyjam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class PostPlayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postplay);
    }

    public void aboutClicked(View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }
}
