package com.trackit.app.di

import org.koin.dsl.module

val appModule = module {
    includes(networkModule)
}
