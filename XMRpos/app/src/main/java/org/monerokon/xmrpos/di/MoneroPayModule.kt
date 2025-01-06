package org.monerokon.xmrpos.di

import MoneroPayBaseUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayApi
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPayCallback.MoneroPayCallbackManager
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.MoneroPayRepository
import org.monerokon.xmrpos.data.repository.TransactionRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MoneroPayModule {

    @Provides
    @Named("moneroPayRetrofit")
    fun provideMoneroPayRetrofit(dataStoreRepository: DataStoreRepository): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MoneroPayBaseUrlInterceptor(dataStoreRepository))
            .build()

        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("https://dummyurl.local/") // Dummy base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun provideMoneroPayApi(@Named("moneroPayRetrofit") moneroPayRetrofit: Retrofit): MoneroPayApi {
        return moneroPayRetrofit.create(MoneroPayApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMoneroPayRepository(
        moneroPayRemoteDataSource: MoneroPayRemoteDataSource,
        callbackManager: MoneroPayCallbackManager,
        transactionRepository: TransactionRepository,
        dataStoreRepository: DataStoreRepository
    ): MoneroPayRepository {
        return MoneroPayRepository(moneroPayRemoteDataSource, callbackManager, transactionRepository, dataStoreRepository)
    }

    @Provides
    @Singleton
    fun provideMoneroPayCallbackManager(): MoneroPayCallbackManager {
        return MoneroPayCallbackManager(8080)
    }
}