package net.gombi.door.client;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import net.gombi.door.lib.ApiClient;
import net.gombi.door.lib.Door;
import net.gombi.door.lib.PermissionLevel;

import java.util.List;

public class DoorListActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
  private static final String PREF_NAME_ACCT = "account";
  private static final int REQUEST_CODE_AUTH = 0;
  private static final String LOG_TAG = DoorListActivity.class.getCanonicalName();

  private ApiClient apiClient;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_door_list);
    ListView listView = (ListView) DoorListActivity.this.findViewById(R.id.listView);
    listView.setOnItemClickListener(DoorListActivity.this);
  }

  @Override protected void onStart() {
    apiClient = new ApiClient(this, PREF_NAME_ACCT);
    super.onStart();
    int connectionResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (connectionResult != ConnectionResult.SUCCESS) {
      GooglePlayServicesUtil.getErrorDialog(connectionResult, this, 0 /*requestCode*/);
    }
    String account;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    if (prefs.getString(PREF_NAME_ACCT, null) == null) {
      Account[] accounts =
          AccountManager.get(this).getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
      if (accounts.length == 1) {
        account = accounts[0].name;
        prefs.edit().putString(PREF_NAME_ACCT, account).commit();
      } else {
        // TODO(robert)
      }
    }

    populateDoorList();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.door_list, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_AUTH:
        populateDoorList();
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    Door door = (Door) adapterView.getItemAtPosition(i);
    Intent intent = new Intent(this, DoorDetailsActivity.class);
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra("door", door);
    startActivity(intent);
  }

  @Override public void onStop() {
    super.onStop();
    AsyncTask.execute(new Runnable() {
      @Override public void run() {
        apiClient.close();
      }
    });
  }

  private void populateDoorList() {
    AsyncTask.execute(new Runnable() {
      @Override public void run() {
        List<Door> doors;
        try {
          doors = apiClient.listDoors(PermissionLevel.OPENER);
        } catch (ApiClient.ApiClientException e) {
          if (e.getCause() instanceof UserRecoverableAuthException) {
            UserRecoverableAuthException cause = (UserRecoverableAuthException) e.getCause();
            startActivityForResult(cause.getIntent(), REQUEST_CODE_AUTH);
            return;
          }
          Log.e(LOG_TAG, "Failed to get list of doors", e);
          return;
        }

        final ArrayAdapter<Door> doorArrayAdapter = new ArrayAdapter<Door>(
            DoorListActivity.this, android.R.layout.simple_list_item_1, doors);
        runOnUiThread(new Runnable() {
          @Override public void run() {
            ListView listView = (ListView) DoorListActivity.this.findViewById(R.id.listView);
            listView.setAdapter(doorArrayAdapter);
          }
        });
      }
    });
  }
}
