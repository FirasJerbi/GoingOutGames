package com.goinoutgames.www.goinoutgames;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SignupActivity extends AppCompatActivity implements View.OnClickListener  {
    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonRegister;
    private ProgressDialog progressDialog;
    AuthCredential auth;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);


        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        buttonRegister = (Button) findViewById(R.id.buttonSignin);
        editTextUsername=(EditText) findViewById(R.id.editTextUsername);
        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        buttonRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == buttonRegister) {
            signUp();
        }
    }

    private void signUp() {
        final String email = editTextEmail.getText().toString();
        final String username = editTextUsername.getText().toString();
        final String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(username)) {
            //username is empty
            Toast.makeText(this, "please enter a username", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            //email is empty
            Toast.makeText(this, "please enter an email", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            //email is empty
            Toast.makeText(this, "please enter a password", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.setMessage("Logging in...");
        progressDialog.show();
        String s=serverSignup(username,email,password);
        Boolean b=true;
        final JSONObject o;
        try {
            o=new JSONObject(s);

            b=Boolean.parseBoolean(o.get("exist").toString());
            if(b) {

                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    try {
                                        SharedPreferences.Editor editor=getSharedPreferences("gog",MODE_PRIVATE).edit();
                                        editor.putInt("userId", (Integer) o.get("insertId"));
                                        editor.commit();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    finish();
                                } else {
                                    Toast.makeText(SignupActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    task.getException().printStackTrace();
                                }
                            }
                        });
            }else{
                progressDialog.dismiss();
                Toast.makeText(SignupActivity.this,"Email Adress is already in use", Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
    public String serverSignup(String username, String email, String password){
        StringBuilder result = new StringBuilder();
        BufferedReader bufferedReader = null;
        HttpURLConnection conn=null;
        try {
            conn= (HttpURLConnection) new URL("https://gog-backend.herokuapp.com/gogames/addUser").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

            JSONObject data =new JSONObject();

            try {
                data.put("login",username);
                data.put("email",email);
                data.put("pass",password);
                data.put("nicename",username);
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

}
