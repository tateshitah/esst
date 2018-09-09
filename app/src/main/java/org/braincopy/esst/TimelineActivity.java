package org.braincopy.esst;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.firebase.firestore.FirebaseFirestore;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.StatusesService;
import com.twitter.sdk.android.tweetui.Timeline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import retrofit2.Call;

public class TimelineActivity extends AppCompatActivity implements MyAdapterListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "TimelineActivity";
    public static final int REQUEST_CODE_ACCOUNT_PICKER = 10;
    public static final int REQUEST_CODE_AUTHORIZATION = 20;
    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final int REQUEST_CHECK_SETTINGS = 30;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private GoogleAccountCredential mCredential;
    private final JsonFactory JSON_FACTORY = AndroidJsonFactory.getDefaultInstance();
    private final String APPLICATION_NAME = "Gmail API Java Quickstart!";
    private UserRecoverableAuthIOException mUserRecoverableAuthIOException;

    ListView listView;
    TweetAdapter adapter;
    List<Tweet> tweetList = new ArrayList<>();
    Long latestTweetId = 1015771736439128064L;//for test
    private FirebaseFirestore mFirestore;

    private boolean isSent = false;
    //private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private android.location.Location mCurrentLocation;
    private String mLastUpdateTime;
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationCallback mLocationCallback;
    //private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView mLastUpdateTimeTextView;
    private String mLastUpdateTimeLabel;
    private LocationSettingsRequest mLocationSettingsRequest;
    private String mLatitudeLabel;
    private String mLongitudeLabel;
    private boolean mRequestingLocationUpdates;
    private View mStartUpdatesButton;
    private View mStopUpdatesButton;
    private String mLat;
    private String mLon;
    private String mTimeAndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);

        // Firestore
        mFirestore = FirebaseFirestore.getInstance();

        listView = (ListView) findViewById(R.id.my_list_view);
        adapter = new TweetAdapter(this, tweetList, this);

        listView.setAdapter(adapter);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        //buildGoogleApiClient();

        startLocationUpdates();

        getHomeTimeline();
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI();
            }
        };
    }

    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            mLat = String.format(Locale.ENGLISH, "%s: %f", mLatitudeLabel,
                    mCurrentLocation.getLatitude());
            mLon = String.format(Locale.ENGLISH, "%s: %f", mLongitudeLabel,
                    mCurrentLocation.getLongitude());
            mTimeAndDate = String.format(Locale.ENGLISH, "%s: %s",
                    mLastUpdateTimeLabel, mLastUpdateTime);
        }
    }

    /*
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }
*/
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        updateUI();
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(TimelineActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        if (ActivityCompat.checkSelfPermission(TimelineActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(TimelineActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(TimelineActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(TimelineActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateUI();
                    }
                });




    }

    private void getHomeTimeline() {
        TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();

        StatusesService statusesService = twitterApiClient.getStatusesService();

        Call<List<Tweet>> call = statusesService.homeTimeline(20, this.latestTweetId, null, false, false, false, false);
        call.enqueue(new Callback<List<Tweet>>() {
            @Override
            public void success(Result<List<Tweet>> result) {

                boolean isEmergency = false;

                List<Tweet> tweets = result.data;

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -1);
                Date oneDayBefere = calendar.getTime();
                //UTC time when this Tweet was created.
//                String tweetCreatedTimeStr;
                Date tweetCreatedTime;
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
                // add each tweet to list of ListView
                for (Tweet tweet : tweets) {
//                    tweetCreatedTimeStr = tweet.createdAt;
                    try {
                        tweetCreatedTime = sdf.parse(tweet.createdAt);
                        if (oneDayBefere.before(tweetCreatedTime)) {
                            tweetList.add(tweet);
                            if (!isEmergency) isEmergency = true;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                String toastText = "タイムライン取得成功";
                if (isEmergency) {
                    toastText = "Emergency Mode";
                    //uploadLocation();

                    try {
                        Location currentLocation = getCurrentLocation();
                        isSent = false;
                        sendEmail();
                    } catch (MessagingException e) {
                        Log.e(TAG, "something wrong!! " + e);
                        //todo do something
                        e.printStackTrace();
                    }
                }
                // ListViewの表示を更新
                adapter.notifyDataSetChanged();

                Toast toast = Toast.makeText(TimelineActivity.this, toastText, Toast.LENGTH_LONG);
                toast.show();
            }

            @Override
            public void failure(TwitterException exception) {
                Toast toast = Toast.makeText(TimelineActivity.this, "タイムライン取得エラー", Toast.LENGTH_LONG);
                toast.show();
            }
        });

       // mGoogleApiClient.connect();

    }

    private Location getCurrentLocation() {
        return this.mCurrentLocation;
    }

    /*
    private void uploadLocation() {
        WriteBatch batch = mFirestore.batch();
        DocumentReference restRef = mFirestore.collection("locations").document();

        // Create location
        Location location = new Location();
        location.height = 0.0;
        location.lat = 40;
        location.lon = 140;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.JAPAN);
        Date date = Calendar.getInstance(Locale.JAPAN).getTime();
        location.dateAndTimeString = sdf.format(date);

        // Add location
        batch.set(restRef, location);
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Write batch succeeded.");
                } else {
                    Log.w(TAG, "write batch failed.", task.getException());
                }
            }
        });
    }
*/

    private void sendEmail() throws MessagingException {
        if (!this.isSent) {
            if (mCredential == null) {
                mCredential = GoogleAccountCredential.usingOAuth2(this, SCOPES);
            }
            if (!setSelectedAccount()) {
                return;
            }

            final HttpTransport HTTP_TRANSPORT;
            HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

            Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, mCredential)
                    .setApplicationName(APPLICATION_NAME).build();
            if (!auth()) {
                return;
            }

            new SendMailTask(this, service).execute("");
            isSent = true;
            //mGoogleApiClient.disconnect();
        }
        return;
    }

    /**
     * when user did not receive recoveableAuthIOException,
     * return true.
     * otherwise move to Oauth intent and return false.
     *
     * @return
     */
    private boolean auth() {
        if (mUserRecoverableAuthIOException == null) {
            return true;
        }

        startActivityForResult(mUserRecoverableAuthIOException.getIntent(), REQUEST_CODE_AUTHORIZATION);
        mUserRecoverableAuthIOException = null;
        //Log.v(TAG, "認証ダイアログを起動しました。");
        return false;
    }

    /**
     * when account is already selected, return true.
     * otherwise move to ChooseAccountIntent and return false.
     *
     * @return
     */
    private boolean setSelectedAccount() {
        if (mCredential.getSelectedAccount() != null) {
            return true;
        }

        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_CODE_ACCOUNT_PICKER);
        return false;
    }

    // 追加
    @Override
    public void onClickReplyButton(Tweet tweet) {
        /*
        Intent intent = new Intent(this, PostTweetActivity
                .class);
        intent.putExtra("REPLY_TO_STATUS_ID", tweet.id);
        startActivity(intent);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // アカウント選択ダイアログ
            case REQUEST_CODE_ACCOUNT_PICKER:
                if (resultCode != this.RESULT_OK) {
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "onOAuthNg called");
                            System.err.println("未取得です。");
                            setProgressBarIndeterminateVisibility(false);
                        }
                    }.run();
                    return;
                }

                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                mCredential.setSelectedAccountName(accountName);
                Log.v(TAG, "アカウントが選択されました。accountName=" + accountName);

                // ※必要ならアカウント名を保存しておきます。

                try {
                    sendEmail();
                } catch (MessagingException e) {
                    Log.e(TAG, "try to sendEmail() after select account; " + e);
                    e.printStackTrace();
                }
                break;
            // 認証ダイアログ
            case REQUEST_CODE_AUTHORIZATION:
                //          mOAuthHelper.onAuthorizationResult(requestCode, resultCode, data);
                if (resultCode != this.RESULT_OK) {
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "onOAuthNg called");
                            System.err.println("未取得です。");
                            setProgressBarIndeterminateVisibility(false);
                        }
                    }.run();
                    return;
                }

                try {
                    sendEmail();
                } catch (MessagingException e) {
                    Log.e(TAG, "try to sendEmail() after select account; " + e);
                    e.printStackTrace();
                }
                break;
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
                        startLocationUpdates();
                        break;
                    case RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        //mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        Log.i(TAG, "onLocationChanged");
        // 取得した位置情報を使用して処理を記述します
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //updateUI();
        Toast.makeText(this, getResources().getString(R.string.location_updated_message), Toast.LENGTH_SHORT).show();

    }


    private class SendMailTask extends AsyncTask<String, Void, Boolean> {
        private Context context;
        private final String CREDENTIALS_FILE_PATH = "credentials.json";
        private final String TOKENS_DIRECTORY_PATH = "tokens";
        private Gmail service;

        public SendMailTask(Context context_, Gmail service_) {
            super();
            this.context = context_;
            this.service = service_;

        }

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean result = false;

            Properties props = new Properties();
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.sasl.enable", "false");
            Session session = Session.getDefaultInstance(props);

            MimeMessage m_message = new MimeMessage(session);

            try {
                m_message.setFrom(new InternetAddress("admin@braincopy.org"));
                m_message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress("tateshita.hiroaki@gmail.com"));
                m_message.setSubject("send email by gmail");
                if (mCurrentLocation != null) {
                    m_message.setText("location is :lat-> " + mCurrentLocation.getLatitude()
                            + "deg, lon-> " + mCurrentLocation.getLongitude() + "***" + "\nby using android.");

                } else {
                    m_message.setText("location is not available:" + "\nby using android.");

                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try {
                    m_message.writeTo(buffer);
                    byte[] bytes = buffer.toByteArray();
                    String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
                    Message content = new Message().setRaw(encodedEmail);
                    service.users().messages().send("me", content).execute();
                    result = true;
                } catch (UserRecoverableAuthIOException e) {
                    mUserRecoverableAuthIOException = e;
                    result = true;
                } catch (MessagingException e) {
                    System.err.println("Hey, something wrong. " + e);
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("Hey, something wrong. " + e);
                    e.printStackTrace();
                }
            } catch (MessagingException e1) {
                System.err.println("in createing message: " + e1);
                e1.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                // OAuth処理を再開します。
                try {
                    sendEmail();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this.context, "エラーが発生しました。", Toast.LENGTH_LONG).show();

            }
        }
    }

}