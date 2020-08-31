/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.gson
import com.pibity.erp.entities.embeddables.RoleId
import com.pibity.erp.entities.mappings.GroupRole
import com.pibity.erp.entities.mappings.RolePermission
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "role", schema = "inventory")
data class Role(

    @EmbeddedId
    val id: RoleId,

    @OneToMany(mappedBy = "id.role", cascade = [CascadeType.ALL])
    val rolePermissions: MutableSet<RolePermission> = HashSet(),

    @OneToMany(mappedBy = "id.role")
    val roleGroups: Set<GroupRole> = HashSet(),

    @OneToMany(mappedBy = "id.role")
    val roleUsers: Set<UserRole> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Role
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = serialize(this).toString()
}
