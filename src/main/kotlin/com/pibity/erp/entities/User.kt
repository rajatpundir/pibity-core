/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.entities.embeddables.UserId
import com.pibity.erp.entities.mappings.UserGroup
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "user", schema = "inventory")
data class User(

    @EmbeddedId
    val id: UserId,

    @Column(name = "active", nullable = false)
    var active: Boolean,

    @Column(name = "email", nullable = false)
    var email: String,

    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @OneToMany(mappedBy = "id.user", cascade = [CascadeType.ALL])
    val userRoles: MutableSet<UserRole> = HashSet(),

    @OneToMany(mappedBy = "id.user", cascade = [CascadeType.ALL])
    val userGroups: MutableSet<UserGroup> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as User
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = serialize(this).toString()
}
