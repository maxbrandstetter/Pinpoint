package com.example.max.pinpoint.fragment;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.example.max.pinpoint.BeaconData;
import com.example.max.pinpoint.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.max.pinpoint.fragment.SetupMap1Fragment.MAX_WALLS;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapFinishedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapFinishedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFinishedFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private double length;
    private double width;
    private String filepath;
    private List<BeaconData> beacons = new ArrayList<BeaconData>();

    private OnFragmentInteractionListener mListener;

    public MapFinishedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFinishedFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFinishedFragment newInstance(String param1, String param2) {
        MapFinishedFragment fragment = new MapFinishedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_map_finished, container, false);

        // Gets selected arguments
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            length = bundle.getDouble("length");
            width = bundle.getDouble("width");

            for(int i = 0; i < MAX_WALLS; ++i) {
                BeaconData beacon = bundle.getParcelable("beacon" + Integer.toString(i + 1));
                // Store them for use
                beacons.add(beacon);
            }

            new AlertDialog.Builder(getActivity())
                    .setMessage("Length: " + Double.toString(length) + " Width: " + Double.toString(width))
                    .setPositiveButton("Ok", null)
                    .show();
        }

        generateMap();

        Button continueBtn = (Button) rootView.findViewById(R.id.continueButton);
        continueBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Fragment frag = new HomeFragment();
                FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
                fragTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

                // Pass the bundle to the main activity
                Bundle args = new Bundle();

                // Add the filepath to the bundle
                args.putString("filepath", filepath);

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

        return rootView;
    }

    public void generateMap() {
        Paint myRectPaint = new Paint();
        float x1 = 0;
        float y1 = 0;
        float x2 = (float) length * 100;
        float y2 = (float) width * 100;

        // Create a new image bitmap and attach a brand new canvas to it
        Bitmap tempBitmap = Bitmap.createBitmap((int)(x2), (int)(y2), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);
        myRectPaint.setColor(Color.parseColor("#26686D"));
        myRectPaint.setStyle(Paint.Style.STROKE);
        myRectPaint.setStrokeWidth(10);

        tempCanvas.drawBitmap(tempBitmap, 0, 0, null);

        // Draw what you want on the canvas
        tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);

        // Save to bundle for use in main activity
        filepath = saveToInternalStorage(tempBitmap);
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getActivity().getApplicationContext());
        // Path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "map.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return directory.getAbsolutePath();
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
