package com.resumemaster.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplayLarge=TextStyle(fontSize=24.sp,fontWeight=FontWeight.SemiBold,letterSpacing=0.sp); val TitleMedium=TextStyle(fontSize=18.sp,fontWeight=FontWeight.SemiBold,letterSpacing=0.sp); val BodyLarge=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Normal,letterSpacing=0.sp); val BodyMedium=TextStyle(fontSize=14.sp,fontWeight=FontWeight.Normal,letterSpacing=0.sp); val LabelSmall=TextStyle(fontSize=12.sp,fontWeight=FontWeight.Medium,letterSpacing=0.05.sp); val CaptionText=TextStyle(fontSize=11.sp,fontWeight=FontWeight.Normal,letterSpacing=0.sp)
val AppTypography=Typography(displayLarge=DisplayLarge,titleMedium=TitleMedium,bodyLarge=BodyLarge,bodyMedium=BodyMedium,labelSmall=LabelSmall)

