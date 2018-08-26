package com.example.andre.ascomel;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class GoogleAccount {

    private static GoogleAccount googleAccount = null;
    private GoogleAccountCredential credential;

    private GoogleAccount() {

    }

    public static GoogleAccount getGoogleAccount() {
        if (googleAccount == null) {
            googleAccount = new GoogleAccount();
        }
        return googleAccount;
    }

    public GoogleAccountCredential getCredential() {
        return credential;
    }

    public void setCredential(GoogleAccountCredential credential) {
        this.credential = credential;
    }
}
