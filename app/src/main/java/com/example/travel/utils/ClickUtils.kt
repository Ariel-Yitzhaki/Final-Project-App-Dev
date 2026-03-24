package com.example.travel.utils

import android.view.View

// Prevents double-clicks by ignoring taps within the cooldown period
fun View.setDebouncedClickListener(cooldownMs: Long = 800, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= cooldownMs) {
            lastClickTime = now
            action(view)
        }
    }
}