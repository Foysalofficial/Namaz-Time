package com.foysaltech.PrayerTimes.datamodels;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class City implements Serializable {

    private int id;
    private String name;
    private final String country;
    private final String timezoneEN;
    private final String timezone;
    private final float latitude;
    private final float longitude;
    private final float altitude;
    private final String countryCode;

    public City(int i, String n, String c, String tzE, String tz, float lat, float lon, float alt, String cc) {
        id = i;
        name = n;
        country = c;
        timezoneEN = tzE;
        timezone = tz;
        latitude = lat;
        longitude = lon;
        altitude = alt;
        countryCode = cc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getCountryCode() { return countryCode; }

    public String getTimezoneEN() { return timezoneEN; }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getAltitude() {
        return altitude;
    }

    @Override
    public String toString() {
        return toLongString();
    }

    public String toShortString() {
        return name + " (" + country + ")";
    }

    public String toLongString() {
        return name + " (" + timezone + ")";
    }

    public String serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static City deserialize(String str) {
        try {
            byte [] data = Base64.decode(str, Base64.DEFAULT);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            City city = (City) ois.readObject();
            ois.close();
            return city;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
