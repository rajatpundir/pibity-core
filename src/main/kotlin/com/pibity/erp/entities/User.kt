/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.gson
import com.pibity.erp.entities.embeddables.UserId
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "user", schema = "inventory")
data class User(

    @EmbeddedId
    val id: UserId,

    val active: Boolean = true,

    @ManyToMany
    @JoinTable(name = "mapping_user_roles", schema = "inventory",
        joinColumns = [JoinColumn(name = "organization_id"), JoinColumn(name = "username")],
        inverseJoinColumns = [JoinColumn(name = "role_organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "role_name", referencedColumnName = "role_name")])
    val roles: MutableSet<Role> = HashSet(),

    @ManyToMany
    @JoinTable(name = "mapping_user_groups", schema = "inventory",
        joinColumns = [JoinColumn(name = "organization_id"), JoinColumn(name = "username")],
        inverseJoinColumns = [JoinColumn(name = "group_organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "group_name", referencedColumnName = "group_name")])
    val groups: MutableSet<Group> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as User
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = gson.toJson(this)
}
