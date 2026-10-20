package com.example.waterbottletracker.util

import java.util.Locale

fun formatVolume(ml: Int): String {
    return if (ml >= 1000) {
        val liters = ml / 1000f
        if (liters % 1f == 0f) "${liters.toInt()}L" else String.format(Locale.US, "%.1fL", liters)
    } else {
        "${ml}ml"
    }
}