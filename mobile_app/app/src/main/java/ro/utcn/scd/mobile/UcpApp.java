package ro.utcn.scd.mobile;

import android.app.Application;

import ro.utcn.scd.mobile.data.Settings;

/** Pornirea aplicatiei: aplica tema aleasa (implicit intunecata) inainte de orice ecran. */
public class UcpApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Settings.get(this).applyTheme();
    }
}
