package com.astratechnologies.astrasathi;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public final class ContactResolver {
    private final Context context;

    public ContactResolver(Context context) {
        this.context = context;
    }

    public String resolvePhone(String target) {
        String direct = BengaliText.toAsciiDigits(target).replaceAll("[^0-9+]", "");
        if (direct.replace("+", "").length() >= 7) return direct;

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] args = {"%" + target + "%"};
        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, args,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        }
        return "";
    }

    public String resolveDisplayName(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return "";
        Uri lookup = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(lookup, projection,
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (SecurityException ignored) { }
        return "";
    }
}
