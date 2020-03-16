package com.example.officeapp

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.officeapp.MainActivity.Companion.context
import com.google.firebase.firestore.FirebaseFirestore
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MyLocation: BeaconConsumer, MonitorNotifier, RangeNotifier {

    var beaconManager:BeaconManager = BeaconManager.getInstanceForApplication(context)
    var db= FirebaseFirestore.getInstance()
    var TAG="MyLocation"

    var list=ArrayList<ProximityData>()
    lateinit var beaconLocation:HashMap<String,DoubleArray>

    //data stored in firebase firestore
    data class Trilat(var time:String, var xcor: Double, var ycor: Double)

    //data recieved from Proximity class
    data class ProximityData(var beaconID:String, var distance: Double)

    //initializes BeaconManager Specs
    init {
        updateBeaconList()
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))
        BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter::class.java)
        beaconManager.backgroundBetweenScanPeriod = 60000
        beaconManager.backgroundScanPeriod = 20000
        beaconManager.foregroundBetweenScanPeriod = 60000
        beaconManager.foregroundScanPeriod = 20000
    }

    fun recordLocation(){

    }

    fun stopRecording(){

    }

    override fun getApplicationContext(): Context {
        return context
    }

    override fun unbindService(p0: ServiceConnection?) {
        beaconManager.unbind(this)
        Log.d(TAG,"Unbinding Beacon Consumer")
    }

    override fun bindService(p0: Intent?, p1: ServiceConnection?, p2: Int): Boolean {
        Log.d(TAG,"Binding Beacon Consumer")
        return true
    }

    override fun onBeaconServiceConnect() {

        // Set the two identifiers below to null to detect any beacon regardless of identifiers
        var region = Region("nearby-Region",null,null, null)
        beaconManager.addMonitorNotifier(this)

        try
        {
            beaconManager.startMonitoringBeaconsInRegion(region)
        }
        catch (e: RemoteException) { e.printStackTrace() }

        Log.d(TAG, "Looking for beacons in region")
    }

    override fun didDetermineStateForRegion(p0: Int, p1: Region?) {

        //Finding distance(range) from a beacon in region
        if(p1!=null)
        {
            Log.d(TAG, "Saw a beacon $p0")
            beaconManager.startRangingBeaconsInRegion(p1)
            beaconManager.addRangeNotifier(this)
        }

    }

    override fun didEnterRegion(p0: Region?) {
        //When found in Beacon Region
        Log.d(TAG,"Look am in region ")
    }

    override fun didExitRegion(p0: Region?) {
        //Moving out of range region
        Log.d(TAG,"Existing beacon region")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, p1: Region) {

        list.clear()
        Log.d(TAG, "Inside ranging")

        for (beacon in beacons)
        {
            var d=ProximityData(beacon.toString(),beacon.distance)
            list.add(d)
            Log.d(TAG, "I see a beacon " + beacon.toString() + " that is less than " + beacon.distance + " meters away.")
        }
        updatedata()

    }

    private fun updateBeaconList() {

        beaconLocation=HashMap()

        //Function to populate beacons Id and the pos

        var c1=DoubleArray(2)
        var c2=DoubleArray(2)
        var c3=DoubleArray(2)
        var c4=DoubleArray(2)

        c1[0]=0.0;c1[1]=0.0
        beaconLocation["id1: 0xfffffaaaaaaaaaafffff id2: 0x00a0500632a9"] = c1

        c2[0]=0.0;c2[1]=5.5
        beaconLocation["id1: 0xfffffaaaaaaaaaafffff id2: 0x00a0500698e8"] = c2

        c3[0]=3.0;c3[1]=0.0
        beaconLocation["id1: 0xfffffaaaaaaaaaafffff id2: 0x00a0500636df"] = c3

        c4[0]=6.0;c4[1]=5.5
        beaconLocation["id1: 0xfffffaaaaaaaaaafffff id2: 0x00a05006971d"] = c4
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatedata() {
        if (MainActivity.username == "") {
            Log.d("DataSync", "User is null" )
        }

        else {

            Log.d("DataSync", "Got list")

            var positions = arrayOfNulls<DoubleArray>(list.size)
            var distances = DoubleArray(list.size)
            var i = 0
            list.forEach {
                Log.d("Values", "ID:" + it.beaconID + " Dist:" + it.distance)

                //trial Code for Trilatereation
                positions[i] = beaconLocation.getValue(it.beaconID)
                distances[i] = it.distance
                //end of trail code
                i++

                //code for saving distance from each beacon to firestore
                // db.collection("USERS").document(MainActivity.loggedUser).collection("Beacons Seen").add(newEntry)
            }

            //trial Code for Trilateration
            if (list.size > 2) {
                Log.d(
                    "Values",
                    "Postions : ${positions.contentDeepToString()}  \n Distances : ${distances.contentToString()}"
                )
                val solver = NonLinearLeastSquaresSolver(
                    TrilaterationFunction(positions, distances),
                    LevenbergMarquardtOptimizer()
                )
                val optimum = solver.solve()

                // the answer
                val centroid = optimum.point.toArray()


                // error and geometry information; may throw SingularMatrixException depending the threshold argument provided
                //val standardDeviation = optimum.getSigma(0.0)
                // val covarianceMatrix = optimum.getCovariances(0.0)

                Log.d("Trilat", "The mobile is at : " + centroid[0] + " , " + centroid[1])

                //end of trilat calc

                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                val formatted = current.format(formatter)

                var newEntry=Trilat(formatted,centroid[0],centroid[1])
                //update to Firebase
                db.collection("USERS").document(MainActivity.username).collection("Beacons Seen").add(newEntry)
            }
        }
    }
}