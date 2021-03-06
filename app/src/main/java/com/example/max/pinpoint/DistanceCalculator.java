package com.example.max.pinpoint;

import com.accent_systems.ibks_sdk.scanner.ASResultParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
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
    public double calculateDistance(double rssi)
    {
        /*
            We'll be using a power regression formula, or y = A * x^B + C in order to estimate
            distance.  This is based on known values of RSSI at certain distances, with 1m
            measurement being the basis, as it provides the most accuracy at distances less than
            20 meters.
            Output will always be a distance in meters.
            For iBKS beacons, which are being used almost exclusively for this project,
            the values are:
            A = 2.614351365
            B = 4.897481636
            C = -1.614351365
            x = known RSSI / RSSI at 1m
         */
        double A = 2.614351365;
        double B = 4.897481636;
        double C = -1.614351365;
        double X = rssi / -59;

        double result = A * Math.pow(X, B) + C;

        /*
            Distances at less than 1 meter result in negative value, attempt to approximate.
            Since C is meant to "zero" the formula for 1m measurements, removing the constant
            gives a close approximation for 0m measurements.  Rather than using a different
            formula, we'll just remove C and recalculate when the result is negative, for the
            sake of simplicity.
         */
        if (result < 0)
        {
            result = A * Math.pow(X, B);
        }

        return result;
    }

    // Gets the distance from the user's current position to beacons at given indexes
    public double getDistance(int index, List<BeaconData> currentBeacons)
    {
        ArrayList<Integer> values = new ArrayList<>();
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
                // advertBeaconA = ASResultParser.getDataFromAdvertising(beacon.getResult());

                // Add RSSI for current beacon
                values.add(beaconA.getResult().getRssi());
            }
        }

        // Save original values through deep copy
        ArrayList<Integer> originalValues = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            originalValues.add(0);
        }
        Collections.copy(originalValues, values);

        // Calculate confidence interval and remove outliers
        // Get mean
        double sum = 0;
        for (Integer d : values)
            sum += d;
        double mean = sum / values.size();

        // Get standard deviation
        double difsum = 0;
        for (Integer d : values)
            difsum += (d - mean) * (d - mean);
        double stddev = Math.sqrt(difsum / (values.size() - 1));

        // Get confidence interval (95 = 2.093, 90 = 1.729)
        double low = mean - 1.729 * (stddev / Math.sqrt(values.size()));
        double high = mean + 1.729 * (stddev / Math.sqrt(values.size()));

        // Remove outliers
        for (int i = 0; i < values.size(); i++)
        {
            if (values.get(i) < low || values.get(i) > high)
            {
                values.remove(i);
                i--;
            }
        }

        // Recalculate mean
        sum = 0;
        for (Integer d : values)
            sum += d;
        mean = sum / values.size();

        // If all values are removed, just calculate mean of original values
        if (values.isEmpty())
        {
            sum = 0;
            for (Integer d : originalValues)
                sum += d;
            mean = sum / originalValues.size();
        }

        // Return mean distance calculation
        return calculateDistance(mean);
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
