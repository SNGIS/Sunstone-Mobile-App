/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sunstone.dfu;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sunstone.R;
import com.sunstone.adapter.DfuDevicesListAdapter;
import com.sunstone.dfu.adapter.FileBrowserAppsAdapter;
import com.sunstone.dfu.fragment.UploadCancelFragment;
import com.sunstone.dfu.fragment.ZipInfoFragment;
import com.sunstone.dfu.settings.SettingsActivity;
import com.sunstone.dfu.settings.SettingsFragment;
import com.sunstone.model.DfuDeviceStatus;
import com.sunstone.nordic.AppHelpFragment;
import com.sunstone.nordic.PermissionRationaleFragment;
import com.sunstone.model.ExtendedBluetoothDevice;
import com.sunstone.scanner.ScannerFragment;
import com.sunstone.utility.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

import static com.sunstone.utility.PrefsHelper.SCAN_DURATION_PREFS;
import static com.sunstone.utility.PrefsHelper.SHARED_PREFS;
import static com.sunstone.utility.PrefsHelper.USER_ID_TAGS;

/**
 * DfuActivity is the main DFU activity It implements DFUManagerCallbacks to receive callbacks from DFUManager class It implements
 * DeviceScannerFragment.OnDeviceSelectedListener callback to receive callback when device is selected from scanning dialog The activity supports portrait and
 * landscape orientations
 */
public class DfuMultiActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>, ScannerFragment.OnDeviceSelectedListener,
		UploadCancelFragment.CancelFragmentListener, PermissionRationaleFragment.PermissionDialogListener {
	private static final String TAG = "DfuMultiActivity";

	private static final String INTENT_BTLE_DEVICE = "btle_device";
	private static final String INTENT_BTLE_DEVICE_LIST = "btle_devices_list";

	private static final String PREFS_DEVICE_NAME = "com.sunstone.dfu.PREFS_DEVICE_NAME";
	private static final String PREFS_FILE_NAME = "com.sunstone.dfu.PREFS_FILE_NAME";
	private static final String PREFS_FILE_TYPE = "com.sunstone.dfu.PREFS_FILE_TYPE";
	private static final String PREFS_FILE_SCOPE = "com.sunstone.dfu.PREFS_FILE_SCOPE";
	private static final String PREFS_FILE_SIZE = "com.sunstone.dfu.PREFS_FILE_SIZE";



	private static final String DATA_DEVICE = "device";
	private static final String DATA_FILE_TYPE = "file_type";
	private static final String DATA_FILE_TYPE_TMP = "file_type_tmp";
	private static final String DATA_FILE_PATH = "file_path";
	private static final String DATA_FILE_STREAM = "file_stream";
	private static final String DATA_INIT_FILE_PATH = "init_file_path";
	private static final String DATA_INIT_FILE_STREAM = "init_file_stream";
	private static final String DATA_STATUS = "status";
	private static final String DATA_SCOPE = "scope";
	private static final String DATA_DFU_COMPLETED = "dfu_completed";
	private static final String DATA_DFU_ERROR = "dfu_error";

	private static final String EXTRA_URI = "uri";

	private static final int PERMISSION_REQ = 25;
	private static final int ENABLE_BT_REQ = 0;
	private static final int SELECT_FILE_REQ = 1;
	private static final int SELECT_INIT_FILE_REQ = 2;

	private TextView mFileNameView;
	private TextView mFileSizeView;
	private TextView mFileStatusView;
	private TextView mTextPercentage;
	private ProgressBar mProgressBar;

	private String mFilePath;
	private Uri mFileStreamUri;
	private String mInitFilePath;
	private Uri mInitFileStreamUri;
	private int mFileType;
	private int mFileTypeTmp; // This value is being used when user is selecting a file not to overwrite the old value (in case he/she will cancel selecting file)
	private Integer mScope;
	private boolean mStatusOk;
	/** Flag set to true in {@link #onRestart()} and to false in {@link #onPause()}. */
	private boolean mResumed;
	/** Flag set to true if DFU operation was completed while {@link #mResumed} was false. */
	private boolean mDfuCompleted;
	/** The error message received from DFU service while {@link #mResumed} was false. */
	private String mDfuError;

	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor sharedPreferencesEditor;
	private Set<String> userDevicesSet;
	private int SCAN_DURATION_SHARED;

	private ArrayList<ExtendedBluetoothDevice> devicesToUpdate = new ArrayList<>();
	private ArrayList<DfuDeviceStatus> deviceStatus = new ArrayList<>();
	private int lolanDeviceAddress;
	private WindowManager.LayoutParams layoutParams;

	private int currentlyUpdatedDevice;

	private Button mSelectFileButton, mUploadButton, mConnectButton;
	private ListView dfuDevicesList;
	private DfuDevicesListAdapter devicesListAdapter;
	private Context mContext;


	/**
	 * The progress listener receives events from the DFU Service.
	 * If is registered in onCreate() and unregistered in onDestroy() so methods here may also be called
	 * when the screen is locked or the app went to the background. This is because the UI needs to have the
	 * correct information after user comes back to the activity and this information can't be read from the service
	 * as it might have been killed already (DFU completed or finished with error).
	 */
	private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
		@Override
		public void onDeviceConnecting(final String deviceAddress) {
			Log.d(TAG, "onDeviceConnecting: ");
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_connecting);
		}

		@Override
		public void onDfuProcessStarting(final String deviceAddress) {
			Log.d(TAG, "onDfuProcessStarting: ");
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_starting);
		}

		@Override
		public void onEnablingDfuMode(final String deviceAddress) {
			Log.d(TAG, "onEnablingDfuMode: ");
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_switching_to_dfu);
		}

		@Override
		public void onFirmwareValidating(final String deviceAddress) {
			Log.d(TAG, "onFirmwareValidating: ");
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_validating);
		}

		@Override
		public void onDeviceDisconnecting(final String deviceAddress) {
			Log.d(TAG, "onDeviceDisconnecting: ");
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_disconnecting);
		}

		@Override
		public void onDfuCompleted(final String deviceAddress) {
			Log.d(TAG, "onDfuCompleted: ");
			mTextPercentage.setText(R.string.dfu_status_completed);
			if (mResumed) {
				// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
				new Handler().postDelayed(() -> {
					onTransferCompleted();

					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}, 200);
			} else {
				mDfuCompleted = true;
			}
		}

		@Override
		public void onDfuAborted(final String deviceAddress) {
			Log.d(TAG, "onDfuAborted: ");
			mTextPercentage.setText(R.string.dfu_status_aborted);
			// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
			new Handler().postDelayed(() -> {
				onUploadCanceled();

				// if this activity is still open and upload process was completed, cancel the notification
				final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				manager.cancel(DfuService.NOTIFICATION_ID);
			}, 200);
		}

		@Override
		public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
			Log.d(TAG, "onProgressChanged: ");
			mProgressBar.setIndeterminate(false);
			mProgressBar.setProgress(percent);
			mTextPercentage.setText(getString(R.string.dfu_uploading_percentage, percent));
			if (partsTotal > 1) {
				changeStateList(getString(R.string.dfu_status_uploading_part));
			} else {
				changeStateList(getString(R.string.dfu_status_uploading));
			}
		}

		@Override
		public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
			Log.d(TAG, "onError: " + message);
			if (mResumed) {
				// We have to wait a bit before canceling notification. This is called before DfuService creates the last notification.
				new Handler().postDelayed(() -> {
					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
					Log.d(TAG, "onError:  isDfuServiceRunning()" + isDfuServiceRunning());
					showErrorMessage(message);
					endUpdate();
				}, 200);


			} else {
				mDfuError = message;
			}
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_dfu_multi);

		mContext = this;

		isBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}

		// Enable Notification Channel for Android OREO
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Context context = getApplicationContext();
			DfuServiceInitiator.createDfuNotificationChannel(context);
		}

		layoutParams = getWindow().getAttributes();
		layoutParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setAttributes(layoutParams);

		loadSharedData();
		initGui();

		Intent intent = getIntent();
		try {
			if (intent.hasExtra(INTENT_BTLE_DEVICE_LIST)) {
				devicesToUpdate = intent.getParcelableArrayListExtra(INTENT_BTLE_DEVICE_LIST);
				Log.d(TAG, "onCreate: got devices extra: size:  " + devicesToUpdate.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Try to create sample files
		if (FileHelper.newSamplesAvailable(this)) {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				FileHelper.createSamples(this);
			} else {
				final DialogFragment dialog = PermissionRationaleFragment.getInstance(R.string.permission_sd_text, Manifest.permission.WRITE_EXTERNAL_STORAGE);
				dialog.show(getSupportFragmentManager(), null);
			}
		}

		// restore saved state
		mFileType = DfuService.TYPE_AUTO; // Default
		if (savedInstanceState != null) {
			mFileType = savedInstanceState.getInt(DATA_FILE_TYPE);
			mFileTypeTmp = savedInstanceState.getInt(DATA_FILE_TYPE_TMP);
			mFilePath = savedInstanceState.getString(DATA_FILE_PATH);
			mFileStreamUri = savedInstanceState.getParcelable(DATA_FILE_STREAM);
			mInitFilePath = savedInstanceState.getString(DATA_INIT_FILE_PATH);
			mInitFileStreamUri = savedInstanceState.getParcelable(DATA_INIT_FILE_STREAM);

			mStatusOk = mStatusOk || savedInstanceState.getBoolean(DATA_STATUS);
			mScope = savedInstanceState.containsKey(DATA_SCOPE) ? savedInstanceState.getInt(DATA_SCOPE) : null;

			mUploadButton.setEnabled(devicesToUpdate.get(currentlyUpdatedDevice).device != null && mStatusOk);

			mDfuCompleted = savedInstanceState.getBoolean(DATA_DFU_COMPLETED);
			mDfuError = savedInstanceState.getString(DATA_DFU_ERROR);
		}

		DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);

		if(devicesToUpdate.size() > 0){
			for (ExtendedBluetoothDevice device : devicesToUpdate){
				deviceStatus.add(new DfuDeviceStatus(device.name, device.global_id, "Waiting..."));
			}
			devicesListAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(DATA_FILE_TYPE, mFileType);
		outState.putInt(DATA_FILE_TYPE_TMP, mFileTypeTmp);
		outState.putString(DATA_FILE_PATH, mFilePath);
		outState.putParcelable(DATA_FILE_STREAM, mFileStreamUri);
		outState.putString(DATA_INIT_FILE_PATH, mInitFilePath);
		outState.putParcelable(DATA_INIT_FILE_STREAM, mInitFileStreamUri);
		outState.putParcelable(DATA_DEVICE, devicesToUpdate.get(0).device);
		outState.putBoolean(DATA_STATUS, mStatusOk);
		if (mScope != null) outState.putInt(DATA_SCOPE, mScope);
		outState.putBoolean(DATA_DFU_COMPLETED, mDfuCompleted);
		outState.putString(DATA_DFU_ERROR, mDfuError);
	}

	private void initGui() {
        final Toolbar toolbar = findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFileNameView = findViewById(R.id.file_name);
        mFileSizeView = findViewById(R.id.file_size);
        mFileStatusView = findViewById(R.id.file_status);
        mSelectFileButton = findViewById(R.id.action_select_file);
        mUploadButton = findViewById(R.id.action_upload);
        mTextPercentage = findViewById(R.id.textviewProgress);
        mProgressBar = findViewById(R.id.progressbar_file);

        dfuDevicesList = findViewById(R.id.lv_dfu_devices);
        devicesListAdapter = new DfuDevicesListAdapter(deviceStatus, mContext, R.layout.dfu_device_list_row);
        dfuDevicesList.setAdapter(devicesListAdapter);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (isDfuServiceRunning()) {
            // Restore image file information
            mFileNameView.setText(preferences.getString(PREFS_FILE_NAME, ""));
            mFileSizeView.setText(preferences.getString(PREFS_FILE_SIZE, ""));
            mFileStatusView.setText(R.string.dfu_file_status_ok);
            mStatusOk = true;
            showProgressBar();
        }

        mUploadButton.setOnClickListener(v -> {
            onUploadClicked();
        });
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume: ");
		super.onResume();
		mResumed = true;
		if (mDfuCompleted)
			onTransferCompleted();
		if (mDfuError != null)
			showErrorMessage(mDfuError);
		if (mDfuCompleted || mDfuError != null) {
			// if this activity is still open and upload process was completed, cancel the notification
			final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.cancel(DfuService.NOTIFICATION_ID);
			mDfuCompleted = false;
			mDfuError = null;
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause: ");
		super.onPause();
		mResumed = false;
	}

	@Override
	public void onRequestPermission(final String permission) {
		ActivityCompat.requestPermissions(this, new String[] { permission }, PERMISSION_REQ);
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case PERMISSION_REQ: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// We have been granted the Manifest.permission.WRITE_EXTERNAL_STORAGE permission. Now we may proceed with exporting.
					FileHelper.createSamples(this);
				} else {
					Toast.makeText(this, R.string.no_required_permission, Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}
	}

	private void isBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			showToast(R.string.no_ble);
			finish();
		}
	}

	private boolean isBLEEnabled() {
		final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter adapter = manager.getAdapter();
		return adapter != null && adapter.isEnabled();
	}

	private void showBLEDialog() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, ENABLE_BT_REQ);
	}

	private void showDeviceScanningDialog() {
		final ScannerFragment dialog = ScannerFragment.getInstance(null); // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
		dialog.show(getSupportFragmentManager(), "scan_fragment");
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.settings_and_about, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				break;
			case R.id.action_about:
				final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.dfu_about_text);
				fragment.show(getSupportFragmentManager(), "help_fragment");
				break;
			case R.id.action_settings:
				final Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			case R.id.action_scan_duration:
				scanDurationAlert();
				break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult: requestCode: " + requestCode + " resultCode: " + resultCode);
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
			case SELECT_FILE_REQ: {
				// clear previous data
				mFileType = mFileTypeTmp;
				mFilePath = null;
				mFileStreamUri = null;

				// and read new one
				final Uri uri = data.getData();
			/*
			 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
			 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
			 */
				if (uri.getScheme().equals("file")) {
					// the direct path to the file has been returned
					final String path = uri.getPath();
					final File file = new File(path);
					mFilePath = path;

					updateFileInfo(file.getName(), file.length(), mFileType);
				} else if (uri.getScheme().equals("content")) {
					// an Uri has been returned
					mFileStreamUri = uri;
					// if application returned Uri for streaming, let's us it. Does it works?
					// FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
					final Bundle extras = data.getExtras();
					if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
						mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

					// file name and size must be obtained from Content Provider
					final Bundle bundle = new Bundle();
					bundle.putParcelable(EXTRA_URI, uri);
					getLoaderManager().restartLoader(SELECT_FILE_REQ, bundle, this);
				}
				break;
			}
			case SELECT_INIT_FILE_REQ: {
				mInitFilePath = null;
				mInitFileStreamUri = null;

				// and read new one
				final Uri uri = data.getData();
			/*
			 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
			 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
			 */
				if (uri.getScheme().equals("file")) {
					// the direct path to the file has been returned
					mInitFilePath = uri.getPath();
					mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
				} else if (uri.getScheme().equals("content")) {
					// an Uri has been returned
					mInitFileStreamUri = uri;
					// if application returned Uri for streaming, let's us it. Does it works?
					// FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
					final Bundle extras = data.getExtras();
					if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
						mInitFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);
					mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
				}
				break;
			}
			default:
				break;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		final Uri uri = args.getParcelable(EXTRA_URI);
		/*
		 * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain
		 * all columns and than check which columns are present.
		 */
		// final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
		return new CursorLoader(this, uri, null /* all columns, instead of projection */, null, null, null);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mFileNameView.setText(null);
		mFileSizeView.setText(null);
		mFilePath = null;
		mFileStreamUri = null;
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		if (data != null && data.moveToNext()) {
			/*
			 * Here we have to check the column indexes by name as we have requested for all. The order may be different.
			 */
			final String fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
			final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);
			String filePath = null;
			final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
			if (dataIndex != -1)
				filePath = data.getString(dataIndex /* 2 DATA */);
			if (!TextUtils.isEmpty(filePath))
				mFilePath = filePath;

			updateFileInfo(fileName, fileSize, mFileType);
		} else {
			mFileNameView.setText(null);
			mFileSizeView.setText(null);
			mFilePath = null;
			mFileStreamUri = null;
			mFileStatusView.setText(R.string.dfu_file_status_error);
			mStatusOk = false;
		}
	}

	/**
	 * Updates the file information on UI
	 *
	 * @param fileName file name
	 * @param fileSize file length
	 */
	private void updateFileInfo(final String fileName, final long fileSize, final int fileType) {
		mFileNameView.setText(fileName);
		mFileSizeView.setText(getString(R.string.dfu_file_size_text, fileSize));

		final String extension = mFileType == DfuService.TYPE_AUTO ? "(?i)ZIP" : "(?i)HEX|BIN"; // (?i) =  case insensitive
		final boolean statusOk = mStatusOk = MimeTypeMap.getFileExtensionFromUrl(fileName).matches(extension);
		mFileStatusView.setText(statusOk ? R.string.dfu_file_status_ok : R.string.dfu_file_status_invalid);
		mUploadButton.setEnabled(devicesToUpdate.get(currentlyUpdatedDevice).device != null && statusOk);

		// Ask the user for the Init packet file if HEX or BIN files are selected. In case of a ZIP file the Init packets should be included in the ZIP.
		if (statusOk) {
			if (fileType != DfuService.TYPE_AUTO) {
				mScope = null;
				new AlertDialog.Builder(this).setTitle(R.string.dfu_file_init_title).setMessage(R.string.dfu_file_init_message)
						.setNegativeButton(R.string.no, (dialog, which) -> {
							mInitFilePath = null;
							mInitFileStreamUri = null;
						}).setPositiveButton(R.string.yes, (dialog, which) -> {
							final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
							intent.setType(DfuService.MIME_TYPE_OCTET_STREAM);
							intent.addCategory(Intent.CATEGORY_OPENABLE);
							startActivityForResult(intent, SELECT_INIT_FILE_REQ);
						}).show();
			} else {
				new AlertDialog.Builder(this).setTitle(R.string.dfu_file_scope_title).setCancelable(false)
						.setSingleChoiceItems(R.array.dfu_file_scope, 0, (dialog, which) -> {
							switch (which) {
								case 0:
									mScope = null;
									break;
								case 1:
									mScope = DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS;
									break;
								case 2:
									mScope = DfuServiceInitiator.SCOPE_APPLICATION;
									break;
							}
						}).setPositiveButton(R.string.ok, (dialogInterface, i) -> {
							int index;
							if (mScope == null) {
								index = 0;
							} else if (mScope == DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS) {
								index = 1;
							} else {
								index = 2;
							}

						}).show();
			}
		}
	}

	/**
	 * Called when the question mark was pressed
	 *
	 * @param view a button that was pressed
	 */
//	public void onSelectFileHelpClicked(final View view) {
//		new AlertDialog.Builder(this).setTitle(R.string.dfu_help_title).setMessage(R.string.dfu_help_message).setPositiveButton(R.string.ok, null)
//				.show();
//	}

	/**
	 * Called when Select File was pressed
	 *
	 * @param view a button that was pressed
	 */
	public void onSelectFileClicked(final View view) {
		mFileTypeTmp = mFileType;
		int index = 0;
		switch (mFileType) {
			case DfuService.TYPE_AUTO:
				index = 0;
				break;
			case DfuService.TYPE_SOFT_DEVICE:
				index = 1;
				break;
			case DfuService.TYPE_BOOTLOADER:
				index = 2;
				break;
			case DfuService.TYPE_APPLICATION:
				index = 3;
				break;
		}
		// Show a dialog with file types
		new AlertDialog.Builder(this).setTitle(R.string.dfu_file_type_title)
				.setSingleChoiceItems(R.array.dfu_file_type, index, (dialog, which) -> {
					switch (which) {
						case 0:
							mFileTypeTmp = DfuService.TYPE_AUTO;
							break;
						case 1:
							mFileTypeTmp = DfuService.TYPE_SOFT_DEVICE;
							break;
						case 2:
							mFileTypeTmp = DfuService.TYPE_BOOTLOADER;
							break;
						case 3:
							mFileTypeTmp = DfuService.TYPE_APPLICATION;
							break;
					}
				}).setPositiveButton(R.string.ok, (dialog, which) -> openFileChooser()).setNeutralButton(R.string.dfu_file_info, (dialog, which) -> {
			final ZipInfoFragment fragment = new ZipInfoFragment();
			fragment.show(getSupportFragmentManager(), "help_fragment");
		}).setNegativeButton(R.string.cancel, null).show();
	}

	private void openFileChooser() {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(mFileTypeTmp == DfuService.TYPE_AUTO ? DfuService.MIME_TYPE_ZIP : DfuService.MIME_TYPE_OCTET_STREAM);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		if (intent.resolveActivity(getPackageManager()) != null) {
			// file browser has been found on the device
			startActivityForResult(intent, SELECT_FILE_REQ);
		} else {
			// there is no any file browser app, let's try to download one
			final View customView = getLayoutInflater().inflate(R.layout.app_file_browser, null);
			final ListView appsList = customView.findViewById(android.R.id.list);
			appsList.setAdapter(new FileBrowserAppsAdapter(this));
			appsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			appsList.setItemChecked(0, true);
			new AlertDialog.Builder(this).setTitle(R.string.dfu_alert_no_filebrowser_title).setView(customView)
					.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss()).setPositiveButton(R.string.ok, (dialog, which) -> {
						final int pos = appsList.getCheckedItemPosition();
						if (pos >= 0) {
							final String query = getResources().getStringArray(R.array.dfu_app_file_browser_action)[pos];
							final Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
							startActivity(storeIntent);
						}
					}).show();
		}
	}

	/**
	 * Callback of UPDATE/CANCEL button on DfuActivity
	 */
	public void onUploadClicked() {

		Log.d(TAG, "onUploadClicked: isDfuServiceRunning? " + isDfuServiceRunning());
		if (isDfuServiceRunning()) {
			showUploadCancelDialog();
			return;
		}

		// Check whether the selected file is a HEX file (we are just checking the extension)
		if (!mStatusOk) {
			Toast.makeText(this, R.string.dfu_file_status_invalid_message, Toast.LENGTH_LONG).show();
			return;
		}

		// Save current state in order to restore it if user quit the Activity
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = preferences.edit();

		editor.putString(PREFS_DEVICE_NAME, devicesToUpdate.get(currentlyUpdatedDevice).device.getName());
		editor.putString(PREFS_FILE_NAME, mFileNameView.getText().toString());
		editor.putString(PREFS_FILE_SIZE, mFileSizeView.getText().toString());
		editor.apply();

		showProgressBar();

		final boolean keepBond = preferences.getBoolean(SettingsFragment.SETTINGS_KEEP_BOND, false);
		final boolean forceDfu = preferences.getBoolean(SettingsFragment.SETTINGS_ASSUME_DFU_NODE, false);
		final boolean enablePRNs = preferences.getBoolean(SettingsFragment.SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED, Build.VERSION.SDK_INT < Build.VERSION_CODES.M);
		String value = preferences.getString(SettingsFragment.SETTINGS_NUMBER_OF_PACKETS, String.valueOf(DfuServiceInitiator.DEFAULT_PRN_VALUE));
		int numberOfPackets;
		try {
			numberOfPackets = Integer.parseInt(value);
		} catch (final NumberFormatException e) {
			numberOfPackets = DfuServiceInitiator.DEFAULT_PRN_VALUE;
		}

		final DfuServiceInitiator starter = new DfuServiceInitiator(devicesToUpdate
				.get(currentlyUpdatedDevice).device.getAddress())
				.setDeviceName(devicesToUpdate.get(currentlyUpdatedDevice).device.getName())
				.setKeepBond(keepBond)
				.setForceDfu(forceDfu)
				.setPacketsReceiptNotificationsEnabled(enablePRNs)
				.setPacketsReceiptNotificationsValue(numberOfPackets)

				.setPrepareDataObjectDelay(300L)

				.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(false);

		if (mFileType == DfuService.TYPE_AUTO) {
			starter.setZip(mFileStreamUri, mFilePath);
			if (mScope != null)
				starter.setScope(mScope);
		} else {
			starter.setBinOrHex(mFileType, mFileStreamUri, mFilePath).setInitFile(mInitFileStreamUri, mInitFilePath);
		}
		starter.start(this, DfuService.class);
	}

	private void showUploadCancelDialog() {
		Log.d(TAG, "showUploadCancelDialog: ");
		final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
		pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
		manager.sendBroadcast(pauseAction);
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on DfuActivity
	 */
	public void onConnectClicked() {
		if (isBLEEnabled()) {
			showDeviceScanningDialog();
		} else {
			showBLEDialog();
		}
	}

	public void onConnectClicked(final View view) {
		Log.d(TAG, "onConnectClicked: ");
		if (isBLEEnabled()) {
			showDeviceScanningDialog();
		} else {
			showBLEDialog();
		}
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		Log.d(TAG, "onDeviceSelected: ");
		mUploadButton.setEnabled(mStatusOk);
	}

	@Override
	public void onDialogCanceled() {
		Log.d(TAG, "onDialogCanceled: ");
		// do nothing
	}

	private void changeStateList(String s){
		Log.d(TAG, "changeStateList: s: " + s);
		deviceStatus.get(currentlyUpdatedDevice).setCurrentStatus(s);
		devicesListAdapter.notifyDataSetChanged();
	}

	private void showProgressBar() {
		Log.d(TAG, "showProgressBar: ");
		mProgressBar.setVisibility(View.VISIBLE);
		mTextPercentage.setVisibility(View.VISIBLE);
		mTextPercentage.setText(null);
		changeStateList(getString(R.string.dfu_status_uploading));
		mSelectFileButton.setEnabled(false);
		mUploadButton.setEnabled(true);
		mUploadButton.setText(R.string.dfu_action_upload_cancel);
	}

	private void onTransferCompleted() {
		Log.d(TAG, "onTransferCompleted: ");
		showToast(devicesToUpdate.get(currentlyUpdatedDevice).device_type + " "
				+ devicesToUpdate.get(currentlyUpdatedDevice).global_id + "\nUPDATED");
		mStatusOk = true;

		changeStateList(getString(R.string.dfu_status_completed));
		endUpdate();
	}

	private void endUpdate(){
		Log.d(TAG, "endUpdate: currentlyUpdatedDevice : " + currentlyUpdatedDevice);

		currentlyUpdatedDevice++;
        if(currentlyUpdatedDevice < devicesToUpdate.size()){
            onUploadClicked();
        } else {
			clearUI(true);
            enableButtonFinish();
        }
    }

    private void enableButtonFinish(){
        mTextPercentage.setText(R.string.dfu_status_completed);
        mTextPercentage.setVisibility(View.VISIBLE);
        mUploadButton.setText(getString(R.string.str_go_back));
        mUploadButton.setEnabled(true);
        mUploadButton.setOnClickListener(v -> {
            onBackPressed();
        });
    }

	public void onUploadCanceled() {
		clearUI(false);
		showToast(R.string.dfu_aborted);
		changeStateList("Canceled");
	}

	@Override
	public void onCancelUpload() {
		mProgressBar.setIndeterminate(true);
		changeStateList(getString(R.string.dfu_status_aborted));
		mTextPercentage.setText(null);
	}

	private void showErrorMessage(final String message) {
		showToast("Upload failed: " + message);
		changeStateList(message);
	}

	private void clearUI(final boolean clearDevice) {
		Log.d(TAG, "clearUI: ");
		mProgressBar.setVisibility(View.INVISIBLE);
		mSelectFileButton.setEnabled(false);
		mUploadButton.setEnabled(false);

		// Application may have lost the right to these files if Activity was closed during upload (grant uri permission). Clear file related values.
		mFileNameView.setText(null);
		mFileSizeView.setText(null);
		mFileStatusView.setText(R.string.dfu_file_status_no_file);
		mFilePath = null;
		mFileStreamUri = null;
		mInitFilePath = null;
		mInitFileStreamUri = null;
		mStatusOk = false;
	}

	private void showToast(final int messageResId) {
		Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
	}

	private void showToast(final String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private boolean isDfuServiceRunning() {
		final ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (DfuService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public void loadSharedData() {
		sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
		userDevicesSet = sharedPreferences.getStringSet(USER_ID_TAGS, new ArraySet<String>());
		SCAN_DURATION_SHARED = sharedPreferences.getInt(SCAN_DURATION_PREFS, 7500);
	}

	private void scanDurationAlert(){
		final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View dialogView = inflater.inflate(R.layout.alert_timer, null);
		EditText etScanDuration = dialogView.findViewById(R.id.et_alert_scan_duration);
		Button btnScanDurationSet = dialogView.findViewById(R.id.btn_alert_scan_duration_set);
		Button btnScanDurationOk = dialogView.findViewById(R.id.btn_alert_scan_duration_cancel);
		TextView tvReply = dialogView.findViewById(R.id.tv_alert_scan_duration_reply);

		btnScanDurationOk.setOnClickListener(view -> {
			dialogBuilder.dismiss();
		});

		btnScanDurationSet.setOnClickListener(view -> {
			if(etScanDuration.getText() != null
					&& !etScanDuration.getText().toString().equals("")){
				try {
					int currentValueToSet = Integer.parseInt(etScanDuration.getText().toString()) * 1000;
					sharedPreferencesEditor = sharedPreferences.edit();
					sharedPreferencesEditor.putInt(SCAN_DURATION_PREFS, currentValueToSet);
					sharedPreferencesEditor.apply();
					SCAN_DURATION_SHARED = currentValueToSet;
					tvReply.setText("Duration set to: " + (currentValueToSet / 1000) + "s" );
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		dialogBuilder.setView(dialogView);
		dialogBuilder.show();
	}

	@Override
	public void onBackPressed() {
		devicesToUpdate.clear();
		super.onBackPressed();
	}
}
