/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.entities.embeddables.KeyId
import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.function.FunctionInputKey
import com.pibity.erp.entities.function.FunctionOutput
import com.pibity.erp.entities.function.FunctionOutputKey
import com.pibity.erp.entities.permission.KeyPermission
import com.pibity.erp.serializers.serialize
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

    @OneToMany(mappedBy = "id.key", cascade = [CascadeType.ALL])
    val permissions: Set<KeyPermission> = HashSet(),

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

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "referenced_variable_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "referenced_variable_super_list_id", referencedColumnName = "super_list_id"),
      JoinColumn(name = "referenced_variable_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "referenced_variable_type_name", referencedColumnName = "type_name"),
      JoinColumn(name = "referenced_variable_name", referencedColumnName = "variable_name")])
    var referencedVariable: Variable? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumns(*[JoinColumn(name = "formula_id", referencedColumnName = "id")])
    var formula: Formula? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "list_id")
    var list: TypeList? = null,

    @Column(name = "is_dependency")
    var isDependency: Boolean = false,

    @Column(name = "is_variable_dependency")
    var isVariableDependency: Boolean = false,

    @ManyToMany(mappedBy = "keyDependencies")
    val dependentFormulas: Set<Formula> = HashSet(),

    @ManyToMany(mappedBy = "variableNameKeyDependencies")
    val dependentFunctionInputVariableNames: Set<FunctionInput> = HashSet(),

    @ManyToMany(mappedBy = "keyDependencies")
    val dependentFunctionInputKeys: Set<FunctionInputKey> = HashSet(),

    @ManyToMany(mappedBy = "variableNameKeyDependencies")
    val dependentFunctionOutputVariableNames: Set<FunctionOutput> = HashSet(),

    @ManyToMany(mappedBy = "keyDependencies")
    val dependentFunctionOutputKeys: Set<FunctionOutputKey> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Key
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = serialize(this).toString()
}
