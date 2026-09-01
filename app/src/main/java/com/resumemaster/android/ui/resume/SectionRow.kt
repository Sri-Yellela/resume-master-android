package com.resumemaster.android.ui.resume

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.ResumeSection
import com.resumemaster.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SectionRow(section:ResumeSection,onEdit:()->Unit,onHide:()->Unit,onDelete:()->Unit){ val state=rememberSwipeToDismissBoxState(confirmValueChange={v-> when(v){ SwipeToDismissBoxValue.StartToEnd->onEdit(); SwipeToDismissBoxValue.EndToStart->if(section.isVisible)onHide()else onDelete(); SwipeToDismissBoxValue.Settled->Unit}; false }); SwipeToDismissBox(state=state,backgroundContent={ Row(Modifier.fillMaxSize().background(if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Primary else if(section.isVisible)SurfaceOffset else Error).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Arrangement.Start else Arrangement.End){ Icon(if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Icons.Rounded.Edit else if(section.isVisible)Icons.Rounded.VisibilityOff else Icons.Rounded.Delete,contentDescription=null,tint=if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Color.White else TextMuted); Text(if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)"Edit" else if(section.isVisible)"Hide" else "Delete",color=if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd)Color.White else TextMuted,modifier=Modifier.padding(start=8.dp)) } }){ Surface(Modifier.fillMaxWidth().clickable(onClick=onEdit),color=MaterialTheme.colorScheme.surface){ Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Rounded.DragHandle,contentDescription="Reorder",tint=TextFaint); Column(Modifier.weight(1f).padding(horizontal=14.dp)){Text(section.title,style=BodyLarge);Text("${section.fields.size} fields",style=LabelSmall,color=TextMuted)}; Icon(Icons.Rounded.ChevronRight,contentDescription="Open",tint=TextFaint) } } } }
