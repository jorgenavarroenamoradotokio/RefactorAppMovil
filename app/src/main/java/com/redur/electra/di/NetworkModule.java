package com.redur.electra.di;

import com.redur.electra.BuildConfig;
import com.redur.electra.core.logger.SensitiveAwareHttpLoggingInterceptor;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

/** Cliente de red compartido: todas las peticiones Retrofit pasan por el mismo logger. */
@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {

    private NetworkModule() {}

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        if (BuildConfig.LOG_TO_LOGCAT || BuildConfig.LOG_TO_FILE) {
            HttpLoggingInterceptor.Logger logger = message -> Timber.i("HTTP %s", message);
            builder.addInterceptor(new SensitiveAwareHttpLoggingInterceptor(logger, Set.of()));
        }
        return builder.build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
