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
import com.resumemaster.android.models.SwipeAction
import com.resumemaster.android.ui.components.ActionLabel
import com.resumemaster.android.ui.theme.*
import com.resumemaster.android.util.SwipeThresholds
import kotlin.math.abs

@Composable fun SwipeOverlay(offset:Offset){ val p=((abs(offset.x)-SwipeThresholds.SOFT_OFFSET)/SwipeThresholds.SOFT_OFFSET).coerceIn(0f,1f); Box(Modifier.fillMaxSize().padding(18.dp)){ /* The label is whatever a release HERE would do -- SwipeThresholds.resolve, the same call CardStackLayout makes. Below SOFT_OFFSET it is null, where p is 0 and nothing was visible anyway. */ when(SwipeThresholds.resolve(offset.x,offset.y)){ SwipeAction.Star->ActionLabel("STAR",Icons.Rounded.Star,Gold,Modifier.align(Alignment.CenterStart).graphicsLayer{alpha=p}); SwipeAction.QueuePriority->ActionLabel("QUEUE FIRST",Icons.Rounded.ArrowCircleUp,Primary,Modifier.align(Alignment.CenterStart).graphicsLayer{alpha=p}); SwipeAction.Queue->ActionLabel("QUEUE",Icons.Rounded.AddCircle,Primary,Modifier.align(Alignment.CenterStart).graphicsLayer{alpha=p}); SwipeAction.Skip->ActionLabel("SKIP",Icons.Rounded.Close,TextMuted,Modifier.align(Alignment.CenterEnd).graphicsLayer{alpha=p}); SwipeAction.Dislike,null->Unit } } }

