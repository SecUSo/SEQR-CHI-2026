package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

object RedirectResolver {
    /**
     * Returns the URL that the given `url` redirects to, or an empty string if no
     * redirect header is present or if an error (other than a timeout) occurs.
     *
     * The method opens an `HttpURLConnection` to the supplied URL, disables automatic
     * following of redirects (`instanceFollowRedirects = false`), and then reads the
     * value of the `Location` header. If a timeout happens
     * (`SocketTimeoutException`) the exception is propagated to the caller; all
     * other exceptions are swallowed and an empty string is returned.
     *
     * @param url the original URL to check for a redirect.
     * @param connectTimeout the timeout in milliseconds used for establishing the
     *   connection. Default is `2000`.
     * @param readTimeout the timeout in milliseconds used for reading the response.
     *   Default is `2000`.
     * @return the redirect target URL as a string, or an empty string if there is
     *   no redirect or if a non‑timeout exception occurs.
     * @throws java.net.SocketTimeoutException if the connection or read operation
     *   times out.
     */
    @JvmStatic
    @Throws(SocketTimeoutException::class)
    fun resolveRedirect(url: String, connectTimeout: Int = 2000, readTimeout: Int = 2000): String {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = connectTimeout
            conn.readTimeout = readTimeout

            return conn.getHeaderField("Location")
        } catch (e: SocketTimeoutException) {
            throw e
        } catch (e: Exception) {
            return ""
        }
    }
}