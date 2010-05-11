package com.snaptic.account;

import com.snaptic.account.AccountPreferences;
import com.snaptic.account.AndroidAccount;

import android.net.Uri;
import android.content.SharedPreferences;
import android.content.Context;

public class AccountUtil {
  public static AndroidAccount parseAndroidAccount(Uri uri) {
    String[] emailToken = uri.getUserInfo().split(":");
    AndroidAccount androidAccount = new AndroidAccount();
    androidAccount.setEmail(emailToken[0]);
    String [] tokenSplit = emailToken[1].split("=");
    androidAccount.setAuthToken(tokenSplit[1]);
    androidAccount.setUsername(uri.getEncodedAuthority().split("@")[1]);
    return androidAccount;
  }

  public static void saveAndroidAccount(Context context, AndroidAccount account) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(AccountPreferences.PREFS_NAME, 0);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(AccountPreferences.EMAIL, account.getEmail());
    editor.putString(AccountPreferences.AUTHTOKEN, account.getAuthToken());
    editor.putString(AccountPreferences.USERNAME, account.getUsername());
    editor.putString(AccountPreferences.PASSWORD, account.getPassword());
    editor.putLong(AccountPreferences.CREATED_ON, account.getCreatedOn());
    editor.putLong(AccountPreferences.ID, account.getId());
    editor.putString(AccountPreferences.LOGINTYPE, account.getLoginType());
    editor.putLong(AccountPreferences.LAST_SYNC_ID, account.getLastSyncId());
    editor.commit();
  }

  public static AndroidAccount loadAndroidAccount(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(AccountPreferences.PREFS_NAME, 0);
    AndroidAccount androidAccount = new AndroidAccount();
    androidAccount.setEmail(sharedPreferences.getString(AccountPreferences.EMAIL, ""));
    androidAccount.setAuthToken(sharedPreferences.getString(AccountPreferences.AUTHTOKEN, ""));
    androidAccount.setUsername(sharedPreferences.getString(AccountPreferences.USERNAME, ""));
    androidAccount.setPassword(sharedPreferences.getString(AccountPreferences.PASSWORD, ""));
    androidAccount.setCreatedOn(sharedPreferences.getLong(AccountPreferences.CREATED_ON, 0));
    androidAccount.setId(sharedPreferences.getLong(AccountPreferences.ID, 0));
    androidAccount.setLoginType(sharedPreferences.getString(AccountPreferences.LOGINTYPE, AccountPreferences.LOGINTYPE_NONE));
    androidAccount.setLastSyncId(sharedPreferences.getLong(AccountPreferences.LAST_SYNC_ID, -1));
    return androidAccount;
  }
}
