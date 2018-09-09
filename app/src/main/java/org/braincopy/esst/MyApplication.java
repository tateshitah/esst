package org.braincopy.esst;

import android.app.Application;
import android.util.Log;

import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;

public class MyApplication extends Application {
    private static final String CONSUMER_KEY = "QLJbaZFhAL7ufXQXkxMkYrfeJ";
    private static final String CONSUMER_SECRET = "kA3vuh2KZ9p5dRfveE8TUnqr0obJRiINzPred7hD8J0Lk2dLTf";

    @Override
    public void onCreate() {
        super.onCreate();

        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(CONSUMER_KEY, CONSUMER_SECRET))
                .debug(true)
                .build();
        Twitter.initialize(config);
    }
}
