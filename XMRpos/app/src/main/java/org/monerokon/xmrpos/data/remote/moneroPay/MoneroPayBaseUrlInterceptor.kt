import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        val newRequest = originalRequest.newBuilder().url(newUrl).build()
        return chain.proceed(newRequest)
    }
}