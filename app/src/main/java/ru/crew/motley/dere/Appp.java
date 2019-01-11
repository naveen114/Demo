package ru.crew.motley.dere;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import org.acra.ACRA;
import org.acra.annotation.AcraMailSender;

import io.branch.referral.Branch;
import io.branch.referral.BranchUtil;


@AcraMailSender(mailTo = "dereappcrashreports@gmail.com", resSubject = R.string.crash_subject)
public class Appp extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        // The following line triggers the initialization of ACRA
        if (!BuildConfig.DEBUG) ACRA.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Branch logging for debugging
        Branch.enableLogging();

        // Branch object initialization
        Branch.getAutoInstance(this);

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }
}
