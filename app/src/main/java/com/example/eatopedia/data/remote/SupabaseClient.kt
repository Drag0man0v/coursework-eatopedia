//по ідеї має юзатись тіко в repository

package com.example.eatopedia.data.remote

import com.example.eatopedia.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY){
        install(Postgrest)
        install(Auth)
        install(Storage)
    }

}