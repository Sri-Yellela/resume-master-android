package com.resumemaster.android.ui.resume

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.ResumeSection
import com.resumemaster.android.ui.theme.*

/**
 * One section of the resume builder. Swipe right edits; swipe left HIDES a visible section and
 * DELETES a hidden one.
 *
 * The delete arm was an unconfirmed, persisted delete (sweep Shape 2 #6): the same gesture that
 * hides a section a moment earlier destroys it the second time. It now asks first.
 *
 * The swipe state keeps the FIRST composition's confirmValueChange, so the lambda read a stale
 * section.isVisible: a hidden row painted "Delete" and the gesture un-hid it instead — the delete
 * arm was unreachable. It reads the current values through rememberUpdatedState now, and a hidden
 * row says so. The drag handle that stood at the row's start is gone: it was described "Reorder" and nothing reordered
 * (ResumeViewModel.reorderSections has no caller) — a control that does nothing is Shape 2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SectionRow(section:ResumeSection,onEdit:()->Unit,onHide:()->Unit,onDelete:()->Unit){ var confirmDelete by remember{mutableStateOf(false)}; val visibleNow by rememberUpdatedState(section.isVisible); val editNow by rememberUpdatedState(onEdit); val hideNow by rememberUpdatedState(onHide); if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Delete “${section.title}”?")},text={Text("The section and its ${section.fields.size} fields are removed from this resume. This cannot be undone.")},confirmButton={TextButton(onClick={confirmDelete=false;onDelete()}){Text("Delete",color=Error)}},dismissButton={TextButton(onClick={confirmDelete=false}){Text("Keep")}}); val state=rememberSwipeToDismissBoxState(confirmValueChange={v-> when(v){ SwipeToDismissBoxValue.StartToEnd->editNow(); SwipeToDismissBoxValue.EndToStart->if(visibleNow)hideNow()else confirmDelete=true; SwipeToDismissBoxValue.Settled->Unit}; false }); SwipeToDismissBox(state=state,backgroundContent={ Row(Modifier.fillMaxSize().background(if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Primary else if(section.isVisible)SurfaceOffset else Error).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Arrangement.Start else Arrangement.End){ Icon(if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Icons.Rounded.Edit else if(section.isVisible)Icons.Rounded.VisibilityOff else Icons.Rounded.Delete,contentDescription=null,tint=if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Color.White else TextMuted); Text(if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)"Edit" else if(section.isVisible)"Hide" else "Delete",color=if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Color.White else TextMuted,modifier=Modifier.padding(start=8.dp)) } }){ Surface(Modifier.fillMaxWidth().clickable(onClick=onEdit),color=MaterialTheme.colorScheme.surface){ Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){ Column(Modifier.weight(1f).padding(end=14.dp)){Text(section.title,style=BodyLarge);Text("${section.fields.size} fields"+if(section.isVisible)"" else " · hidden",style=LabelSmall,color=TextMuted)}; Icon(Icons.Rounded.ChevronRight,contentDescription="Open",tint=TextFaint) } } } }
