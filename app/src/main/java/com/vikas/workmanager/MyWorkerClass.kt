package com.vikas.workmanager

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MyWorkerClass constructor(context: Context, workerParams: WorkerParameters) : Worker(context,
    workerParams
) {

    companion object {
        private const val DEFAULT_START_TIME = "08:00"
        private const val DEFAULT_END_TIME = "19:00"

        private const val TAG = "MyWorkerClass"

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value.
         */
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    /**
     * The current location.
     */
    private var mLocation: Location? = null

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    private var mContext: Context? = null

    /**
     * Callback for changes in location.
     */
    private var mLocationCallback: LocationCallback? = null

    init {
        mContext = context
    }

    override fun doWork(): Result {
        Log.d(TAG, "doWork: Done")
        Log.d(TAG, "onStartJob: STARTING JOB..")
        val dateFormat: DateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val c = Calendar.getInstance()
        val date = c.time
        val formattedDate = dateFormat.format(date)
        try {
            val currentDate = dateFormat.parse(formattedDate)
            val startDate = dateFormat.parse(DEFAULT_START_TIME)
            val endDate = dateFormat.parse(DEFAULT_END_TIME)
            if (currentDate.after(startDate) && currentDate.before(endDate)) {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
                mLocationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                    }
                }
                val mLocationRequest = LocationRequest()
                mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
                mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                try {
                     mFusedLocationClient?.let {
                         it.lastLocation
                         .addOnCompleteListener { task ->
                             if (task.isSuccessful && task.result != null) {
                                 mLocation = task.result
                                 Log.d(TAG, "Location : $mLocation")

                                 // Create the NotificationChannel, but only on API 26+ because
                                 // the NotificationChannel class is new and not in the support library
                                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                     val name: CharSequence = "WorkManager"
                                     val description = "WorkManager"
                                     val importance = NotificationManager.IMPORTANCE_DEFAULT
                                     val channel = NotificationChannel(
                                         "WorkManager",
                                         name,
                                         importance
                                     )
                                     channel.description = description
                                     // Register the channel with the system; you can't change the importance
                                     // or other notification behaviors after this
                                     val notificationManager =
                                         mContext!!.getSystemService(
                                             NotificationManager::class.java
                                         )
                                     notificationManager.createNotificationChannel(channel)
                                 }
                                 val builder = NotificationCompat.Builder(
                                     mContext!!,
                                     "WorkManager"
                                 )
                                     .setSmallIcon(R.drawable.ic_menu_mylocation)
                                     .setContentTitle("New Location Update")
                                     .setContentText(
                                         "You are at " + getCompleteAddress(
                                             mLocation!!.latitude,
                                             mLocation!!.longitude
                                         )
                                     )
                                     .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                     .setStyle(
                                         NotificationCompat.BigTextStyle().bigText(
                                             "You are at " + getCompleteAddress(
                                                 mLocation!!.latitude, mLocation!!.longitude
                                             )
                                         )
                                     )
                                 val notificationManager = NotificationManagerCompat.from(
                                     mContext!!
                                 )

                                 // notificationId is a unique int for each notification that you must define
                                 notificationManager.notify(1001, builder.build())
                                 mFusedLocationClient?.let {
                                     it.removeLocationUpdates(mLocationCallback)
                                 }
                             } else {
                                 Log.w(TAG, "Failed to get location.")
                             }
                         }
                     }
                } catch (unlikely: SecurityException) {
                    Log.e(TAG, "Lost location permission.$unlikely")
                }
                try {
                    mFusedLocationClient?.let{
                        it.requestLocationUpdates(mLocationRequest, null)
                    }
                } catch (unlikely: SecurityException) {
                    //Utils.setRequestingLocationUpdates(this, false);
                    Log.e(
                        TAG,
                        "Lost location permission. Could not request updates. $unlikely"
                    )
                }
            } else {
                Log.d(
                    TAG,
                    "Time up to get location. Your time is : $DEFAULT_START_TIME to $DEFAULT_END_TIME"
                )
            }
        } catch (ignored: ParseException) {
        }
        return Result.success()
    }

    private fun getCompleteAddress(lat:Double, lng:Double):String {

        var finalAddress =""
        val geocoder:Geocoder = Geocoder(mContext, Locale.getDefault())
        try {
            val address:List<Address> = geocoder.getFromLocation(lat,lng,1)
            val returnedAddress:Address = address[0]
            val strReturnedAddress = StringBuilder()

            for (i in 0..returnedAddress.maxAddressLineIndex) {
                strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
            }
            finalAddress = returnedAddress.toString()

        } catch (e:Exception) {
            e.printStackTrace()
        }
        return finalAddress

    }
}