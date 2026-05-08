package com.resumemaster.android.ui.jobs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.resumemaster.android.ui.components.ActionLabel
import com.resumemaster.android.ui.theme.*
import com.resumemaster.android.util.SwipeThresholds
import kotlin.math.abs

@Composable fun SwipeOverlay(offset:Offset){ val p=((abs(offset.x)-SwipeThresholds.SOFT_OFFSET)/SwipeThresholds.SOFT_OFFSET).coerceIn(0f,1f); Box(Modifier.fillMaxSize().padding(18.dp)){ if(offset.x>0){ val hard=abs(offset.x)>SwipeThresholds.HARD_OFFSET; val diagonal=offset.y < -60f; val text=if(diagonal)"STAR" else if(hard)"APPLY NOW" else "QUEUE"; val icon=if(diagonal)Icons.Rounded.Star else if(hard)Icons.Rounded.CheckCircle else Icons.Rounded.AddCircle; val color:Color=if(diagonal)Gold else if(hard)Success else Primary; ActionLabel(text,icon,color,Modifier.align(Alignment.CenterStart).graphicsLayer{alpha=p}) }; if(offset.x<0) ActionLabel("SKIP",Icons.Rounded.Close,TextMuted,Modifier.align(Alignment.CenterEnd).graphicsLayer{alpha=p}) } }

