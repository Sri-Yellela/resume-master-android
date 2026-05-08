package com.resumemaster.android.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable fun ActionLabel(text:String,icon:ImageVector,color:Color,modifier:Modifier=Modifier){ Row(modifier.background(color.copy(alpha=.12f),RoundedCornerShape(999.dp)).border(BorderStroke(1.5.dp,color),RoundedCornerShape(999.dp)).padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){ Icon(icon,contentDescription=text,tint=color); Text(text,color=color,fontWeight=FontWeight.Black,fontSize=14.sp,letterSpacing=.08.sp,modifier=Modifier.padding(start=6.dp)) } }

