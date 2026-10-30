package com.resumemaster.android.data.portals

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyCookiesTest {

  @Test
  fun `only the family's three origins are read`() {
    val asked = mutableListOf<String>()
    val read = FamilyCookies.read(PortalFamily.AMAZON) { url -> asked += url; "a=1" }
    assertEquals(listOf("https://www.amazon.jobs/", "https://amazon.jobs/", "https://account.amazon.jobs/"), asked)
    assertEquals(3, read.size)
    assertTrue(read.all { PortalFamily.AMAZON.owns(it.url) })
  }

  @Test
  fun `cookie headers parse, values keep their equals signs, duplicates collapse`() {
    val read = FamilyCookies.read(PortalFamily.AMAZON) { url ->
      if (url == "https://www.amazon.jobs/") "x=1; tok=ab==cd; x=2; ; junk; =novalue" else null
    }
    assertEquals(listOf("x" to "2", "tok" to "ab==cd"), read.map { it.name to it.value })
  }

  @Test
  fun `the plaintext is the fill engine's storageState shape`() {
    val p = JSONObject(FamilyCookies.plaintext(listOf(EngineCookie("n", "v", "https://amazon.jobs/"))))
    val c = p.getJSONArray("cookies").getJSONObject(0)
    assertEquals("n", c.getString("name"))
    assertEquals("https://amazon.jobs/", c.getString("url"))
    assertTrue(c.getBoolean("secure"))
  }

  @Test
  fun `the family owns its exact https origins and nothing that merely contains them`() {
    val f = PortalFamily.AMAZON
    assertTrue(f.owns("https://account.amazon.jobs/en-US"))
    assertTrue(f.owns("https://amazon.jobs/"))
    assertFalse(f.owns("http://www.amazon.jobs/"))
    assertFalse(f.owns("https://evil.amazon.jobs.example.com/"))
    assertFalse(f.owns("https://notamazon.jobs/"))
    assertFalse(f.owns("https://www.amazon.com/"))
    assertFalse(f.owns(null))
    assertFalse(f.isAccountPage("https://example.com/en/applicant"))
  }
}
