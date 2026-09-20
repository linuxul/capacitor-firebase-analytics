package com.getcapacitor.community.firebaseanalytics

import android.os.Bundle
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseAnalyticsTest {
    private fun toBundle(json: String): Bundle = FirebaseAnalytics.convertJsonToBundle(JSONObject(json))

    private fun assertBundle(json: String, bundleStr: String) {
        assertEquals(bundleStr, toBundle(json).toString())
    }

    @Test
    fun convertJsonToBundle() {
        // basic fields
        assertBundle("{}", "Bundle[{}]")
        assertBundle("{ \"hello\": \"world\" }", "Bundle[{hello=world}]")
        assertBundle("{ \"foo\": 123.456 }", "Bundle[{foo=123.456}]")
        assertBundle("{ \"foo\": null }", "Bundle[{}]")

        // nested objects
        assertBundle("{ \"foo\": { \"bar\": 123 } }", "Bundle[{foo=Bundle[{bar=123}]}]")
        assertBundle("{ \"foo\": {} }", "Bundle[{foo=Bundle[{}]}]")

        // don't allow mismatched array elements
        assertBundle("{ \"foo\": [{ \"bar\": 123 }, 456.789], \"fizz\": 123 }", "Bundle[{fizz=123}]")

        // array of bundles
        @Suppress("DEPRECATION")
        val bundles = toBundle("{ \"foo\": [{ \"bar\": 123 }, { \"fizz\": 456 }] }").getParcelableArray("foo")
        assertArrayEquals(
            arrayOf("Bundle[{bar=123}]", "Bundle[{fizz=456}]"),
            bundles!!.map { it.toString() }.toTypedArray()
        )

        // any combination of number types in array should be fine
        assertArrayEquals(
            floatArrayOf(123f, 456f),
            toBundle("{ \"foo\": [123, 456] }").getFloatArray("foo"),
            0f
        )
        assertArrayEquals(
            floatArrayOf(123f, 456.789f),
            toBundle("{ \"foo\": [123, 456.789] }").getFloatArray("foo"),
            0f
        )
    }
}