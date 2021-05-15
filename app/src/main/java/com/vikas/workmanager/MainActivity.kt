package com.vikas.workmanager

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATED_IDENTITY_EQUALS")
class MainActivity : AppCompatActivity() {

    companion object {
        const val LOCATION_PERMISSION_CODE = 100
        private const val TAG = "LocationUpdate"
    }

    private lateinit var appCompatButtonStart:AppCompatButton
    private lateinit var message: TextView
    private lateinit var logs:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appCompatButtonStart = findViewById(R.id.appCompatButtonStart)
        message = findViewById(R.id.message)
        logs = findViewById(R.id.logs)
        init()

    }


    private fun init() {
        if(!checkPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_CODE)
        }

        try {
            if (isWorkScheduled(WorkManager.getInstance(this).getWorkInfosByTag(TAG).get())) {
                appCompatButtonStart.text = getString(R.string.button_text_stop);
                message.text = getString(R.string.message_worker_running);
                logs.text = getString(R.string.log_for_running);
            } else {
                appCompatButtonStart.text = getString(R.string.button_text_start);
                message.text = getString(R.string.message_worker_stopped);
                logs.text = getString(R.string.log_for_stopped);
            }
        } catch (e:Exception) {
            e.printStackTrace()
        }

        appCompatButtonStart.setOnClickListener {
            if(appCompatButtonStart.text.toString() == getString(R.string.button_text_start)){
                //start work

                val periodicWorkRequest:PeriodicWorkRequest = PeriodicWorkRequest.Builder(
                    MyWorkerClass::class.java,
                    1,
                    TimeUnit.MINUTES
                )
                    .addTag(TAG)
                    .build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork("Location", ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest)

                Toast.makeText(this, "Location Worker Started : " + periodicWorkRequest.id, Toast.LENGTH_SHORT).show()
                appCompatButtonStart.text = getString(R.string.button_text_stop);
                message.text = periodicWorkRequest.id.toString();
                logs.text = getString(R.string.log_for_running);
            } else {

                WorkManager.getInstance(this).cancelAllWorkByTag(TAG)

                appCompatButtonStart.text = getString(R.string.button_text_start);
                message.text = getString(R.string.message_worker_stopped);
                logs.text = getString(R.string.log_for_stopped);
            }

        }

    }

    private fun isWorkScheduled(workInfos: List<WorkInfo>?):Boolean {

        var isRunnig = false
        if(workInfos == null || workInfos.isEmpty())
            return false
        for(workStatus in workInfos) {
            isRunnig = workStatus.state == WorkInfo.State.RUNNING || workStatus.state == WorkInfo.State.ENQUEUED
        }
        return isRunnig
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
       when(requestCode) {
           LOCATION_PERMISSION_CODE -> {
               if(grantResults.isNotEmpty()) {
                   val coarseLocation = grantResults[0] === PackageManager.PERMISSION_GRANTED
                   val fineLocation = grantResults[1] === PackageManager.PERMISSION_GRANTED
                   if (coarseLocation && fineLocation) Toast.makeText(
                       this,
                       "Permission Granted",
                       Toast.LENGTH_SHORT
                   ).show() else {
                       Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                   }
               }
           }
       }

    }


    private fun checkPermission():Boolean {
        return ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}