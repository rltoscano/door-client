package net.gombi.door.client;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import net.gombi.door.lib.Door;

public class DoorDetailsActivity extends Activity
    implements DoorDetailsFragment.DoorDetailsHostActivity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_door_details);
  }

  @Override protected void onStart() {
    super.onStart();
    DoorDetailsFragment detailsFragment =
        (DoorDetailsFragment) getFragmentManager().findFragmentById(R.id.detailsFragment);
    Door d = (Door) getIntent().getParcelableExtra("door");
    if (d == null) {
      detailsFragment.loadDefaultDoor();
    } else {
      detailsFragment.loadDoor(d);
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.door, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        navigateUp();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void onNoDoorAvailable() {
    navigateUp();
  }

  private void navigateUp() {
    TaskStackBuilder.create(this)
        .addNextIntentWithParentStack(NavUtils.getParentActivityIntent(this))
        .startActivities();
  }
}
