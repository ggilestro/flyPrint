package ro.gilest.flyprint.api

import okhttp3.Interceptor
import okhttp3.Response
import ro.gilest.flyprint.config.AppConfig

/**
 * OkHttp interceptor that adds X-API-Key header to every request.
 */
class AuthInterceptor(private val config: AppConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-API-Key", config.apiKey)
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
