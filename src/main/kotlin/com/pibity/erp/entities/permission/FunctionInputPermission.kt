/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.permission

import com.pibity.erp.entities.permission.embeddables.FunctionInputPermissionId
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "function_input_permission", schema = "inventory")
data class FunctionInputPermission(

    @EmbeddedId
    val id: FunctionInputPermissionId,

    @Column(name = "access_level", nullable = false)
    var accessLevel: Boolean

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInputPermission
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
