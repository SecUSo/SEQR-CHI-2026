package com.secuso.privacyfriendlycodescanner.qrscanner.helpers

import com.google.common.net.InternetDomainName
import java.net.URL

/**
 * Utility object that inspects a URL and extracts the value of a redirect query parameter
 * if the URL belongs to a set of known redirect‑service domains.
 *
 * The object is intended for use in a QR‑code scanner where it is important to detect
 * and report indirect links that can lead to phishing or malware sites.
 *
 * The list `KNOWN_REDIRECT_URLS` can be expanded with additional domains and query
 * parameter names if new redirect services are discovered.
 */
object RedirectChecker {
    /**
     * List of domain → query‑parameter pairs that are considered known redirect
     * services. Each entry defines:
     *
     * * The domain (or a suffix wildcard) that should be matched against the
     *   base domain of the supplied URL.
     * * The query‑parameter key that holds the actual target URL (e.g. `url=`).
     *
     * Example entries:
     *
     * * `"google.", "url="` – matches any sub‑domain of `google.com` that contains
     *   a `url=` query parameter.
     * * `"deref-gmx.net", "redirectUrl="` – matches the exact domain `deref-gmx.net`
     *   with a `redirectUrl=` parameter.
     *
     * A trailing dot (`.`) in the domain part indicates a wildcard match on the
     * suffix; for instance `"google."` will match both `google.com` and
     * `google.net`.
     *
     */
    private val KNOWN_REDIRECT_URLS = listOf(
        Pair("google.", "url="),
        Pair("google.", "q="),
        Pair("deref-gmx.net", "redirectUrl="),
        Pair("deref-web-02.de", "redirectUrl="),
        Pair("deref-web.de", "redirectUrl=")
    )

    /**
     * Examines the supplied URL and, if it matches one of the known redirect
     * services, returns the part of the query string that contains the
     * redirect‑target parameter.
     *
     * @param url The full URL string to be inspected.  It may be any well‑formed
     *            HTTP/HTTPS URL.
     * @return The redirect query parameter value (e.g. `"https://malicious.example/"`)
     *         if the URL is identified as a known redirect service; otherwise an
     *         empty string.  The returned string contains only the value part of
     *         the query parameter and may include further parameters if they
     *         follow the matched key in the original query string.
     *
     */
    fun getRedirectURL(url: String): String {
        val uri = URL(url)
        val parameters = uri.query
        val baseDomain = Utils.extractBaseDomainFromURI(url)
        if (parameters == null) {
            return ""
        }
        val suffix = InternetDomainName.from(Utils.extractHostFromURI(url)).registrySuffix()
        for (knownRedirect in KNOWN_REDIRECT_URLS) {
            if (knownRedirect.first == baseDomain // Check if the domain matches exactly
                ||
                (knownRedirect.first.endsWith(".") && (suffix != null && baseDomain.removeSuffix(suffix.toString()) == knownRedirect.first))// Check if the domain matches wildcard
            ) {
                if (parameters.contains(knownRedirect.second)) {
                    return parameters.substring(parameters.indexOf(knownRedirect.second) + knownRedirect.second.length, parameters.length)
                }
            }
        }
        return ""
    }
}