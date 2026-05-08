package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import com.resumemaster.android.data.MockData
import com.resumemaster.android.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdminViewModel:ViewModel(){ private val _users=MutableStateFlow(MockData.adminUsers); val users:StateFlow<List<AdminUser>> = _users; private val _flags=MutableStateFlow(MockData.flags); val flags:StateFlow<List<FeatureFlag>> = _flags; private val _analytics=MutableStateFlow(MockData.analytics); val analytics:StateFlow<AdminAnalytics> = _analytics; fun updateFlag(key:String,enabled:Boolean){_flags.value=_flags.value.map{if(it.key==key)it.copy(enabled=enabled)else it}}; fun suspendUser(userId:String){_users.value=_users.value.map{if(it.id==userId)it.copy(suspended=!it.suspended)else it}}; fun deleteUser(userId:String){_users.value=_users.value.filterNot{it.id==userId}} }

