package com.example.cafeadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class RegisterSeller extends AppCompatActivity {


    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private EditText etUsername, etPassword,etdesc, etmail, etphone;
    private MaterialButton registerbtn;
    private TextView catlocation;
    private ImageView profileIv;

    //permission constants
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;

    //image pick constants
    private static int IMAGE_PICK_GALLERY_CODE = 400;
    private static int IMAGE_PICK_CAMERA_CODE = 500;

    //permission arrays
    private String [] cameraPermissions;
    private String [] storagePermissions;

    //image picked uri
    private Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_seller);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //init ui views
        etUsername = findViewById(R.id.etusername);
        etPassword = findViewById(R.id.etpassword);
        etdesc = findViewById(R.id.etdesc);
        etmail = findViewById(R.id.etmail);
        catlocation = findViewById(R.id.catlocation);
        registerbtn = findViewById(R.id.registerbtn);
        profileIv = findViewById(R.id.shopProfile);
        etphone = findViewById(R.id.etphone);

        //init permission arrays
        cameraPermissions = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};

        registerbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Register User
                inputData();
            }
        });

        catlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //pick category
                categoryDialog();
            }
        });
        profileIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Pick Image
                showImagePickDialog();
            }
        });
    }


    private String Username,location,Password,ShopDesc,email,phone;
    private void inputData() {
//1) Input Data
        Username = etUsername.getText().toString().trim();
        email = etmail.getText().toString().trim();
        phone = etphone.getText().toString().trim();
        location = catlocation.getText().toString().trim();
        Password = etPassword.getText().toString().trim();
        ShopDesc = etdesc.getText().toString().trim();

        //2) Validate Data


        if(TextUtils.isEmpty(Username)){
            Toast.makeText(this,"Shop Name is Required",Toast.LENGTH_SHORT).show();
            return; //don't proceed further
        }
        if(TextUtils.isEmpty(Password)){
            Toast.makeText(this,"Password is Required",Toast.LENGTH_SHORT).show();
            return; //don't proceed further
        }
        if(TextUtils.isEmpty(ShopDesc)){
            Toast.makeText(this,"Description is Required",Toast.LENGTH_SHORT).show();
            return; //don't proceed further
        }
        if(TextUtils.isEmpty(location)){
            Toast.makeText(this,"Location is Required",Toast.LENGTH_SHORT).show();
            return; //don't proceed further
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this,"Invalid Mail Id",Toast.LENGTH_SHORT).show();
            return;
            //don't proceed further
        }
        if(phone.length()!=10){
            Toast.makeText(this,"Phone must be 10 charcaters",Toast.LENGTH_SHORT).show();
            return; //don't proceed further
        }
        if(Password.length()<6){
            Toast.makeText(this,"Password must be atleast 6 charcaters",Toast.LENGTH_SHORT).show();
            return; //don't proceed further
        }

        createSeller();
        }

    private void createSeller() {
        //3) Add Data to DB
        progressDialog.setTitle("Creating Account...");
        progressDialog.show();

        //Create Account
        firebaseAuth.createUserWithEmailAndPassword(email,Password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //Account created
                        saveFirebasedata();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failed to Account creation
                        Toast.makeText(RegisterSeller.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void saveFirebasedata() {
        progressDialog.setMessage("Saving Account Info...");

        //3) Add Data to DB
        progressDialog.setTitle("Adding Seller...");
        progressDialog.show();

        String timestamp = "" + System.currentTimeMillis();

        if(image_uri == null){
            //upload without image

            //setup data to upload
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid",""+firebaseAuth.getUid());
            hashMap.put("email",""+email);
            hashMap.put("phone",""+phone);
            hashMap.put("shopName",""+Username);
            hashMap.put("location",""+location);
            hashMap.put("Description",""+ShopDesc);
            hashMap.put("shopIcon",""); //no image, set empty
            hashMap.put("timestamp",""+timestamp);

            //Add to DB
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Sellers");
            reference.child(firebaseAuth.getUid()).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //added to DB
                            progressDialog.dismiss();
                            Toast.makeText(RegisterSeller.this,"Seller Added Successfully!",Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(RegisterSeller.this,MainActivity.class);
                            startActivity(i);
//                            clearData();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //FAILED TO ADD SELLER
                            progressDialog.dismiss();
                            Toast.makeText(RegisterSeller.this, ""+e.getMessage(),Toast.LENGTH_SHORT).show();

                        }
                    });



        }else{
            //upload with image

            //First upload img to storage

            //name and path of img to be uploaded
            String filePathAndName = "profile_images/"+""+firebaseAuth.getUid();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //image uploaded
                            //get url of uploaded img
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();

                            if(uriTask.isSuccessful()){
                                //url of img received,upload to db
                                //setup data to upload
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("uid",""+firebaseAuth.getUid());
                                hashMap.put("email",""+email);
                                hashMap.put("phone",""+phone);
                                hashMap.put("shopName",""+Username);
                                hashMap.put("location",""+location);
                                hashMap.put("Description",""+ShopDesc);
                                hashMap.put("shopIcon",""+downloadImageUri); //no image, set empty
                                hashMap.put("timestamp",""+timestamp);

                                //Add to DB
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Sellers");
                                reference.child(firebaseAuth.getUid()).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                //added to DB
                                                progressDialog.dismiss();
                                                Toast.makeText(RegisterSeller.this,"Seller Added Successfully!",Toast.LENGTH_SHORT).show();
//                                                clearData();

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //FAILED TO ADD SELLER
                                                progressDialog.dismiss();
                                                Toast.makeText(RegisterSeller.this, ""+e.getMessage(),Toast.LENGTH_SHORT).show();

                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(RegisterSeller.this, ""+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });

        }

    }


    private void categoryDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location").setItems(Constants.location, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //GET PICKED CATEGORY
                    String category = Constants.location[i];

                    //SET PICKED CATEGORY
                    catlocation.setText(category);
                }
            }).show();
        }

    private void showImagePickDialog(){
        //OPTIONS TO DISPLAY IN DIALOG
        String [] options = {"Camera","Gallery"};
        //DIALOG
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //handle item clicks
                        if(i==0){
                            //camera clicked
                            if(checkCameraPermission()){
                                //permission granted
                                pickFromCamera();
                            }else{
                                //if permission not granted, request
                                requestCameraPermission();
                            }
                        }else{
                            if(checkStoragePermission()){
                                //permission granted
                                pickFromGallery();
                            }else{
                                //if permission not granted, request
                                requestStoragePermission();
                            }
                        }
                    }
                }).show();

    }
    private void pickFromGallery(){
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }
    private void pickFromCamera(){

        //intent to pick image from CAMERA

        //USING MEDIASTORE TO PICK HIGH/ORIGINAL QUALITY IMAGE
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image_Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image_Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result; //returns result as true/ false
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean results = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result && results; //returns result as true/ false

    }
    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    //handle permission results

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && storageAccepted){
                        //both permission granted
                        pickFromCamera();
                    }else{
                        //both or one permission denied
                        Toast.makeText(this, " Camera & Storage Permission Needed", Toast.LENGTH_SHORT).show();
                    }

                }
            }
            case STORAGE_REQUEST_CODE: {
                if(grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        //both permission granted
                        pickFromGallery();
                    }else{
                        //both or one permission denied
                        Toast.makeText(this, " Storage Permission Needed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
//            case default:{
//
//            }

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
//handle image pick results

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                //IMAGE PICKED FROM GALLERY
                //SAVE PICKED IMG URI

                image_uri = data.getData();

                //SET IMAGE
                profileIv.setImageURI(image_uri);
            } else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //IMAGE PICKED FROM CAMERA

                profileIv.setImageURI(image_uri);


            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
