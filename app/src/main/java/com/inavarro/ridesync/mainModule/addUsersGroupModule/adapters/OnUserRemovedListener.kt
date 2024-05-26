package com.inavarro.ridesync.mainModule.addUsersGroupModule.adapters

import com.inavarro.ridesync.common.entities.User

interface OnUserRemovedListener {
    fun onUserRemoved(user: User)

}