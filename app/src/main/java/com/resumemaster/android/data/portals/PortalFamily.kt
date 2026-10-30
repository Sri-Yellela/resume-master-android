package com.resumemaster.android.data.portals

import java.net.URI
import org.json.JSONArray
import org.json.JSONObject

/**
 * A portal family draft's apply worker may hold a sealed sign-in for.
 *
 * Mirrors draft's `services/applyOrigins.js` APPLY_ORIGIN_FAMILIES and the extension's APPLY_AWAY_FAMILIES
 * (`extension/applyAway.js`). Only ACCOUNT portals belong here — Greenhouse, Lever, Ashby and Workable apply on
 * open forms and need no session.
 *
 * @param ats         the family's name — the server's `portal_family`
 * @param name        the portal, in a sentence
 * @param origins     EVERY origin whose cookies may be read, and no other — exactly the family's
 * @param originHost  the host the session is stored under (the worker matches it and its subdomains)
 * @param signInUrl   where the in-app browser opens (the worker's own account adapter starts here)
 * @param accountPage a page only a signed-in user reaches — one of the two "signed in" signals (⚠ unverified
 *                    against the live site; the other signal, a sign-out control on the page, is the primary)
 */
data class PortalFamily(
  val ats: String,
  val name: String,
  val origins: List<String>,
  val originHost: String,
  val signInUrl: String,
  val accountPage: Regex,
) {
  val hosts: List<String> get() = origins.map { URI(it).host }

  /** Is this URL on one of the family's own origins (https, exact origin)? */
  fun owns(url: String?): Boolean {
    val u = try { URI(url ?: return false) } catch (_: Exception) { return false }
    if (u.scheme != "https" || u.host == null) return false
    return origins.contains("https://${u.host.lowercase()}") && (u.port == -1 || u.port == 443)
  }

  /** The URL signal: on the family's origin AND on a page only a signed-in user reaches. */
  fun isAccountPage(url: String?): Boolean = owns(url) && accountPage.containsMatchIn(url!!)

  companion object {
    val AMAZON = PortalFamily(
      ats = "amazon",
      name = "amazon.jobs",
      origins = listOf("https://www.amazon.jobs", "https://amazon.jobs", "https://account.amazon.jobs"),
      originHost = "amazon.jobs",
      signInUrl = "https://account.amazon.jobs/en-US",
      accountPage = Regex("""^https://(www\.)?amazon\.jobs/[a-z-]+/(applicant|dashboard)|^https://account\.amazon\.jobs/[A-Za-z-]+/(dashboard|profile|account)""", RegexOption.IGNORE_CASE),
    )

    val ALL: List<PortalFamily> = listOf(AMAZON)

    fun byAts(ats: String?): PortalFamily? = ALL.firstOrNull { it.ats == ats }
  }
}

/**
 * Where cookies come from. Production is `android.webkit.CookieManager::getCookie` — which answers the
 * `Cookie:` header the WebView WOULD send to that URL ("a=b; c=d", HttpOnly ones included), nothing more.
 * An interface so the JVM tests can prove which URLs are asked about.
 */
fun interface CookieSource {
  fun cookieHeaderFor(url: String): String?
}

/** One cookie in the fill engine's shape (puppeteer `page.setCookie`, which the worker hands `storageState.cookies`). */
data class EngineCookie(val name: String, val value: String, val url: String) {
  fun toJson(): JSONObject = JSONObject()
    .put("name", name)
    .put("value", value)
    // `url` makes it host-only on exactly the host it was read for. CookieManager does not say whether a cookie
    // was set for ".amazon.jobs" or one host, so a domain cookie is carried once per host it applied to — the
    // same cookies the WebView itself would send to each of the family's hosts, and no further.
    .put("url", url)
    .put("path", "/")
    .put("secure", true)
    .put("httpOnly", false)
    // CookieManager does not expose expiry either; a session cookie is enough for the worker's run.
    .put("expires", -1)
}

object FamilyCookies {

  /**
   * ⛔ Read ONLY the family's cookies: the source is asked about the family's own origins and NOTHING else —
   * never a wildcard, never another site. Deduplicated by name · host.
   */
  fun read(family: PortalFamily, source: CookieSource): List<EngineCookie> {
    val seen = LinkedHashMap<String, EngineCookie>()
    for (origin in family.origins) {
      val url = "$origin/"
      val header = source.cookieHeaderFor(url) ?: continue
      for (pair in header.split(';')) {
        val eq = pair.indexOf('=')
        if (eq <= 0) continue
        val name = pair.substring(0, eq).trim()
        val value = pair.substring(eq + 1).trim()
        if (name.isEmpty()) continue
        seen["$name|$origin"] = EngineCookie(name, value, url)
      }
    }
    return seen.values.toList()
  }

  /** The plaintext the worker unseals: `{ cookies: [...] }` — what its runLease JSON.parses into storageState. */
  fun plaintext(cookies: List<EngineCookie>): String =
    JSONObject().put("cookies", JSONArray().apply { cookies.forEach { put(it.toJson()) } }).toString()

  /**
   * Injected into the WebView's top frame: does the page show a sign-out control? A boolean leaves the page —
   * nothing else. The same test as the extension's `portalSignedInInPage`.
   */
  const val SIGNED_IN_SCRIPT: String = """(function(){
  var words=/\b(sign|log)[\s-]?out\b/i, href=/(sign|log)[-_]?out/i;
  var ns=document.querySelectorAll('a, button, [role="menuitem"], [role="button"]');
  for (var i=0;i<ns.length;i++){var n=ns[i];
    if (words.test(String(n.textContent||'').trim().slice(0,40))) return true;
    if (href.test(String((n.getAttribute&&n.getAttribute('href'))||''))) return true;}
  return false;})()"""
}
