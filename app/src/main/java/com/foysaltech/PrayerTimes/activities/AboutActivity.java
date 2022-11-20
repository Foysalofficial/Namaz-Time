package com.foysaltech.PrayerTimes.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Keep;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import com.foysaltech.PrayerTimes.PrayerTimesApp;
import com.foysaltech.PrayerTimes.R;

@Keep
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tv = findViewById(R.id.about_textview);
        tv.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PrayerTimesApp.updateLocale(newBase));
    }
}
