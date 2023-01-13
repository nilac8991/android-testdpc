package com.afwsamples.testdpc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.RestrictionEntry
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.afwsamples.testdpc.common.NotificationUtil
import com.afwsamples.testdpc.common.RestrictionManagerCompat
import com.afwsamples.testdpc.util.ManagedConfigurationsBatchUtil
import java.io.File

class ManagedConfigurationsBatchImporterReceiver : BroadcastReceiver() {

    @SuppressLint("LongLogTag")
    override fun onReceive(context: Context, intent: Intent) {
        //Discard intents which are not ours
        if (intent.action != IMPORT_ACTION) {
            return
        }

        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = DeviceAdminReceiver.getComponentName(context)

        val filePath = intent.getStringExtra(FILE_PATH)!!
        val packageName = intent.getStringExtra(PACKAGE_NAME)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Log.e(TAG, "Unable to process the request, missing permissions were found")
            createNotification(
                context,
                "Unable to process file, please allow Manage External Storage permission from Settings and try again"
            )
            return
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Unable to process the request, missing permissions were found")
                createNotification(
                    context,
                    "Unable to process file, please allow Write External Storage permission from Settings and try again"
                )
                return
            }
        }

        Log.i(TAG, "Received batch import request for application with packageName: $packageName")
        createNotification(context, "Importing batch configurations file for $packageName")

        val configFileToImport = File(filePath)
        if (!configFileToImport.exists()) {
            Log.e(
                TAG,
                "Unable to continue import process, file doesn't exist at the specified location"
            )
            createNotification(
                context,
                "Unable to continue import process, file doesn't exist at the specified location"
            )
            return
        }

        ManagedConfigurationsBatchUtil.importFromJsonFile(
            configFileToImport,
            context,
            object : ManagedConfigurationsBatchUtil.ProcessStateCallBack() {
                override fun onFinished() {
                    //Nothing to see here
                }

                override fun onFinished(list: MutableList<RestrictionEntry>) {
                    RestrictionManagerCompat.convertTypeChoiceAndNullToString(list)

                    devicePolicyManager.setApplicationRestrictions(
                        adminComponent,
                        packageName,
                        RestrictionManagerCompat.convertRestrictionsToBundle(list)
                    )
                    Log.i(
                        TAG,
                        "Successfully imported managed configurations for app: $packageName"
                    )
                    createNotification(
                        context,
                        "Successfully imported managed configurations for $packageName"
                    )
                }
            }
        )
    }

    private fun createNotification(context: Context, notificationBody: String) {
        val notification = NotificationUtil.getNotificationBuilder(context)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Managed Configurations Batch Importer")
            .setContentText(notificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationBody))
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setOnlyAlertOnce(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val TAG = "ManagedConfigurationsImporter"
        const val NOTIFICATION_ID = 34858

        const val IMPORT_ACTION = "com.afwsamples.testdpc.IMPORT_ACTION"

        const val FILE_PATH = "file_path"
        const val PACKAGE_NAME = "package_name"
    }
}