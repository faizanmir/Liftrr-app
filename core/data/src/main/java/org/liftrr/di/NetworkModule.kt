package org.liftrr.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.liftrr.data.BuildConfig
import org.liftrr.data.remote.AccountApiService
import org.liftrr.data.remote.AuthApiService
import org.liftrr.data.remote.UserProfileApiService
import org.liftrr.data.remote.UserWeightApiService
import org.liftrr.data.remote.WorkoutSessionApiService
import org.liftrr.data.remote.WorkoutVideoApiService
import org.liftrr.data.remote.auth.AuthInterceptor
import org.liftrr.data.remote.auth.TokenRefreshAuthenticator
import org.liftrr.di.annotations.network.BaseUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.SERVER_URL.trimEnd('/') + "/"

    @Provides
    @Singleton
    fun provideAuthApiService(
        @Named("UnauthenticatedRetrofit") retrofit: Retrofit
    ): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideUserProfileApiService(retrofit: Retrofit): UserProfileApiService =
        retrofit.create(UserProfileApiService::class.java)

    @Provides
    @Singleton
    fun provideAccountApiService(retrofit: Retrofit): AccountApiService =
        retrofit.create(AccountApiService::class.java)

    @Provides
    @Singleton
    fun provideWorkoutSessionApiService(retrofit: Retrofit): WorkoutSessionApiService =
        retrofit.create(WorkoutSessionApiService::class.java)

    @Provides
    @Singleton
    fun provideWorkoutVideoApiService(retrofit: Retrofit): WorkoutVideoApiService =
        retrofit.create(WorkoutVideoApiService::class.java)

    @Provides
    @Singleton
    fun provideUserWeightApiService(retrofit: Retrofit): UserWeightApiService =
        retrofit.create(UserWeightApiService::class.java)

    @Provides
    @Singleton
    @Named("UnauthenticatedOkHttp")
    fun provideUnauthenticatedOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @Named("UnauthenticatedOkHttp") baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator
    ): OkHttpClient = baseClient.newBuilder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenRefreshAuthenticator)
        .build()

    @Provides
    @Singleton
    @Named("UnauthenticatedRetrofit")
    fun provideUnauthenticatedRetrofit(
        @Named("UnauthenticatedOkHttp") okHttpClient: OkHttpClient,
        @BaseUrl baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @BaseUrl baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
