package ro.utcn.scd.mobile.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Primeste alarma programata de Reminders si afiseaza notificarea "Ai ora in 15 minute". */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Reminders.notify(context,
                intent.getIntExtra(Reminders.EXTRA_ID, 0),
                intent.getStringExtra(Reminders.EXTRA_TITLE),
                intent.getStringExtra(Reminders.EXTRA_TEXT));
    }
}
