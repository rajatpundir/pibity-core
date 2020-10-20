/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.permission

import com.pibity.erp.entities.mappings.RoleFunctionPermission
import com.pibity.erp.entities.permission.embeddables.FunctionPermissionId
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_permission", schema = "inventory")
data class FunctionPermission(

    @EmbeddedId
    val id: FunctionPermissionId,

    @OneToMany(mappedBy = "id.functionPermission", cascade = [CascadeType.ALL])
    var functionInputPermissions: MutableSet<FunctionInputPermission> = HashSet(),

    @OneToMany(mappedBy = "id.functionPermission", cascade = [CascadeType.ALL])
    var functionOutputPermissions: MutableSet<FunctionOutputPermission> = HashSet(),

    @OneToMany(mappedBy = "id.permission", cascade = [CascadeType.ALL])
    val permissionRoles: MutableSet<RoleFunctionPermission> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionPermission
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = serialize(this).toString()
}
