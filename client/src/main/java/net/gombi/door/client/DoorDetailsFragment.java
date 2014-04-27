package net.gombi.door.client;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.gombi.door.lib.ApiClient;
import net.gombi.door.lib.Door;


/**
 * Fragment that shows a detailed view of a door.
 *
 * Handles network communication with the door service.
 */
public class DoorDetailsFragment extends Fragment {
  /** Interface that a host activities of {@ link DoorDetailsFragment}s must implement. */
  public interface DoorDetailsHostActivity {
    /** Will always be called on the UI thread. */
    void onNoDoorAvailable();
  }

  private static final String DEFAULT_DOOR_PREF_NAME = "default-door";

  private ApiClient apiClient;
  private Door door;
  private Button openDoorButton;
  private Button makeDefaultButton;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof DoorDetailsHostActivity)) {
      throw new RuntimeException(
          activity.toString() + " must implement the DoorDetailsHostActivity interface");
    }
    apiClient = new ApiClient(activity, "account");
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_door_details, container, false);
    openDoorButton = (Button) v.findViewById(R.id.openDoorButton);
    openDoorButton.setOnClickListener(new OpenDoorButtonOnClickListener());
    makeDefaultButton = (Button) v.findViewById(R.id.makeDefaultButton);
    makeDefaultButton.setOnClickListener(new MakeDefaultButtonOnClickListener());
    return v;
  }

  @Override public void onDetach() {
    super.onDetach();
    AsyncTask.execute(new Runnable() {
      @Override public void run() {
        apiClient.close();
        apiClient = null;
      }
    });
  }

  /** Loads the given door in the UI. */
  public void loadDoor(Door door) {
    this.door = door;
    openDoorButton.setEnabled(true);
    makeDefaultButton.setEnabled(true);
    TextView doorDetailsDisplayNameTextView =
        (TextView) getActivity().findViewById(R.id.doorDetailsDisplayNameTextView);
    doorDetailsDisplayNameTextView.setText(door.getDisplayName());
  }

  /** Loads the default door set by the user, or notifies host activity if there is none. */
  public void loadDefaultDoor() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    final String defaultDoorKey = prefs.getString(DEFAULT_DOOR_PREF_NAME, null);
    if (defaultDoorKey == null) {
      ((DoorDetailsHostActivity) getActivity()).onNoDoorAvailable();
      return;
    }

    AsyncTask.execute(new Runnable() {
      @Override public void run() {
        Door d;
        try {
          d = apiClient.lookupDoor(defaultDoorKey);
        } catch (ApiClient.ApiClientException e) {
          getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
              ((DoorDetailsHostActivity) getActivity()).onNoDoorAvailable();
            }
          });
          return;
        }
        final Door finalDoor = d;
        getActivity().runOnUiThread(new Runnable() {
          @Override public void run() {
            loadDoor(finalDoor);
          }
        });
      }
    });
  }

  private class OpenDoorButtonOnClickListener implements Button.OnClickListener {
    @Override public void onClick(View view) {
      openDoorButton.setEnabled(false);
      AsyncTask.execute(new Runnable() {
        @Override public void run() {
          try {
            apiClient.openDoor(door.getKey());
          } catch (ApiClient.ApiClientException e) {
            e.printStackTrace();
            // TODO
            return;
          }
          getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
              openDoorButton.setEnabled(true);
            }
          });
        }
      });
    }
  }

  private class MakeDefaultButtonOnClickListener implements Button.OnClickListener {
    @Override public void onClick(View view) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
      prefs.edit().putString(DEFAULT_DOOR_PREF_NAME, door.getKey()).commit();
    }
  }
}
