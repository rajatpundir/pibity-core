/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.gson
import com.pibity.erp.entities.embeddables.TypePermissionId
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "type_permission", schema = "inventory")
data class TypePermission(

        @EmbeddedId
        val id: TypePermissionId,

        @Column(name = "max_access_level", nullable = false)
        var maxAccessLevel: Int = 0,

        @Column(name = "min_access_level", nullable = false)
        var minAccessLevel: Int = 0,

        @OneToMany(mappedBy = "id.typePermission", cascade = [CascadeType.ALL])
        var keyPermissions: MutableSet<KeyPermission> = HashSet(),

        @OneToMany(mappedBy = "id.permission", cascade = [CascadeType.ALL])
        val permissionRoles: MutableSet<RolePermission> = HashSet()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (this === other) return true
        other as TypePermission
        return this.id == other.id
    }

    override fun hashCode(): Int = Objects.hash(id)

    override fun toString(): String = gson.toJson(this)
}
