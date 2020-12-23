/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.mappings.embeddables

import com.pibity.erp.entities.Role
import com.pibity.erp.entities.permission.FunctionPermission
import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne

@Embeddable
data class RoleFunctionPermissionId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "role_id", referencedColumnName = "id")])
    val role: Role,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_permission_id", referencedColumnName = "id")])
    val permission: FunctionPermission

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as RoleFunctionPermissionId
    return this.role == other.role && this.permission == other.permission
  }

  override fun hashCode(): Int = Objects.hash(role, permission)
}
