/**  WeBikeSD, Copyright 2014 Code for Philly
 *   
 *   @author Lloyd Emelle <lloyd@codeforamerica.org>
 *   @author Christopher Le Dantec <ledantec@gatech.edu>
 *   @author Anhong Guo <guoanhong15@gmail.com>
 *
 *   Updated/Modified for Philly's app deployment. Based on the
 *   CycleTracks codebase for SFCTA and Cycle Atlanta.
 *
 *   CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 *                                    San Francisco, CA, USA
 *
 *   @author Billy Charlton <billy.charlton@sfcta.org>
 *
 *   This file is part of CycleTracks.
 *
 *   CycleTracks is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   CycleTracks is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with CycleTracks.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.phillyopen.mytracks.cyclephilly;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.*;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainInput extends ActionBarActivity {

    private Toolbar toolbar;
    private final static int MENU_USER_INFO = 0;
    private final static int MENU_CONTACT_US = 1;
    private final static int MENU_MAP = 2;
    private final static int MENU_LEGAL_INFO = 3;
    public final static int PREF_ANONID = 13;
    final String DEGREE  = "\u00b0";
    public final static String FIREBASE_REF = "https://cyclephilly.firebaseio.com";
    Firebase indegoRef;
    Firebase indegoGeofireRef;


    private final static int CONTEXT_RETRY = 0;
    private final static int CONTEXT_DELETE = 1;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private LocationManager locationManager = null;
    private LatLng mySpot = null;

    private ValueEventListener connectedListener;
    TextView weatherText;
    private TextView debugLocation;
    Typeface weatherFont;

    // Commented out following line to remove indego from layout-v11\main.xml

    //private RecyclerView nearbyStations;
    private List<IndegoStation> indegoList = Collections.emptyList();
    private DataSnapshot indegoDataList;
    private RideIndegoAdapter indegoAdapter;

    DbAdapter mDb;
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
                switch (resultCode) {
                    case Activity.RESULT_OK :
                    //TODO: ...try the request again?
                    break;
                }
        	}
        }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d("Location Updates", "Google Play services is available.");
            return;
        // Google Play services was not available for some reason
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(), "Location Updates");
            }
        }
    }
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);

        final Firebase ref = new Firebase("https://cyclephilly.firebaseio.com");
        final Firebase phlref = new Firebase("https://phl.firebaseio.com");
        // Let's handle some launcher lifecycle issues:
		// If we're recording or saving right now, jump to the existing activity.
		// (This handles user who hit BACK button while recording)
		setContentView(R.layout.main);
        weatherFont = Typeface.createFromAsset(getAssets(), "cyclephilly.ttf");

        weatherText = (TextView) findViewById(R.id.weatherView);
        weatherText.setTypeface(weatherFont);
        weatherText.setText(R.string.cloudy);







		
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				int state = rs.getState();
				if (state > RecordingService.STATE_IDLE) {
					if (state == RecordingService.STATE_FULL) {
						startActivity(new Intent(MainInput.this, SaveTrip.class));
					} else {  // RECORDING OR PAUSED:
						startActivity(new Intent(MainInput.this, RecordingActivity.class));
					}
					MainInput.this.finish();
				} else {
					// Idle. First run? Switch to user prefs screen if there are no prefs stored yet
			        SharedPreferences settings = getSharedPreferences("PREFS", 0);
                    String anon = settings.getString(""+PREF_ANONID,"NADA");

			        if (settings.getAll().isEmpty()) {
                        showWelcomeDialog();
			        }else if(anon == "NADA"){
                        showWelcomeDialog();
                    }
					// Not first run - set up the list view of saved trips
					ListView listSavedTrips = (ListView) findViewById(R.id.ListSavedTrips);
					populateList(listSavedTrips);
				}
				MainInput.this.unbindService(this); // race?  this says we no longer care
			}
		};
		// This needs to block until the onServiceConnected (above) completes.
		// Thus, we can check the recording status before continuing on.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);

		// And set up the record button
		final Button startButton = (Button) findViewById(R.id.ButtonStart);
		final Intent i = new Intent(this, RecordingActivity.class);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        final String anon = settings.getString(""+PREF_ANONID,"NADA");

        Firebase weatherRef = new Firebase("https://publicdata-weather.firebaseio.com/philadelphia");
        Firebase tempRef = new Firebase("https://publicdata-weather.firebaseio.com/philadelphia/currently");

//        Commented following block of code to remove weather data in layout-v11\main.xml

        /*tempRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object val = dataSnapshot.getValue();
                String cardinal = null;

                TextView tempState = (TextView) findViewById(R.id.temperatureView);
//                TextView liveTemp = (TextView) findViewById(R.id.warning);
                String apparentTemp = ((Map)val).get("apparentTemperature").toString();
                String windSpeed  = ((Map)val).get("windSpeed").toString();
                Double windValue = (Double)((Map)val).get("windSpeed");
                Long windBearing = (Long)((Map)val).get("windBearing");

//                liveTemp.setText(" "+apparentTemp.toString()+DEGREE);
                WindDirection[] windDirections = WindDirection.values();
                for(int i=0; i<windDirections.length; i++ ){
                    if(windDirections[i].startDegree < windBearing && windDirections[i].endDegree > windBearing){
                        //Get Cardinal direction
                        cardinal = windDirections[i].cardinal;
                    }
                }

                if(windValue > 4){
                    tempState.setTextColor(0xFFDC143C);
                    tempState.setText("winds " + cardinal + " at " + windSpeed + " mph. Ride with caution.");
                }else{
                    tempState.setTextColor(0xFFFFFFFF);
                    tempState.setText("winds " + cardinal + " at " + windSpeed + " mph.");
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });*/

        connectedListener = ref.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean)dataSnapshot.getValue();
                if (connected) {
                    System.out.println("connected "+dataSnapshot.toString());
                    Firebase cycleRef = new Firebase(FIREBASE_REF+"/"+anon+"/connections");
//                    cycleRef.setValue(Boolean.TRUE);
//                    cycleRef.onDisconnect().removeValue();
                } else {
                    System.out.println("disconnected");
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                // No-op
            }
        });
        weatherRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Object value = snapshot.getValue();
                Object hourly = ((Map) value).get("currently");
                String alert = ((Map) hourly).get("summary").toString();
//                TextView weatherAlert = (TextView) findViewById(R.id.weatherAlert);
//                weatherAlert.setText(alert);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        // Acquire a reference to the system Location Manager
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.

                mySpot=new LatLng(location.getLatitude(), location.getLongitude());
                makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Commented out following 2 lines to remove Indego from layout-v11\main.xml

        //nearbyStations = (RecyclerView) findViewById(R.id.nearbyStationList);
        //nearbyStations.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        //Listener for Indego Changes
        indegoRef = new Firebase("https://phl.firebaseio.com/indego/kiosks");
        indegoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Updates! Add them to indego data list
                indegoDataList = dataSnapshot;


            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

// Register the listener with the Location Manager to receive location updates

        indegoGeofireRef = new Firebase("https://phl.firebaseio.com/indego/_geofire");
        GeoFire geoFire = new GeoFire(indegoGeofireRef);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        mySpot = myCurrentLocation();
        indegoList = new ArrayList<IndegoStation>();
        System.out.println("lo: "+mySpot.toString());

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mySpot.longitude,mySpot.latitude), 0.5);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                //Create Indego Station object. To-do: check if object exists
                IndegoStation station = new IndegoStation();
                station.kioskId = key;
                station.location = location;
                if(indegoDataList != null){
                    //get latest info from list
                    station.name = (String) indegoDataList.child(key).child("properties").child("name").getValue();
                }
                System.out.println(station.name);
                indegoList.add(station);
                //To-do: Add indego station info to RideIndegoAdapter

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                System.out.println("GEO READY :"+indegoList.toString());
                indegoAdapter = new RideIndegoAdapter(getApplicationContext(),indegoList);

                // Commented out following line to remove indego from layout-11\main.xml

                //nearbyStations.setAdapter(indegoAdapter);


            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                System.out.println("GEO error");
            }
        });


        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Before we go to record, check GPS status
                final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                } else {
                    startActivity(i);
                    MainInput.this.finish();
                }
            }
        });

        toolbar = (Toolbar) findViewById(R.id.dashboard_bar);
        toolbar.setTitle("WeBikeSD");

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
	}

    private LatLng myCurrentLocation(){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        Location loc = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (loc != null) {
            return new LatLng(loc.getLatitude(), loc.getLongitude());
        } else {
            // try with coarse accuracy
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            loc = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));

            if (loc == null) {
                return new LatLng(39.952451,-75.163664); // city hall by default
            }
        }
        return null;
    }

    private void makeUseOfNewLocation(Location location) {
        System.out.println(location.toString());
        System.out.println("Here.");

        // Commented out following line to remove Indego from layout-v11\main.xml

        //((RideIndegoAdapter)nearbyStations.getAdapter()).removeAll();
//        debugLocation = (TextView) findViewById(R.id.locationDebug);
//        debugLocation.setText(location.toString());
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your phone's GPS is disabled. WeBikeSD needs GPS to determine your location.\n\nGo to System Settings now to enable GPS?")
               .setCancelable(false)
               .setPositiveButton("GPS Settings...", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                       final Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                       startActivityForResult(intent, 0);
                   }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                   }
               });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showWelcomeDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please update your personal details so we can learn a bit about you.\n\nThen, try to use WeBikeSD every time you ride. Your trip routes will be sent to regional transportation planners to improve biking in the Philadelphia area!\n\nThanks,\nThe WeBikeSD team")
               .setCancelable(false).setTitle("Welcome to WeBikeSD!")
               .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                       startActivity(new Intent(MainInput.this, UserInfoActivity.class));
                   }
               });

        final AlertDialog alert = builder.create();
        alert.show();
    }

	void populateList(ListView lv) {
		// Get list from the real phone database. W00t!
		DbAdapter mDb = new DbAdapter(MainInput.this);
		mDb.open();

		// Clean up any bad trips & coords from crashes
		int cleanedTrips = mDb.cleanTables();
		if (cleanedTrips > 0) {
		    Toast.makeText(getBaseContext(),""+cleanedTrips+" bad trip(s) removed.", Toast.LENGTH_SHORT).show();
		}

		try {
			Cursor allTrips = mDb.fetchAllTrips();

			SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
					R.layout.twolinelist, allTrips,
						new String[] { "purp", "fancystart", "fancyinfo"},
						new int[] {R.id.TextView01, R.id.TextView03, R.id.TextInfo}
			);

			lv.setAdapter(sca);
			TextView counter = (TextView) findViewById(R.id.TextViewPreviousTrips);

			int numtrips = allTrips.getCount();
			switch (numtrips) {
			case 0:
				counter.setText("No saved trips.");
				break;
			case 1:
				counter.setText("1 saved trip:");
				break;
			default:
				counter.setText("" + numtrips + " saved trips:");
			}
			// allTrips.close();
		} catch (SQLException sqle) {
			// Do nothing, for now!
		}
		mDb.close();

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
		        Intent i = new Intent(MainInput.this, ShowMap.class);
		        i.putExtra("showtrip", id);
		        startActivity(i);
		    }
		});
		registerForContextMenu(lv);
	}

	@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
	        ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    menu.add(0, CONTEXT_RETRY, 0, "Retry Upload");
	    menu.add(0, CONTEXT_DELETE, 0,  "Delete");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	    case CONTEXT_RETRY:
	        retryTripUpload(info.id);
	        return true;
	    case CONTEXT_DELETE:
	        deleteTrip(info.id);
	        return true;
	    default:
	        return super.onContextItemSelected(item);
	    }
	}

	private void retryTripUpload(long tripId) {
	    TripUploader uploader = new TripUploader(MainInput.this);
        uploader.execute(tripId);
	}

	private void deleteTrip(long tripId) {
	    DbAdapter mDbHelper = new DbAdapter(MainInput.this);
        mDbHelper.open();
        mDbHelper.deleteAllCoordsForTrip(tripId);
        mDbHelper.deleteTrip(tripId);
        mDbHelper.close();
        ListView listSavedTrips = (ListView) findViewById(R.id.ListSavedTrips);
        listSavedTrips.invalidate();
        populateList(listSavedTrips);
    }

	 /* Creates the menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_CONTACT_US, 0, "Contact Us").setIcon(android.R.drawable.ic_dialog_email);
        menu.add(0, MENU_USER_INFO, 0, "Edit User Info").setIcon(android.R.drawable.ic_menu_edit);
        menu.add(0, MENU_MAP, 0, "Cycling Map").setIcon(android.R.drawable.ic_menu_compass);
        menu.add(0, MENU_LEGAL_INFO, 0, "Legal Information").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_USER_INFO:
            startActivity(new Intent(this, UserInfoActivity.class));
            return true;
        case MENU_CONTACT_US:
        	Intent myIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto","cyclephilly@gmail.com", null));

            myIntent.putExtra(Intent.EXTRA_SUBJECT, "WeBikeSD Android App");
            startActivity(Intent.createChooser(myIntent, "Send email..."));
            return true;
        case MENU_MAP:
        	startActivity(new Intent(this, ShowMapNearby.class));
        	return true;
        case MENU_LEGAL_INFO:
        	startActivity(new Intent(this, LicenseActivity.class));
      		return true;
        }
        return false;
    }
}

class FakeAdapter extends SimpleAdapter {
	public FakeAdapter(Context context, List<? extends Map<String, ?>> data,
			int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}
}
