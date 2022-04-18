package com.example.discoverer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*


class MainActivity : AppCompatActivity() {
    val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    val SERVICE_ID = "120001"
    private var strendPointId: String? = null
    lateinit var context: Context
    private var PERMISSIONS: Array<String> = arrayOf<String>()
    lateinit var payloadListener: PayloadListener
    lateinit var button: Button
    lateinit var button1: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.button)
        button1 = findViewById(R.id.button2)
        context = this
        payloadListener = PayloadListener()
        PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        button1.setOnClickListener {
            strendPointId?.let { it1 -> sendPayLoad(it1, "Payment Success") }
        }
    }

    private fun hasPermissions(context: Context?, PERMISSIONS: Array<String>): Boolean {
        if (context != null && PERMISSIONS != null) {
            for (permission in PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    inner class PayloadListener: PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            Toast.makeText(context, "onPayloadReceived "+ p1.asBytes()?.let { String(it) }, Toast.LENGTH_LONG).show()
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            Toast.makeText(context, "onPayloadTransferUpdate "+ p1, Toast.LENGTH_LONG).show()
        }
    }

    fun startDiscovery(view: View) {
        if (!hasPermissions(this@MainActivity, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this@MainActivity, PERMISSIONS, 1)
            return
        }
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        Nearby.getConnectionsClient(
            context
        ).startDiscovery(SERVICE_ID, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(
                @NonNull endpointId: String,
                @NonNull discoveredEndpointInfo: DiscoveredEndpointInfo
            ) {
                Toast.makeText(context, "onEndpointFound "+ endpointId, Toast.LENGTH_LONG).show()
                Nearby.getConnectionsClient(
                    context
                ).requestConnection("Device B", endpointId, object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                        Toast.makeText(context, "onConnectionInitiated "+ p0, Toast.LENGTH_LONG).show()
                        Nearby.getConnectionsClient(context)
                            .acceptConnection(
                                endpointId!!, payloadListener
                            )
                    }

                    override fun onConnectionResult(
                        @NonNull endPointId: String,
                        @NonNull connectionResolution: ConnectionResolution
                    ) {
                        when (connectionResolution.status.statusCode) {
                            ConnectionsStatusCodes.STATUS_OK -> {
                                strendPointId = endPointId
                                button.text = "Connected"
                                button1.visibility = View.VISIBLE
                            }
                            ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                            }
                            ConnectionsStatusCodes.STATUS_ERROR -> {
                            }
                            else -> {
                            }
                        }
                    }

                    override fun onDisconnected(@NonNull s: String) {
                        Toast.makeText(context, "onDisconnected "+ s, Toast.LENGTH_LONG).show()
                        button.text = "Discover"
                        button1.visibility = View.GONE
                    }
                })
            }

            override fun onEndpointLost(@NonNull s: String) {
                Toast.makeText(context, "onEndpointLost "+ s, Toast.LENGTH_LONG).show()
            }
        }, discoveryOptions).addOnSuccessListener {
            Toast.makeText(context, "Success ", Toast.LENGTH_LONG).show()

        }
            .addOnFailureListener { e: Exception? ->
                Toast.makeText(context, "Fail " + e, Toast.LENGTH_LONG).show()
            }
    }

    private fun sendPayLoad(endPointId: String, msg: String) {
        val bytesPayload = Payload.fromBytes(java.lang.String.valueOf(msg).toByteArray())
        Nearby.getConnectionsClient(
            context
        ).sendPayload(endPointId, bytesPayload).addOnSuccessListener { }.addOnFailureListener { }
    }

}