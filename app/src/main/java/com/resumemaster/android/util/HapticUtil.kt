package com.resumemaster.android.util

import android.content.Context
import android.os.*

object HapticUtil {
    private fun vibrator(context:Context):Vibrator?=if(Build.VERSION.SDK_INT>=31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator else @Suppress("DEPRECATION") (context.getSystemService(Vibrator::class.java))
    private fun predefined(context:Context,effect:Int,fallback:LongArray){val v=vibrator(context)?:return; if(Build.VERSION.SDK_INT>=29) v.vibrate(VibrationEffect.createPredefined(effect)) else @Suppress("DEPRECATION") v.vibrate(fallback,-1)}
    fun softTap(context:Context)=predefined(context,VibrationEffect.EFFECT_CLICK,longArrayOf(0,18)); fun mediumTap(context:Context)=predefined(context,VibrationEffect.EFFECT_HEAVY_CLICK,longArrayOf(0,32)); fun threshold(context:Context)=predefined(context,VibrationEffect.EFFECT_TICK,longArrayOf(0,16)); fun trigger(context:Context)=predefined(context,VibrationEffect.EFFECT_HEAVY_CLICK,longArrayOf(0,44)); fun dismiss(context:Context)=predefined(context,VibrationEffect.EFFECT_DOUBLE_CLICK,longArrayOf(0,24,70,24)); fun star(context:Context)=predefined(context,VibrationEffect.EFFECT_CLICK,longArrayOf(0,16,80,16))
}

