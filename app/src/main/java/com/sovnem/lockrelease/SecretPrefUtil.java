package com.sovnem.lockrelease;

import android.content.Context;
import android.content.SharedPreferences;

public class SecretPrefUtil {
	private static final String SECRET_KEY = "sovnem.lock.key";

	public static String getSecretString(Context context) {
		SharedPreferences sp = context.getSharedPreferences(SECRET_KEY,
				Context.MODE_WORLD_READABLE);
		return sp.getString(SECRET_KEY, "");
	}

	public static void saveSecret(String secret, Context context) {
		SharedPreferences sp = context.getSharedPreferences(SECRET_KEY,
				Context.MODE_WORLD_READABLE);
		sp.edit().putString(SECRET_KEY, secret).commit();
	}
}
