package com.example.eatopedia.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(

    val id: String,
    val username: String,
    val bio: String?,

    @SerialName("avatar_url")
    val avatarUrl: String?,

    @SerialName("created_at")
    val createdAt: String?
)