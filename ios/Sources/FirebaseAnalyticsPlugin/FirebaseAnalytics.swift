import Foundation
import Capacitor
import FirebaseCore
import FirebaseAnalytics

@objc(FirebaseAnalytics)
public class FirebaseAnalytics: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "FirebaseAnalytics"
    public let jsName = "FirebaseAnalytics"
    public let pluginMethods: [CAPPluginMethod] = [
        .promise("setUserId", FirebaseAnalytics.setUserId),
        .promise("setUserProperty", FirebaseAnalytics.setUserProperty),
        .promise("getAppInstanceId", FirebaseAnalytics.getAppInstanceId),
        .promise("setScreenName", FirebaseAnalytics.setScreenName),
        .promise("reset", FirebaseAnalytics.reset),
        .promise("logEvent", FirebaseAnalytics.logEvent),
        .promise("setCollectionEnabled", FirebaseAnalytics.setCollectionEnabled),
        .promise("setSessionTimeoutDuration", FirebaseAnalytics.setSessionTimeoutDuration),
        .promise("enable", FirebaseAnalytics.enable),
        .promise("disable", FirebaseAnalytics.disable)
    ]

    // Every method stays synchronous: the bridge queue runs the setters and the logged events in the order of the
    // calls, which async methods would not keep.

    public override func load() {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
    }

    /// Sets the user ID property.
    /// - Parameter call: userId - unique identifier of the user to log
    func setUserId(_ call: CAPPluginCall) throws {
        guard let userId = call.getString("userId") else {
            throw CAPPluginError("userId property is missing")
        }
        Analytics.setUserID(userId)
        call.resolve()
    }

    /// Sets a user property to a given value.
    /// - Parameter call: name - The name of the user property to set.
    ///                   value - The value of the user property.
    func setUserProperty(_ call: CAPPluginCall) throws {
        guard let name = call.getString("name") else {
            throw CAPPluginError("name property is missing")
        }
        guard let value = call.getString("value") else {
            throw CAPPluginError("value property is missing")
        }
        Analytics.setUserProperty(value, forName: name)
        call.resolve()
    }

    /// Retrieves the app instance id from the service.
    /// - Parameter call: instanceId - current instance if of the app
    func getAppInstanceId(_ call: CAPPluginCall) {
        let instanceId = Analytics.appInstanceID()
        call.resolve([
            "instanceId": instanceId
        ])
    }

    /// Sets the current screen name, which specifies the current visual context in your app.
    /// - Parameter call: screenName - the activity to which the screen name and class name apply.
    ///                   nameOverride - the name of the current screen. Set to null to clear the current screen name.
    func setScreenName(_ call: CAPPluginCall) throws {
        guard let screenName = call.getString("screenName") else {
            throw CAPPluginError("screenName property is missing")
        }
        let nameOverride = call.getString("nameOverride") ?? nil
        DispatchQueue.main.async {
            Analytics.logEvent(AnalyticsEventScreenView,
                parameters: [AnalyticsParameterScreenName: screenName,
                            AnalyticsParameterScreenClass: nameOverride])
        }
        call.resolve()
    }


    /// Clears all analytics data for this app from the device and resets the app instance id.
    func reset(_ call: CAPPluginCall) {
        Analytics.resetAnalyticsData()
        call.resolve()
    }


    /// Logs an app event.
    /// - Parameter call: name - unique name of the event
    ///                   params - the map of event parameters.
    func logEvent(_ call: CAPPluginCall) throws {

        /// Name is a required argument to logEvent()
        guard let name = call.getString("name"), !name.isEmpty else {
            throw CAPPluginError("Event name is required and can't be empty")
        }

        /// logEvent() expects `nil` when there are no parameters
        guard var params = call.getObject("params"), !params.isEmpty else {
            Analytics.logEvent(name, parameters: nil)
            call.resolve()
            return
        }

        /// FirebaseAnalytics silently converts any item quantity that is not an
        /// integer to zero, this includes any NSNumber or string value passed
        /// as an option to CAPPluginCall.
        if var items = params["items"] as? NSArray as? [[String:Any]] {
            for (idx, item) in items.enumerated() {
                if let quantity = item["quantity"] {
                    guard let intVal = quantity as? Int else {
                        throw CAPPluginError("Item quantity must be specified as an integer value")
                    }
                    items[idx]["quantity"] = intVal
                }
            }
            params["items"] = items
        }

        Analytics.logEvent(name, parameters: params)
        call.resolve()
    }


    /// Sets whether analytics collection is enabled for this app on this device.
    /// - Parameter call: enabled - boolean true/false to enable/disable logging
    func setCollectionEnabled(_ call: CAPPluginCall) {
        if let enabled = call.getBool("enabled") {
            Analytics.setAnalyticsCollectionEnabled(enabled)
        } else {
            Analytics.setAnalyticsCollectionEnabled(false)
        }
        call.resolve()
    }


    /// Sets the duration of inactivity that terminates the current session.
    /// - Parameter call: duration - duration of inactivity
    func setSessionTimeoutDuration(_ call: CAPPluginCall) {
        let duration = call.getInt("duration") ?? 1800

        Analytics.setSessionTimeoutInterval(TimeInterval(duration))
        call.resolve()
    }

    /// Deprecated - use setCollectionEnabled instead
    /// Enable analytics collection for this app on this device.
    /// - Parameter call
    func enable(_ call: CAPPluginCall) {
        Analytics.setAnalyticsCollectionEnabled(true)
        call.resolve()
    }

    /// Deprecated - use setCollectionEnabled instead
    /// Disable analytics collection for this app on this device.
    /// - Parameter call
    func disable(_ call: CAPPluginCall) {
        Analytics.setAnalyticsCollectionEnabled(false)
        call.resolve()
    }
}
