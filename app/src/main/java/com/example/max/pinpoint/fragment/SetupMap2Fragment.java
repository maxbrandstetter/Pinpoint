package com.example.max.pinpoint.fragment;


import android.app.ProgressDialog;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.widget.ExpandableListAdapter;
import android.widget.ProgressBar;

import com.accent_systems.ibks_sdk.scanner.ASBleScanner;
import com.accent_systems.ibks_sdk.scanner.ASResultParser;
import com.accent_systems.ibks_sdk.scanner.ASScannerCallback;
import com.example.max.pinpoint.BackPressObserver;
import com.example.max.pinpoint.BeaconData;
import com.example.max.pinpoint.DistanceCalculator;
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
    private ArrayList<BeaconData> beacons = new ArrayList<BeaconData>();
    private ArrayList<ArrayList<BeaconData>> currentBeacons = new ArrayList<ArrayList<BeaconData>>();
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

        // Initialize currentBeacons as 4 empty lists
        for (int i = 0; i < MAX_WALLS; i++) {
            currentBeacons.add(new ArrayList<>());
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
                // Add each length to the bundle
                args.putDouble("length", avgLength);
                args.putDouble("width", avgWidth);

                // Add the beacons to the bundle
                for (int i = 0; i < MAX_WALLS; ++i)
                {
                    args.putParcelable("beacon" + Integer.toString(i + 1), beacons.get(i));
                }

                frag.setArguments(args);

                fragTransaction.replace(R.id.frame, frag);
                fragTransaction.commitAllowingStateLoss();
            }
        });

        // Setup progress dialog
        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Scanning for beacons...");
        pd.setTitle("Scanning");
        pd.setCancelable(false);

        // Create thread to handle showing and hiding the loading icon
        Handler handler = new Handler();
        Runnable showLoadingIcon = new Runnable() {
            @Override
            public void run() {
                pd.show();
            }
        };
        Runnable hideLoadingIcon = new Runnable() {
            @Override
            public void run() {
                pd.hide();
            }
        };

        Button scanBtn = (Button) rootView.findViewById(R.id.scanButton);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Scan and store beacons values, get wall lengths, normalize lengths
                // Start scanning
                startScan();

                // Show loading icon
                handler.post(showLoadingIcon);

                // Executing code on a timer
                final Handler timerHandler = new Handler();
                timerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Check if there are enough elements in currentBeacons. If not, alert the user.
                        if (currentBeacons.size() == MAX_WALLS) {
                            // Check that enough scans have been made for all beacons
                            if (currentBeacons.get(0).size() == 10 && currentBeacons.get(1).size() == 10
                                    && currentBeacons.get(2).size() == 10 && currentBeacons.get(3).size() == 10) {
                                // Stop Scanning
                                ASBleScanner.stopScan();

                                // Get distances
                                DistanceCalculator distanceCalculator = new DistanceCalculator(beacons);

                                if (timesScanned < MAX_WALLS) {
                                    switch (timesScanned) {
                                        case 0:
                                            // Get distance to first and second
                                            wall0 = wall0 + distanceCalculator.getDistance(0, currentBeacons.get(0));
                                            wall1 = wall1 + distanceCalculator.getDistance(1, currentBeacons.get(1));
                                            ++timesScanned;
                                            // Empty the array lists
                                            for (ArrayList<BeaconData> list : currentBeacons) {
                                                list.clear();
                                            }
                                            // Hide loading icon
                                            handler.post(hideLoadingIcon);
                                            break;
                                        case 1:
                                            // Get distance to second and third
                                            wall1 = wall1 + distanceCalculator.getDistance(1, currentBeacons.get(1));
                                            wall2 = wall2 + distanceCalculator.getDistance(2, currentBeacons.get(2));
                                            ++timesScanned;
                                            // Empty the array lists
                                            for (ArrayList<BeaconData> list : currentBeacons) {
                                                list.clear();
                                            }
                                            // Hide loading icon
                                            handler.post(hideLoadingIcon);
                                            break;
                                        case 2:
                                            // Get distance to third and fourth
                                            wall2 = wall2 + distanceCalculator.getDistance(2, currentBeacons.get(2));
                                            wall3 = wall3 + distanceCalculator.getDistance(3, currentBeacons.get(3));
                                            ++timesScanned;
                                            // Empty the array lists
                                            for (ArrayList<BeaconData> list : currentBeacons) {
                                                list.clear();
                                            }
                                            // Hide loading icon
                                            handler.post(hideLoadingIcon);
                                            break;
                                        case 3:
                                            // Get distance to fourth and first
                                            wall3 = wall3 + distanceCalculator.getDistance(3, currentBeacons.get(3));
                                            wall0 = wall0 + distanceCalculator.getDistance(0, currentBeacons.get(0));
                                            ++timesScanned;
                                            // Empty the array lists
                                            for (ArrayList<BeaconData> list : currentBeacons) {
                                                list.clear();
                                            }
                                            // Hide loading icon
                                            handler.post(hideLoadingIcon);
                                            break;
                                    }
                                    // Stop Scanning
                                    ASBleScanner.stopScan();
                                }

                                if (timesScanned == MAX_WALLS) {
                                    // Hard coded for rectangular rooms
                                    // Set averages for opposite walls
                                    avgWidth = (wall0 + wall2) / 2;
                                    avgLength = (wall1 + wall3) / 2;
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
                            } else {
                                // If not enough beacons have been scanned, recurse and delay again
                                timerHandler.postDelayed(this, 1000);
                            }
                        } else {
                            // If not enough beacons have been scanned, recurse and delay again
                            timerHandler.postDelayed(this, 1000);
                        }
                    }
                }, 1000);
            }
        });
        return rootView;
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
                // List is empty, add to a list equivalent in index to selected beacons
                if (currentBeacons.get(i).isEmpty()) {
                    currentBeacons.get(i).add(new BeaconData(result));
                    break;
                }

                if (Objects.equals(currentBeacons.get(i).get(0).getResult().getDevice(), result.getDevice())) {
                    // Add another device with updated values, if less than some value are in the list
                    if (currentBeacons.get(i).size() < 10) {
                        currentBeacons.get(i).add(new BeaconData(result));
                    }
                }
            }
        }
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
