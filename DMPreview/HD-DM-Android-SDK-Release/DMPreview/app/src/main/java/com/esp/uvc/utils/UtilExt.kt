/*****************************************************************************/
/**
 * @date     May 2019
 *
 * @copyright (c) 2019 - Seven Sensing Software
 *
 */
/*****************************************************************************/

package com.esp.uvc.utils

import android.app.Activity
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.afollestad.materialdialogs.MaterialDialog
import com.esp.uvc.BuildConfig
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.coroutines.CoroutineContext
import kotlin.math.round
import kotlin.reflect.KMutableProperty0

fun AppCompatActivity.replaceFragmentInActivity(fragment: Fragment, @IdRes frameId: Int) {
    supportFragmentManager.transact {
        replace(frameId, fragment)
    }
}

private inline fun FragmentManager.transact(action: FragmentTransaction.() -> Unit) {
    beginTransaction().apply {
        action()
    }.commit()
}

fun Any.logd(txt: String, enabled: Boolean = true, t: String = this::class.java.simpleName) { //internal debug logging
    if (BuildConfig.DEBUG && enabled) {
        val tag = if (!t.toLowerCase().contains(BuildConfig.APPLICATION_ID.toLowerCase())) BuildConfig.APPLICATION_ID + " " + t else t
        Log.d(tag, txt)
    }
}

fun Any.loge(txt: String, enabled: Boolean = true, t: String = this::class.java.simpleName) { //internal error logging
    if (BuildConfig.DEBUG && enabled) {
        val tag = if (!t.toLowerCase().contains(BuildConfig.APPLICATION_ID.toLowerCase())) BuildConfig.APPLICATION_ID + " " + t else t
        Log.e(tag, txt)
    }
}

fun Any.logi(txt: String, enabled: Boolean = true, t: String = this::class.java.simpleName) { //internal error logging
    if (BuildConfig.DEBUG && enabled) {
        val tag = if (!t.toLowerCase().contains(BuildConfig.APPLICATION_ID.toLowerCase())) BuildConfig.APPLICATION_ID + " " + t else t
        Log.i(tag, txt)
    }
}

fun Fragment.roUI(f: () -> Unit) {
    this.activity?.runOnUiThread(f)
}

fun MaterialDialog.timeout(timeout: Long): MaterialDialog {
    try {
        GlobalScope.launch {
            delay(timeout)
            this@timeout.dismiss()
        }
    } catch (e: Exception) {
        loge("Failed to do timeout for Dialog - ${e.localizedMessage}")
    }
    return this
}

fun KMutableProperty0<Boolean>.toggleTimeout(timeout: Long = 2000, f: (() -> Unit)? = null): Boolean {
    try {
        GlobalScope.launch {
            delay(timeout)
            set(true)
            f?.invoke()
        }
    } catch (e: Exception) {
        loge("Failed to delay successfully for toggle - ${e.localizedMessage}")
    }
    set(false)
    return get()
}

fun Activity.roUI(f: () -> Unit) {
    try {
        this.runOnUiThread(f)
    } catch (e: Exception) {
        loge("Failed to run on ui thread - $e")
    }
}

fun View.addRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

fun View.addCircleRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
    setBackgroundResource(resourceId)
}

fun <T> debounce(delayMs: Long = 500L, coroutineContext: CoroutineContext, f: (T) -> Unit): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        if (debounceJob?.isCompleted != false) {
            debounceJob = CoroutineScope(coroutineContext).launch {
                delay(delayMs)
                f(param)
            }
        }
    }
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun Float.toRadian(): Float = this / 180f * Math.PI.toFloat()
fun Float.toDegree(): Float = this * 180f / Math.PI.toFloat()

fun debounce(delayMs: Long = 500L, coroutineContext: CoroutineContext, f: () -> Unit): () -> Unit {
    var debounceJob: Job? = null
    return {
        if (debounceJob?.isCompleted != false) {
            debounceJob = CoroutineScope(coroutineContext).launch {
                delay(delayMs)
                f()
            }
        }
    }
}

fun Any.isSupportOpenCL(): Boolean {
    // TODO Check more path
    val cmd = ArrayList<String>()
    cmd.add("/system/bin/sh")
    cmd.add("-c")
    cmd.add("ls system/vendor/lib64/libOpenCL.so")
    try {
        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        val process = pb.start()
        val br = BufferedReader(InputStreamReader(process.inputStream))
        var line = br.readLine()
        while (line != null) {
            if (line.contains("No such file or directory"))
                return false
            line = br.readLine() ?: break
        }
        return true
    } catch (e: Exception) {
        loge("isSupportOpenCL Exception : $e")
        return false
    }
}

operator fun <E> java.util.ArrayList<E>.component6() = this[5]
operator fun <E> java.util.ArrayList<E>.component7() = this[6]
operator fun <E> java.util.ArrayList<E>.component8() = this[7]
operator fun <E> java.util.ArrayList<E>.component9() = this[8]
operator fun <E> java.util.ArrayList<E>.component10() = this[9]
operator fun <E> java.util.ArrayList<E>.component11() = this[10]
operator fun <E> java.util.ArrayList<E>.component12() = this[11]
operator fun <E> java.util.ArrayList<E>.component13() = this[12]
operator fun <E> java.util.ArrayList<E>.component14() = this[13]
operator fun <E> java.util.ArrayList<E>.component15() = this[14]
