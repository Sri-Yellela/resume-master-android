package com.resumemaster.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resumemaster.android.ui.theme.Border

@Composable fun SurfaceCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){ Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,Border),elevation=CardDefaults.cardElevation(1.dp)){ Column(Modifier.padding(16.dp),content=content) } }

