package com.dc2cell.brankaspdf

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object Supabase {
    val client: SupabaseClient by lazy {
        require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in local.properties"
        }
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
