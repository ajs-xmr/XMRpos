import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import org.monerokon.xmrpos.data.repository.DataStoreRepository

class MoneroPayBaseUrlInterceptor(private val dataStoreRepository: DataStoreRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val baseUrl = runBlocking { dataStoreRepository.getMoneroPayServerAddress().first() }
        val newUrl = originalRequest.url().newBuilder()
            .scheme(HttpUrl.parse(baseUrl)?.scheme() ?: originalRequest.url().scheme())
            .host(HttpUrl.parse(baseUrl)?.host() ?: originalRequest.url().host())
            .port(HttpUrl.parse(baseUrl)?.port() ?: originalRequest.url().port())
            .build()

        // check if baseUrl contains credentials
        val pattern = Regex("^[a-zA-Z]+://[^:]+:[^@]+@.+$")
        if (pattern.matches(baseUrl)) {
            val pattern1 = Regex("^[a-zA-Z]+://([^:]+):([^@]+)@.+$")
            val matchResult = pattern1.find(baseUrl)

            val username = matchResult?.groupValues[1] ?: ""
            val password = matchResult?.groupValues[2] ?: ""

            val credentials = Credentials.basic(username, password)

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .header("Authorization", credentials)
                .build()

            return chain.proceed(newRequest)
        } else {
            val newRequest = originalRequest.newBuilder().url(newUrl).build()
            return chain.proceed(newRequest)
        }
    }
}