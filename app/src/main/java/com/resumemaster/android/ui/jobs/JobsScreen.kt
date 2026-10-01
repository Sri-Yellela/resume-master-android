package com.resumemaster.android.ui.jobs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumemaster.android.models.SwipeAction
import com.resumemaster.android.ui.theme.*
import com.resumemaster.android.util.HapticUtil
import com.resumemaster.android.viewmodel.DeckViewModel

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Today's draft (ANDROID.md 2c, revised 09-30): the day's frozen deal, shared with the web. Right DRAFTS
 * (held for review — nothing is sent), left PASSES (saved), the star button SAVES.
 */
@Composable fun JobsScreen(vm:DeckViewModel=viewModel(),onSessionExpired:()->Unit={}){ val jobs by vm.jobs.collectAsState(); val deck by vm.deck.collectAsState(); val drafted by vm.drafted.collectAsState(); val expired by vm.sessionExpired.collectAsState(); LaunchedEffect(expired){ if(expired) onSessionExpired() }; val last by vm.lastAction.collectAsState(); val error by vm.error.collectAsState(); val loading by vm.loading.collectAsState(); val context=LocalContext.current; val snackbar=remember{SnackbarHostState()}; LaunchedEffect(error,jobs.isEmpty()){ val e=error; if(e!=null&&jobs.isNotEmpty()){ snackbar.showSnackbar(e); vm.errorShown() } }; val size=deck?.size?:0; Scaffold(snackbarHost={SnackbarHost(snackbar)},topBar={CenterAlignedTopAppBar(title={ Column(horizontalAlignment=Alignment.CenterHorizontally){ Text("Today",style=TitleMedium); if(size>0) Text("${(size-jobs.size).coerceAtMost(size)} of $size decided · $drafted drafted",style=CaptionText,color=TextMuted) } },colors=TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor=MaterialTheme.colorScheme.background))}){padding-> Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),horizontalAlignment=Alignment.CenterHorizontally){ Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=22.dp),contentAlignment=Alignment.Center){ CardStackLayout(jobs,Modifier.fillMaxWidth().fillMaxHeight(.85f)){a,j->vm.onSwipe(a,j)}; DeckState(jobs.isEmpty(),loading,error,size,drafted,deck?.reason); Box(Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp)){ActionBadge(last)} }; if(jobs.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.SpaceEvenly){ Act(Icons.Rounded.Close,TextMuted,"Pass"){HapticUtil.dismiss(context);vm.onButtonAction(SwipeAction.Dislike)}; Act(Icons.Rounded.Star,Gold,"Save"){HapticUtil.star(context);vm.onButtonAction(SwipeAction.Star)}; Act(Icons.Rounded.Check,Primary,"Draft"){HapticUtil.trigger(context);vm.onButtonAction(SwipeAction.Queue)} } } } }
/**
 * What the board says when it has no cards.
 *
 * An empty stack used to render as an EMPTY SCREEN, whatever the reason -- a failed request, an
 * empty board and a board still loading were pixel-identical. That is the same defect this
 * repository keeps finding on the write side: a surface that reports nothing about what happened.
 * A failed feed in particular was invisible, because JobsViewModel already tracked the error and
 * nothing rendered it.
 */
/**
 * What the deck says when no card is left. Every reason is said — a failed request, a deck still
 * dealing, an EMPTY day (never padded), and a FINISHED one are four different screens, not one blank.
 */
@Composable private fun DeckState(empty:Boolean,loading:Boolean,error:String?,size:Int,drafted:Int,reason:String?){ if(!empty) return; Column(horizontalAlignment=Alignment.CenterHorizontally){ when { error!=null -> { Text("Could not deal today's draft",style=TitleMedium,color=MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(8.dp)); Text(error,style=BodyMedium,color=MaterialTheme.colorScheme.error) }; loading -> Text("Dealing today's draft...",style=BodyMedium,color=TextMuted); size==0 && reason=="no_profile" -> { Text("Set up your first job profile",style=TitleMedium,color=MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(8.dp)); Text("Today's draft is dealt from jobs ranked against a profile. Set one up on the web or in Profile, and your first draft is dealt straight away.",style=BodyMedium,color=TextMuted) }; size==0 && reason=="no_resume" -> { Text("Add your résumé to get today's draft",style=TitleMedium,color=MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(8.dp)); Text("Fit is judged against your résumé, so there is nothing to rank until it's there.",style=BodyMedium,color=TextMuted) }; size==0 -> { Text("Nothing strong enough today",style=TitleMedium,color=MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(8.dp)); Text("Today's draft only deals Strong and Moderate matches you haven't decided yet. A fuller résumé changes this more than anything.",style=BodyMedium,color=TextMuted) }; else -> { Text("That's today's draft",style=TitleMedium,color=MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(8.dp)); Text(if(drafted>0) "$drafted drafted. Nothing has been sent — each draft waits for your review." else "Tomorrow's draft is dealt fresh.",style=BodyMedium,color=TextMuted) } } } }

@Composable private fun Act(icon:androidx.compose.ui.graphics.vector.ImageVector,tint:androidx.compose.ui.graphics.Color,label:String,onClick:()->Unit){ IconButton(onClick=onClick,modifier=Modifier.size(52.dp).background(SurfaceOffset,CircleShape)){Icon(icon,contentDescription=label,tint=tint)} }
