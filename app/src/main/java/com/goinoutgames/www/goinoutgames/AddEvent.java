package com.goinoutgames.www.goinoutgames;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEvent extends AppCompatActivity implements
        View.OnClickListener,
        OnConnectionFailedListener,
        TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener{


    TextView startDate,endDate,place,eventName;

    private GoogleApiClient mGoogleApiClient;
    Place p;
    Time Stime,Etime,Ctime;
    Date Sdate,Edate,Cdate;
    String startD,endD;
    DatePicker startPicker,endPicker;
    Boolean b;
    Button addButton;
    int PLACE_PICKER_REQUEST = 1;
    private String country;
    private String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);



        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);


        addButton=(Button)findViewById(R.id.add_button);
        startDate=(TextView)findViewById(R.id.event_start_date);
        endDate=(TextView)findViewById(R.id.event_end_date);
        place=(TextView)findViewById(R.id.event_place);
        eventName=(TextView)findViewById(R.id.event_name);

        addButton.setOnClickListener(this);
        startDate.setOnClickListener(this);
        endDate.setOnClickListener(this);
        place.setOnClickListener(this);
        eventName.setOnClickListener(this);


    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,dayOfMonth);
        c.set(Calendar.MONTH,month);
        c.set(Calendar.YEAR,year);
        Log.e("date Picker", view.toString());
        String day=c.getDisplayName(Calendar.DAY_OF_WEEK,Calendar.SHORT,Locale.getDefault());
        String monthD=c.getDisplayName(Calendar.MONTH,Calendar.LONG,Locale.getDefault());

        if(view==startPicker) {
            Sdate=java.sql.Date.valueOf(year+"-"+(month+1)+"-"+dayOfMonth);

            Log.e("start date",Sdate.toString());
            startD = day + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + monthD + " " + c.get(Calendar.YEAR);
            b=true;
        }
        else if (view==endPicker) {
            Edate=java.sql.Date.valueOf(year+"-"+(month+1)+"-"+dayOfMonth);
            Log.e("end date",Sdate.toString());
            endD = day + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + monthD + " " + c.get(Calendar.YEAR);
            b=false;
        }

        int hour=c.get(Calendar.HOUR_OF_DAY);
        int minute=c.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog=new TimePickerDialog(AddEvent.this,AddEvent.this,hour,minute,true);
        timePickerDialog.show();

    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        if(b){
            Stime=new Time(hourOfDay,minute,00);
            startDate.setText(startD+" at "+hourOfDay+":"+minute);
        }
        else{
            Etime=new Time(hourOfDay,minute,00);
            endDate.setText(endD+" at "+hourOfDay+":"+minute);
        }



    }

    @Override
    public void onClick(View v) {
        if(v==startDate){
            int day,month,year;
            day=Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            month=Calendar.getInstance().get(Calendar.MONTH);
            year=Calendar.getInstance().get(Calendar.YEAR);
            DatePickerDialog datePickerDialog=new DatePickerDialog(AddEvent.this,AddEvent.this,year,month,day);
            startPicker=datePickerDialog.getDatePicker();
            datePickerDialog.show();
        }
        if(v==endDate){
            int day,month,year;
            day=Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            month=Calendar.getInstance().get(Calendar.MONTH);
            year=Calendar.getInstance().get(Calendar.YEAR);
            DatePickerDialog datePickerDialog=new DatePickerDialog(AddEvent.this,AddEvent.this,year,month,day);
            Log.e("datepicker dialog id",datePickerDialog.toString());
            endPicker=datePickerDialog.getDatePicker();
            datePickerDialog.show();
        }
        if(v==place){

            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

            try {
                startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        }
        if(v==addButton){
            if (TextUtils.isEmpty(eventName.getText())) {
                Toast.makeText(this, "Event name is missing !", Toast.LENGTH_LONG).show();
                return;
            }
            if (TextUtils.isEmpty(place.getText())) {
                Toast.makeText(this, "please pick a place", Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(startDate.getText())) {
                Toast.makeText(this, "please pick a start date", Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(endDate.getText())) {
                Toast.makeText(this, "please pick an end date", Toast.LENGTH_LONG).show();
                return;
            }

            Calendar c =Calendar.getInstance();
            int day=c.get((Calendar.DAY_OF_MONTH));
            int month= c.get(Calendar.MONTH);
            int year=c.get(Calendar.YEAR);
            Cdate=java.sql.Date.valueOf(year+"-"+(month+1)+"-"+day);

            if(Sdate.compareTo(Cdate)<0){
                Toast.makeText(this, "Cannot create an event in the past", Toast.LENGTH_LONG).show();
                return;
            }

            if(Sdate.compareTo(Edate)>0){
                Toast.makeText(this, "End date should be after the start date", Toast.LENGTH_LONG).show();
                return;
            }
            Ctime=new Time(c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),00);

            if((Sdate.compareTo(Edate)==0)&&((Stime.after(Etime))||(Stime.before(Ctime)))){
                Toast.makeText(this, "please check the time again", Toast.LENGTH_LONG).show();
                return;
            }

            saveData task = new saveData();
            task.execute();


        }




    }
    class saveData extends AsyncTask<String, Void, String>{
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(AddEvent.this);
            progressDialog.setMessage("Adding a new event...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            SaveEvent();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.e("post execute message","  s= "+s);
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            finish();
        }

    }

    public String SaveEvent (){
        StringBuilder result = new StringBuilder();
        BufferedReader bufferedReader = null;
        HttpURLConnection conn=null;
        try {
            conn= (HttpURLConnection) new URL("https://gog-backend.herokuapp.com/gogames/createEvent").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

            JSONObject data =new JSONObject();

            try {
                data.put("event_name",eventName.getText());
                data.put("event_owner",getSharedPreferences("gog",MODE_PRIVATE).getInt("userId",0));
                data.put("event_start_time",Stime);
                data.put("event_start_date",Sdate);
                data.put("event_end_time",Etime);
                data.put("event_end_date",Edate);
                data.put("longitude",p.getLatLng().longitude);
                data.put("latitude",p.getLatLng().latitude);
                data.put("country",country);
                data.put("city",city);
                data.put("event_date_created",new Timestamp(Cdate.getYear(),Cdate.getMonth(),Cdate.getDay(),Ctime.getHours(),Ctime.getMinutes(),00,00));
                data.put("address",p.getAddress());
                data.put("name",p.getName());
            } catch (JSONException e) {
                e.printStackTrace();
            }


            wr.write(data.toString());
            wr.flush();

            InputStream inputStream = conn.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }

        } catch (IOException e) {
            Toast.makeText(this,"Connexion error",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }finally {
            if(conn!=null){
                conn.disconnect();
            }
            if(bufferedReader!=null){
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,"Cannot open google map",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                p = PlacePicker.getPlace(data, this);
                String msg = String.format("Place: %s", p.getName());
                setLocation(p.getLatLng().latitude,p.getLatLng().longitude);
                place.setText(msg);
            }
        }
    }

    private void setLocation(double latitude,double longitude) {
        Geocoder geocoder = new Geocoder(AddEvent.this, Locale.getDefault());

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

                    /*if(addressList.get(0).getLocality()!=null){
                        currentCity=addressList.get(0).getLocality();
                    }
                    else currentCity = addressList.get(0).getAddressLine(1);*/
                    currentCity=addressList.get(0).getAdminArea();
                    currentCountry = addressList.get(0).getCountryCode();
                    country = currentCountry;
                    city = currentCity;
                    Log.e("LOCATION update locaion", "Country = " + country + " city = " + city);
                } else {
                    Log.e("Geocoder ERROR ! ", "cannot get lcoation status");
                    Toast.makeText(AddEvent.this, "Error ! cannot get location status", Toast.LENGTH_SHORT).show();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else Log.e("Geocoder ERROR ! ", "cannot get location status");


    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
