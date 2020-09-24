package com.example.bikesharingsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.bikesharingsystem.CustomerMap;
import com.example.bikesharingsystem.Model.LocationHelper;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Customer extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;     //Google map variable 
    Location currentLocation;       //Location variable
    FusedLocationProviderClient fusedLocationProviderClient; 
    private static final int REQUEST_CODE = 101; 

    ArrayList markerPoints= new ArrayList();
    ImageButton button1,button2;
    EditText searchSource,searchDestination;    //fields to enter source and destination by user
    private FusedLocationProviderClient mLocationClient;
    LatLng source,destination;  //variables to store locations in latitude,longitude form
    Button requestBTN;
    @Override
    protected void onCreate(Bundle savedInstanceState) {//Activity creation
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);

        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        requestBTN=findViewById(R.id.request_ride);//Request Ride button
        searchSource = findViewById(R.id.editText);
        searchDestination = findViewById(R.id.editText2);
        mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        fetchLocation();//Function to fetch user's current location 

        button1.setOnClickListener(new View.OnClickListener() {//Searching and setting pickup/source location by user
            @Override
            public void onClick(View v) {
                String locationName = searchSource.getText().toString();//user input in Edittext converted to string type
                Geocoder geocoder = new Geocoder(Customer.this, Locale.getDefault());
                try {
                    List<Address> addressList = geocoder.getFromLocationName(locationName, 1); 
                    if (addressList.size() > 0) {
                        Address address = addressList.get(0);//fetch address
                        source = new LatLng(address.getLatitude(), address.getLongitude()); //source LatLng object created 
                        markerPoints.add(source);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(address.getLatitude(), address.getLongitude()), 12.0f)); //create map marker for source location
                        //Toast.makeText(Welcome.this, address.getLocality(), Toast.LENGTH_SHORT).show();
                        mMap.clear();//clear map
                        mMap.addMarker(new MarkerOptions().position(new LatLng(address.getLatitude(), address.getLongitude())).title("Pickup here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));//Add marker to map
                        set_ride();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        button2.setOnClickListener(new View.OnClickListener() {//Searching and setting destination/drop off location by user
            @Override
            public void onClick(View v) {
                String locationName = searchDestination.getText().toString();//user input in Edittext converted to string type
                Geocoder geocoder = new Geocoder(Customer.this, Locale.getDefault());
                try {
                    List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                    if (addressList.size() > 0) {
                        Address address = addressList.get(0);
                        destination = new LatLng(address.getLatitude(), address.getLongitude());//destination LatLng object created 
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(source).title("Pickup here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        markerPoints.add(destination);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(address.getLatitude(), address.getLongitude()), 12.0f));//create map marker for destination
                        //Toast.makeText(Welcome.this, address.getLocality(), Toast.LENGTH_SHORT).show();
                        mMap.addMarker(new MarkerOptions().position(new LatLng(address.getLatitude(), address.getLongitude())).title("Your destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//add marker to map
                        set_ride();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });//button2


    }

    private void fetchLocation() {//FETCH USERS CURRENT LOCATION AND DISPLAY ON MAP
        if ((ActivityCompat.checkSelfPermission(//check location permissions,if not granted ask to grant permission for continuing with the service
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();//get last known location
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {//if location successfully retrieved 
                if (location != null) {
                    currentLocation = location;
                    Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    assert supportMapFragment != null;
                    supportMapFragment.getMapAsync(Customer.this);

                /*
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Customer Location");
                    GeoFire geoFire = new GeoFire(ref);
                    String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    geoFire.setLocation(userid, new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));*/

                }
            }
        });
    }
    public String getAddress(double lat, double lng) {//obtain address from latitude and longitude of location
        Geocoder geocoder = new Geocoder(Customer.this, Locale.getDefault());
        String add = "";
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            add = obj.getAddressLine(0);
            //add = add + "\n" + obj.getCountryName();
            //add = add + "\n" + obj.getCountryCode();
            //add = add + "\n" + obj.getAdminArea();
            //add = add + "\n" + obj.getPostalCode();
            //add = add + "\n" + obj.getSubAdminArea();
            //add = add + "\n" + obj.getLocality();
            //add = add + "\n" + obj.getSubThoroughfare();


            //Log.v("IGA", "Address" + add);
            // Toast.makeText(this, "Address=>" + add,
            // Toast.LENGTH_SHORT).show();

            // TennisAppActivity.showDialog(add);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return add;
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());//convert users curently fetched location to LatLng object
        String add = getAddress(currentLocation.getLatitude(), currentLocation.getLongitude());//obtain address of location in a readable string
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(add);//create marker
        searchSource.setText(add);//set address on serach bar
        //googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5));
        googleMap.addMarker(markerOptions);//add marker to the map
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLocation();
                }
                break;
        }

        return;
    }
    public void set_ride()//if source and destination both are set by the user,allow to create a ride
    {
        if(source!=null && destination!=null)
            findViewById(R.id.request_ride).setEnabled(true);

    }

    public void requestRide(View view){//User requests ride
        String cust_s=getAddress(source.latitude,source.longitude);//customer source address
        String cust_d=getAddress(destination.latitude,destination.longitude);//customer destination address
        //Get system date and time
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());//current date(system generated)
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());//current time(system generated)
        String time=new SimpleDateFormat("HH", Locale.getDefault()).format(new Date());

        String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();//Firebase Authentication object to authenticate current user
        FirebaseDatabase.getInstance().getReference().child("Requests").child(uid).child(currentDate+";"+time).child("source").setValue(cust_s).addOnCompleteListener(new OnCompleteListener<Void>() {//add data to Firebase DB
            @Override
            public void onComplete(@NonNull Task<Void> task) {//data successfully set in DB
                if(task.isSuccessful())
                {
                    Toast.makeText(Customer.this, "Requested ride", Toast.LENGTH_SHORT).show();
                }
               else
                {
                    Toast.makeText(Customer.this, "Unable to create ride", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //helper=new LocationHelper(destination.latitude,destination.longitude);
        FirebaseDatabase.getInstance().getReference().child("Requests").child(uid).child(currentDate+";"+time).child("destination").setValue(cust_d).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                     Toast.makeText(Customer.this, "Location saved", Toast.LENGTH_SHORT).show();
                }
            }
        });
        startActivity(new Intent(getApplicationContext(), CustomerMap.class));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        switch (item.getItemId()) {
            case R.id.Options:
                startActivity(new Intent(getApplicationContext(),Customer_Profile.class));
                return true;


            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

}
