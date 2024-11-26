package org.monerokon.xmrpos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.monerokon.xmrpos.data.local.datastore.DataStoreLocalDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayApi
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.MoneroPayRepository
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
        // Fetch the base URL from DataStoreRepository
        val baseUrl = runBlocking { dataStoreRepository.getMoneroPayServerAddress().first() }

        return Retrofit.Builder()
            .baseUrl(baseUrl)  // Use the dynamic base URL from DataStoreRepository
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
        dataStoreLocalDataSource: DataStoreLocalDataSource
    ): MoneroPayRepository {
        return MoneroPayRepository(moneroPayRemoteDataSource, dataStoreLocalDataSource)
    }
}