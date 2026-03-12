package com.encorekit.kmp.models

data class UserAttributes(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val state: String? = null,
    val countryCode: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val language: String? = null,
    val subscriptionTier: String? = null,
    val monthsSubscribed: String? = null,
    val billingCycle: String? = null,
    val lastPaymentAmount: String? = null,
    val lastActiveDate: String? = null,
    val totalSessions: String? = null,
    val custom: Map<String, String> = emptyMap(),
)
