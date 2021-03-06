/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
package org.eclipse.ceylon.cmr.spi;

import java.io.IOException;
import java.io.InputStream;

/**
 * Transform stream into content instance.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ContentTransformer {
    <T> T transform(Class<T> contentType, InputStream stream) throws IOException;
}
