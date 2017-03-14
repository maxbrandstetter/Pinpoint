package com.example.max.pinpoint.fragment;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v4.content.ContextCompat;


import com.accent_systems.ibks_sdk.EDSTService.ASEDSTCallback;
import com.accent_systems.ibks_sdk.EDSTService.ASEDSTDefs;
import com.accent_systems.ibks_sdk.EDSTService.ASEDSTService;
import com.accent_systems.ibks_sdk.EDSTService.ASEDSTSlot;
import com.accent_systems.ibks_sdk.GlobalService.ASGlobalCallback;
import com.accent_systems.ibks_sdk.GlobalService.ASGlobalDefs;
import com.accent_systems.ibks_sdk.GlobalService.ASGlobalService;
import com.accent_systems.ibks_sdk.connections.ASConDevice;
import com.accent_systems.ibks_sdk.connections.ASConDeviceCallback;
import com.accent_systems.ibks_sdk.iBeaconService.ASiBeaconCallback;
import com.accent_systems.ibks_sdk.iBeaconService.ASiBeaconService;
import com.accent_systems.ibks_sdk.iBeaconService.ASiBeaconSlot;
import com.accent_systems.ibks_sdk.scanner.ASBleScanner;
import com.accent_systems.ibks_sdk.scanner.ASResultParser;
import com.accent_systems.ibks_sdk.scanner.ASScannerCallback;
import com.accent_systems.ibks_sdk.utils.ASUtils;
import com.accent_systems.ibks_sdk.utils.AuthorizedServiceTask;
import com.example.max.pinpoint.BackPressObserver;
import com.example.max.pinpoint.BeaconData;
import com.google.android.gms.common.AccountPicker;
import com.google.sample.libproximitybeacon.ProximityBeacon;
import com.google.sample.libproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.max.pinpoint.R;

import static android.os.Debug.waitForDebugger;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupMap1Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SetupMap1Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupMap1Fragment extends Fragment implements BackPressObserver, ASScannerCallback, ASConDeviceCallback, ASEDSTCallback, ASiBeaconCallback, ASGlobalCallback {

    private OnFragmentInteractionListener mListener;

    public static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    public static final int SCOPE_USERLOCATION = 0;
    public static final int SCOPE_CLOUDPLATFORM = 1;
    public static final int MAX_WALLS = 4;

    private List<String> scannedDevicesList;
    private ArrayAdapter<String> adapter;
    private List<BeaconData> activeBeacons = new ArrayList<BeaconData>();
    private List<BeaconData> chosenBeacons = new ArrayList<BeaconData>();

    // Used to store characteristics when reading/writing
    BluetoothGattCharacteristic myCharRead;
    BluetoothGattCharacteristic myCharWrite;

    //DEFINE LAYOUT
    ListView devicesList;

    public static ProximityBeacon client;
    SharedPreferences getPrefs;
    public static Activity actv;
    public int numBeacons  = 0;
    public int mPosition;

    static ProgressDialog connDialog;
    private static String TAG = "SetupMap1Fragment";

    public SetupMap1Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetupMap1Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetupMap1Fragment newInstance(String param1, String param2) {
        SetupMap1Fragment fragment = new SetupMap1Fragment();
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
        View rootView = inflater.inflate(R.layout.fragment_setup_map1, container, false);

        actv = this.getActivity();

        //Define listview in layout
        devicesList = (ListView) rootView.findViewById(R.id.devicesList);

        //Setup list on device click listener
        setupListClickListener();

        // Initialize the devices list
        scannedDevicesList = new ArrayList<>();

        // Initialize the list adapter for the listview with params: Context / Layout file / TextView ID in layout file / Devices list
        adapter = new ArrayAdapter<>(actv, android.R.layout.simple_list_item_1, android.R.id.text1, scannedDevicesList);

        // Set the adapter to the listview
        devicesList.setAdapter(adapter);

        connDialog = new ProgressDialog(SetupMap1Fragment.this.getActivity());
        connDialog.setTitle("Please wait...");

        // checkBlePermissions();

        startScan();
        getPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

        if(getPrefs.getString("clientName", null)!=null){

            try{
                client = new ProximityBeaconImpl(SetupMap1Fragment.this.getActivity(), getPrefs.getString("clientName", null));
                new AuthorizedServiceTask(SetupMap1Fragment.this.getActivity(), getPrefs.getString("clientName", null),SCOPE_USERLOCATION).execute();
                new AuthorizedServiceTask(SetupMap1Fragment.this.getActivity(), getPrefs.getString("clientName", null),SCOPE_CLOUDPLATFORM).execute();
                getProjectList();

            } catch (final Exception ee) {
                Log.i(TAG,"CLIENT ERROR: " + ee.toString());
                pickUserAccount();
            }
        }else{
            pickUserAccount();
        }

        Button goBack = (Button) rootView.findViewById(R.id.goBackButton);
        goBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO: Stop the scanner, throws null reference sometimes
                // ASBleScanner.stopScan();

                // Go back
                new AlertDialog.Builder(getActivity())
                        .setMessage("Going back now will remove current progress.\nContinue?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                                // Go Back
                                Fragment frag = new SetupStartFragment();
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
                // Stop the scanner for now
                ASBleScanner.stopScan();

                // Get ONLY the selected beacons
                for (BeaconData beacon : activeBeacons) {
                    if (beacon.isSelected())
                    {
                        // TODO: Fix bug; beacons store in scanned order not selected order
                        chosenBeacons.add(beacon);
                    }
                }

                // Move to the next page
                Fragment frag = new SetupMap2Fragment();
                FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
                fragTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

                // Create a bundle to share the selected beacons with the next activity
                Bundle args = new Bundle();
                // Add each active beacon to the activity
                for (int i = 0; i < MAX_WALLS; ++i)
                {
                    args.putParcelable("beacon" + Integer.toString(i + 1), chosenBeacons.get(i));
                }
                frag.setArguments(args);

                fragTransaction.replace(R.id.frame, frag);
                fragTransaction.commitAllowingStateLoss();
            }
        });

        return rootView;
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


    /*
        Everything from here on concerns beacon loading and connections
        There's a lot
     */

    private void pickUserAccount() {
        Log.i(TAG, "PICK USER - CALLED");
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) {
            return;
        }else{
            if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
                if (resultCode == Activity.RESULT_OK) {
                    String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    try{
                        client = new ProximityBeaconImpl(SetupMap1Fragment.this.getActivity(), name);

                        new AuthorizedServiceTask(SetupMap1Fragment.this.getActivity(), name,SCOPE_USERLOCATION).execute();
                        new AuthorizedServiceTask(SetupMap1Fragment.this.getActivity(), name,SCOPE_CLOUDPLATFORM).execute();
                        PreferenceManager.getDefaultSharedPreferences(SetupMap1Fragment.this.getActivity()).edit().putString("clientName", name).commit();
                        getProjectList();
                    } catch (final Exception ee) {
                        Log.i(TAG,"CLIENT ERROR: "+ ee.toString());
                    }

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static void getProjectList()
    {
        Callback listProjectsCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d(TAG, "listProjectsCallback - Failed request: " + request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String body = response.body().string();
                if (response.isSuccessful()) {

                    try {
                        JSONObject json = new JSONObject(body);
                        final JSONArray projects = json.getJSONArray("projects");
                        final int numprojects = projects.length();
                        final String [] items = new String[numprojects+1];
                        for(int i=0;i<numprojects;i++)
                        {
                            items[i] =  projects.getJSONObject(i).getString("name");
                        }
                        items[numprojects] = "None";
                        AlertDialog.Builder builder=new AlertDialog.Builder(actv);
                        builder.setTitle("Select a project");
                        builder.setCancelable(false);
                        builder.setItems(items, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {

                                    if(which == numprojects)
                                    {
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", "null").commit();
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", "null").commit();
                                    }
                                    else {
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", projects.getJSONObject(which).getString("projectId")).commit();
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", projects.getJSONObject(which).getString("name")).commit();
                                        String projectId = PreferenceManager.getDefaultSharedPreferences(actv).getString("projectId", "null");
                                        String projectName = PreferenceManager.getDefaultSharedPreferences(actv).getString("projectName", "null");
                                        Log.i(TAG,"Project selected: "+ projectId);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "listProjectsCallback - JSONException", e);
                                }
                            }
                        });

                        builder.show();

                    } catch (JSONException e) {
                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", "null").commit();
                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", "null").commit();
                        Log.i(TAG, "listProjectsCallback - This account has no an associated project");
                    }
                } else {
                    PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", "null").commit();
                    PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", "null").commit();
                    Log.d(TAG, "Unsuccessful project list request: " + body);
                }
            }
        };

        client.getProjectList(listProjectsCallback);
    }

    void setupListClickListener(){
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;

                //Stop the scan
                Log.i(TAG, "SCAN STOPPED");
                ASBleScanner.stopScan();

                //Get the string from the item clicked
                String fullString = scannedDevicesList.get(position);
                //Get only the address from the previous string. Substring from '(' to ')'
                String address = fullString.substring(fullString.indexOf("(")+1, fullString.indexOf(")"));

                Log.i(TAG,"*************************************************");
                Log.i(TAG, "CONNECTION STARTED TO DEVICE "+address);
                Log.i(TAG,"*************************************************");

                // Check that the list of active beacons isn't empty
                if (!activeBeacons.isEmpty())
                {
                    for (BeaconData beacon : activeBeacons)
                    {
                        // Check that the addresses are equal
                        if (Objects.equals(address, beacon.getResult().getDevice().getAddress()))
                        {
                            // Set to active if inactive, inactive if active
                            if (beacon.isSelected())
                            {
                                beacon.setSelected(false);
                                --numBeacons;
                                String temp = Integer.toString(numBeacons);
                                Log.i(TAG, temp);

                                // Remove the highlighting on the list view item
                                parent.getChildAt(position).setBackgroundColor(Color.parseColor("#CCDBDC"));

                                // Make the continue button invisible if it is active
                                Button continueButton = (Button) getView().findViewById(R.id.continueButton);
                                if (continueButton.getVisibility() == View.VISIBLE)
                                {
                                    continueButton.setVisibility(View.INVISIBLE);
                                }
                            }
                            else if (!beacon.isSelected())
                            {
                                // If the max required beacons is already reached, alert the user and do nothing
                                if (numBeacons == MAX_WALLS)
                                {
                                    AlertDialog alertDialog = new AlertDialog.Builder(actv).create();
                                    alertDialog.setTitle("Alert");
                                    alertDialog.setMessage("The maximum number of beacons was reached.\nPlease remove an active beacon before continuing");
                                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.show();
                                    return;
                                }
                                // Otherwise, increment
                                else
                                {
                                    beacon.setSelected(true);
                                    ++numBeacons;
                                    String temp = Integer.toString(numBeacons);
                                    Log.i(TAG, temp);

                                    // Set the highlighting on the list view item
                                    parent.getChildAt(position).setBackgroundColor(Color.parseColor("#80CED7"));

                                    // Allow navigation to the next step if the number of required beacons is reached
                                    if (numBeacons == MAX_WALLS)
                                    {
                                        Button continueButton = (Button) getView().findViewById(R.id.continueButton);
                                        continueButton.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        }
                    }
                }

                ASBleScanner.startScan();
            }
        });
    }

    private void startScan(){
        int err;
        new ASBleScanner(this.getActivity(), this).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        err = ASBleScanner.startScan();
        if(err != ASUtils.TASK_OK) {
            Log.i(TAG, "startScan - Error (" + Integer.toString(err) + ")");

            if(err == ASUtils.ERROR_LOCATION_PERMISSION_NOT_GRANTED){
                requestLocationPermissions();
            }
        }
    }

    @TargetApi(23)
    public void requestLocationPermissions(){
        if (ContextCompat.checkSelfPermission(actv, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "LOCATION PERMISSION GRANTED");
                    startScan();
                } else {
                    Log.i(TAG, "LOCATION PERMISSION NOT GRANTED");
                }
                return;
            }
        }
    }

    @Override
    public void scannedBleDevices(ScanResult result){

        String advertisingString = ASResultParser.byteArrayToHex(result.getScanRecord().getBytes());

        String logstr = result.getDevice().getAddress()+" / RSSI: "+result.getRssi()+" / Adv packet: "+advertisingString;

        // Check if scanned device is already in the list by mac address
        boolean contains = false;
        for(int i=0; i<scannedDevicesList.size(); i++){
            if(scannedDevicesList.get(i).contains(result.getDevice().getAddress())){
                // Device already added
                contains = true;
                // Replace the device with updated values in that position
                scannedDevicesList.set(i, result.getRssi()+"  "+result.getDevice().getName()+ "\n       ("+result.getDevice().getAddress()+")");
                break;
            }
        }

        if(!contains){
            // Scanned device not found in the list. NEW => add to list
            scannedDevicesList.add(result.getRssi()+"  "+result.getDevice().getName()+ "\n       ("+result.getDevice().getAddress()+")");
            // Devices are only added to the activeBeacons list once
            activeBeacons.add(new BeaconData(result));
        }

        // After modify the list, notify the adapter that changes have been made so it updates the UI.
        // UI changes must be done in the main thread
        if (isAdded())
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        JSONObject advData;
        switch (ASResultParser.getAdvertisingType(result)){
            case ASUtils.TYPE_IBEACON:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" AdvTxPower = "+advData.getString("AdvTxPower")+" UUID = "+advData.getString("UUID")+" Major = "+advData.getString("Major")+" Minor = "+advData.getString("Minor"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - iBEACON - "+logstr);
                break;
            case ASUtils.TYPE_EDDYSTONE_UID:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" AdvTxPower = "+advData.getString("AdvTxPower")+" Namespace = "+advData.getString("Namespace")+" Instance = "+advData.getString("Instance"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - UID - "+logstr);
                break;
            case ASUtils.TYPE_EDDYSTONE_URL:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+"  AdvTxPower = "+advData.getString("AdvTxPower")+" Url = "+advData.getString("Url"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - URL - "+logstr);

                break;
            case ASUtils.TYPE_EDDYSTONE_TLM:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 if(advData.getString("Version").equals("0")){
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" Version = "+advData.getString("Version")+" Vbatt = "+advData.getString("Vbatt")+" Temp = "+advData.getString("Temp")+" AdvCount = "+advData.getString("AdvCount")+" TimeUp = "+advData.getString("TimeUp"));
                 }
                 else{
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" Version = "+advData.getString("Version")+" EncryptedTLMData = "+advData.getString("EncryptedTLMData")+" Salt = "+advData.getString("Salt")+" IntegrityCheck = "+advData.getString("IntegrityCheck"));
                 }
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - TLM - "+logstr);
                break;
            case ASUtils.TYPE_EDDYSTONE_EID:
                /**** Example to get EID in Clear by the air ***
                 if(!readingEID) {
                 readingEID = true;
                 new ASEDSTService(null,this,10);
                 ASEDSTService.setClient_ProjectId(client, getPrefs.getString("projectId", null));
                 ASEDSTService.getEIDInClearByTheAir(result);
                 }
                 /**************************************************/
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" AdvTxPower = "+advData.getString("AdvTxPower")+" EID = "+advData.getString("EID"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - EID - "+logstr);
                break;
            case ASUtils.TYPE_DEVICE_CONNECTABLE:
                Log.i(TAG,result.getDevice().getName()+" - CONNECTABLE - "+logstr);
                break;
            case ASUtils.TYPE_UNKNOWN:
                Log.i(TAG,result.getDevice().getName()+" - UNKNOWN - "+logstr);
                break;
            default:
                Log.i(TAG,"ADVERTISING TYPE: "+ "ERROR PARSING");
                break;
        }
    }

    @Override
    // Implementation of ASConDeviceCallback
    public void onChangeStatusConnection(int result, BluetoothGatt blgatt){
        switch (result){
            case ASUtils.GATT_DEV_CONNECTED:
                Log.i(TAG,"onChangeStatusConnection - DEVICE CONNECTED: "+blgatt.getDevice().getName());
                break;
            case ASUtils.GATT_DEV_DISCONNECTED:
                Log.i(TAG,"onChangeStatusConnection - DEVICE DISCONNECTED: "+blgatt.getDevice().getName());
                if (connDialog != null && connDialog.isShowing()) {
                    connDialog.dismiss();
                }
                break;
            default:
                Log.i(TAG,"onChangeStatusConnection - ERROR PARSING");
                break;
        }

    }

    // Implementation of ASConDeviceCallback
    public void onServicesCharDiscovered(int result, BluetoothGatt blgatt, ArrayList<BluetoothGattService> services, ArrayList<BluetoothGattCharacteristic> characteristics)
    {
        switch (result){
            case ASUtils.GATT_SERV_DISCOVERED_OK:
                int err;
                Log.i(TAG, "onServicesCharDiscovered - SERVICES DISCOVERED OK: "+blgatt.getDevice().getName());


                /**** Example to read a characteristic ***
                 myCharRead = ASConDevice.findCharacteristic("00002a28");
                 ASConDevice.readCharacteristic(myCharRead);
                 /*****************************************/


                /**** Example to set Eddystone Slots a characteristic ***

                 ASEDSTSlot[] slots = new ASEDSTSlot[4];
                 ASEDSTService.setClient_ProjectId(client,getPrefs.getString("projectId", null));

                 slots[0] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_UID,800,-4,-35,"0102030405060708090a0b0c0d0e0f11");
                 slots[1] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_EID,950,-4,-35,"1112131415161718191a1b1c1d1e1f200a");
                 slots[2] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_URL,650,0,-21,"http://goo.gl/yb6Mgt");
                 slots[3] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_TLM,60000,4,-17,null);
                 ASEDSTService.setEDSTSlots(slots);
                 /********************************************************/

                /**** Example to set iBeacon Slots a characteristic ***
                 ASiBeaconSlot[] slotsib = new ASiBeaconSlot[2];
                 slotsib[0] = new ASiBeaconSlot(false,800,0,-21,"01010101010101010101010101010101","0002","0003",false);
                 slotsib[1] = new ASiBeaconSlot(false,400,-8,-40,"01010101010101010101010101010102","0004","0005",true);
                 ASiBeaconService.setiBeaconSlots(slotsib);
                 /******************************************************/


                /*** Example to get EDST or iBeacon Slots ***/
                // ASEDSTService.setClient_ProjectId(client,getPrefs.getString("projectId", null));
                ASEDSTService.getEDSTSlots();
                //ASiBeaconService.getiBeaconSlots();
                /********************************************/

                /*** Example to set Characteristics ***
                 ASEDSTService.setActiveSlot(2);
                 //ASEDSTService.setRadioTxPower(-4);
                 //ASiBeaconService.setExtraByte(true);
                 //ASiBeaconService.setUUIDMajorMinor(false,"0102030405060708090a0b0c0d0e0f10","0001","0002");
                 //ASGlobalService.setONOFFAdvertising(9,22);
                 //ASGlobalService.setDeviceName("iBKS-TEST");
                 /*****************************************************/

                /*** Example to get Characteristics ***
                 ASEDSTService.getLockState();
                 //ASiBeaconService.getActiveSlot();
                 //ASGlobalService.getDeviceName();
                 /*****************************************************/

                break;
            case ASUtils.GATT_SERV_DISCOVERED_ERROR:
                Log.i(TAG, "onServicesCharDiscovered - SERVICES DISCOVERED ERROR: "+blgatt.getDevice().getName());
                break;
            default:
                Log.i(TAG, "onServicesCharDiscovered - ERROR PARSING");
                break;
        }
    }

    // Implementation of ASConDeviceCallback
    public void onReadDeviceValues(int result, BluetoothGattCharacteristic characteristic, String value){
        switch (result){
            case ASUtils.GATT_READ_SUCCESSFULL:
                Log.i(TAG, "onReadDeviceValues - READ VALUE: " + value);
                break;
            case ASUtils.GATT_READ_ERROR:
                Log.i(TAG, "onReadDeviceValues - READ ERROR");
                break;
            case ASUtils.GATT_NOTIFICATION_RCV:
                Log.i(TAG, "onReadDeviceValues - READ NOTIFICATION: " + value);
                break;
            case ASUtils.GATT_RSSI_OK:
                Log.i(TAG, "onReadDeviceValues - READ RSSI: " + value);
                break;
            case ASUtils.GATT_RSSI_ERROR:
                Log.i(TAG, "onReadDeviceValues - READ RSSI ERROR");
                break;
            default:
                Log.i(TAG, "onReadDeviceValues - ERROR PARSING");
                break;
        }
    }

    // Implementation of ASConDeviceCallback
    public void onWriteDeviceChar(int result, BluetoothGattCharacteristic characteristic) {
        switch (result) {
            case ASUtils.GATT_WRITE_SUCCESSFULL:
                Log.i(TAG, "onWriteDeviceChar - WRITE SUCCESSFULL on: " + characteristic.getUuid().toString() );
                break;
            case ASUtils.GATT_WRITE_ERROR:
                Log.i(TAG, "onWriteDeviceChar - WRITE ERROR on: " + characteristic.getUuid().toString() );
                break;
            default:
                Log.i(TAG, "onWriteDeviceChar - ERROR PARSING");
                break;
        }
    }

    // Implementation of ASEDSTCallback
    public void onReadEDSTCharacteristic(int result, BluetoothGattCharacteristic characteristic, byte[] readval)
    {
        Log.i(TAG,"onReadEDSTCharacteristic - result = " + result + " characteristic = " + characteristic.getUuid() +" readval = " + ASResultParser.byteArrayToHex(readval));
    }
    // Implementation of ASEDSTCallback
    public void onWriteEDSTCharacteristic(int result, BluetoothGattCharacteristic characteristic)
    {
        Log.i(TAG,"onWriteEDSTCharacteristic - result = " + result /*+ " characteristic = " + characteristic.getUuid()*/ );
    }
    // Implementation of ASEDSTCallback
    public void onEDSTSlotsWrite(int result)
    {
        if(result == ASUtils.WRITE_OK) {
            Log.i(TAG, "onEDSTSlotsWrite - Write OK!");
        }
        else
            Log.i(TAG,"onEDSTSlotsWrite - Error (" + Integer.toString(result) + ")");

    }
    // Implementation of ASEDSTCallback
    public void onGetEDSTSlots(int result, ASEDSTSlot[] slots){
        if(result == ASUtils.READ_OK)
        {
            /**** Reading EID In Clear (if there's a slot configured as EID) ****
             for(int i=0;i<slots.length;i++) {
             if(slots[i].frame_type == ASEDSTDefs.FT_EDDYSTONE_EID) {
             ASEDSTService.setClient_ProjectId(client, getPrefs.getString("projectId", null));
             ASEDSTService.getEIDInClear(i);
             }
             }
             /********************************************************************/
            for(int i=0;i<slots.length;i++){
                Log.i(TAG,"onGetEDSTSlots - slot "+i+" advint = "+ Integer.toString(slots[i].adv_int)+ " txpower = "+ slots[i].tx_power + " advtxpower = "+ slots[i].adv_tx_power +" frame type = 0x"+ Integer.toHexString(slots[i].frame_type)+" data = "+ slots[i].data );
            }
        }
        else
            Log.i(TAG,"onGetEDSTSlots - Error (" + Integer.toString(result) + ")");

        //Close dialog
        if (connDialog != null && connDialog.isShowing()) {
            connDialog.dismiss();
        }
    }
    // Implementation of ASEDSTCallback
    public void onGetEIDInClear(int result, String EID, String msg){
        if(result == ASUtils.READ_OK) {
            Log.i(TAG, "onGetEIDInClear - EID read OK = "+ EID);
        }
        else
            Log.i(TAG,"onGetEIDInClear - Error reading EID (" + Integer.toString(result) + "): "+ msg);

    }


    // Implementation of ASiBeaconCallback
    public void onReadiBeaconCharacteristic(int result, BluetoothGattCharacteristic characteristic, byte[] readval)
    {
        Log.i(TAG,"onReadiBeaconCharacteristic - result = " + result/* + " characteristic = " + characteristic.getUuid() +" readval = " + ASResultParser.byteArrayToHex(readval)*/);
    }
    public void onWriteiBeaconCharacteristic(int result, BluetoothGattCharacteristic characteristic)
    {
        Log.i(TAG,"onWriteiBeaconCharacteristic - result = " + result /*+ " characteristic = " + characteristic.getUuid()*/ );
    }

    // Implementation of ASiBeaconCallback
    public void oniBeaconSlotsWrite(int result)
    {
        if(result == ASUtils.WRITE_OK) {
            Log.i(TAG, "oniBeaconSlotsWrite - Write OK!");
        }
        else
            Log.i(TAG,"oniBeaconSlotsWrite - Error (" + Integer.toString(result) + ")");

    }
    // Implementation of ASiBeaconCallback
    public void onGetiBeaconSlots(int result, ASiBeaconSlot[] slots){
        if(result == ASUtils.READ_OK)
        {
            for(int i=0;i<slots.length;i++){
                Log.i(TAG,"onGetiBeaconSlots - slot "+i+" clear slot = "+ slots[i].clearslot+" advint = "+ Integer.toString(slots[i].adv_int)+" txpower = "+ slots[i].tx_power+" advtxpower = "+ slots[i].adv_tx_power+" uuid = "+ slots[i].UUID+" major = "+ slots[i].Major+" minor = "+ slots[i].Minor+" extra byte = "+ slots[i].ExtraByte);
            }
        }
        else
            Log.i(TAG,"onGetiBeaconSlots - Error (" + Integer.toString(result) + ")");
    }

    // Implementation of ASGlobalCallback
    public void onReadGlobalCharacteristic(int result, BluetoothGattCharacteristic characteristic, byte[] readval)
    {
        if(result == ASUtils.READ_OK) {
            Log.i(TAG, "onReadGlobalCharacteristic - read OK!");
            if(characteristic.getUuid().toString().contains(ASGlobalDefs.DEVICE_NAME)){
                Log.i(TAG,"Device Name is "+ASResultParser.StringHexToAscii(ASResultParser.byteArrayToHex(readval)));
            }
        }
        else
            Log.i(TAG,"onReadGlobalCharacteristic - Error (" + Integer.toString(result) + ")");
    }
    // Implementation of ASGlobalCallback
    public void onWriteGlobalCharacteristic(int result, BluetoothGattCharacteristic characteristic)
    {
        if(result == ASUtils.WRITE_OK) {
            Log.i(TAG, "onWriteGlobalCharacteristic - Write OK!");
        }
        else
            Log.i(TAG,"onWriteGlobalCharacteristic - Error (" + Integer.toString(result) + ")");
    }

}
