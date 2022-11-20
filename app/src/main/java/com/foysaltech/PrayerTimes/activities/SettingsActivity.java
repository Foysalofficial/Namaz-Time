package com.foysaltech.PrayerTimes.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import androidx.appcompat.app.ActionBar;

import android.view.MenuItem;

import com.foysaltech.PrayerTimes.PrayerTimesApp;
import com.foysaltech.PrayerTimes.PrayerTimesManager;
import com.foysaltech.PrayerTimes.R;
import com.foysaltech.PrayerTimes.helpers.UserSettings;
import com.foysaltech.PrayerTimes.helpers.WakeLocker;
import com.foysaltech.PrayerTimes.services.AthanService;

import java.util.List;

import org.arabeyes.prayertime.Method;

import timber.log.Timber;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final int REQUEST_SEARCH_CITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        setTitle(getString(R.string.app_name));
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName) || GeneralPreferenceFragment.class.getName().equals(fragmentName) || LocationsPreferenceFragment.class.getName().equals(fragmentName)
                //|| DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    private static int setListPrefSummary(Preference pref, String value) {
        ListPreference listPref = (ListPreference) pref;
        int index = listPref.findIndexOfValue(value);
        listPref.setSummary(index >= 0 ? listPref.getEntries()[index] : null);
        return index;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Set language summary to current user setting
            Preference pref = findPreference("general_language");
            String language = UserSettings.getPrefLanguage(pref.getContext());
            setListPrefSummary(pref, language);

            // bind it to change listener
            pref.setOnPreferenceChangeListener(sGeneralPrefsListener);


            // Set numerals summary to current user setting
            pref = findPreference("general_numerals");
            setListPrefSummary(pref, UserSettings.getNumerals(pref.getContext()));

            // bind it to change listener
            pref.setOnPreferenceChangeListener(sGeneralPrefsListener);
            // Numerals pref. available only when language is arabic.
            if (UserSettings.languageIsArabic(getActivity(), language)) {
                pref.setEnabled(true);
            } else {
                pref.setEnabled(false);
            }

            // Bind to change listener
            pref = findPreference("general_rounding");
            pref.setOnPreferenceChangeListener(sGeneralPrefsListener);
        }

        private final /*static*/ Preference.OnPreferenceChangeListener sGeneralPrefsListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                String key = preference.getKey();
                Context context = preference.getContext();

                switch (key) {
                    case "general_language":
                        // Set the summary to reflect the new value.
                        setListPrefSummary(preference, stringValue);

                        // Change locale?
                        if (!stringValue.equals(UserSettings.getPrefLanguage(context))) {
                            Timber.d("New language: " + stringValue);
                            UserSettings.setLocale(context, stringValue, null);

                            // numerals pref. only ON for arabic.
                            Preference numeralsPref = findPreference("general_numerals");
                            if (UserSettings.languageIsArabic(getActivity(), stringValue)) {
                                numeralsPref.setEnabled(true);
                            } else {
                                numeralsPref.setEnabled(false);
                            }

                            refreshUI(context);
                        }
                        break;

                    case "general_numerals":
                        // Set the summary to reflect the new value.
                        setListPrefSummary(preference, stringValue);

                        // Change locale?
                        if (!stringValue.equals(UserSettings.getNumerals(context))) {
                            Timber.d("New numerals: " + stringValue);
                            UserSettings.setLocale(context, null, stringValue);
                            refreshUI(context);
                        }
                        break;

                    case "general_rounding":
                        // Trigger new calc if value change
                        int oldRound = UserSettings.getRounding(context);
                        int newRound = stringValue.equals("true") ? 1 : 0;
                        if (oldRound != newRound) {
                            PrayerTimesManager.handleSettingsChange(context, -1, newRound, -1);
                        }
                        break;

                    default:
                        // For other preferences, set the summary to the value's
                        // simple string representation.
                        //preference.setSummary(stringValue);
                        break;
                }

                return true;
            }

            private void refreshUI(Context context) {
                // refresh UI (framework part) with new Locale
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LocationsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_locations);

            // Set city summary to current user setting
            Preference pref = findPreference("locations_search_city");
            Context context = pref.getContext();
            pref.setSummary(UserSettings.getCityName(context));

            // bind on click listener to start SearchCityActivity
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Timber.d("onPrefClick");
                    startActivityForResult(preference.getIntent(), REQUEST_SEARCH_CITY);
                    return true;
                }
            });


            // Set method summary to current user setting
            pref = findPreference("locations_method");
            int method = UserSettings.getCalculationMethod(context);
            setListPrefSummary(pref, String.valueOf(method));
            // Bind to onchange listener
            pref.setOnPreferenceChangeListener(sMethodChangeListener);

            // Bind mathhab pref to its change listener
            pref = findPreference("locations_mathhab_hanafi");
            pref.setOnPreferenceChangeListener(sMethodChangeListener);
            // Mathhab hanafi pref. only for Karachi method.
            if (method == Method.V2_KARACHI) {
                pref.setEnabled(true);
            } else {
                pref.setEnabled(false);
            }
        }

        private final /*static*/ Preference.OnPreferenceChangeListener sMethodChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                Context context = preference.getContext();

                switch (preference.getKey()) {
                    case "locations_method":
                        // Set the summary to reflect the new value.
                        int index = setListPrefSummary(preference, stringValue);

                        // Trigger new calc if value change
                        index += Method.V2_MWL;
                        int oldMethodIdx = UserSettings.getCalculationMethod(context);
                        if (oldMethodIdx != index) {
                            Timber.d("New calc method: " + index);

                            // Mathhab hanafi pref. only for Karachi method.
                            Preference mathhabPref = findPreference("locations_mathhab_hanafi");
                            if (index == Method.V2_KARACHI) {
                                mathhabPref.setEnabled(true);
                            } else {
                                mathhabPref.setEnabled(false);
                            }

                            PrayerTimesManager.handleSettingsChange(context, index, -1, -1);
                        }
                        break;
                    case "locations_mathhab_hanafi":
                        // Trigger new calc if value change
                        boolean oldMathhab = UserSettings.isMathhabHanafi(context);
                        boolean newMathhab = stringValue.equals("true");
                        if (oldMathhab != newMathhab) {
                            PrayerTimesManager.handleSettingsChange(context, -1, -1, newMathhab ? 2 : 1);
                        }
                        break;
                    default:
                        // For other preferences, set the summary to the value's
                        // simple string representation.
                        //preference.setSummary(stringValue);
                        break;
                }
                return true;
            }
        };

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            Timber.d("onActivityResult");
            if (requestCode == REQUEST_SEARCH_CITY) {
                if (resultCode == Activity.RESULT_OK) {
                    Preference pref = findPreference("locations_search_city");
                    pref.setSummary(data.getStringExtra("name"));
                    PrayerTimesManager.handleSettingsChange(getActivity(), -1, -1, -1);
                }
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notifications);

            // Bind prayer time pref to its change listener
            Preference pref = findPreference("notifications_prayer_time");
            pref.setOnPreferenceChangeListener(sNotifPrayerTimeListener);

            // Bind muezzin to its change listener
            pref = findPreference("notifications_muezzin");
            pref.setOnPreferenceChangeListener(sMuezzinChangeListener);

            // Set summary to current value
            setListPrefSummary(pref, UserSettings.getMuezzin(pref.getContext()));
        }

        private static final Preference.OnPreferenceChangeListener sNotifPrayerTimeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Timber.d("sNotifPrayerTimeListener: " + newValue.toString());
                if (newValue.toString().equals("true")) {
                    PrayerTimesManager.enableAlarm(preference.getContext());
                } else {
                    PrayerTimesManager.disableAlarm(preference.getContext());
                }
                return true;
            }
        };

        private static final Preference.OnPreferenceChangeListener sMuezzinChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String stringValue = newValue.toString();
                final Context context = preference.getContext();
                final ListPreference listPref = (ListPreference) preference;
                int index = listPref.findIndexOfValue(stringValue);
                final String name = index >= 0 ? listPref.getEntries()[index].toString() : "";

                Timber.d("sMuezzinChangeListener: " + name);
                playAthan(context, stringValue);

                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getString(R.string.select_muezzin, name)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listPref.setValue(stringValue);
                        listPref.setSummary(name);
                        UserSettings.setMuezzin(context, stringValue);
                        stopAthan(context);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stopAthan(context);
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        stopAthan(context);
                    }
                }).create().show();

                return false;
            }

            private void playAthan(Context context, String stringValue) {
                // start Athan Audio
                WakeLocker.acquire(context);
                Intent playIntent = new Intent(context, AthanService.class);
                playIntent.setAction(AthanService.ACTION_PLAY_ATHAN);
                playIntent.putExtra(AthanService.EXTRA_PRAYER, 2);
                playIntent.putExtra(AthanService.EXTRA_MUEZZIN, stringValue);
                context.startService(playIntent);
            }

            private void stopAthan(Context context) {
                AthanService.stopAthanAction(context);
            }
        };
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PrayerTimesApp.updateLocale(newBase));
    }
}
