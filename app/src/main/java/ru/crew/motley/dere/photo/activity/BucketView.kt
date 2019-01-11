package ru.crew.motley.dere.photo.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import ru.crew.motley.dere.R
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import kotlinx.android.synthetic.main.bottom_nav_bar_new.*
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.support.annotation.NonNull
import java.util.*
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.support.v4.app.FragmentManager
import android.widget.Toast
import ru.crew.motley.dere.gallery.fragment.GalleryFragment
import ru.crew.motley.dere.photo.fragment.MyBucketFragment
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import android.hardware.usb.UsbDevice.getDeviceId
import android.support.v7.app.AlertDialog
import android.telephony.TelephonyManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.crew.motley.dere.networkrequest.RestClient
import ru.crew.motley.dere.networkrequest.feedmodels.LoginResponse
import java.lang.Error


class BucketView : AppCompatActivity() {

    private val PERMISSIONS_REQUEST = 211
    var deviceId = "";
    lateinit var sp : SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bucket_view)
        sp = getSharedPreferences("sp",Context.MODE_PRIVATE);
        //greyBucket.setImageResource(R.drawable.gray_bucket)
        //greyBucket.setEnabled(false)
        //cameraRoll.setEnabled(false)
        loadHome()
        if(checkPermission()){
            checkGoHome()
        }

       /* loadd(object : DlgCallback{

            override fun onConfirm(comment: String) {
                Toast.makeText(applicationContext,comment,Toast.LENGTH_SHORT).show()
            }

            override fun onCancel() {
                Toast.makeText(applicationContext,"Yehhh Cancel!!",Toast.LENGTH_SHORT).show()
            }

        })*/

    }

    interface DlgCallback {
        fun onConfirm(comment: String)
        fun onCancel()
    }

    public fun loadd(callback: DlgCallback) {
       var dialog = AlertDialog.Builder(this)
                .setMessage("Helllelelelle")
                .setPositiveButton("Okk", DialogInterface.OnClickListener {
                    dialogInterface, i ->
                    callback.onConfirm("Toast from AlertDialog")
                    dialogInterface.dismiss()
                })
               .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                   dialogInterface, i ->
                   callback.onCancel()
                   dialogInterface.dismiss()
               })
               .show()
    }

    public fun loadHome(){
        greyBucket.setImageResource(R.drawable.bucketactive)
        cameraRoll.setImageResource(R.drawable.rollnew)
        tvBucket.setTextColor(ContextCompat.getColor(this,R.color.green))
        tvRoll.setTextColor(ContextCompat.getColor(this,R.color.blackish))

        supportFragmentManager.beginTransaction().replace(R.id.fcontainer, MyBucketFragment()).commitAllowingStateLoss()
    }

    fun loadOurPartner(){
        greyBucket.setImageResource(R.drawable.bucketnew)
        cameraRoll.setImageResource(R.drawable.rollnew)
        tvBucket.setTextColor(ContextCompat.getColor(this, R.color.blackish))
        tvRoll.setTextColor(ContextCompat.getColor(this, R.color.blackish))
        supportFragmentManager.beginTransaction().replace(R.id.fcontainer, OurPartnersActivity()).addToBackStack(null).commitAllowingStateLoss()
    }

    fun loadMyRoll(){
        greyBucket.setImageResource(R.drawable.bucketnew)
        cameraRoll.setImageResource(R.drawable.rollnewactive)
        tvBucket.setTextColor(ContextCompat.getColor(this,R.color.blackish))
        tvRoll.setTextColor(ContextCompat.getColor(this,R.color.green))
        val fragment = GalleryFragment.newInstance()
        supportFragmentManager.beginTransaction()
                .replace(R.id.fcontainer, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }






    fun navClick(v: View) {
        if(!v.isEnabled()) return
        if (checkPermission()) {
            when (v.id) {
                R.id.greyBucket -> loadHome() /*startActivity(Intent(this, BucketView::class.java))*/
                R.id.cameraRoll -> loadMyRoll() /*startActivity(Intent(this, GalleryActivity::class.java))*/
                R.id.openCameraButton -> {
                    greyBucket.setImageResource(R.drawable.bucketnew)
                    cameraRoll.setImageResource(R.drawable.rollnew)
                    tvBucket.setTextColor(ContextCompat.getColor(this, R.color.blackish))
                    tvRoll.setTextColor(ContextCompat.getColor(this, R.color.blackish))
                    startActivity(Intent(this, Photo2Activity::class.java))
                }
                R.id.openPartners -> loadOurPartner() /*startActivity(Intent(this, OurPartnersActivity::class.java))*/
            }
        }

    }

    private fun checkPermission() : Boolean{
        val needPermissions = getPermissionToRequest()
        if (needPermissions.size > 0) {
            requestPermissions(needPermissions)
            return false
        }else{
            return true
        }
    }


    private fun getPermissionToRequest(): Array<String> {
        val permissions = arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA)
        val needPermissions = LinkedList<String>()
        for (permission in permissions) {
            val status = ContextCompat.checkSelfPermission(this, permission)
            if (status != PackageManager.PERMISSION_GRANTED) {
                needPermissions.add(permission)
            }
        }
        return needPermissions.toTypedArray()
    }

    private fun requestPermissions(permissions: Array<String>) {
        Log.v("permission", "Requesting runtime permissions")
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST)
    }


    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>,
                                            @NonNull grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                var allGranted = true
                for (i in permissions.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.v("permission", "Permission granted: " + permissions[i])
                    } else {
                        Log.v("permission", "Permission denied: " + permissions[i])
                        allGranted = false
                    }
                }
                if (allGranted) {
                    checkGoHome()
                } else {
                   toast(R.string.request_permission)
                    //finish()
                }
            }
        }
    }

    private fun checkGoHome() {
        if (ContextCompat.checkSelfPermission(this@BucketView,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val TelephonyMgr = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val m_deviceId = TelephonyMgr.deviceId
            Log.d("m_deviceId", m_deviceId)
            deviceId = m_deviceId;
            onLogin();
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            greyBucket.setImageResource(R.drawable.bucketactive)
            cameraRoll.setImageResource(R.drawable.rollnew)
            tvBucket.setTextColor(ContextCompat.getColor(this,R.color.green))
            tvRoll.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } else {
            super.onBackPressed()
        }
    }


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    public fun onLogin() {
        RestClient.get().onLogin(deviceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this:: handleResponse,this:: handleError)
    }

    fun handleResponse(loginResponse: LoginResponse){
        if (loginResponse!=null){
            if (loginResponse.status.equals("true",true)){
                Log.d("user_idddd",loginResponse.response!!.get(0).userId)
                sp.edit().putString("user_id",loginResponse.response!!.get(0).userId).apply()
            }
        }
    }

    fun handleError(error: Throwable){
        Log.d("ddddddddddd", error.localizedMessage)
        Toast.makeText(this@BucketView, "Error ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
    }

}
