package com.resumemaster.android.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable fun MinimalButton(text:String,onClick:()->Unit,enabled:Boolean=true){ Button(onClick=onClick,enabled=enabled,contentPadding=PaddingValues(horizontal=18.dp,vertical=10.dp),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.primary)){ Text(text) } }

