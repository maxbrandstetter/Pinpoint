package com.example.max.pinpoint.fragment;

import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Objects;

import com.accent_systems.ibks_sdk.scanner.ASBleScanner;
import com.accent_systems.ibks_sdk.scanner.ASScannerCallback;
import com.example.max.pinpoint.BeaconData;
import com.example.max.pinpoint.DistanceCalculator;
import com.example.max.pinpoint.R;
import com.example.max.pinpoint.TouchImageView;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

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
    private ArrayList<BeaconData> beacons = new ArrayList<>();
    private ArrayList<BeaconData> retrievedBeacons = new ArrayList<>(); // Beacons retrieved from shared preferences
    private ArrayList<String> beaconAddresses = new ArrayList<>();
    private ArrayList<ArrayList<BeaconData>> currentBeacons = new ArrayList<ArrayList<BeaconData>>();
    private double[] currentDistances = new double[MAX_WALLS];
    private double length;
    private double width;
    private double[] location = new double[2]; // location stored as array of 2 values for a point
    private String filepath;
    private Bitmap map = null;
    private boolean incompatible = false;
    private boolean full = false;

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

            // Store filepath locally
            filepath = bundle.getString("filepath");

            // Get beacons
            for(int i = 0; i < MAX_WALLS; ++i) {
                BeaconData beacon = bundle.getParcelable("beacon" + Integer.toString(i + 1));
                // Store them for use
                beacons.add(beacon);
            }

            // Store filepath in shared preferences
            SharedPreferences settings = this.getContext().getSharedPreferences("pinpoint", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("filepath", filepath);

            // Store beacon addresses
            for (int i = 0; i < MAX_WALLS; i++) {
                editor.putString("Beacon" + Integer.toString(i), beacons.get(i).getResult().getDevice().getAddress());
            }

            // Get length and width
            length = bundle.getDouble("length");
            width = bundle.getDouble("width");

            // Store length and width
            editor.putLong("length", (long)length);
            editor.putLong("width", (long)width);

            // Commit shared preferences
            editor.commit();
        }
        // If no arguments have been passed, check for an existing filepath and load it
        else
        {
            SharedPreferences settings = this.getContext().getSharedPreferences("pinpoint", 0);
            // Returns null if the shared preference is not found
            filepath = settings.getString("filepath", null);
            // If not null, load the image
            if (filepath != null) {
                loadImageFromStorage(filepath, rootView);
            }
            // Get beacon addresses
            // Populate beacons if possible (if not, notify user that map is incompatible); occurs during normal scanning
            for (int i = 0; i < MAX_WALLS; i++) {
                String beaconAddress = settings.getString("Beacon" + Integer.toString(i), "");
                if (beaconAddress != "") {
                    beaconAddresses.add(beaconAddress);
                }
            }

            // Get width and length
            long l = settings.getLong("length", 0);
            if (l != 0) {
                length = l;
            }
            long w = settings.getLong("width", 0);
            if (w != 0) {
                width = w;
            }
        }

        // Initialize currentBeacons as 4 empty lists
        for (int i = 0; i < MAX_WALLS; i++) {
            currentBeacons.add(new ArrayList<>());
        }

        Button pinpoint = (Button) rootView.findViewById(R.id.pinpointme);
        pinpoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Show stop button
                Button stopButton = (Button) getView().findViewById(R.id.stop);
                if (stopButton.getVisibility() == View.INVISIBLE) {
                    stopButton.setVisibility(View.VISIBLE);
                }
                // Remove start button
                Button startButton = (Button) getView().findViewById(R.id.pinpointme);
                if (startButton.getVisibility() == View.VISIBLE) {
                    startButton.setVisibility(View.INVISIBLE);
                }

                // Start scanning
                startScan();

                // Scan until new value for each beacon, done on callback
                // Executes code on a 1 second timer
                final Handler timerHandler = new Handler();
                timerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // If incompatible is true, return since the map and beacons don't match
                        if (incompatible)
                            return;

                        // Check if there are enough elements in currentBeacons. If not, recurse
                        if (currentBeacons.size() == MAX_WALLS) {
                            // Check that enough scans have been done for all beacons
                            if (currentBeacons.get(0).size() == 20 && currentBeacons.get(1).size() == 20
                                    && currentBeacons.get(2).size() == 20 && currentBeacons.get(3).size() == 20) {
                                // Set full to true to enable replacement
                                full = true;

                                DistanceCalculator distanceCalculator = new DistanceCalculator(beacons);

                                // Calculate distance to each beacon
                                for (int i = 0; i < MAX_WALLS; ++i) {
                                    // Ensure that the distances are stored in the same
                                    // order as the current beacons for proper positioning
                                    for (int j = 0; j < MAX_WALLS; ++j) {
                                        if (Objects.equals(currentBeacons.get(i).get(0).getResult().getDevice(),
                                                beacons.get(j).getResult().getDevice())) {
                                            currentDistances[j] = (distanceCalculator.getDistance(i, currentBeacons.get(i)));
                                        }
                                    }
                                }

                                // Set positions of each beacon based on stored/displayed order, where (0,0) is the bottom left corner of the map/room
                                double[][] positions = {{width / 2, 0}, {width, length / 2}, {width / 2, length}, {0, length / 2}};

                                // Input distance and coordinates to trilateration calc
                                NonLinearLeastSquaresSolver triSolver = new NonLinearLeastSquaresSolver
                                        (new TrilaterationFunction(positions, currentDistances), new LevenbergMarquardtOptimizer());
                                LeastSquaresOptimizer.Optimum optimum = triSolver.solve();

                                // Get centroid
                                location = optimum.getPoint().toArray();

                                // Location debug
                                /*
                                new AlertDialog.Builder(getActivity())
                                        .setMessage("X: " + Double.toString(location[0]) + " Y: " + Double.toString(location[1]))
                                        .setPositiveButton("Ok", null)
                                        .show();
                                */

                                // Prevent locations from exceeding bounds
                                if (location[0] > width)
                                    location[0] = width;
                                if (location[0] < 0)
                                    location[0] = 0;
                                if (location[1] > length)
                                    location[1] = length;
                                if (location[1] < 0)
                                    location[1] = 0;

                                // Increase by factor used in map creation (for better resolution)
                                location[0] = location[0] * distanceCalculator.expansionScale(width, length);
                                location[1] = location[1] * distanceCalculator.expansionScale(width, length);

                                // Refresh map with location drawn on
                                if (filepath != null) {
                                    try {
                                        // Set image view
                                        TouchImageView img = (TouchImageView) rootView.findViewById(R.id.mapView);

                                        File f = new File(filepath, "map.jpg");
                                        Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));

                                        // Convert to mutable bitmap to draw on canvas
                                        Bitmap mutableBitmap = b.copy(Bitmap.Config.ARGB_8888, true);

                                        // Create paint object
                                        Paint paint = new Paint();
                                        paint.setColor(Color.parseColor("#80CED7"));
                                        paint.setStyle(Paint.Style.FILL);

                                        float expansion = distanceCalculator.expansionScale(width, length) / 4;
                                        if (expansion < 1)
                                            expansion = 1;

                                        // Create canvas and draw map and location icon to it
                                        Canvas tempCanvas = new Canvas(mutableBitmap);
                                        tempCanvas.drawBitmap(mutableBitmap, 0, 0, null);
                                        tempCanvas.drawCircle((float) location[0], (float) location[1], expansion, paint);

                                        img.setImageBitmap(mutableBitmap);
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // If full, recurse after 5 seconds
                                if (full) {
                                    timerHandler.postDelayed(this, 5000);
                                }
                                else {
                                    // Empty the array lists
                                    for (ArrayList<BeaconData> list : currentBeacons) {
                                        list.clear();
                                    }
                                }
                            }
                            else
                            {
                                // If not enough beacons have been scanned, recurse and delay again
                                timerHandler.postDelayed(this, 1000);
                            }
                        }
                        else
                        {
                            // If not enough beacons have been scanned, recurse and delay again
                            timerHandler.postDelayed(this, 1000);
                        }
                    }
                }, 1000);
            }
        });

        Button stop = (Button) rootView.findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Stop scanning
                ASBleScanner.stopScan();

                // Show start button
                Button startButton = (Button) getView().findViewById(R.id.pinpointme);
                if (startButton.getVisibility() == View.INVISIBLE) {
                    startButton.setVisibility(View.VISIBLE);
                }
                // Remove stop button
                Button stopButton = (Button) getView().findViewById(R.id.stop);
                if (stopButton.getVisibility() == View.VISIBLE) {
                    stopButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        return rootView;
    }

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
            map = b;
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
            // If not enough beacons, attempt to find beacons based on stored addresses
            if (beacons.size() != MAX_WALLS) {
                // Ensure that we have addresses
                if (!beaconAddresses.isEmpty()) {
                    // Add beacon to beacons if it matches an existing address and isn't stored yet
                    if (Objects.equals(result.getDevice().getAddress(), beaconAddresses.get(i))) {
                        boolean stored = false;
                        for (int j = 0; j < retrievedBeacons.size(); j++) {
                            if (retrievedBeacons.isEmpty())
                                break;

                            if (Objects.equals(beaconAddresses.get(i), retrievedBeacons.get(j).getResult().getDevice().getAddress())) {
                                stored = true;
                            }
                        }
                        // If the beacon isn't stored, add it in the order the addresses are stored in
                        if (!stored) {
                            retrievedBeacons.add(new BeaconData(result));
                            // Order beacons if enough have been stored
                            if (retrievedBeacons.size() == MAX_WALLS) {
                                for (int addressIdx = 0; addressIdx < retrievedBeacons.size(); addressIdx++) {
                                    for (int retrievedIdx = 0; retrievedIdx < beaconAddresses.size(); retrievedIdx++) {
                                        // If the retrieved beacon matches the beacon address, add that beacon to beacons
                                        if (Objects.equals(retrievedBeacons.get(retrievedIdx).getResult().getDevice().getAddress(), beaconAddresses.get(addressIdx))) {
                                            beacons.add(retrievedBeacons.get(retrievedIdx));
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                else {
                    // Notify user that map is incompatible
                    new AlertDialog.Builder(getActivity())
                            .setMessage("The current map and beacon setup is incompatible, please create a new map.")
                            .setPositiveButton("Ok", null)
                            .show();
                    // Set incompatible to true to prevent further activity
                    incompatible = true;
                    return;
                }
            }
            // Add beacons and stuff
            else {
                if (Objects.equals(result.getDevice(), beacons.get(i).getResult().getDevice())) {
                    if (currentBeacons.get(i).isEmpty()) {
                        // List is empty, add to a list equivalent in index to selected beacons
                        currentBeacons.get(i).add(new BeaconData(result));
                        break;
                    }

                    // Check if the beacon (device) is already stored
                    boolean contains = false;
                    if (Objects.equals(currentBeacons.get(i).get(0).getResult().getDevice(), result.getDevice())) {

                        // Add another device with updated values, if less than some value are in the list
                        if (currentBeacons.get(i).size() < 20) {
                            currentBeacons.get(i).add(new BeaconData(result));
                        }
                        else
                        {
                            // Treat as queue, remove first element, add new element to back
                            shiftLeft(i);
                            // Add new element
                            currentBeacons.get(i).add(new BeaconData(result));
                        }
                    }
                }
            }
        }
    }

    // Shift currentBeacons left by one place
    public void shiftLeft(int index)
    {
        //make a loop to run through the array list, starting at the first element
        for (int i = 1; i < currentBeacons.get(index).size(); i++) {
            //set the current element in the current list to the previous element
            currentBeacons.get(index).set(i - 1, currentBeacons.get(index).get(i));
        }
        //remove the last element
        currentBeacons.get(index).remove(currentBeacons.get(index).size() - 1);
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
        void onFragmentInteraction(Uri uri);
    }
}
