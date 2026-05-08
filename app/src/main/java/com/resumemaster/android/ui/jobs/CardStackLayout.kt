package com.resumemaster.android.ui.jobs

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.*
import com.resumemaster.android.util.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable fun CardStackLayout(jobs:List<Job>,modifier:Modifier=Modifier,onSwipe:(SwipeAction,Job)->Unit){ val scope=rememberCoroutineScope(); val context=LocalContext.current; val x=remember{Animatable(0f)}; val y=remember{Animatable(0f)}; var soft by remember{mutableStateOf(false)}; var hard by remember{mutableStateOf(false)}; Box(modifier.fillMaxSize()){ jobs.take(4).asReversed().forEachIndexed{ri,job-> val si=jobs.take(4).lastIndex-ri; val front=si==0; val scale=1f-(si*SwipeThresholds.STACK_SCALE); val oy=-(si*SwipeThresholds.STACK_OFFSET).dp; val offset=if(front)Offset(x.value,y.value)else Offset.Zero; JobCard(job,offset,Modifier.graphicsLayer{translationX=if(front)x.value else 0f; translationY=if(front)y.value else oy.toPx(); rotationZ=if(front)x.value*SwipeThresholds.ROTATION_FACTOR else 0f; scaleX=scale; scaleY=scale; alpha=1f-(si*.1f)}.then(if(front) Modifier.pointerInput(job.id){ detectDragGestures(onDrag={change,drag-> change.consume(); scope.launch{x.snapTo(x.value+drag.x); y.snapTo(y.value+drag.y)}; val ax=abs(x.value); if(!soft && ax>SwipeThresholds.SOFT_OFFSET){soft=true; HapticUtil.threshold(context)}; if(!hard && ax>SwipeThresholds.HARD_OFFSET){hard=true; HapticUtil.trigger(context)} }, onDragEnd={ val action=resolve(Offset(x.value,y.value)); if(action!=null) scope.launch{x.animateTo(if(x.value>=0)1200f else -1200f,spring(stiffness=Spring.StiffnessMedium)); onSwipe(action,job); x.snapTo(0f); y.snapTo(0f); soft=false; hard=false} else scope.launch{x.animateTo(0f,spring(dampingRatio=Spring.DampingRatioMediumBouncy,stiffness=Spring.StiffnessMedium)); y.animateTo(0f,spring(dampingRatio=Spring.DampingRatioMediumBouncy,stiffness=Spring.StiffnessMedium)); soft=false; hard=false} }) } else Modifier)) } } }
private fun resolve(offset:Offset):SwipeAction?{ val ax=abs(offset.x); if(ax<SwipeThresholds.SOFT_OFFSET)return null; return if(offset.x<0) SwipeAction.Skip else if(offset.y < -60f) SwipeAction.Star else if(ax>SwipeThresholds.HARD_OFFSET) SwipeAction.Apply else SwipeAction.Queue }
