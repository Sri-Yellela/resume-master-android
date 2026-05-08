package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import com.resumemaster.android.data.MockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TemplatesViewModel:ViewModel(){ val templates=MockData.templates; private val _selectedTemplateId=MutableStateFlow("modern"); val selectedTemplateId:StateFlow<String> = _selectedTemplateId; fun select(id:String){_selectedTemplateId.value=id} }

