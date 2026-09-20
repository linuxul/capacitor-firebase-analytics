package com.getcapacitor.community.firebaseanalytics

import android.Manifest
import android.os.Bundle
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.google.firebase.analytics.FirebaseAnalytics as GoogleFirebaseAnalytics
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@CapacitorPlugin(
    name = "FirebaseAnalytics",
    permissions = [
        Permission(strings = [Manifest.permission.ACCESS_NETWORK_STATE], alias = "network"),
        Permission(strings = [Manifest.permission.INTERNET], alias = "internet"),
        Permission(strings = [Manifest.permission.WAKE_LOCK], alias = "wakelock")
    ]
)
public class FirebaseAnalytics : Plugin() {
    private var firebaseAnalytics: GoogleFirebaseAnalytics? = null

    override fun load() {
        super.load()

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = GoogleFirebaseAnalytics.getInstance(bridge.activity)
    }

    /**
     * Sets the user ID property.
     * @param call - userId: unique identifier of the user to log
     */
    @PluginMethod
    public fun setUserId(call: PluginCall) {
        try {
            val analytics = firebaseAnalytics
            if (analytics == null) {
                call.reject(MISSING_REF_MSSG)
                return
            }

            if (!call.data.has("userId")) {
                call.reject("userId property is missing")
                return
            }

            analytics.setUserId(call.getString("userId"))
            call.resolve()
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    /**
     * Sets a user property to a given value.
     * @param call - name: The name of the user property to set.
     *               value: The value of the user property.
     */
    @PluginMethod
    public fun setUserProperty(call: PluginCall) {
        try {
            val analytics = firebaseAnalytics
            if (analytics == null) {
                call.reject(MISSING_REF_MSSG)
                return
            }

            if (!call.data.has("name")) {
                call.reject("name property is missing")
                return
            }

            if (!call.data.has("value")) {
                call.reject("value property is missing")
                return
            }

            // Firebase requires a name. One that is present but not a string is reported like a missing one.
            val name = call.getString("name")
            if (name == null) {
                call.reject("name property is missing")
                return
            }
            val value = call.getString("value")

            analytics.setUserProperty(name, value)
            call.resolve()
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    /**
     * Retrieves the app instance id from the service.
     * @param call - instanceId: current instance if of the app
     */
    @PluginMethod
    public fun getAppInstanceId(call: PluginCall) {
        val analytics = firebaseAnalytics
        if (analytics == null) {
            call.reject(MISSING_REF_MSSG)
            return
        }
        analytics.appInstanceId.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val instanceId = task.result
                if (instanceId != null && instanceId.isEmpty()) {
                    call.reject("failed to obtain app instance id")
                } else {
                    val result = JSObject()
                    result.put("instanceId", instanceId)
                    call.resolve(result)
                }
            } else {
                call.reject(task.exception?.localizedMessage)
            }
        }
    }

    /**
     * Sets the current screen name, which specifies the current visual context in your app.
     * @param call - screenName: the activity to which the screen name and class name apply.
     *               nameOverride: the name of the current screen. Set to null to clear the current screen name.
     */
    @PluginMethod
    public fun setScreenName(call: PluginCall) {
        try {
            val analytics = firebaseAnalytics
            if (analytics == null) {
                call.reject(MISSING_REF_MSSG)
                return
            }

            if (!call.data.has("screenName")) {
                call.reject("screenName property is missing")
                return
            }

            val screenName = call.getString("screenName")
            val nameOverride = call.getString("nameOverride", null)

            bridge.activity.runOnUiThread {
                val bundle = Bundle()
                bundle.putString(GoogleFirebaseAnalytics.Param.SCREEN_NAME, screenName)
                bundle.putString(GoogleFirebaseAnalytics.Param.SCREEN_CLASS, nameOverride)
                analytics.logEvent(GoogleFirebaseAnalytics.Event.SCREEN_VIEW, bundle)
                call.resolve()
            }
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    /**
     * Clears all analytics data for this app from the device and resets the app instance id.
     * @param call
     */
    @PluginMethod
    public fun reset(call: PluginCall) {
        try {
            val analytics = firebaseAnalytics
            if (analytics == null) {
                call.reject(MISSING_REF_MSSG)
                return
            }

            analytics.resetAnalyticsData()
            call.resolve()
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    /**
     * Logs an app event.
     * @param call - name: unique name of the event
     *               params: the map of event parameters.
     */
    @PluginMethod
    public fun logEvent(call: PluginCall) {
        try {
            val analytics = firebaseAnalytics
            if (analytics == null) {
                call.reject(MISSING_REF_MSSG)
                return
            }

            if (!call.data.has("name")) {
                call.reject("name property is missing")
                return
            }

            // Firebase requires a name. One that is present but not a string is reported like a missing one.
            val name = call.getString("name")
            if (name == null) {
                call.reject("name property is missing")
                return
            }
            val params = call.data.getJSObject("params")
            analytics.logEvent(name, params?.let { convertJsonToBundle(it) })
            call.resolve()
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    /**
     * Sets whether analytics collection is enabled for this app on this device.
     * @param call - enabled: boolean true/false to enable/disable logging
     */
    @PluginMethod
    public fun setCollectionEnabled(call: PluginCall) {
        val analytics = firebaseAnalytics
        if (analytics == null) {
            call.reject(MISSING_REF_MSSG)
            return
        }

        val enabled = call.getBoolean("enabled", false) ?: false

        analytics.setAnalyticsCollectionEnabled(enabled)
        call.resolve()
    }

    /**
     * Deprecated: use setCollectionEnabled() instead
     * Enable analytics collection for this app on this device.
     * @param call - enabled: boolean true/false to enable/disable logging
     */
    @Deprecated("Use setCollectionEnabled() instead")
    @PluginMethod
    public fun enable(call: PluginCall) {
        val analytics = firebaseAnalytics
        if (analytics == null) {
            call.reject(MISSING_REF_MSSG)
            return
        }

        analytics.setAnalyticsCollectionEnabled(true)
        call.resolve()
    }

    /**
     * Deprecated: use setCollectionEnabled() instead
     * Disable analytics collection for this app on this device.
     * @param call
     */
    @Deprecated("Use setCollectionEnabled() instead")
    @PluginMethod
    public fun disable(call: PluginCall) {
        val analytics = firebaseAnalytics
        if (analytics == null) {
            call.reject(MISSING_REF_MSSG)
            return
        }

        analytics.setAnalyticsCollectionEnabled(false)
        call.resolve()
    }

    /**
     * Sets the duration of inactivity that terminates the current session.
     * @param call: options - duration: duration of inactivity
     */
    @PluginMethod
    public fun setSessionTimeoutDuration(call: PluginCall) {
        val analytics = firebaseAnalytics
        if (analytics == null) {
            call.reject(MISSING_REF_MSSG)
            return
        }

        val duration = call.getInt("duration", 1800) ?: 1800

        analytics.setSessionTimeoutDuration(duration.toLong())
        call.resolve()
    }

    public companion object {
        private const val MISSING_REF_MSSG = "Firebase analytics is not initialized"

        @JvmStatic
        public fun convertJsonToBundle(json: JSONObject?): Bundle {
            val bundle = Bundle()
            if (json == null || json.length() == 0) return bundle

            for (key in json.keys()) {
                try {
                    when (val value = json.get(key)) {
                        is String -> bundle.putString(key, value)
                        is Boolean -> bundle.putBoolean(key, value)
                        is Int -> bundle.putInt(key, value)
                        is Long -> bundle.putLong(key, value)
                        is Float -> bundle.putFloat(key, value)
                        is Double -> bundle.putDouble(key, value)
                        is JSONObject -> bundle.putBundle(key, convertJsonToBundle(value))
                        is JSONArray -> putArray(bundle, key, value)
                    }
                } catch (e: ClassCastException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            return bundle
        }

        // The first element decides the type of the array; an element that does not fit throws, and the
        // caller then leaves the key out.
        @Throws(JSONException::class)
        private fun putArray(bundle: Bundle, key: String, array: JSONArray) {
            when (if (array.length() == 0) null else array.get(0)) {
                is JSONObject -> bundle.putParcelableArray(key, Array(array.length()) { convertJsonToBundle(array.getJSONObject(it)) })

                is String -> bundle.putStringArray(key, Array(array.length()) { array.getString(it) })

                is Int, is Float, is Double ->
                    bundle.putFloatArray(key, FloatArray(array.length()) { (array.get(it) as Number).toFloat() })
            }
        }
    }
}