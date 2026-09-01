package com.resumemaster.android.ui.resume

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.ResumeField

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FieldRow(field:ResumeField,onLabel:(String)->Unit,onValue:(String)->Unit,onDelete:()->Unit){ val state=rememberSwipeToDismissBoxState(confirmValueChange={ if(it!=SwipeToDismissBoxValue.Settled){onDelete();false}else true }); SwipeToDismissBox(state=state,backgroundContent={Box(Modifier.fillMaxSize().padding(end=16.dp),contentAlignment=Alignment.CenterEnd){Icon(Icons.Rounded.Delete,contentDescription="Delete")}}){ Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedTextField(field.label,onLabel,modifier=Modifier.weight(.42f),singleLine=true); OutlinedTextField(field.value,onValue,modifier=Modifier.weight(.58f),minLines=1) } } }
