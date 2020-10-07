package com.example.socialapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.squareup.picasso.Picasso;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.User;


import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private LoginButton loginButton;
    private ImageView imageView;
    private TextView txtName, txtEmail;
    private SignInButton signIn;
    int RC_SIGN_IN = 0;
    GoogleSignInClient mGoogleSignInClient;

    private CallbackManager callbackManager;

    //Twitter local variables//
    private TwitterLoginButton twitterLoginButton;
    private ImageView userProfileImageView;
    private TextView userDetailsLabel;
    private TwitterAuthClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))//enable logging when app is in debug mode
                .twitterAuthConfig(new TwitterAuthConfig(getResources().getString(R.string.twitter_consumer_key), getResources().getString(R.string.twitter_CONSUMER_SECRET)))
                .debug(true)//enable debug mode
                .build();
        Twitter.initialize(config);
        setContentView(R.layout.activity_main);

        signIn = findViewById(R.id.sign_in_button);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.sign_in_button:
                        signIn();
                        break;
                }
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);



        twitterLoginButton = findViewById(R.id.twitter_login_button);
        twitterLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {

                // Do something with result, which provides a TwitterSession for making API calls
                TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
                TwitterAuthToken authToken = session.getAuthToken();
                String token = authToken.token;
                String secret = authToken.secret;
                login(session);

            }
            @Override
            public void failure(TwitterException exception) {
                // Do something on failure
                Toast.makeText(MainActivity.this, "Failed to authenticate. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });




        loginButton = findViewById(R.id.login_button);
        txtName = findViewById(R.id.profile_name);
        txtEmail = findViewById(R.id.profile_email);
        imageView = findViewById(R.id.profile_pic);

        callbackManager = CallbackManager.Factory.create();
        loginButton.setPermissions(Arrays.asList("email","public_profile"));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });

    }
    protected void signIn()
    {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    public void login(TwitterSession session)
    {
        String username = session.getUserName();
        Intent intent = new Intent(MainActivity.this, Homepage.class);
        intent.putExtra("username", username);
        startActivity(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
           Intent intent = new Intent(MainActivity.this, Homepage.class);
           startActivity(intent);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Error", "signInResult:failed code=" + e.getStatusCode());
        }
    }
    AccessTokenTracker tokenTracker = new AccessTokenTracker() {
        @Override
        protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken)
        {
            if (currentAccessToken == null){
                txtName.setText("");
                txtEmail.setText("");

                imageView.setImageResource(0);
                Toast.makeText(MainActivity.this,"User Logged out",Toast.LENGTH_LONG).show();

            }
            else {
                loadUserProfile(currentAccessToken);
            }

        }
    };
    private  void  loadUserProfile(final AccessToken newAccessToken)
    {
        final GraphRequest request = GraphRequest.newMeRequest(newAccessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response)
            {
                try {
                    String first_name = object.getString("first_name");
                    String last_name = object.getString("last_name");
                    String email = object.getString("email");
                    String id = object.getString("id");
                    Picasso.get().load(MainActivity.this + "http://graph.facebook.com/"+id + "/picture?type=large").into(imageView);

                    txtEmail.setText(email);
                    txtName.setText(first_name +" "+ last_name);
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.dontAnimate();

                    Glide.with(MainActivity.this).load(String.valueOf(Picasso.get())).into(imageView);

                    Intent intent = new Intent(MainActivity.this, Homepage.class);
                    startActivity(intent);



                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name,last_name,email,id");
        request.setParameters(parameters);
        request.executeAsync();

    }
}