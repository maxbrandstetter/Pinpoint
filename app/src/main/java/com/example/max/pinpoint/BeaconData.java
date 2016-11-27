package com.example.max.pinpoint;

import android.bluetooth.le.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Max on 11/26/2016.
 */

public class BeaconData implements Parcelable {

    private ScanResult result;
    private boolean selected;

    // Constructor
    public BeaconData(ScanResult result){
        this.result = result;
        this.selected = false;
    }

    // Empty constructor
    public BeaconData(){}

    // Getter and setter methods
    public ScanResult getResult() {
        return result;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setResult(ScanResult result) {
        this.result = result;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    // Parcelling part
    public BeaconData(Parcel in){
        this.result = in.readParcelable(ScanResult.class.getClassLoader());
        this.selected = (in.readInt() == 0) ? false : true;
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.result, flags);
        dest.writeInt(this.selected ? 1 : 0);
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
