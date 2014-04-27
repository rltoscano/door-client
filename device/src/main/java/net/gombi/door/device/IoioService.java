package net.gombi.door.device;

import ioio.lib.DigitalOutputMode;
import ioio.lib.IOIO;
import ioio.lib.IOIOException;
import ioio.lib.Output;
import ioio.lib.pic.Constants;
import ioio.lib.pic.IOIOImpl;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class IoioService extends IntentService {

  private static final String LOG_TAG = "NET_GOMBI_DOOR_IOIO_SERVICE";

  private static Output<Boolean> led = null;
  private static Output<Boolean> relay = null;

  private IOIO ioio = null;

  public IoioService() {
    super(IoioService.class.getCanonicalName());
  }

  @Override public void onCreate() {
    super.onCreate();
    ioio = IOIOImpl.getInstance();

    if (!ioio.isConnected()) {
      Log.i(LOG_TAG, "Connecting to IOIO...");
      try {
        ioio.waitForConnect();
      } catch (Exception e) {
        Log.e(LOG_TAG, "Error connecting to IOIO.", e);
        return;
      }

      try {
        led = ioio.openDigitalOutput(Constants.LED_PIN, true, DigitalOutputMode.OPEN_DRAIN);
        relay = ioio.openDigitalOutput(36, false, DigitalOutputMode.NORMAL);
      } catch (IOIOException e) {
        Log.e(LOG_TAG, "Error getting reference to LED and Relay.", e);
        return;
      }

      Log.i(LOG_TAG, "IOIO connection established");
    }
  }

  @Override protected void onHandleIntent(Intent intent) {
    Log.i(LOG_TAG, "Received open intent.");
    setRelayState(true);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "Unexpected exception while opening door.", e);
    } finally {
      setRelayState(false);
      Log.i(LOG_TAG, "Door opened. ");
      GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
  }

  private void setRelayState(boolean isOn) {
    // The led is connected in a way, where 0 is light, 1 is off.
    try {
      led.write(!isOn);
      relay.write(isOn);
    } catch (IOIOException e) {
      Log.e(LOG_TAG, "Error writing to IO ports.", e);
    }
  }
}
