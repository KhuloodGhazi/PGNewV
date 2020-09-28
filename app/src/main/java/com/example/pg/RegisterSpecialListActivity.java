package com.example.pg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.regex.Pattern;

public class RegisterSpecialListActivity extends AppCompatActivity {
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^"
                    +"(?=.*[0-9])"+           // at least 1 digit
                    "(?=.*[a-zA-Z])"+      // any letter
                    "(?=.*[@#$%^&+=.])"+   // at least 1 special character
                    "(?=\\S+$)"+         // no white spaces
                    ".{6,}"+           // at least 6 character
                    "$");
    //views
    EditText mEmailEditText, mPasswordEditText,  mUserNameEditText, mSpecialisationEditText,  mNameEditText;
    Button mRegisterButton;
    TextView mHaveAccountTv;

    //progressbar to display while registering user
    ProgressDialog progressDialog;

    //Declare an instance of FirebaseAuth
    private FirebaseAuth mAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_special_list);

        //Actionbar and its title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create Account");
        //enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //init
        mEmailEditText = findViewById(R.id.emailS);
        mPasswordEditText = findViewById(R.id.passwordS);
        mRegisterButton = findViewById(R.id.registerBtnS);
        mUserNameEditText = findViewById(R.id.usernameS);
        mNameEditText = findViewById(R.id.nameS);
        mHaveAccountTv = findViewById(R.id.have_accountTv);
        mSpecialisationEditText = findViewById(R.id.specialisationS);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // In the onCreate() method, initialize the FirebaseAuth instance.
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");

        //handle register button click
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //input email, password, username, specialisation
                final String email = mEmailEditText.getText().toString().trim();
                final String password = mPasswordEditText.getText().toString().trim();
                final String username = mUserNameEditText.getText().toString().trim();
                final String name = mNameEditText.getText().toString().trim();
                final String specialisation = mSpecialisationEditText.getText().toString().trim();
                final Query query  = FirebaseDatabase.getInstance().getReference().child("Specialisations").orderByChild("username").equalTo(username);
                final Query query2  = FirebaseDatabase.getInstance().getReference().child("Users").orderByChild("username").equalTo(username);

                //validate

                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    //set error and focus to email edittext
                    mEmailEditText.setError("Invalid Email");
                    mEmailEditText.setFocusable(true);
                }
                else if (!PASSWORD_PATTERN.matcher(password).matches()){
                    //set error and focus to password edittext

                    mPasswordEditText.setError("at least 1 digit \n"+
                            "any letter \n"+
                            "at least 1 special character \n"+
                            "no white spaces \n"+
                            "at least 6 character \n");
                    mPasswordEditText.setFocusable(true);
                }
                // check empty
                else if(email.isEmpty()){
                    mEmailEditText.setError("Field can't be empty");
                }
                else if (password.isEmpty()){
                    mPasswordEditText.setError("Field can't be empty");
                }
                else if (username.isEmpty()) {
                    mUserNameEditText.setError("Field can't be empty");
                }
                else if (name.isEmpty()) {
                    mNameEditText.setError("Field can't be empty");
                }
                else if (specialisation.isEmpty()) {
                    mSpecialisationEditText.setError("Field can't be empty");
                }else {
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getChildrenCount() > 0) {
                                mUserNameEditText.setError("Already exit...");
                            }

                            else{
                                query2.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.getChildrenCount() > 0) {
                                            mUserNameEditText.setError("Already exit...");
                                        }

                                        else{
                                            registerUser(email, name, username, specialisation, password); //register the user
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }

                                });


                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }

                    });


                }

            }

        });

        // handle login textview click listener
        mHaveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterSpecialListActivity.this, LoginActivity.class));
            }
        });

    }

    private void registerUser(final String email,final String name,  final String username, final String specialisation, final String password) {
        //email and password pattern is valid, show progress dialog and start registering user
        progressDialog.show();

        //Create a new createAccount method which takes in an email address and password, validates them and then creates a new user with the createUserWithEmailAndPassword method.

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, dismiss dialog and start register activity
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(RegisterSpecialListActivity.this, "Registered...\n"+user.getEmail(),
                                    Toast.LENGTH_SHORT).show();

                            // Get user email and uid from auth
                            String email = mAuth.getCurrentUser().getEmail();
                            String uid = mAuth.getCurrentUser().getUid();


                            //when user is register store user information in database
                            HashMap<Object , String> hashMap = new HashMap<>();
                            hashMap.put("email" , email);
                            hashMap.put("uid" , uid);
                            hashMap.put("name" , name); // will add later (e.g. edit profile)
                            hashMap.put("username" , username);
                            hashMap.put("specialisation" , specialisation); // will add later (e.g. edit profile)
                            hashMap.put("image", ""); // will add later (e.g. edit profile)
                            hashMap.put("cover", ""); // will add later (e.g. edit profile)
                            hashMap.put("bio", ""); // will add later (e.g. edit profile)
                            hashMap.put("Birth date", ""); // will add later (e.g. edit profile)
                            hashMap.put("location", ""); // will add later (e.g. edit profile)



                            // initializing firebase database
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            // path to store the user data named "Specialisations"

                            DatabaseReference ref = database.getReference("Specialisations");


                            //put data with in hashmap in database
                            ref.child(uid).setValue(hashMap);

                            startActivity(new Intent(RegisterSpecialListActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            progressDialog.dismiss();
                            Toast.makeText(RegisterSpecialListActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //error, dismiss progress dialog and get and show the error message
                progressDialog.dismiss();
                Toast.makeText(RegisterSpecialListActivity.this, ""+e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

    }
    @Override
    public boolean onSupportNavigateUp(){
        onBackPressed(); // go previous activity
        return super.onSupportNavigateUp();
    }
}