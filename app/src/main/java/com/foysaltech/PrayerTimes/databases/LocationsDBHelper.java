package com.foysaltech.PrayerTimes.databases;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import timber.log.Timber;

import com.foysaltech.PrayerTimes.datamodels.City;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.List;

public class LocationsDBHelper extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "locations.db3";
    private static final int DATABASE_VERSION = 1;
    private static final int DATABASE_CLOSED = -1;
    // Add tables and keys names

    private static final String FORMAT_CITY_QUERY_PREFIX = "SELECT\n" + "cities.cityId as id,\n";
    private static final String FORMAT_CITY_QUERY_SUFFIX = "cities.name%s as nameL,\n" +           // Local name
            "countries.name%s as country,\n" + "timezones.nameEN,\n" +                 // used calculations, not display
            "timezones.name%s,\n" + "cities.latitude as lat,\n" + "cities.longitude as lng,\n" + "cities.altitude,\n" + "cities.countryCode\n" + "FROM cities\n" + "INNER JOIN countries ON cities.countryCode = countries.countryCode\n" + "INNER JOIN timezones ON timezones.tzId = cities.tzId\n" + "%s;";          // WHERE placeholder


    private static final String FORMAT_CITY_QUERY_2NAMES = FORMAT_CITY_QUERY_PREFIX + "cities.nameEN as nameEN,\n" + FORMAT_CITY_QUERY_SUFFIX;

    private static final String FORMAT_CITY_QUERY_1NAME = FORMAT_CITY_QUERY_PREFIX + FORMAT_CITY_QUERY_SUFFIX;


    private SQLiteDatabase mDatabase;
    private int mOpenMode;

    public LocationsDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mOpenMode = DATABASE_CLOSED;
    }

    public void openReadable() {
        switch (mOpenMode) {
            case SQLiteDatabase.OPEN_READONLY:
                Timber.d("DB already open readonly!");
                break;

            case SQLiteDatabase.OPEN_READWRITE:
                mDatabase.close();
                // FALLTHROUGH

            case DATABASE_CLOSED:
            default:
                mOpenMode = SQLiteDatabase.OPEN_READONLY;
                mDatabase = this.getReadableDatabase();
                break;
        }
    }

    public void openWritable() {
        switch (mOpenMode) {
            case SQLiteDatabase.OPEN_READWRITE:
                Timber.d("DB already open readwrite!");
                break;

            case SQLiteDatabase.OPEN_READONLY:
                mDatabase.close();
                // FALLTHROUGH

            case DATABASE_CLOSED:
            default:
                mOpenMode = SQLiteDatabase.OPEN_READWRITE;
                mDatabase = this.getWritableDatabase();
                break;
        }
    }

    public void close() {
        if (null == mDatabase) {
            Timber.d("DB already closed!");
            return;
        }
        mOpenMode = DATABASE_CLOSED;
        mDatabase.close();
        mDatabase = null;
    }

    private String prepareCityQuery(String format, String nameL, String language, String where) {
        String query = String.format(format, nameL,      // city local name
                language,   // country
                language,   // timezone
                where);
        Timber.d(query);
        return query;
    }


    @NonNull
    private String sanitizeLanguage(String language) {
        // TODO update DB & query when a new language support is added
        switch (language) {
            case "AR":
            case "EN":
                break;
            default:
                Timber.e("Language " + language + " is not supported! Falling back to EN");
                language = "EN";
                break;
        }
        return language;
    }

    public City getCity(int id, String language) {
        if (-1 >= id) {
            Timber.e("Bad city id: " + id);
            return null;
        }
        language = sanitizeLanguage(language);

        String query = prepareCityQuery(FORMAT_CITY_QUERY_2NAMES, language, language, "WHERE id = ?");
        Cursor cursor = mDatabase.rawQuery(query, new String[]{String.valueOf(id)});
        City city = null;
        if (cursor.moveToNext()) {
            city = new City(cursor.getInt(0), cursor.getString(2).isEmpty() ? cursor.getString(1) : cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getFloat(6), cursor.getFloat(7), cursor.getInt(8), cursor.getString(9));
        }
        cursor.close();
        Timber.d("city:\n" + city);

        return city;
    }


    // Remaining methods are called only for city search.
    public List<City> searchCity(String city, String language) {
        if (null == mDatabase) {
            Timber.w("Open database first!");
            return null;
        }

        language = sanitizeLanguage(language);

        city = "%" + city + "%";

        // search local name column first, then fall back to english name if none
        String query = prepareCityQuery(FORMAT_CITY_QUERY_1NAME, language, language, "WHERE nameL LIKE ?");
        Cursor cursor = mDatabase.rawQuery(query, new String[]{city});
        if (cursor.getCount() == 0) {
            cursor.close();
            query = prepareCityQuery(FORMAT_CITY_QUERY_1NAME, "EN", language, "WHERE nameL LIKE ?");
            cursor = mDatabase.rawQuery(query, new String[]{city});
        }
        List<City> cityList = new ArrayList<>();
        while (cursor.moveToNext()) {
            cityList.add(new City(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getFloat(5), cursor.getFloat(6), cursor.getInt(7), cursor.getString(8)));
        }
        cursor.close();
        Timber.d("cityList:\n" + cityList);

        return cityList;
    }

    public List<City> searchCity(double lat, double lng, String language) {
        if (null == mDatabase) {
            Timber.w("Open database first!");
            return null;
        }

        language = sanitizeLanguage(language);

        String query = prepareCityQuery(FORMAT_CITY_QUERY_2NAMES, language, language, "WHERE lat BETWEEN ? AND ? AND lng BETWEEN ? AND ?");

        Cursor cursor = mDatabase.rawQuery(query, new String[]{String.valueOf(lat - 0.08), String.valueOf(lat + 0.08), String.valueOf(lng - 0.08), String.valueOf(lng + 0.08)});

        List<City> cityList = new ArrayList<>();
        while (cursor.moveToNext()) {
            cityList.add(new City(cursor.getInt(0), cursor.getString(2).isEmpty() ? cursor.getString(1) : cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getFloat(6), cursor.getFloat(7), cursor.getInt(8), cursor.getString(9)));
        }
        cursor.close();
        Timber.d("cityList:\n" + cityList);

        return cityList;
    }

    public List<String> getAllTimezones(String language) {
        if (null == mDatabase) {
            Timber.d("Open DB first!");
            return null;
        }

        language = sanitizeLanguage(language);

        String query = "SELECT name" + language + " FROM timezones";

        Cursor cursor = mDatabase.rawQuery(query, null);

        List<String> timezones = new ArrayList<>();
        while (cursor.moveToNext()) {
            timezones.add(cursor.getString(0));
        }
        cursor.close();
        Timber.d("timezones:\n" + timezones);

        return timezones;
    }

    public List<String> getAllCountries(String language) {
        if (null == mDatabase) {
            Timber.d("Open DB first!");
            return null;
        }

        language = sanitizeLanguage(language);

        String query = "SELECT name" + language + " FROM countries";

        Cursor cursor = mDatabase.rawQuery(query, null);

        List<String> countries = new ArrayList<>();
        while (cursor.moveToNext()) {
            countries.add(cursor.getString(0));
        }
        cursor.close();
        Timber.d("countries:\n" + countries);

        return countries;
    }

    public List<City> getCities(String country, String language) {
        if (null == mDatabase) {
            Timber.d("Open DB first!");
            return null;
        }

        language = sanitizeLanguage(language);

        String query = prepareCityQuery(FORMAT_CITY_QUERY_2NAMES, language, language, "WHERE country LIKE ?");

        Cursor cursor = mDatabase.rawQuery(query, new String[]{"%" + country + "%"});

        List<City> cities = new ArrayList<>();
        while (cursor.moveToNext()) {
            cities.add(new City(cursor.getInt(0), cursor.getString(2).isEmpty() ? cursor.getString(1) : cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getFloat(6), cursor.getFloat(7), cursor.getInt(8), cursor.getString(9)));
        }
        cursor.close();
        Timber.d("cities:\n" + cities);

        return cities;
    }
}
