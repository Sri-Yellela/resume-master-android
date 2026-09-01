package com.resumemaster.android.ui.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.Template
import com.resumemaster.android.ui.theme.Primary

@Composable fun TemplateCard(template:Template,selected:Boolean,onClick:()->Unit){ Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(220.dp).clickable(onClick=onClick)){ Card(Modifier.size(200.dp,280.dp).then(if(selected)Modifier.border(BorderStroke(2.dp,Primary),RoundedCornerShape(16.dp))else Modifier),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(3.dp)){ Canvas(Modifier.fillMaxSize().padding(18.dp)){ val accent=Color(android.graphics.Color.parseColor(template.accentColorHex)); if(template.layout=="split")drawRect(accent,size=Size(38f,size.height)); if(template.name=="Executive")drawRect(Color(0xFF1D1D1F),size=Size(size.width,44f)); drawRect(accent,topLeft=Offset(if(template.layout=="split")48f else 0f,12f),size=Size(size.width*.5f,8f)); repeat(5){i-> drawRoundRect(Color(0xFFE0E0E0),topLeft=Offset(if(template.layout=="split")48f else 0f,42f+i*34f),size=Size(size.width*(.82f-i*.05f),7f),cornerRadius=CornerRadius(3f,3f)); drawLine(accent.copy(alpha=.7f),Offset(if(template.layout=="split")48f else 0f,62f+i*34f),Offset(size.width,62f+i*34f),strokeWidth=1f)}; repeat(8){i->drawRoundRect(Color(0xFFF0F0F0),topLeft=Offset(if(template.name=="Technical")8f else 22f,218f+i*8f),size=Size(size.width*.65f,3f),cornerRadius=CornerRadius(2f,2f))} } }; Text(template.name,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=10.dp)); AnimatedVisibility(selected){Text("Selected",color=Primary,style=MaterialTheme.typography.labelSmall)} } }
