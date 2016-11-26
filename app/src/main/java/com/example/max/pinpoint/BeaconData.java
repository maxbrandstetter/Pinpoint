package com.example.max.pinpoint;

import android.bluetooth.le.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Max on 11/26/2016.
 */

public class BeaconData implements Parcelable {

    private ScanResult result;
    private String name;

    // Constructor
    public BeaconData(ScanResult result, String name){
        this.result = result;
        this.name = name;
    }

    // Getter and setter methods
    public ScanResult getResult() {
        return result;
    }

    public String getName() {
        return name;
    }

    public void setResult(ScanResult result) {
        this.result = result;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Parcelling part
    public BeaconData(Parcel in){
        this.result = in.readParcelable(ScanResult.class.getClassLoader());
        this.name = in.readString();
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.result, flags);
        dest.writeString(this.name);
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BeaconData createFromParcel(Parcel in) {
            return new BeaconData(in);
        }

        public BeaconData[] newArray(int size) {
            return new BeaconData[size];
        }
    };
}
