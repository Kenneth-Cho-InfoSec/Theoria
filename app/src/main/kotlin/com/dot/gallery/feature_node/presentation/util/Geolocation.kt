/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.feature_node.presentation.util

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.text.isDigitsOnly

@Composable
fun rememberGeocoder(): Geocoder? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !Geocoder.isPresent()) return null
    val context = LocalContext.current
    return try {
        Geocoder(context)
    } catch (_: RuntimeException) {
        null
    }
}

fun Geocoder.getLocation(lat: Double, long: Double, onLocationFound: (Address?) -> Unit) {
    if (!lat.isFinite() || !long.isFinite() || lat !in -90.0..90.0 || long !in -180.0..180.0) {
        onLocationFound(null)
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            getFromLocation(lat, long, 1) { results ->
                onLocationFound(results.firstOrNull())
            }
        }.onFailure { onLocationFound(null) }
    } else {
        onLocationFound(null)
    }
}

val Address.formattedAddress: String get() {
    var address = ""
    if (!featureName.isNullOrBlank() && !featureName.isDigitsOnly()) address += featureName
    else if (!subLocality.isNullOrBlank()) address += subLocality
    if (!locality.isNullOrBlank()) {
        address += if (address.isEmpty()) locality
        else ", $locality"
    }
    if (!countryName.isNullOrBlank()) {
        address += if (address.isEmpty()) countryName
        else ", $countryName"
    }

    return address
}

val Address.locationTag: String get() =
    if (!featureName.isNullOrBlank() && !featureName.isDigitsOnly()) featureName
    else if (!subLocality.isNullOrBlank()) subLocality
    else locality
