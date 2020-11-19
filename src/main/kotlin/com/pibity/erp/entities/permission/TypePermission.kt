/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.permission

import com.pibity.erp.entities.Type
import com.pibity.erp.entities.mappings.RoleTypePermission
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "type_permission", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["type_id", "name"])])
data class TypePermission(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_permission_generator")
    @SequenceGenerator(name = "type_permission_generator", sequenceName = "type_permission_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "type_id", referencedColumnName = "id")])
    val type: Type,

    @Column(name = "name", nullable = false)
    val name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "create_permission", nullable = false)
    var creatable: Boolean,

    @Column(name = "deletion_permission", nullable = false)
    var deletable: Boolean,

    @OneToMany(mappedBy = "typePermission", cascade = [CascadeType.ALL])
    var keyPermissions: MutableSet<KeyPermission> = HashSet(),

    @OneToMany(mappedBy = "id.permission", cascade = [CascadeType.ALL])
    val permissionRoles: MutableSet<RoleTypePermission> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as TypePermission
    return this.type == other.type && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
