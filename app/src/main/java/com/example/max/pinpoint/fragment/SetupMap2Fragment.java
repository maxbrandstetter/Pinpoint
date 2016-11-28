package com.example.max.pinpoint.fragment;


import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.accent_systems.ibks_sdk.scanner.ASBleScanner;
import com.accent_systems.ibks_sdk.scanner.ASResultParser;
import com.accent_systems.ibks_sdk.scanner.ASScannerCallback;
import com.example.max.pinpoint.BackPressObserver;
import com.example.max.pinpoint.BeaconData;
import com.example.max.pinpoint.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.os.Debug.waitForDebugger;
import static com.example.max.pinpoint.fragment.SetupMap1Fragment.MAX_WALLS;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupMap2Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SetupMap2Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupMap2Fragment extends Fragment implements BackPressObserver, ASScannerCallback {
    private OnFragmentInteractionListener mListener;
    private List<BeaconData> beacons = new ArrayList<>();
    private List<BeaconData> currentBeacons = new ArrayList<>();
    private int timesScanned = 0;
    // TODO: Dynamically create variables based on number of walls
    private double wall0 = 0;
    private double wall1 = 0;
    private double wall2 = 0;
    private double wall3 = 0;
    private double avgLength;
    private double avgWidth;

    public SetupMap2Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetupMap2Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetupMap2Fragment newInstance(String param1, String param2) {
        SetupMap2Fragment fragment = new SetupMap2Fragment();
        return fragment;
    }

    // BackPressObserver override
    @Override
    public boolean isReadyToInterceptBackPress()
    {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_setup_map2, container, false);

        // Gets selected beacons
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            for(int i = 0; i < MAX_WALLS; ++i) {
                BeaconData beacon = bundle.getParcelable("beacon" + Integer.toString(i + 1));
                // Store them for use
                beacons.add(beacon);
            }
            String msg = "";
            for (int i = 0; i < MAX_WALLS; ++i) {
                msg = msg + beacons.get(i).getResult().getScanRecord().getDeviceName() + " ";
            }

            new AlertDialog.Builder(getActivity())
                    .setMessage("The beacon order is: " + msg)
                    .setPositiveButton("Ok", null)
                    .show();
        }


        Button goBack = (Button) rootView.findViewById(R.id.goBackButton);
        goBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setMessage("Going back now will remove current progress.\nContinue?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                                // Go Back
                                Fragment frag = new SetupMap1Fragment();
                                FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
                                fragTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                                fragTransaction.replace(R.id.frame, frag);
                                fragTransaction.commitAllowingStateLoss();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        Button continueBtn = (Button) rootView.findViewById(R.id.continueButton);
        continueBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Fragment frag = new MapFinishedFragment();
                FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
                fragTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

                // Create an bundle to share the wall lengths with the next step
                Bundle args = new Bundle();
                // Add each length to the activity
                args.putDouble("length", avgLength);
                args.putDouble("width", avgWidth);
                frag.setArguments(args);

                fragTransaction.replace(R.id.frame, frag);
                fragTransaction.commitAllowingStateLoss();
            }
        });

        Button scanBtn = (Button) rootView.findViewById(R.id.scanButton);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Scan and store beacons values, get wall lengths, normalize lengths

                // Start scanning
                startScan();

                // Executing code on a 2000ms timer
                final Handler timerHandler = new Handler();
                timerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Check if there are enough elements in currentBeacons. If not, alert the user.
                        if (currentBeacons.size() < MAX_WALLS)
                        {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage("The application needs time to scan. Please try again.")
                                    .setPositiveButton("Ok", null)
                                    .show();

                            // Stop Scanning
                            ASBleScanner.stopScan();
                        }
                        else {
                            // TODO: Alternative check?
                            // Get distances
                            if (timesScanned < MAX_WALLS) {
                                switch (timesScanned) {
                                    case 0:
                                        // Get distance to first and second
                                        wall0 = wall0 + getDistance(0);
                                        wall1 = wall1 + getDistance(1);
                                        ++timesScanned;
                                        currentBeacons.clear(); // Empty the array list
                                        break;
                                    case 1:
                                        // Get distance to second and third
                                        wall1 = wall1 + getDistance(1);
                                        wall2 = wall2 + getDistance(2);
                                        ++timesScanned;
                                        currentBeacons.clear(); // Empty the array list
                                        break;
                                    case 2:
                                        // Get distance to third and fourth
                                        wall2 = wall2 + getDistance(2);
                                        wall3 = wall3 + getDistance(3);
                                        ++timesScanned;
                                        currentBeacons.clear(); // Empty the array list
                                        break;
                                    case 3:
                                        // Get distance to fourth and first
                                        wall3 = wall3 + getDistance(3);
                                        wall0 = wall0 + getDistance(0);
                                        ++timesScanned;
                                        currentBeacons.clear(); // Empty the array list
                                        break;
                                }
                                // Stop Scanning
                                ASBleScanner.stopScan();
                            }

                            if (timesScanned == MAX_WALLS) {
                                // Hard coded for rectangular rooms

                                // Set averages for adjacent walls
                                avgLength = (wall0 + wall2) / 2;
                                avgWidth = (wall1 + wall3) / 2;
                                // Show continue button
                                Button continueButton = (Button) getView().findViewById(R.id.continueButton);
                                if (continueButton.getVisibility() == View.INVISIBLE) {
                                    continueButton.setVisibility(View.VISIBLE);
                                }
                                // Remove scan button
                                Button scanButton = (Button) getView().findViewById(R.id.scanButton);
                                if (scanButton.getVisibility() == View.VISIBLE) {
                                    scanButton.setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                    }
                }, 2000);

            }
        });

        return rootView;
    }

    // Gets the distance from the user's current position to beacons at given indexes
    private double getDistance(int first)
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

    private void startScan()
    {
        new ASBleScanner(this.getActivity(), this).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        ASBleScanner.startScan();
    }

    @Override
    //Callback from ASBleScanner
    public void scannedBleDevices(ScanResult result)
    {
        Log.d("Debug", "Scanned result");
        // Loop through the selected beacons
        // Store result in beacons, IF the results refer to the same device
        for (int i = 0; i < MAX_WALLS; ++i) {
            if (Objects.equals(result.getDevice(), beacons.get(i).getResult().getDevice())) {

                // Check if the beacon (device) is already stored
                boolean contains = false;
                for (int idx = 0; idx < currentBeacons.size(); idx++) {
                    if (Objects.equals(currentBeacons.get(idx).getResult().getDevice(),
                            new BeaconData(result).getResult().getDevice())) {
                        // Device already added
                        contains = true;
                        // Replace the device with updated values in that position
                        currentBeacons.get(idx).setResult(result);
                        break;
                    }
                }

                if (!contains) {
                    // Scanned device not found in the list. NEW => add to list
                    currentBeacons.add(new BeaconData(result));
                }
            }
        }
    }

    // Return the distance
    private double calculateDistance(BeaconData beacon, JSONObject advert)
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

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    /*
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    */

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
