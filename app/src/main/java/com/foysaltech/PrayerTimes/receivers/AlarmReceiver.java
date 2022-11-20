package com.foysaltech.PrayerTimes.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.foysaltech.PrayerTimes.PrayerTimesManager;
import com.foysaltech.PrayerTimes.activities.MainActivity;
import com.foysaltech.PrayerTimes.helpers.UserSettings;
import com.foysaltech.PrayerTimes.helpers.WakeLocker;
import com.foysaltech.PrayerTimes.services.AthanService;

import timber.log.Timber;

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        int prayer = intent.getIntExtra(AthanService.EXTRA_PRAYER, 2);
        Timber.i("=============== Athan alarm is ON: " + prayer);

        if (UserSettings.isNotificationEnabled(context)) {
            WakeLocker.acquire(context);
            Intent athanIntent = new Intent(context, AthanService.class);
            athanIntent.setAction(AthanService.ACTION_NOTIFY_ATHAN);
            athanIntent.putExtra(AthanService.EXTRA_PRAYER, prayer);
            athanIntent.putExtra(AthanService.EXTRA_MUEZZIN, UserSettings.getMuezzin(context));
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(athanIntent);
                }
            else {
                context.startService(athanIntent);
            }
        }
        else {
            Timber.e("Alarm received when set off by user!");
        }

        // Broadcast to MainActivity so it updates its screen if on
        Intent updateIntent = new Intent(MainActivity.UPDATE_VIEWS);
        context.sendBroadcast(updateIntent);

        // Re-arm alarm.
        PrayerTimesManager.updatePrayerTimes(context, false);
    }
}
