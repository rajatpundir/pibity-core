/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.function.jpa

import com.pibity.erp.entities.function.FunctionOutputType
import org.springframework.data.repository.CrudRepository

interface FunctionOutputTypeJpaRepository : CrudRepository<FunctionOutputType, Long>
