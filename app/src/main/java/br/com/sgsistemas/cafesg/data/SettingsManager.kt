package br.com.sgsistemas.cafesg.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cafe_sg_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_SHOW_RANKING = "show_ranking"
        private const val DEFAULT_URL = "http://192.168.1.252:8000"
        //private const val DEFAULT_URL = "http://192.168.200.135:8000"
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun setBaseUrl(url: String) {
        var formattedUrl = url
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        if (!formattedUrl.endsWith("/")) {
            formattedUrl = "$formattedUrl/"
        }
        prefs.edit().putString(KEY_BASE_URL, formattedUrl).apply()
    }

    fun isRankingEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_RANKING, true)
    }

    fun setRankingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_RANKING, enabled).apply()
    }
}
