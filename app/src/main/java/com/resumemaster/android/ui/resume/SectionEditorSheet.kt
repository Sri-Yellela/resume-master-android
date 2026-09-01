package com.resumemaster.android.ui.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.ResumeSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SectionEditorSheet(section:ResumeSection,onDismiss:()->Unit,onTitle:(String)->Unit,onFieldLabel:(String,String)->Unit,onFieldValue:(String,String)->Unit,onDeleteField:(String)->Unit,onAddField:()->Unit){ ModalBottomSheet(onDismissRequest=onDismiss,dragHandle={Surface(Modifier.padding(top=8.dp).size(width=42.dp,height=4.dp),shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.outline){}}){ Column(Modifier.padding(18.dp)){ TextField(section.title,onTitle,textStyle=MaterialTheme.typography.titleMedium,colors=TextFieldDefaults.colors(unfocusedContainerColor=MaterialTheme.colorScheme.surface,focusedContainerColor=MaterialTheme.colorScheme.surface)); LazyColumn(Modifier.heightIn(max=420.dp).padding(top=12.dp)){ items(section.fields,key={it.id}){field-> FieldRow(field,{onFieldLabel(field.id,it)},{onFieldValue(field.id,it)},{onDeleteField(field.id)}) }; item{TextButton(onClick=onAddField,modifier=Modifier.fillMaxWidth()){Text("＋ Add Field")}} } } } }
