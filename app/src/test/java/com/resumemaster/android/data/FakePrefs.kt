package com.resumemaster.android.data

import android.content.SharedPreferences

/**
 * An in-memory SharedPreferences whose commit() can be made to FAIL.
 *
 * SecureTokenStore's real backing store is EncryptedSharedPreferences, which needs the Android
 * Keystore and cannot be built on the JVM. The property under test is not the encryption -- it is
 * whether a write that did not reach disk is reported as one that did. A failing commit() is the
 * only way to ask that question, and a real device will not fail one on request.
 *
 * A failed commit leaves the map untouched, as a real failed commit leaves the file untouched.
 */
class FakePrefs : SharedPreferences {
  val values = mutableMapOf<String, Any?>()
  var failCommits = false

  override fun getAll(): MutableMap<String, *> = values.toMutableMap()
  override fun getString(key: String, defValue: String?): String? = values[key] as String? ?: defValue
  @Suppress("UNCHECKED_CAST")
  override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
    values[key] as MutableSet<String>? ?: defValues
  override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue
  override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue
  override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue
  override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
  override fun contains(key: String): Boolean = key in values
  override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
  override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

  override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
    private val puts = mutableMapOf<String, Any?>()
    private val removes = mutableSetOf<String>()
    private var clearAll = false

    override fun putString(key: String, value: String?) = apply { puts[key] = value }
    override fun putStringSet(key: String, values: MutableSet<String>?) = apply { puts[key] = values }
    override fun putInt(key: String, value: Int) = apply { puts[key] = value }
    override fun putLong(key: String, value: Long) = apply { puts[key] = value }
    override fun putFloat(key: String, value: Float) = apply { puts[key] = value }
    override fun putBoolean(key: String, value: Boolean) = apply { puts[key] = value }
    override fun remove(key: String) = apply { removes += key }
    override fun clear() = apply { clearAll = true }

    override fun commit(): Boolean {
      if (failCommits) return false
      if (clearAll) values.clear()
      removes.forEach { values.remove(it) }
      values.putAll(puts)
      return true
    }

    override fun apply() { commit() }
  }
}
