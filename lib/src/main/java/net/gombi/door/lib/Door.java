package net.gombi.door.lib;

import android.os.Parcel;
import android.os.Parcelable;

/** Immutable Door object. */
public class Door implements Parcelable {
  public static final Creator<Door> CREATOR = new Creator<Door>() {
    @Override public Door createFromParcel(Parcel parcel) {
      return new Door(
          parcel.readString(),
          parcel.readString(),
          parcel.readString(),
          parcel.readString());
    }

    @Override public Door[] newArray(int i) {
      return new Door[0];
    }
  };

  private String key;
  private String displayName;
  private String regId;
  private String devId;

  @SuppressWarnings("unused")  // Used by Gson.
  private Door() {}

  public Door(String key, String displayName, String regId, String devId) {
    this.key = key;
    this.displayName = displayName;
    this.regId = regId;
    this.devId = devId;
  }

  public String getKey() { return key; }
  public String getDisplayName() { return displayName; }
  public String getRegId() { return regId; }
  public Door setRegId(String regId) { return new Door(key, displayName, regId, devId); }
  public String getDevId() { return devId; }
  @Override public String toString() { return displayName; }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(key);
    parcel.writeString(displayName);
    parcel.writeString(regId);
    parcel.writeString(devId);
  }
}
