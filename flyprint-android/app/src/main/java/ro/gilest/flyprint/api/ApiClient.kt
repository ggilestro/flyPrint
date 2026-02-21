package ro.gilest.flyprint.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ro.gilest.flyprint.BuildConfig
import ro.gilest.flyprint.config.AppConfig
import java.util.concurrent.TimeUnit

/**
 * Singleton factory for the Retrofit API client.
 *
 * Usage:
 *   val api = ApiClient.create(config)
 */
object ApiClient {

    @Volatile
    private var instance: FlyPrintApi? = null
    private var currentBaseUrl: String? = null

    /**
     * Create or return cached FlyPrintApi instance.
     *
     * Recreates the client if the server URL has changed.
     */
    fun create(config: AppConfig): FlyPrintApi {
        val baseUrl = config.serverUrl.trimEnd('/')
        if (instance != null && currentBaseUrl == baseUrl) {
            return instance!!
        }

        synchronized(this) {
            if (instance != null && currentBaseUrl == baseUrl) {
                return instance!!
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val okHttp = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(config))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("$baseUrl/")
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            instance = retrofit.create(FlyPrintApi::class.java)
            currentBaseUrl = baseUrl
            return instance!!
        }
    }

    /** Force recreation on next call (e.g. after config change). */
    fun invalidate() {
        synchronized(this) {
            instance = null
            currentBaseUrl = null
        }
    }
}
