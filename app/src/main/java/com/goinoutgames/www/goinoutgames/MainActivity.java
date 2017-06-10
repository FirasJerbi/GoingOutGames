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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;
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
    private LocationListener locationListener;

    TextView t;
    Double longitude, latitude;
    String city, country;
    GetDataTask task;
    String q;
    List<JSONObject> results;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermissions();

        mAuth = FirebaseAuth.getInstance();

        listViewEvents = (ExpandableListView) findViewById(R.id.listViewEvents);
        t = (TextView) findViewById(R.id.textView);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
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
        };
        configureLocation();

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
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET
            }, 10);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20000, 0, locationListener);
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc != null) {
            Log.e("LastKnownLoction", "longitude = " + loc.getLongitude() + " latitude = " + loc.getLatitude());
            longitude = loc.getLongitude();
            latitude = loc.getLatitude();
        }
        updateLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            try {
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
                    details.add("Event owner : " + j.get("user_nicename").toString());
                    String date = j.get("event_start_date").toString().substring(0, 10);
                    details.add("Date : " + date);
                    details.add("Start time : " + j.get("event_start_time").toString());
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
            super.onPostExecute(result);
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
        String currentCity = "";
        String currentCountry = "";
        country = currentCountry;
        city = currentCity;

        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        List<Address> addressList;
        try {
            addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList.size() > 0) {
                currentCity = addressList.get(0).getLocality();
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


    }

    public void getPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET
            }, 10);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            logOut();
        }
        return super.onOptionsItemSelected(item);
    }
}
