package com.resumemaster.android.ui.preview

import android.content.*
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.*
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import com.resumemaster.android.models.*
import java.io.File

object PdfExporter{ fun exportToPdf(resume:Resume,template:Template,context:Context):Uri{ val dir=File(context.cacheDir,"exports").apply{mkdirs()}; val file=File(dir,"${resume.name.replace(" ","_")}_resume.pdf"); val doc=Document(PdfDocument(PdfWriter(file))); val a=android.graphics.Color.parseColor(template.accentColorHex); doc.add(Paragraph(resume.name).setFontSize(22f).simulateBold().setFontColor(DeviceRgb(android.graphics.Color.red(a),android.graphics.Color.green(a),android.graphics.Color.blue(a)))); resume.sections.filter{it.isVisible}.sortedBy{it.order}.forEach{s-> doc.add(Paragraph(s.title).simulateBold().setFontSize(13f)); val table=Table(UnitValue.createPercentArray(floatArrayOf(28f,72f))).useAllAvailableWidth(); s.fields.forEach{f->table.addCell(Paragraph(f.label).simulateBold()); table.addCell(Paragraph(f.value))}; doc.add(table)}; doc.close(); return FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)}; fun share(uri:Uri,context:Context){ val intent=ShareCompat.IntentBuilder(context).setType("application/pdf").setStream(uri).setChooserTitle("Share resume PDF").intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); context.startActivity(Intent.createChooser(intent,"Share resume PDF")) } }
