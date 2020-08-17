package com.pibity.erp.entities

import com.pibity.erp.commons.gson
import com.pibity.erp.entities.embeddables.RoleId
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "role", schema = "inventory")
data class Role(

    @EmbeddedId
    val id: RoleId,

    @ManyToMany
    @JoinTable(name = "mapping_role_permissions", schema = "inventory",
        joinColumns = [JoinColumn(name = "organization_id"), JoinColumn(name = "role_name")],
        inverseJoinColumns = [JoinColumn(name = "permission_organization_id", referencedColumnName = "organization_id"),
          JoinColumn(name = "permission_super_type_name", referencedColumnName = "super_type_name"),
          JoinColumn(name = "permission_type_name", referencedColumnName = "type_name"),
          JoinColumn(name = "permission_name", referencedColumnName = "permission_name")])
    val permissions: MutableSet<TypePermission> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Role
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = gson.toJson(this)
}
