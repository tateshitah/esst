package org.braincopy.esst;

import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.twitter.sdk.android.core.TwitterCore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = null;
 //       StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
 //       StrictMode.setThreadPolicy(policy);

 //       Intent intent = new Intent(this, GoogleSignInActivity.class);
  //      startActivity(intent);

        if (TwitterCore.getInstance().getSessionManager().getActiveSession() == null) {
            //Intent intent = new Intent(this, LoginActivity.class);
            intent = new Intent(this, TwitterLoginActivity.class);
            startActivity(intent);
        } else {
            Toast toast = Toast.makeText(MainActivity.this, "ログイン中", Toast.LENGTH_LONG);
            toast.show();

            intent = new Intent(MainActivity.this, TimelineActivity.class);
            startActivity(intent);
        }
    }
}
