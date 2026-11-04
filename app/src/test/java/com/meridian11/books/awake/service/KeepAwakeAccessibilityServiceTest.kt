package com.meridian11.books.awake.service

import androidx.test.core.app.ApplicationProvider
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class KeepAwakeAccessibilityServiceTest {

    @Test
    fun `KeepAwakeAccessibilityService never activates overlay or tap engine without affirmative disclosure consent`() {
        val service = Robolectric.buildService(KeepAwakeAccessibilityService::class.java)
            .create()
            .get()

        // 1. Even if isAwake = true and tapSimulationEnabled = true, if accessibilityDisclosureAccepted = false,
        // the service MUST NOT attach overlay or run tap engine (Google Play Accessibility API Policy gate)
        service.applyState(
            AwakeState(
                isAwake = true,
                tapSimulationEnabled = true,
                accessibilityDisclosureAccepted = false
            )
        )
        assertFalse(
            "Overlay must NOT attach when accessibilityDisclosureAccepted is false",
            service.isOverlayAttached
        )

        // 2. Once user accepts prominent disclosure, overlay attaches when awake
        service.applyState(
            AwakeState(
                isAwake = true,
                tapSimulationEnabled = true,
                accessibilityDisclosureAccepted = true
            )
        )
        assertTrue(
            "Overlay must attach when isAwake and accessibilityDisclosureAccepted are both true",
            service.isOverlayAttached
        )

        // 3. When user revokes disclosure consent, overlay immediately detaches
        service.applyState(
            AwakeState(
                isAwake = true,
                tapSimulationEnabled = false,
                accessibilityDisclosureAccepted = false
            )
        )
        assertFalse(
            "Overlay must detach immediately when accessibilityDisclosureAccepted is revoked",
            service.isOverlayAttached
        )
    }

    @Test
    fun `accessibility_service_config XML enforces canRetrieveWindowContent false and isAccessibilityTool false`() {
        val app = ApplicationProvider.getApplicationContext<AwakeApp>()
        val parser = app.resources.getXml(R.xml.accessibility_service_config)
        val androidNs = "http://schemas.android.com/apk/res/android"
        var foundTag = false
        var canRetrieveWindowContent = true
        var isAccessibilityTool = true
        var eventTypes: String? = "non-empty"

        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "accessibility-service") {
                foundTag = true
                canRetrieveWindowContent = parser.getAttributeBooleanValue(androidNs, "canRetrieveWindowContent", true)
                isAccessibilityTool = parser.getAttributeBooleanValue(androidNs, "isAccessibilityTool", true)
                eventTypes = parser.getAttributeValue(androidNs, "accessibilityEventTypes")
            }
        }
        assertTrue(foundTag)
        assertFalse("canRetrieveWindowContent must be false", canRetrieveWindowContent)
        assertFalse("isAccessibilityTool must be false for utility keep-awake service", isAccessibilityTool)
        assertTrue("accessibilityEventTypes must be empty", eventTypes.isNullOrEmpty() || eventTypes == "0x0")
    }
}
