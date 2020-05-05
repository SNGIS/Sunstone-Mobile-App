package com.sunstone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.auth0.android.Auth0;
import com.auth0.android.Auth0Exception;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.SecureCredentialsManager;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.VoidCallback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.sunstone.screens.MainMenuActivity;

import io.sentry.event.BreadcrumbBuilder;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String SHARED_PREFS = "SharedPrefs";
    public static final String USER_ID_TAGS = "USER_ID_TAGS";

    public static final String EXTRA_CLEAR_CREDENTIALS = "com.auth0.CLEAR_CREDENTIALS";
    public static final String EXTRA_ACCESS_TOKEN = "com.auth0.ACCESS_TOKEN";
    public static final String EXTRA_ID_TOKEN = "com.auth0.ID_TOKEN";

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 98;
    public static final int REQUEST_ENABLE_BT = 97;
    public static final int REQUEST_ACCESS_BACKGROUND_LOCATION = 96;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    private Auth0 auth0;
    private SecureCredentialsManager credentialsManager;


    Button btnLoginMain, btnQuitMain;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (android.os.Build.VERSION.SDK_INT < 23) {
            Toast.makeText(MainActivity.this, "Android version is too low.\nMinimum version is 6.0", Toast.LENGTH_SHORT).show();
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }



        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_LOCATION);

        if(Build.VERSION.SDK_INT > 28){
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_ACCESS_BACKGROUND_LOCATION);
        }

        initGui();

        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        Context context = this.getApplicationContext();
        String sentryDNS = null;
        Sentry.init(sentryDNS, new AndroidSentryClientFactory(context));

        try {
            Sentry.getContext().recordBreadcrumb(new BreadcrumbBuilder().setMessage("MainActivity auth0 setup, logout").build());
            auth0 = new Auth0(this);
            auth0.setOIDCConformant(true);
            credentialsManager = new SecureCredentialsManager(this,
                    new AuthenticationAPIClient(auth0),
                    new SharedPreferencesStorage(this));

            if(getIntent().getBooleanExtra(EXTRA_CLEAR_CREDENTIALS, false)) {
                logout();
                return;
            }

            if(credentialsManager.hasValidCredentials()){
                startActivity(new Intent(MainActivity.this, MainMenuActivity.class));
            }

        } catch (Exception e) {
            Sentry.capture(e);
            Sentry.getContext().clearBreadcrumbs();
        }
    }

    private void initGui(){
        btnLoginMain = (Button) findViewById(R.id.btn_login_new);
        btnQuitMain = (Button) findViewById(R.id.btn_quit_new);

        progressBar = (ProgressBar) findViewById(R.id.pb_login);
        progressBar.setVisibility(View.GONE);

        btnLoginMain.setOnClickListener(view -> {
            String authDomain = getString(R.string.com_auth0_domain);
            if(!"".equals(authDomain)){
                login();
            } else {
                showAuthAlert();
            }
        });

        btnQuitMain.setOnClickListener(view -> btnQuit());
    }

    private void btnQuit() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(credentialsManager.checkAuthenticationResult(requestCode, resultCode)){
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void login() {
        Sentry.getContext().recordBreadcrumb(new BreadcrumbBuilder().setMessage("MainActivity login").build());
        try {
            runOnUiThread(() -> changeButton(btnLoginMain, progressBar));
            WebAuthProvider.login(auth0)
                    .withScheme("demo")
                    .withAudience(String.format("https://%s/api/v2/", getString(R.string.com_auth0_domain)))
                    .withScope("openid profile email offline_access read:current_user update:current_user_metadata app_metadata read:app_metadata update:current_user_identities")
                    .start(MainActivity.this, new AuthCallback() {
                        @Override
                        public void onFailure(@NonNull Dialog dialog) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                                changeButton(btnLoginMain, progressBar);
                            });
                        }

                        @Override
                        public void onFailure(AuthenticationException exception) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                                changeButton(btnLoginMain, progressBar);
                            });
                        }

                        @Override
                        public void onSuccess(@NonNull Credentials credentials) {
                            Sentry.getContext().recordBreadcrumb(new BreadcrumbBuilder().setMessage("MainActivity login successful, user credentials").build());
                            try {
                                credentialsManager.saveCredentials(credentials);
                                Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                                intent.putExtra(EXTRA_ACCESS_TOKEN, credentials.getAccessToken());
                                intent.putExtra(EXTRA_ID_TOKEN, credentials.getIdToken());
                                runOnUiThread(() -> changeButton(btnLoginMain, progressBar));
                                startActivity(intent);
                                finish();
                            } catch (Exception e) {
                                Sentry.capture(e);
                            }
                        }
                    });

        } catch (Exception e) {
            Sentry.capture(e);
            Sentry.getContext().clearBreadcrumbs();
        }
    }


    private void logout() {
        Sentry.getContext().recordBreadcrumb(new BreadcrumbBuilder().setMessage("MainActivity logout WebAuthProvider").build());
        try {
            WebAuthProvider.logout(auth0)
                    .withScheme("demo")
                    .start(this, logoutCallback);
        } catch (Exception e) {
            Sentry.capture(e);
            Sentry.getContext().clearBreadcrumbs();
        }
    }

    private VoidCallback logoutCallback = new VoidCallback() {
        @Override
        public void onSuccess(Void payload) {
            try {
                credentialsManager.clearCredentials();
                sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.clear();
                sharedPreferencesEditor.apply();
            } catch (Exception e) {
                Sentry.capture(e);
            }
        }

        @Override
        public void onFailure(Auth0Exception error) {
            // Log out canceled, keep the user logged in
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Failed to logout", Toast.LENGTH_SHORT).show();
                changeButton(btnLoginMain, progressBar);
            });
        }

    };


    private void changeButton(Button b, ProgressBar p) {
        Sentry.getContext().recordBreadcrumb(new BreadcrumbBuilder().setMessage("MainActivity changeButton").build());
        try {
            if (b.isEnabled()) {
                b.setEnabled(false);
                b.setAlpha(0.1f);
                p.setVisibility(View.VISIBLE);
            } else {
                b.setEnabled(true);
                b.setAlpha(1.0f);
                p.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Sentry.capture(e);
            Sentry.getContext().clearBreadcrumbs();
        }
    }

    private void showAuthAlert(){
        AlertDialog authAlert = new AlertDialog.Builder(this).create();
        authAlert.setTitle(getString(R.string.auth_alert_title));
        authAlert.setMessage(getString(R.string.auth_alert_message));
        authAlert.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.str_ok), (dialog, which) -> dialog.dismiss());
        authAlert.show();
    }

    @Override
    public void onBackPressed() {
    }
}
