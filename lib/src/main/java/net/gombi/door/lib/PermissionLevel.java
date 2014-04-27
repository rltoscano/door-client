package net.gombi.door.lib;

/** Permission level enumeration. */
public enum PermissionLevel {
  PENDING(0),
  OPENER(1),
  OWNER(2);

  private final int value;

  private PermissionLevel(int value) {
    this.value = value;
  }

  public int getValue() { return value; }
}
