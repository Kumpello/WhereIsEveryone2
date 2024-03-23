package com.kumpello.whereiseveryone.common.ui

import android.content.Context
import android.widget.Toast

fun shortToast(context: Context, text: String) {
    Toast.makeText(
        context,
        text,
        Toast.LENGTH_SHORT
    ).show()
}

fun longToast(context: Context, text: String) {
    Toast.makeText(
        context,
        text,
        Toast.LENGTH_LONG
    ).show()
}