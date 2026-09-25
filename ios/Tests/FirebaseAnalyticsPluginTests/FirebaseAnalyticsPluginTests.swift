import XCTest
import Capacitor
@testable import FirebaseAnalyticsPlugin

// These tests never reach Firebase: every call below stops at an argument check before it.
class PluginTests: XCTestCase {
    func testMissingArgumentsAreRejected() {
        let plugin = FirebaseAnalytics()
        XCTAssertEqual(thrownError(plugin.setUserId, "setUserId")?.message, "userId property is missing")
        XCTAssertEqual(thrownError(plugin.setUserProperty, "setUserProperty")?.message, "name property is missing")
        XCTAssertEqual(thrownError(plugin.setUserProperty, "setUserProperty", ["name": "plan"])?.message, "value property is missing")
        XCTAssertEqual(thrownError(plugin.setScreenName, "setScreenName")?.message, "screenName property is missing")
    }

    func testEventsNeedANameAndIntegerQuantities() {
        let plugin = FirebaseAnalytics()
        let nameRequired = "Event name is required and can't be empty"
        XCTAssertEqual(thrownError(plugin.logEvent, "logEvent")?.message, nameRequired)
        XCTAssertEqual(thrownError(plugin.logEvent, "logEvent", ["name": ""])?.message, nameRequired)

        let items: JSArray = [["item_id": "sku", "quantity": "2"] as JSObject]
        let options: JSObject = ["name": "purchase", "params": ["items": items] as JSObject]
        let error = thrownError(plugin.logEvent, "logEvent", options)
        XCTAssertEqual(error?.message, "Item quantity must be specified as an integer value")
        XCTAssertNil(error?.code)
    }

    /// The error `method` throws, which the bridge rejects the call with; nil when it does not throw.
    private func thrownError(_ method: (CAPPluginCall) throws -> Void, _ name: String, _ options: JSObject = [:]) -> CAPPluginError? {
        let call = CAPPluginCall(callbackId: "test", methodName: name, options: options, success: { _, _ in
            XCTFail("\(name) answers by throwing")
        }, error: { _ in
            XCTFail("\(name) answers by throwing")
        })
        do {
            try method(call)
            XCTFail("\(name) must throw")
            return nil
        } catch let error as CAPPluginError {
            return error
        } catch {
            XCTFail("unexpected error \(error)")
            return nil
        }
    }
}
