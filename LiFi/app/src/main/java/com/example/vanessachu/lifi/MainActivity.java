package com.example.vanessachu.lifi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    //private ScheduledExecutorService scheduleTaskExecutor;
    private static final int REQUEST_RECORD_AUDIO = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScheduledExecutorService scheduleTaskExecutor= Executors.newScheduledThreadPool(1);
        Receiver Receiver1 = new Receiver();
        RecordInput Record1 = new RecordInput(Receiver1);

        int index;
        int array_length = 12;
        int[] array = new int[array_length];


        System.out.println("Hello!");

        AudioRecordPermission();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            Thread t1 = new Thread(Record1);
            t1.start();
        }

        System.out.println("Recorder initialized");

        scheduleTaskExecutor.scheduleAtFixedRate(Receiver1, 0, 125, TimeUnit.MICROSECONDS);

        while(true){
            for(index = 0; index < array_length; index++){
                array[index] = Receiver1.getCurrentFrame(index);
            }

            for(index = 0; index < array_length; index++) {
                System.out.print(array[index]);
            }

            System.out.println(" ");


            try {
                TimeUnit.MICROSECONDS.sleep(150000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    } // end of onCreate()




    private void AudioRecordPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
