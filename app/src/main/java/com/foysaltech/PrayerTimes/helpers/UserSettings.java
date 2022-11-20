package com.foysaltech.PrayerTimes.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.foysaltech.PrayerTimes.PrayerTimesApp;
import com.foysaltech.PrayerTimes.R;
import com.foysaltech.PrayerTimes.databases.LocationsDBHelper;
import com.foysaltech.PrayerTimes.datamodels.City;

import org.arabeyes.prayertime.Method;
import org.arabeyes.prayertime.Prayer;
import java.util.Locale;

public class UserSettings {

    public static boolean isNotificationEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("notifications_prayer_time", true);
    }

    public static boolean isAthanEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("notifications_athan", true);
    }

    public static void setMuezzin(Context context, String newValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("notifications_muezzin", newValue);
        editor.apply();
    }

    public static String getMuezzin(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("notifications_muezzin", "ABDELBASET");
    }

    public static int getMuezzinRes(String name, int prayerIdx) {
        int resId;

        if (prayerIdx == 0 || prayerIdx == Prayer.NB_PRAYERS) {
            switch (name) {
                case "ABDELBASET":
                    resId = R.raw.abdulbaset_fajr;
                    break;
                case "ALIMULLA":
                    resId = R.raw.alimulla_fajr;
                    break;
                case "ALQATAMI":
                    resId = R.raw.alqatami_fajr;
                    break;
                case "ASEREHY":
                    resId = R.raw.aserehy_fajr;
                    break;
                case "JOSHAR":
                    resId = R.raw.joshar_fajr;
                    break;
                case "KEFAH":
                    resId = R.raw.kefah_fajr;
                    break;
                case "RIAD":
                    resId = R.raw.riad_fajr;
                    break;
                default:
                    resId = R.raw.abdulbaset_fajr;
                    break;
            }
        }
        else {
            switch (name) {
                case "ABDELBASET":
                    resId = R.raw.abdulbaset;
                    break;
                case "ALIMULLA":
                    resId = R.raw.alimulla;
                    break;
                case "ALQATAMI":
                    resId = R.raw.alqatami;
                    break;
                case "ASEREHY":
                    resId = R.raw.aserehy;
                    break;
                case "JOSHAR":
                    resId = R.raw.joshar;
                    break;
                case "KEFAH":
                    resId = R.raw.kefah;
                    break;
                case "RIAD":
                    resId = R.raw.riad;
                    break;
                default:
                    resId = R.raw.abdulbaset;
                    break;
            }
        }

        return resId;
    }

    public static boolean isVibrateEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("notifications_vibrate", true);
    }

/*
    private int getPrefSyncFrequency(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String str = sharedPref.getString("sync_frequency", "180");
        return Integer.parseInt(str);
    }
*/

    public static City getCity(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String cityStream = sharedPref.getString("locations_search_city", "");
        if (!cityStream.isEmpty()) {
            return City.deserialize(cityStream);
        }
        return null;
    }

    private static boolean numeralsUseCountryCode(Context context, String numerals) {
        return numerals.equals(context.getString(R.string.pref_numerals_default_value));
    }

    public static void setCity(Context context, City city) {
        if (numeralsUseCountryCode(context, getNumerals(context))) {
            // update Locale as it depends on city country
            setLocale(context, null, city.getCountryCode());
        }
        saveCity(context, city);
    }

    private static void saveCity(Context context, City city) {
        // serialize & save new city in shared pref.
        String cityStream = city.serialize();
        if (null != cityStream) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("locations_search_city", cityStream);
            editor.apply();
        }
    }

    private static City updateCity(Context context, String language) {
        City city = getCity(context);
        if (null != city) {
            LocationsDBHelper dbHelper = new LocationsDBHelper(context);
            dbHelper.openReadable();
            city = dbHelper.getCity(city.getId(), language.toUpperCase());
            dbHelper.close();
            if (null != city) {
                saveCity(context, city);
                return city;
            }
        }
        return null;
    }

    public static String getCityName(Context context) {
        City city = getCity(context);
        return null != city ? city.toShortString() : context.getString(R.string.pref_undefined_city);
    }

    public static int getCalculationMethod(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String method = sharedPref.getString("locations_method", String.valueOf(Method.V2_MWL));
        return Integer.parseInt(method);
    }

    public static boolean isMathhabHanafi(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("locations_mathhab_hanafi", false);
    }

    public static int getRounding(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("general_rounding", true) ? 1 : 0;
    }

    public static boolean languageUsesDeviceSettings(Context context, String language) {
        return language.equals(context.getString(R.string.pref_language_default_value));
    }

    private static String getDeviceLanguage()
    {
        return Resources.getSystem().getConfiguration().locale.getLanguage();
    }

    public static String getLanguage(Context context) {
        String lang = getPrefLanguage(context);
        if (languageUsesDeviceSettings(context, lang)) {
            lang = getDeviceLanguage();
        }
        return lang;
    }

    public static String getPrefLanguage(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("general_language", context.getString(R.string.pref_language_default_value));
    }

    public static boolean languageIsArabic(Context context, String language)
    {
        return language.equals("ar") ||
                (languageUsesDeviceSettings(context, language) && getDeviceLanguage().equals("ar"));
    }

    public static String getNumerals(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("general_numerals", context.getString(R.string.pref_numerals_default_value));
    }

    public static Locale getLocale(Context context) {
        String lang = getPrefLanguage(context);
        if (languageUsesDeviceSettings(context, lang)) {
            lang = getDeviceLanguage();
        }

        String numerals = getNumerals(context);
        String countryCode;
        if (numeralsUseCountryCode(context, numerals)) {
            City city = getCity(context);
            countryCode = null != city ? city.getCountryCode() : null;
        }
        else {
            countryCode = numerals;
        }
        return null != countryCode? new Locale(lang, countryCode) : new Locale(lang);
    }

    public static void setLocale(Context context, String newLang, String newCountryCode) {
        // saving in shared prefs. is handled by the framework after the caller
        City city = null;

        if (null != newLang) {
            if (languageUsesDeviceSettings(context, newLang)) {
                newLang = getDeviceLanguage();
            }
            // update saved city as it depends on language
            city = updateCity(context, newLang);
        }
        else {
            newLang = getLanguage(context);
        }

        if (null == newCountryCode) {
            newCountryCode = getNumerals(context);
        }

        if (numeralsUseCountryCode(context, newCountryCode)) {
            if (null == city) {
                city = getCity(context);
            }
            if (null != city) {
                newCountryCode = city.getCountryCode();
            }
            else {
                newCountryCode = null;
            }
        }

        PrayerTimesApp.setLocale(null != newCountryCode ? new Locale(newLang, newCountryCode) : new Locale(newLang));
    }
}
