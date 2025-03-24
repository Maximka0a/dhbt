package com.example.dhbt.di

import com.example.dhbt.data.repository.FakeSubscriptionRepository
import com.example.dhbt.domain.repository.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SubscriptionModule {

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        fakeSubscriptionRepository: FakeSubscriptionRepository
    ): SubscriptionRepository = fakeSubscriptionRepository
}