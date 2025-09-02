package com.secuso.privacyfriendlycodescanner.qrscanner

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Case
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Companion.getClassification
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.QRClassification.Companion.getURLClassification
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class URLClassificationTest {
    lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        QRClassification.setShortLinkDomains(listOf("bit.ly", "shorturl.at", "tinyurl.com").toSet())
        QRClassification.setKnownDomains(listOf("google.com", "nytimes.com").toSet())
    }

    @Test
    @Throws(Exception::class)
    fun check_high_risk_basic_case_red() {
        runBlocking {
            Assert.assertEquals(Case.RED, getURLClassification("http://evrii-uk95.top").case)
        }
    }

    @Test
    @Throws(Exception::class)
    fun check_high_risk_short_url_case_red() {
        runBlocking {
            // https://tinyurl.com/yep765ss -> https://telstra-100582.weeblysite.com
            Assert.assertEquals(Case.RED, getURLClassification("https://tinyurl.com/yep765ss").case)
        }
    }

    @Test
    @Throws(Exception::class)
    fun check_high_risk_hex_encoded_case_red() {
        runBlocking {
            // https://pay.secure-mll.com/JqoR32bYPLy3Vj5
            Assert.assertEquals(Case.RED, getURLClassification("https://pay.secure%2D%6D%6C%6C%2E%63%6F%6D%2F%4A%71%6F%52%33%32%62%59%50%4C%79%33%56%6A%35").case)
        }
    }


    @Test
    @Throws(Exception::class)
    fun check_high_risk_but_trusted_domain_case_red() {
        runBlocking {
            Assert.assertEquals(
                Case.RED,
                getURLClassification("https://docs.google.com/presentation/d/e/2PACX-1vR_185hXTs60aZU0Z0aOf6--q149Ps6X-LVKp-VLGbtXwSa3jTIeyQs7sI6Pn50O8LjvVn9AP5ptYK%75/pub").case
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun check_high_risk_punycode_case_red() {
        runBlocking {
            Assert.assertEquals(Case.RED, getURLClassification("https://xn--h32b13vza.weebly.com/").case)
        }
    }

    @Test
    @Throws(Exception::class)
    fun check_high_risk_unicode_case_red() {
        runBlocking {
            Assert.assertEquals(Case.RED, getURLClassification("https://이메일.weebly.com/").case)
        }
    }

    @Test
    @Throws(Exception::class)
    fun check_low_risk_known_domain_case_green() {
        runBlocking {
            Assert.assertEquals(Case.GREEN, getURLClassification("https://google.com/").case)
        }
    }

    @Ignore("Broken short link")
    @Test
    @Throws(Exception::class)
    fun check_low_risk_known_domain_short_url_case_green() {
        runBlocking {
            Assert.assertEquals(Case.GREEN, getURLClassification("https://tinyurl.com/2nwnzezw").case)
        }
    }

    @Test
    @Throws(Exception::class)
    fun check_low_risk_known_domain_redirect_case_green() {
        runBlocking {
            Assert.assertEquals(
                Case.GREEN,
                getURLClassification("https://www.google.com/url?sa=t&source=web&rct=j&opi=89978449&url=https://www.nytimes.com/&ved=2ahUKEwj8iPXI5K2PAxX48LsIHRQtM-kQFnoECB0QAQ&usg=AOvVaw3xRAfpgZ5h2qWlKkumrA0Y").case
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun check_unknown_risk_unknown_domain_case_grey() {
        runBlocking {
            Assert.assertEquals(Case.GREY_UNKNOWN, getURLClassification("https://example.com/").case)
        }
    }

    @Ignore("Does not work with mocked context")
    @Test
    @Throws(Exception::class)
    fun check_unknown_risk_phone_number_case_grey() {
        runBlocking {
            Assert.assertEquals(Case.GREY_PHONE, getClassification("tel:+49015238987031").case)
        }
    }

    @Test
    fun check_unknown_risk_text_numbers_case_grey() {
        runBlocking {
            Assert.assertEquals(Case.GREY_TEXT, getClassification("8745873265981238658743650812983758743658932720568730168795263487561802364587").case)
        }
    }

    @Test
    fun check_unknown_risk_text_lorem_ipsum_case_grey() {
        runBlocking {
            Assert.assertEquals(
                Case.GREY_TEXT,
                getClassification("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, q").case
            )
        }
    }


    @Throws(InterruptedException::class)
    private suspend fun getURLClassification(url: String): QRClassification {
        return getURLClassification(url, instrumentationContext, null)
    }

    private suspend fun getClassification(text: String): QRClassification {
        val result = Result(text, null, null, BarcodeFormat.QR_CODE)

        return getClassification(result, instrumentationContext, null)
    }
}
