package com.resumemaster.android.ui.jobs

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resumemaster.android.models.Job
import com.resumemaster.android.ui.theme.*

private fun hex(h:String)=Color(android.graphics.Color.parseColor(h))
@Composable fun JobCard(job:Job,dragOffset:Offset,modifier:Modifier=Modifier){ var expanded by remember(job.id){ mutableStateOf(false) }; Card(modifier.fillMaxWidth().heightIn(min=470.dp,max=620.dp).clickable{expanded=!expanded}.animateContentSize(),shape=ShapeExtraLarge,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(4.dp),border=BorderStroke(1.dp,Border)){ Box{ Column(Modifier.padding(22.dp)){ Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(22.dp)).background(SurfaceOffset)){ Box(Modifier.size(72.dp).align(Alignment.Center).clip(CircleShape).background(hex(job.logoColor)),contentAlignment=Alignment.Center){ Text(job.company.first().toString(),color=Color.White,style=DisplayLarge) }; Surface(Modifier.align(Alignment.TopEnd).padding(12.dp),shape=ShapeFull,color=Primary){ Text("${job.matchScore}% match",color=Color.White,style=LabelSmall,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp)) } }; Spacer(Modifier.height(20.dp)); Text(job.role,style=DisplayLarge,color=MaterialTheme.colorScheme.onSurface); Text("${job.company} • ${job.location}",style=BodyMedium,color=TextMuted); job.salary?.let{Text(it,style=BodyMedium,color=TextMuted)}; LazyRow(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){ items(job.tags){tag-> Surface(shape=ShapeFull,color=SurfaceOffset){Text(tag,style=LabelSmall,color=TextMuted,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))} } }; HorizontalDivider(Modifier.padding(vertical=18.dp),color=Divider); AnimatedContent(expanded,label="jobExpand"){open-> Column{ Text(job.description + if(open) " Requirements include strong Kotlin, product judgment, testing discipline, and clear collaboration with design and backend partners." else "",style=BodyMedium,color=TextPrimary,maxLines=if(open)8 else 2,overflow=TextOverflow.Ellipsis); if(open) Button(onClick={},modifier=Modifier.padding(top=16.dp)){Text("Apply")} else Text("Tap to expand",style=CaptionText,color=TextFaint,modifier=Modifier.padding(top=12.dp)) } } }; SwipeOverlay(dragOffset) } } }
