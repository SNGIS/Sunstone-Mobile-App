package com.sunstone.screens;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SecureCredentialsManager;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.management.UsersAPIClient;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.google.gson.internal.LinkedTreeMap;
import com.sunstone.MainActivity;
import com.sunstone.R;
import com.sunstone.scanner.DeviceScannerActivity;
import com.sunstone.scanner.DeviceScannerMultiActivity;
import com.sunstone.scanner.DeviceScannerMultiDfuActivity;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;
import static com.sunstone.utility.PrefsHelper.USER_ID_TAGS;

public class MainMenuActivity extends AppCompatActivity {
    private static final String TAG = "MainMenuActivity";


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 98;
    public static final int REQUEST_ENABLE_BT = 97;
    private static final String ROLES_CLAIM = "https://sunstone-rtls.com/";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private Set<String> userDevicesSet;

    private BluetoothAdapter bluetoothAdapter;

    private SecureCredentialsManager credentialsManager;
//    private Auth0 auth0;
    private UsersAPIClient usersClient;
    AuthenticationAPIClient authenticationAPIClient;

    private Button btnLogout, btnQuit, btnUpdateFirmware, btnUpdateVariables;
    private Context context;
    private Handler mHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(MainMenuActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_LOCATION);

        context = this.getApplicationContext();

        String accessToken = getIntent().getStringExtra(MainActivity.EXTRA_ACCESS_TOKEN);
        Auth0 auth0 = new Auth0(this);
        auth0.setOIDCConformant(true);
        authenticationAPIClient = new AuthenticationAPIClient(auth0);
        usersClient = new UsersAPIClient(auth0, accessToken);
                    credentialsManager = new SecureCredentialsManager(this,
                    new AuthenticationAPIClient(auth0),
                    new SharedPreferencesStorage(this));
        getProfile(accessToken);

        try {
            userDevicesSet = new HashSet<>();
            sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        mHandler = new Handler();

        try {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();

            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initGui();
    }

    private void getProfile(String accessToken) {
        credentialsManager.getCredentials(new BaseCallback<Credentials, CredentialsManagerException>() {
            @Override
            public void onSuccess(Credentials payload) {
                authenticationAPIClient.userInfo(accessToken)
                    .start(new BaseCallback<UserProfile, AuthenticationException>() {
                        @Override
                        public void onSuccess(UserProfile payload) {
                            try {
                                if (payload.getExtraInfo().containsKey("https://sunstone-rtls.com/app_metadata")) {
                                    LinkedTreeMap<String, ArrayList> idsPayload =
                                            (LinkedTreeMap<String, ArrayList>) payload.getExtraInfo().get("https://sunstone-rtls.com/app_metadata");
                                    if ((idsPayload != null) && (idsPayload.get("owned_tags") != null) && (idsPayload.get("owned_tags").size() > 0)) {
                                        for (int i = 0; i < idsPayload.get("owned_tags").size(); i++) {
                                            LinkedTreeMap<String, Double> idPayload = (LinkedTreeMap<String, Double>) idsPayload.get("owned_tags").get(i);
                                            if(idPayload.get("id") != null) {
                                                userDevicesSet.add(BigDecimal.valueOf(idPayload.get("id")).toPlainString());
                                            }
                                        }
                                        sharedPreferencesEditor = sharedPreferences.edit();
                                        sharedPreferencesEditor.remove(USER_ID_TAGS).commit();
                                        sharedPreferencesEditor.putStringSet(USER_ID_TAGS, userDevicesSet);
                                        sharedPreferencesEditor.apply();
//                                        showUserDevices();
                                    } else {
                                        mHandler.post(() -> Toast.makeText(MainMenuActivity.this, "No tags assigned for this user!", Toast.LENGTH_SHORT).show());
                                    }
                                } else {
                                    mHandler.post(() -> Toast.makeText(MainMenuActivity.this, "No tags assigned for this user!", Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(AuthenticationException error) {
                        }
                    });
                }
            @Override
            public void onFailure(CredentialsManagerException error) {
            }
        });
    }

    private void initGui(){
        btnUpdateFirmware = findViewById(R.id.btn_main_firmware_update);
        btnUpdateVariables = findViewById(R.id.btn_main_variables_update);
        btnLogout = findViewById(R.id.btn_main_logout);
        btnQuit = findViewById(R.id.btn_main_quit);

        btnUpdateFirmware.setOnClickListener(v ->
                startActivity(new Intent(MainMenuActivity.this, DeviceScannerMultiDfuActivity.class)));

        btnUpdateVariables.setOnClickListener(v ->
                startActivity(new Intent(MainMenuActivity.this, DeviceScannerMultiActivity.class)));

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_CLEAR_CREDENTIALS, true);
            startActivity(intent);
            finish();
        });

        btnQuit.setOnClickListener(view -> {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        });
    }


    @Override
    public void onBackPressed() {
    }

    private void showUserDevices(){
        StringBuilder sbUserDevices = new StringBuilder();
        sbUserDevices.append("User deices:\n");
        for(String device : userDevicesSet){
            sbUserDevices.append(device + "\n");
        }
        mHandler.post(() -> Toast.makeText(MainMenuActivity.this, sbUserDevices.toString(), Toast.LENGTH_SHORT).show());
    }
}
