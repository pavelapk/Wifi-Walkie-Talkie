package ru.pavelapk.wifi_walkie_talkie.utils

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.toast(text: String) =
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun AppCompatActivity.toast(@StringRes resId: Int) =
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

fun AppCompatActivity.toastLong(text: String) =
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()

fun AppCompatActivity.toastLong(@StringRes resId: Int) =
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()