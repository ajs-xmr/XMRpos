// DataStoreManager.kt
package org.monerokon.xmrpos.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


val Context.settingsDataStore by preferencesDataStore("settings")


class DataStoreManager(private val context: Context) {

    // on first run, populate the DataStore with some default values

    init {
        CoroutineScope(Dispatchers.IO).launch {
            populateDataStore()
        }

    }

    // Define some preferences keys
    companion object {
        val COMPANY_NAME: Preferences.Key<String> = stringPreferencesKey("company_name")
        val CONTACT_INFORMATION: Preferences.Key<String> = stringPreferencesKey("contact_information")
        val PRIMARY_FIAT_CURRENCY: Preferences.Key<String> = stringPreferencesKey("primary_fiat_currency")
        val REFERENCE_FIAT_CURRENCIES: Preferences.Key<String> = stringPreferencesKey("reference_fiat_currencies")
        val REQUIRE_PIN_CODE_ON_APP_START: Preferences.Key<Boolean> = booleanPreferencesKey("require_pin_code_on_app_start")
        val REQUIRE_PIN_CODE_OPEN_SETTINGS: Preferences.Key<Boolean> = booleanPreferencesKey("require_pin_code_open_settings")
        val PIN_CODE: Preferences.Key<String> = stringPreferencesKey("pin_code")
        val MONERO_PAY_CONF_VALUE: Preferences.Key<String> = stringPreferencesKey("monero_pay_conf_value")
        val MONERO_PAY_SERVER_ADDRESS: Preferences.Key<String> = stringPreferencesKey("monero_pay_server_address")
        val MONERO_PAY_USE_CALLBACKS: Preferences.Key<Boolean> = booleanPreferencesKey("monero_pay_use_callbacks")
        val MONERO_PAY_REFRESH_INTERVAL: Preferences.Key<Int> = intPreferencesKey("monero_pay_refresh_interval")
    }


    // Save a string value
    suspend fun saveString(key: Preferences.Key<String>, value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // Retrieve a string value
    fun getString(key: Preferences.Key<String>): Flow<String?> {
        return context.settingsDataStore.data
            .map { preferences ->
                preferences[key]
            }
    }

    // Save a string set value
    suspend fun saveStringSet(key: Preferences.Key<Set<String>>, value: Set<String>) {
        context.settingsDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // Retrieve a string set value
    fun getStringSet(key: Preferences.Key<Set<String>>): Flow<Set<String>?> {
        return context.settingsDataStore.data
            .map { preferences ->
                preferences[key]
            }
    }

    // Save a string list value
    suspend fun saveStringList(key: Preferences.Key<String>, value: List<String>) {
        val joinedString = value.joinToString(",")
        context.settingsDataStore.edit { preferences ->
            preferences[key] = joinedString
        }
    }

    // Retrieve a string list value
    fun getStringList(key: Preferences.Key<String>): Flow<List<String>?> {
        return context.settingsDataStore.data
            .map { preferences ->
                val joinedString: String = preferences[key].toString()
                if (joinedString.toString() != "") joinedString.split(",") else emptyList()
            }
    }

    // Save a boolean value
    suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // Retrieve a boolean value
    fun getBoolean(key: Preferences.Key<Boolean>): Flow<Boolean?> {
        return context.settingsDataStore.data
            .map { preferences ->
                preferences[key]
            }
    }

    // Save an int value
    suspend fun saveInt(key: Preferences.Key<Int>, value: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // Retrieve an int value
    fun getInt(key: Preferences.Key<Int>): Flow<Int?> {
        return context.settingsDataStore.data
            .map { preferences ->
                preferences[key]
            }
    }

    // Check each key individually and populate the DataStore with default values if they don't exist
    suspend fun populateDataStore() {
        println("Populating DataStore")
        println(context.settingsDataStore.data.first().toString())
        if (getString(COMPANY_NAME).first() == null) {
            saveString(COMPANY_NAME, "My Company")
        }
        if (getString(CONTACT_INFORMATION).first() == null) {
            saveString(CONTACT_INFORMATION, "123 Main St, Anytown, USA")
        }
        if (getString(PRIMARY_FIAT_CURRENCY).first() == null) {
            saveString(PRIMARY_FIAT_CURRENCY, "USD")
        }
        if (getString(REFERENCE_FIAT_CURRENCIES).first() == null) {
            saveStringList(
                REFERENCE_FIAT_CURRENCIES,
                arrayListOf(
                    "EUR",
                    "CZK",
                    "MXN",
                )
            )
        }
        if (getBoolean(REQUIRE_PIN_CODE_ON_APP_START).first() == null) {
            saveBoolean(REQUIRE_PIN_CODE_ON_APP_START, false)
        }
        if (getBoolean(REQUIRE_PIN_CODE_OPEN_SETTINGS).first() == null) {
            saveBoolean(REQUIRE_PIN_CODE_OPEN_SETTINGS, false)
        }
        if (getString(PIN_CODE).first() == null) {
            saveString(PIN_CODE, "")
        }
        if (getString(MONERO_PAY_CONF_VALUE).first() == null) {
            saveString(MONERO_PAY_CONF_VALUE, "0-conf")
        }
        if (getString(MONERO_PAY_SERVER_ADDRESS).first() == null) {
            saveString(MONERO_PAY_SERVER_ADDRESS, "http://10.0.2.2:5000")
        }
        if (getBoolean(MONERO_PAY_USE_CALLBACKS).first() == null) {
            saveBoolean(MONERO_PAY_USE_CALLBACKS, false)
        }
        if (getInt(MONERO_PAY_REFRESH_INTERVAL).first() == null) {
            saveInt(MONERO_PAY_REFRESH_INTERVAL, 3)
        }
    }
}
