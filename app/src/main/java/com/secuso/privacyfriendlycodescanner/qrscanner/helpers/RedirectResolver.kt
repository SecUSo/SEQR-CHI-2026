package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import java.net.HttpURLConnection
import java.net.URL

object RedirectResolver {
    /**
     * Returns the URL that the given `url` redirects to, or an empty string if no
     * redirect header is present or if an error occurs.
     *
     * The method opens an `HttpURLConnection` to the supplied URL, disables automatic
     * following of redirects (`instanceFollowRedirects = false`), and then reads the
     * value of the `Location` header. Because the method swallows all exceptions,
     * callers never receive a checked exception – they simply receive an empty
     * string when something goes wrong.
     *
     * @param url the original URL to check for a redirect.
     * @return the redirect target URL as a string, or an empty string if there is no
     * redirect or if an exception occurs.
     */
    @JvmStatic
    fun resolveRedirect(url: String): String {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false

            return conn.getHeaderField("Location")
        } catch (e: Exception) {
            return ""
        }
    }
}