package com.snaptic.account;

public class AccountPreferences {
  public static final String PREFS_NAME = "SnapticAccountPreferences";

  public static final String EMAIL = "email";
  public static final String PASSWORD = "password";
  public static final String AUTHTOKEN = "auth_token";
  public static final String USERNAME = "username";
  public static final String CREATED_ON = "created_on";
  public static final String ID = "id";
  public static final String LOGINTYPE = "logintype";
  public static final String LAST_SYNC_ID = "last_sync_id";
  
  // Values for LOGINTYPE
  public static final String LOGINTYPE_NONE = "loggedout";
  public static final String LOGINTYPE_SNAPTIC = "snaptic";
  public static final String LOGINTYPE_GOOGLE = "google";
}
