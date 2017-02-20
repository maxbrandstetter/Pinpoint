package com.example.max.pinpoint;

import com.accent_systems.ibks_sdk.scanner.ASResultParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

/**
 * Created by Max on 2/7/2017.
 */

public class DistanceCalculator {

    // Variables
    List<BeaconData> beacons;

    // Constructor
    public DistanceCalculator(List<BeaconData> allBeacons)
    {
        beacons = allBeacons;
    }

    // Calculate the distance based on given beacon info
    public double calculateDistance(BeaconData beacon, JSONObject advert)
    {
        /*
         * For distance measurement, we know RSSI and txPower, so we just need to find distance
         *
         * We also know
         * P = -10 * n * log(d) + A
         *
         * So given n = signal propagation constant, assumed 2 across empty space (all cases)
         * d = distance
         * A = txPower
         * P = RSSI
         *
         * d = 10 ^ ((A - P) / (10 * n))
         *
         */
        double n = 2; // Signal propagation constant assumed 2 (across empty space) for all cases
        double A = 0;
        try {
            A = advert.getDouble("AdvTxPower");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 41 dBm is the signal loss that occurs over one meter; negate RSSI (given in negative) by adding since Eddystone is 0 based
        double P = beacon.getResult().getRssi() + 41;

        double result = Math.pow(10, ((A - P) / (10 * n)));
        return result;

    }

    // Gets the distance from the user's current position to beacons at given indexes
    public double getDistance(int first, List<BeaconData> currentBeacons)
    {
        BeaconData beaconA = null;
        JSONObject advertBeaconA = null;

        // Update values for beacons
        // Set new beacon and advertising data to local variables
        // Check if the beacon was updated
        for (BeaconData beacon : currentBeacons) {
            if (Objects.equals(beacon.getResult().getDevice(), beacons.get(first).getResult().getDevice()) &&
                    !Objects.equals(beacon.getResult(), beacons.get(first).getResult())) {
                // If so, set things up
                beaconA = beacon;
                advertBeaconA = ASResultParser.getDataFromAdvertising(beacon.getResult());
                break;
            }
        }

        // Calculate distances
        return calculateDistance(beaconA, advertBeaconA);
    }
}
