/*
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils;

import org.codehaus.janino.ExpressionEvaluator;

import java.lang.reflect.InvocationTargetException;

public final class FormulaUtils {
    public static Object evaluateExpression(ExpressionEvaluator evaluator, Object[] injected_objects) throws InvocationTargetException {
        return evaluator.evaluate(injected_objects);
    }
}
