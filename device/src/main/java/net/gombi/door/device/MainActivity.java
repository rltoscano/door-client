package net.gombi.door.device;

import java.io.IOException;
import java.util.List;

import net.gombi.door.lib.ApiClient;
import net.gombi.door.lib.ApiClient.ApiClientException;
import net.gombi.door.lib.Door;
import net.gombi.door.lib.PermissionLevel;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends Activity {

  public static final String PREF_NAME_ACCT = "account";

  private static final String LOG_TAG = "NET_GOMBI_DOOR_ACTIVITY";

  private ApiClient apiClient;
  private String regId;
  private String devId;
  private Button syncButton;
  private TextView userValueTextView;
  private GoogleCloudMessaging gcm;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    apiClient = new ApiClient(this, PREF_NAME_ACCT);
    devId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
    syncButton = (Button) findViewById(R.id.syncButton);
    String account =
        PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_NAME_ACCT, null);
    userValueTextView = (TextView) findViewById(R.id.userValue);
    userValueTextView.setText(account == null ? getString(R.string.not_logged_in) : account);
    gcm = GoogleCloudMessaging.getInstance(this);
  }

  public void onOpenButtonClick(View view) {
    startService(new Intent(this, IoioService.class));
    Toast.makeText(this, "Door opening...", Toast.LENGTH_SHORT).show();
  }

  public void onSetUserButtonClick(View view) {
    Intent accountPickerIntent = AccountPicker.newChooseAccountIntent(
        null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null);
    startActivityForResult(accountPickerIntent, 0);
  }

  public void onSyncButtonClick(View view) {
    Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();
    syncButton.setEnabled(false);
    execute(new SyncDoorRunnable());
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    String account = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
    Log.v(LOG_TAG, "User selected account: " + account);
    PreferenceManager.getDefaultSharedPreferences(this)
        .edit()
        .putString(PREF_NAME_ACCT, account)
        .commit();
    userValueTextView.setText(account);
  }

  private class SyncDoorRunnable implements Runnable {
    @Override public void run() {
      Log.i(LOG_TAG, "Initiating door sync.");

      List<Door> doors;
      Log.v(LOG_TAG, "Listing doors for devId " + devId);
      try {
        doors = apiClient.listDoors(PermissionLevel.OWNER, devId);
      } catch (ApiClientException e) {
        Log.e(LOG_TAG, "Exception while listing doors.", e);
        toastOnUiThread("Exception while listing doors.");
        return;
      }

      Door matchedDoor = null;
      for (Door d : doors) {
        Log.v(LOG_TAG, "Listed door with devId " + d.getDevId());
        if (devId.equals(d.getDevId())) {
          matchedDoor = d;
          break;
        }
      }

      // Get registration ID if needed.
      if (regId == null) {
        toastOnUiThread("Registration ID needed, registering...");
        try {
          regId = gcm.register(getString(R.string.sender_id));
          toastOnUiThread("Registration ID received.");
        } catch (IOException e) {
          Log.e(LOG_TAG, "Exception getting reg ID.", e);
          toastOnUiThread("Exception getting reg ID.");
          return;
        }
      }

      if (matchedDoor == null) {
        Log.v(LOG_TAG, "No matching door found, creating one...");
        try {
          matchedDoor = apiClient.createDoor(new Door(null, "Prototype door", regId, devId));
        } catch (ApiClientException e) {
          Log.e(LOG_TAG, "Exception while creating door.", e);
          toastOnUiThread("Exception while creating door.");
          return;
        }
      }

      if (!regId.equals(matchedDoor.getRegId())) {
        Log.v(LOG_TAG,
            "Door has a different regId. Local=" + regId + " Remote=" + matchedDoor.getRegId());
        try {
          matchedDoor = apiClient.updateDoor(matchedDoor.setRegId(regId));
        } catch (ApiClientException e) {
          Log.e(LOG_TAG, "Exception while updating door.", e);
          toastOnUiThread("Exception while updating door.");
          return;
        }
      }

      Log.i(LOG_TAG, "Door synced.");
      toastOnUiThread("Door synced.");
    }
  }

  private static void execute(final Runnable r) {
    new AsyncTask<Void, Void, Void>() {
      @Override protected Void doInBackground(Void... arg0) {
        r.run();
        return null;
      }
    }.execute();
  }

  private void toastOnUiThread(final String m) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
        syncButton.setEnabled(true);
      }
    });
  }
}
