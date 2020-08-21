package com.siempre.siemprewatch;

import android.app.Application;

import com.bugfender.sdk.Bugfender;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Bugfender.init(this, "GEMsCVlUHhDb1rP75eixlxFtLOAhcCF2", BuildConfig.DEBUG);
        Bugfender.enableCrashReporting();
        Bugfender.enableUIEventLogging(this);
        Bugfender.enableLogcatLogging(); // optional, if you want logs automatically collected from logcat
    }
}
