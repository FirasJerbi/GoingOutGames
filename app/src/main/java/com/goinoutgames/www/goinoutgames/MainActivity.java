package com.goinoutgames.www.goinoutgames;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;

import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by Firas Jerbi on 06/06/2017.
 */

public class MainActivity extends AppCompatActivity {
    private ExpandableListView listViewEvents;
    private LocationManager locationManager;


    TextView t;
    Double longitude, latitude;
    String city, country;
    GetDataTask task;
    String q;
    List<JSONObject> results;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle mToggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getPermissions();
        longitude= new Double(0);
        latitude= new Double(0);
        final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);


        drawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        mAuth = FirebaseAuth.getInstance();
        mToggle=new ActionBarDrawerToggle(this,drawerLayout,R.string.open,R.string.close);
        drawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.sign_out){
                    logOut();
                }
                if(item.getItemId()==R.id.about_us){

                    startActivity(new Intent(MainActivity.this,AboutUs.class));
                    drawerLayout.closeDrawers();
                }
                return false;
            }
        });




        listViewEvents = (ExpandableListView) findViewById(R.id.listViewEvents);
        t = (TextView) findViewById(R.id.textView);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        /*locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                Log.e("LOCATION listener", "longitude = " + longitude + " latitude = " + latitude);
                updateLocation();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };*/
        configureLocation();

        ImageButton addEvent=(ImageButton)findViewById(R.id.addEvent);
        addEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,AddEvent.class));
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    ;
                configureLocation();
                return;
        }
    }

    private void configureLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);
            }
        }

        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20000, 0, locationListener);
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc != null) {
            Log.e("LastKnownLoction", "longitude = " + loc.getLongitude() + " latitude = " + loc.getLatitude());
            longitude = loc.getLongitude();
            latitude = loc.getLatitude();
        }
        else Log.e("Location unknown", "-----------");
        updateLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            try {
                Log.e("Share Prefs", String.valueOf(getSharedPreferences("gog",MODE_PRIVATE).getInt("userId",0)));
                q = "https://gog-backend.herokuapp.com/gogames/getEventsDetails?city=" + URLEncoder.encode(city, "UTF-8") + "&country=" + country;
                Log.w("URL encoded", q);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            task = new GetDataTask();
            task.execute(q);
        } else {

            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

    }


    public void logOut() {
        mAuth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void populateListView(final List<JSONObject> listEvents) {
        if (listEvents.size() > 0) {

            List<String> eventsName = new ArrayList<>();
            HashMap<String, List<String>> hashMap = new HashMap<>();
            int i = 0;

            for (JSONObject j : listEvents) {
                try {
                    eventsName.add(j.get("event_name").toString());
                    List<String> details = new ArrayList<>();
                    //details.add("Event owner : " + j.get("user_nicename").toString());
                    String date = j.get("event_start_date").toString().substring(0, 10);
                    //details.add("Date : " + date);
                    String endDate = j.get("event_end_date").toString().substring(0,10);
                    String endTime = j.get("event_end_time").toString();

                    details.add(j.get("user_nicename").toString()
                            +":" + j.get("name").toString()
                            +":" + date
                            +":" + j.get("event_start_time").toString().substring(0,5)
                            +":" + endDate
                            +":" +endTime.substring(0,5)
                            +":"+j.get("latitude")
                            +":"+j.get("longitude")
                    );
                    hashMap.put(eventsName.get(i), details);
                    i++;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            MyAdapter myAdapter = new MyAdapter(this, eventsName, hashMap);
            listViewEvents.setAdapter(myAdapter);
        } else {
            t.setText("There are no events in the area");
        }
    }


    class GetDataTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Loading data...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            try {
                return getData(params[0]);
            } catch (IOException e) {
                e.printStackTrace();
                return "Network error !";
            }

        }

        @Override
        protected void onPostExecute(String result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                super.onPostExecute(result);
            }
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            results = convertToJSONObject(result);
            //fill the list view here
            populateListView(results);

        }

        public String getData(String urlPath) throws IOException {
            StringBuilder result = new StringBuilder();
            BufferedReader bufferedReader = null;
            //initialize config here
            URL url = null;
            try {

                url = new URL(urlPath);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(10000);
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.connect();

                //Read data response from server
                InputStream inputStream = urlConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line).append("/n");
                }


            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

            }

            return result.toString();
        }

        private List<JSONObject> convertToJSONObject(String result) {


            try {
                List<JSONObject> data = new ArrayList<>();
                String regex = "\\}";
                String[] list = result.split(regex);

                for (int i = 0; i < list.length - 1; i++) {
                    list[i] = list[i].substring(1, list[i].length());
                    list[i] += "}";
                }

                for (int i = 0; i < list.length - 1; i++) {
                    data.add(new JSONObject(list[i]));
                }
                return data;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private void updateLocation() {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

        if(geocoder.isPresent()) {

            String currentCity = "";
            String currentCountry = "";
            country = currentCountry;
            city = currentCity;


            List<Address> addressList;
            try {
                addressList = geocoder.getFromLocation(latitude, longitude, 1);
                if (addressList.size() > 0) {
                    Log.e("geocoder results",addressList.get(0).toString());

                    if(addressList.get(0).getLocality()!=null){
                        currentCity=addressList.get(0).getLocality();
                    }
                    else currentCity = addressList.get(0).getAddressLine(1);
                    currentCountry = addressList.get(0).getCountryCode();
                    country = currentCountry;
                    city = currentCity;
                    Log.e("LOCATION update locaion", "Country = " + country + " city = " + city);
                } else {
                    Log.e("Geocoder ERROR ! ", "cannot get lcoation status");
                    Toast.makeText(MainActivity.this, "Error ! cannot get location status", Toast.LENGTH_SHORT).show();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else Log.e("Geocoder ERROR ! ", "cannot get location status");


    }

    public void getPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mToggle.onOptionsItemSelected(item);

        if(item.getItemId()==R.id.addEvent){
            Log.e("do your thing bro","");
        }

        return super.onOptionsItemSelected(item);
    }







}
