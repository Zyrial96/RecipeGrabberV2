package com.recipegrabber.presentation.util

import java.text.NumberFormat
import java.util.Locale

fun formatIngredientAmount(amount: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }.format(amount)
