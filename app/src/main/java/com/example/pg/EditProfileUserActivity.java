package com.example.pg;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pg.Adapter.AdapterPost;
import com.example.pg.Model.ModelPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditProfileUserActivity<storageReference> extends AppCompatActivity  implements LocationListener {
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    // views
    private EditText mNameEdit, mLocationEdit, mDateEdit, bioEdit, mUsernameEdit;
    private ImageView mAvatarEdit, mCoverEdit;
    private StorageReference mStorageRefImage;
    private StorageReference mStorageRefCover;
    String postId;
    // Permission constants

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    // image pick constants
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    // Permission arrays

    private String[] cameraPermissions;
    private String[] storagePermissions;

    //image uri
    private Uri imageUri;



    //cover uri
    private Uri coverUri;

    // for checking profile or cover photo
    String profileOrCoverPicture;
    List<ModelPost> postlList ;
    AdapterPost adapterPost ;
    private double latitude = 0.0;
    private double longitude = 0.0;

    //progress dialog
    private ProgressDialog progressDialog;
    ActionBar actionBar;
    FirebaseUser uid;
    //firebase auth
    private FirebaseAuth firebaseAuth;
    //storage
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_user);
        //Actionbar and its title
        actionBar = getSupportActionBar();
        actionBar.setTitle("Edit profile");

        firebaseAuth = FirebaseAuth.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser();

        //get id of the post of using intent
        Intent intent = getIntent();
        postId =  intent.getStringExtra("postId");

        ///path and name of image to be stored in firebase storage
        String filePathAndName = "Users_Profile_Image/"+"_"+firebaseAuth.getUid();
        String filePathAndName2 = "Users_Profile_Cover/"+"_"+firebaseAuth.getUid();
        mStorageRefImage = FirebaseStorage.getInstance().getReference(filePathAndName);
        mStorageRefCover = FirebaseStorage.getInstance().getReference(filePathAndName2);
        //init
        mCoverEdit = findViewById(R.id.coverPEdit);
        mAvatarEdit = findViewById(R.id.avatarPEdit);
        mDateEdit = findViewById(R.id.dateBirthdayPEdit);
        bioEdit = findViewById(R.id.bioPEdit);
        mLocationEdit = findViewById(R.id.locationPEdit);
        mNameEdit = findViewById(R.id.namePEdit);
        mUsernameEdit = findViewById(R.id.usernamePEdit);

        mDateEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MARCH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialog = new DatePickerDialog(EditProfileUserActivity.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mDateSetListener, year,month,day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });
        mDateSetListener = new DatePickerDialog.OnDateSetListener(){
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month +1;
                String date = month + "/" + dayOfMonth + "/" + year;
                mDateEdit.setText(date);

            }

        };


        checkUser();

        //init Permission arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // set up progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog .setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        mAvatarEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileOrCoverPicture = "image";
                // imageProfile = profileOrCoverPicture;
                //pick image
                selectImage(EditProfileUserActivity.this);
            }
        });

        mCoverEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileOrCoverPicture = "cover";
                // coverProfile = profileOrCoverPicture;
                //pick image

                selectImage(EditProfileUserActivity.this);
            }

        });

        mLocationEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // detect location

                if(ContextCompat.checkSelfPermission(EditProfileUserActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != (PackageManager.PERMISSION_GRANTED)){
                    //not allowed, request
                    ActivityCompat.requestPermissions(EditProfileUserActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
                else{
                    //already allowed
                    Toast.makeText(EditProfileUserActivity.this, "Please wait...", Toast.LENGTH_SHORT).show();

                    locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,0, EditProfileUserActivity.this);
                }
            }
        });



    }


    private  boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(EditProfileUserActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }
    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(EditProfileUserActivity.this, storagePermissions, STORAGE_REQUEST_CODE);
    }
    private  boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(EditProfileUserActivity.this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(EditProfileUserActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(EditProfileUserActivity.this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user == null){
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        }
        else{
            loadMyInfo();
        }
    }

    private void loadMyInfo() {
        //load Specialisation info, and set to views
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    String username = ""+ds.child("username").getValue();
                    String name = ""+ds.child("name").getValue();
                    String bio = ""+ds.child("bio").getValue();
                    String image = ""+ds.child("image").getValue();
                    String cover = ""+ds.child("cover").getValue();
                    String location = ""+ds.child("location").getValue();
                    String birthDate = ""+ds.child("Birth date").getValue();

                    mUsernameEdit.setText(username);
                    mNameEdit.setText(name);
                    bioEdit.setText(bio);
                    mDateEdit.setText(birthDate);
                    mLocationEdit.setText(location);
                    try{
                        Picasso.get().load(image).placeholder(R.drawable.ic_add_photo).into(mAvatarEdit);
                    }
                    catch (Exception e){
                        mAvatarEdit.setImageResource(R.drawable.ic_add_photo);
                    }
                    try{
                        Picasso.get().load(cover).placeholder(R.drawable.ic_add_photo).into(mCoverEdit);
                    }
                    catch (Exception e){
                        mCoverEdit.setImageResource(R.drawable.ic_add_photo);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

        latitude =  location.getLatitude();
        longitude =  location.getLongitude();
        findAddress();
    }

    private void findAddress() {
        //find address, country, state and city
        Geocoder geocoder;
        List <Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            String city = addresses.get(0).getLocality();
            String country = addresses.get(0).getCountryName();

            //set addresses
            mLocationEdit.setText(city+", "+country);


        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private  String username, name, specialisation, bio, date, location;
    private void inputData(){
        username = mUsernameEdit.getText().toString().trim();
        name = mNameEdit.getText().toString().trim();
        bio = bioEdit.getText().toString().trim();
        date = mDateEdit.getText().toString().trim();
        location = mLocationEdit.getText().toString().trim();
        if (name.isEmpty()) {
            mNameEdit.setError("Field can't be empty");
        }else if (username.isEmpty()) {
            mUsernameEdit.setError("Field can't be empty");
        }else {
            updateProfile();
        }
    }


    private void selectImage(Context context) {
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose your profile picture");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Take Photo")) {
                    if(!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else{
                        pickFromCamera();
                    }


                } else if (options[item].equals("Choose from Gallery")) {
                    if(!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else{
                        pickFromGallery();
                    }


                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(EditProfileUserActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == (PackageManager.PERMISSION_GRANTED)){
                Toast.makeText(EditProfileUserActivity.this, "Please wait...", Toast.LENGTH_SHORT).show();

                locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, EditProfileUserActivity.this);
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted == true && storageAccepted == true && ContextCompat.checkSelfPermission(EditProfileUserActivity.this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED)){
                        //permission enabled
                        pickFromCamera();
                    }
                    else{
                        //permission denied
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();

                    }
                }
            }
            case STORAGE_REQUEST_CODE:{
                if(grantResults.length > 0 && ContextCompat.checkSelfPermission(EditProfileUserActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED)){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted == true){
                        //permission enabled
                        pickFromGallery();
                    }
                    else{
                        //permission denied
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void pickFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent , IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //put image uri
        if (profileOrCoverPicture == "image"){
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        }
        else{
            coverUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, coverUri);
        }
        startActivityForResult(intent,  IMAGE_PICK_CAMERA_CODE);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Location is disabled", Toast.LENGTH_SHORT).show();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        if (profileOrCoverPicture == "image"){
            if(resultCode == RESULT_OK) {
                if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                    //image picked from gallery, get image of uri

                    imageUri = data.getData();

                    mAvatarEdit.setImageURI(imageUri);
                } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                    //image picked from camera, get image of uri

                    mAvatarEdit.setImageURI(imageUri);
                }
            }
        }
        else {
            if (resultCode == RESULT_OK) {
                if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                    //image picked from gallery, get image of uri

                    coverUri = data.getData();
                    mCoverEdit.setImageURI(coverUri);

                } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                    //image picked from camera, get image of uri
                    mCoverEdit.setImageURI(coverUri);
                }
            }

        }

        super.onActivityResult(requestCode, resultCode, data);


    }

    private void updateProfile() {
        progressDialog.setMessage("Updating Profile...");
        progressDialog.show();
        if (imageUri == null && coverUri == null){
            //update without image
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("username", username);
            hashMap.put("name", name);
            hashMap.put("bio", bio);
            hashMap.put("Birth date", date);
            hashMap.put("location", location);

            // setup data to update
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    //updated
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileUserActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failed to updated
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT);
                }
            });
            // setup data to update username post and name and image if exit
            // if user edit his name and user name and photo change it also in his post
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
            Query query = reference.orderByChild("uid").equalTo(firebaseAuth.getUid());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot ds : snapshot.getChildren()){
                        String child = ds.getKey();
                        snapshot.getRef().child(child).child("uName").setValue(name);
                        snapshot.getRef().child(child).child("uUsername").setValue(username);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            // setup data to update username comment and name and image if exit
            // if user edit his name and user name and photo change it also in his comment
            DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts");
            reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot ds : snapshot.getChildren()){
                        String child = ds.getKey();

                        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts").child(child).child("Comments");
                        Query query2 = reference2.orderByChild("uid").equalTo(firebaseAuth.getUid());
                        query2.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uUsername").setValue(username);
                                    snapshot.getRef().child(child).child("uName").setValue(name);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        } else if(imageUri != null && coverUri == null) {
            // update with image

            //get storage reference
            mStorageRefImage.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //image uploaded, get uri of uploaded image
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) ;
                    final Uri downloadUri = uriTask.getResult();
                    //check if image is uploaded
                    if (uriTask.isSuccessful()) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("username", username);
                        hashMap.put("name", name);
                        hashMap.put("bio", bio);
                        hashMap.put("Birth date", date);
                        hashMap.put("location", location);
                        hashMap.put("image", downloadUri.toString());
                        // setup data to update
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(firebaseAuth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //failed to updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT);
                            }
                        });
                        // setup data to update username post and name and image if exit
                        // if user edit his name and user name and photo change it also in his post
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = reference.orderByChild("uid").equalTo(firebaseAuth.getUid());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uName").setValue(name);
                                    snapshot.getRef().child(child).child("uUsername").setValue(username);
                                    snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        // setup data to update username comment and name and image if exit
                        // if user edit his name and user name and photo change it also in his comment
                        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts");
                        reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();

                                    DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts").child(child).child("Comments");
                                    Query query2 = reference2.orderByChild("uid").equalTo(firebaseAuth.getUid());
                                    query2.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for(DataSnapshot ds : snapshot.getChildren()){
                                                String child = ds.getKey();
                                                snapshot.getRef().child(child).child("uUsername").setValue(username);
                                                snapshot.getRef().child(child).child("uName").setValue(name);
                                                snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        } else if(coverUri != null && imageUri == null){

            // update with image
            //get storage reference
            mStorageRefCover.putFile(coverUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //image uploaded, get uri of uploaded image
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) ;
                    final Uri downloadUriCover = uriTask.getResult();
                    //check if image is uploaded
                    if (uriTask.isSuccessful()) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("username", username);
                        hashMap.put("name", name);
                        hashMap.put("bio", bio);
                        hashMap.put("Birth date", date);
                        hashMap.put("location", location);
                        hashMap.put("cover", downloadUriCover.toString());
                        // setup data to update
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(firebaseAuth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //failed to updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT);
                            }
                        });
                        // setup data to update username post and name and image if exit
                        // if user edit his name and user name and photo change it also in his post
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = reference.orderByChild("uid").equalTo(firebaseAuth.getUid());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uName").setValue(name);
                                    snapshot.getRef().child(child).child("uUsername").setValue(username);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        // setup data to update username comment and name and image if exit
                        // if user edit his name and user name and photo change it also in his comment
                        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts");
                        reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();

                                    DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts").child(child).child("Comments");
                                    Query query2 = reference2.orderByChild("uid").equalTo(firebaseAuth.getUid());
                                    query2.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for(DataSnapshot ds : snapshot.getChildren()){
                                                String child = ds.getKey();
                                                snapshot.getRef().child(child).child("uName").setValue(name);
                                                snapshot.getRef().child(child).child("uUsername").setValue(username);

                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });

        }
        else if(coverUri != null && imageUri != null) {
            // update with image
            //get storage reference
            mStorageRefImage.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //image uploaded, get uri of uploaded image
                    Task<Uri> uriTaskImage = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTaskImage .isSuccessful()) ;
                    final Uri downloadUri = uriTaskImage.getResult();
                    //check if image is uploaded
                    if (uriTaskImage .isSuccessful()) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("username", username);
                        hashMap.put("name", name);
                        hashMap.put("bio", bio);
                        hashMap.put("Birth date", date);
                        hashMap.put("location", location);
                        hashMap.put("image", downloadUri.toString());

                        // setup data to update
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(firebaseAuth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //failed to updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT);
                            }
                        });
                        // setup data to update username post and name and image if exit
                        // if user edit his name and user name and photo change it also in his post
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = reference.orderByChild("uid").equalTo(firebaseAuth.getUid());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uName").setValue(name);
                                    snapshot.getRef().child(child).child("uUsername").setValue(username);
                                    snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        // setup data to update username comment and name and image if exit
                        // if user edit his name and user name and photo change it also in his comment
                        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts");
                        reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();

                                    DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Posts").child(child).child("Comments");
                                    Query query2 = reference2.orderByChild("uid").equalTo(firebaseAuth.getUid());
                                    query2.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for(DataSnapshot ds : snapshot.getChildren()){
                                                String child = ds.getKey();
                                                snapshot.getRef().child(child).child("uUsername").setValue(username);
                                                snapshot.getRef().child(child).child("uName").setValue(name);
                                                snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });


            // update with image

            //get storage reference
            mStorageRefCover.putFile(coverUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //image uploaded, get uri of uploaded image
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) ;
                    Uri downloadUri = uriTask.getResult();
                    //check if image is uploaded
                    if (uriTask.isSuccessful()) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("username", username);
                        hashMap.put("name", name);
                        hashMap.put("bio", bio);
                        hashMap.put("Birth date", date);
                        hashMap.put("location", location);
                        hashMap.put("cover", downloadUri.toString());
                        // setup data to update
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(firebaseAuth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //failed to updated
                                progressDialog.dismiss();
                                Toast.makeText(EditProfileUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT);
                            }
                        });
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.save_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.saveEdittext:
                // User chose the "save" item, save data updated...
                inputData();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}