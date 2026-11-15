package com.example.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppColorScheme
import com.example.model.AppSettings
import com.example.model.HistogramScale
import com.example.model.SortDirection
import com.example.model.SortField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "darktable_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val HISTOGRAM_SCALE = stringPreferencesKey("histogram_scale")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val schemeName = prefs[Keys.COLOR_SCHEME]
            val scaleName = prefs[Keys.HISTOGRAM_SCALE]
            val sortFieldName = prefs[Keys.SORT_FIELD]
            val sortDirName = prefs[Keys.SORT_DIRECTION]
            AppSettings(
                colorScheme = AppColorScheme.entries.firstOrNull { it.name == schemeName } ?: AppColorScheme.DARK,
                histogramScale = HistogramScale.entries.firstOrNull { it.name == scaleName } ?: HistogramScale.LINEAR,
                sortField = SortField.entries.firstOrNull { it.name == sortFieldName } ?: SortField.CAPTURE_DATE,
                sortDirection = SortDirection.entries.firstOrNull { it.name == sortDirName } ?: SortDirection.DESCENDING
            )
        }

    suspend fun setColorScheme(scheme: AppColorScheme) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.COLOR_SCHEME] = scheme.name
        }
    }

    suspend fun setHistogramScale(scale: HistogramScale) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HISTOGRAM_SCALE] = scale.name
        }
    }

    suspend fun setSortOption(field: SortField, direction: SortDirection) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SORT_FIELD] = field.name
            prefs[Keys.SORT_DIRECTION] = direction.name
        }
    }
}
