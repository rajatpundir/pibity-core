/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.gson
import com.pibity.erp.entities.embeddables.KeyId
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "key_names", schema = "inventory")
data class Key(

    @EmbeddedId
    val id: KeyId,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "key_type_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "key_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "key_type_name", referencedColumnName = "type_name")])
    val type: Type,

    @Column(name = "display_name")
    var displayName: String = "",

    @Column(name = "key_order")
    var keyOrder: Int = 0,

    @Column(name = "default_string_value")
    var defaultStringValue: String? = null,

    @Column(name = "default_long_value")
    var defaultLongValue: Long? = null,

    @Column(name = "default_double_value")
    var defaultDoubleValue: Double? = null,

    @Column(name = "default_boolean_value")
    var defaultBooleanValue: Boolean? = null,

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "referenced_variable_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "referenced_variable_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "referenced_variable_type_name", referencedColumnName = "type_name"),
      JoinColumn(name = "referenced_super_variable_name", referencedColumnName = "super_variable_name"),
      JoinColumn(name = "referenced_variable_name", referencedColumnName = "variable_name")])
    var referencedVariable: Variable? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "formula_id")
    var formula: Formula? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "list_id")
    var list: TypeList? = null

//    @ManyToMany
//    @JoinTable(name = "mapping_key_permission", schema = "inventory",
//        joinColumns = [
//          JoinColumn(name = "key_name", referencedColumnName = "key_name")],
//        inverseJoinColumns = [JoinColumn(name = "permission_name")])
//    val permission: MutableSet<KeyPermission> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Key
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = gson.toJson(this)
}
