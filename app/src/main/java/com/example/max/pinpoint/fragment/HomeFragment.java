package com.example.max.pinpoint.fragment;

import android.app.Activity;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;

import android.content.Context;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.accent_systems.ibks_sdk.scanner.ASBleScanner;
import com.accent_systems.ibks_sdk.scanner.ASScannerCallback;
import com.example.max.pinpoint.BeaconData;
import com.example.max.pinpoint.DistanceCalculator;
import com.example.max.pinpoint.R;
import com.example.max.pinpoint.TouchImageView;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;

import static com.example.max.pinpoint.fragment.SetupMap1Fragment.MAX_WALLS;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements ASScannerCallback {

    private OnFragmentInteractionListener mListener;

    Bitmap image = null;
    private List<BeaconData> beacons = new ArrayList<>();
    private List<BeaconData> currentBeacons = new ArrayList<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        Bundle bundle = null;

        if (this.getArguments() != null) {
            bundle = this.getArguments();
            loadImageFromStorage(bundle.getString("filepath"), rootView);

            // TODO: STORE BEACONS VIA DATABASE INSTEAD? PASS THROUGH CLASS OBJECT?
            // Get beacons
            for(int i = 0; i < MAX_WALLS; ++i) {
                BeaconData beacon = bundle.getParcelable("beacon" + Integer.toString(i + 1));
                // Store them for use
                beacons.add(beacon);
            }

            startScan();
        }

        DistanceCalculator distanceCalculator = new DistanceCalculator(beacons);

        // Scan until new value for each beacon, done on callback
        // Calculate distance to each beacon

        // Input distance and coordinates (with center of square being 0,0) to trilateration calc
        
        // Get centroid and draw

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    private void loadImageFromStorage(String path, View v)
    {
        try {
            File f = new File(path, "map.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            TouchImageView img = (TouchImageView) v.findViewById(R.id.mapView);
            img.setImageBitmap(b);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

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
