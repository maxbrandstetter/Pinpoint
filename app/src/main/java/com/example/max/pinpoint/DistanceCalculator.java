package com.example.max.pinpoint;

import com.accent_systems.ibks_sdk.scanner.ASResultParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Max on 2/7/2017.
 */

public class DistanceCalculator {

    // Should always be initial beacons to compare to, not the most recently loaded ones
    List<BeaconData> beacons;

    // Constructor
    public DistanceCalculator(List<BeaconData> allBeacons)
    {
        beacons = allBeacons;
    }
    public DistanceCalculator() {}

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
    public double getDistance(int index, List<BeaconData> currentBeacons)
    {
        ArrayList<Double> distances = new ArrayList<>();
        BeaconData beaconA = null;
        JSONObject advertBeaconA = null;

        // Update values for beacons
        // Set new beacon and advertising data to local variables
        // Check if the beacon was updated
        for (BeaconData beacon : currentBeacons) {
            if (Objects.equals(beacon.getResult().getDevice(), beacons.get(index).getResult().getDevice())
                /*&& !Objects.equals(beacon.getResult(), beacons.get(index).getResult())*/) {
                // If so, set things up
                beaconA = beacon;
                advertBeaconA = ASResultParser.getDataFromAdvertising(beacon.getResult());

                // Calculate distance for current beacon
                distances.add(calculateDistance(beaconA, advertBeaconA));
            }
        }

        // Calculate confidence interval at 95% and remove outliers
        // Get mean
        double sum = 0;
        for (Double d : distances)
            sum += d;
        double mean = sum / distances.size();

        // Get standard deviation
        double difsum = 0;
        for (Double d : distances)
            difsum += (d - mean) * (d - mean);
        double stddev = Math.sqrt(difsum / distances.size());

        // Get confidence interval
        double low = mean - 1.96 * (stddev / Math.sqrt(distances.size()));
        double high = mean + 1.96 * (stddev / Math.sqrt(distances.size()));

        // Remove outliers
        for (int i = 0; i < distances.size(); i++)
        {
            if (distances.get(i) < low || distances.get(i) > high)
            {
                distances.remove(i);
                i--;
            }
        }

        // Recalculate mean
        sum = 0;
        for (Double d : distances)
            sum += d;
        mean = sum / distances.size();

        // Return mean
        return mean;
    }

    public float expansionScale(double width, double length)
    {
        float expansion = 50;

        if (width > length)
            expansion = 1000 / (float) width;
        else if (length > width)
            expansion = 1000 / (float) length;

        return expansion;
    }
}
