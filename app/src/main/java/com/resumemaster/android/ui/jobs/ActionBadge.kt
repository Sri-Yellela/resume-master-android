package com.resumemaster.android.ui.jobs

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.SwipeAction
import com.resumemaster.android.ui.theme.*
import com.resumemaster.android.viewmodel.SwipeEvent
import kotlinx.coroutines.delay

sealed class BadgeState{ data object Hidden:BadgeState(); data class Visible(val action:SwipeAction):BadgeState() }
// Keyed on the EVENT, not the action: two identical swipes are two events, so each one restarts the
// effect. Keyed on a bare SwipeAction the second Queue in a row was invisible (JobsViewModel.lastAction).
@Composable fun ActionBadge(event:SwipeEvent?,onDismiss:()->Unit={}){ val action=event?.action; var visible by remember{mutableStateOf(false)}; LaunchedEffect(event){ if(action!=null){ visible=true; delay(2500); visible=false; onDismiss() } }; AnimatedVisibility(visible && action!=null,enter=scaleIn(initialScale=.6f,animationSpec=spring(dampingRatio=.6f))+fadeIn(),exit=fadeOut()){ val (c,i,t)=meta(action?:SwipeAction.Dislike); Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,Border),shadowElevation=2.dp){ Row(Modifier.padding(horizontal=14.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){ Icon(i,contentDescription=t,tint=c); Text(t,style=LabelSmall,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.padding(start=8.dp)) } } } }
/**
 * What each action DID, said plainly. Right is a server-side Draft since the deck (DeckViewModel,
 * 09-30): previewed and held for the user's review — so "waits for your review", never "sent".
 */
private fun meta(action:SwipeAction):Triple<Color,ImageVector,String> = when(action){ SwipeAction.Queue->Triple(Primary,Icons.Rounded.RadioButtonUnchecked,"Drafted — it waits for your review"); SwipeAction.Star->Triple(Gold,Icons.Rounded.Star,"Saved"); SwipeAction.Dislike->Triple(TextMuted,Icons.Rounded.HorizontalRule,"Passed — it won't come back") }
