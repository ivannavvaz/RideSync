package com.inavarro.ridesync.mainModule.infoGroupModule.adapters

import com.inavarro.ridesync.common.entities.User

interface OnClickListener {
    fun onClick(userEntity: User)
    fun onLongClick(userEntity: User)
}