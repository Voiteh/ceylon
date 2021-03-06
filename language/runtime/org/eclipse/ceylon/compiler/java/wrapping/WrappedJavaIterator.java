/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
package org.eclipse.ceylon.compiler.java.wrapping;

import org.eclipse.ceylon.compiler.java.runtime.model.TypeDescriptor;

import org.eclipse.ceylon.compiler.java.wrapping.Wrapping;

import ceylon.language.finished_;

/**
 * a wrapper for Java Iterator which satisfies {@code ceylon.language.Iterator}
 */
class WrappedJavaIterator<Out,In> extends ceylon.language.impl.BaseIterator<Out> {
    
    private static final long serialVersionUID = 1L;
    private final java.util.Iterator<In> jIterator;
    private Wrapping<In, Out> wrapping;
    
    public WrappedJavaIterator(TypeDescriptor $reified$Element, java.lang.Iterable<In> jIterable, Wrapping<In,Out> wrapping) {
        super($reified$Element);
        this.jIterator = jIterable.iterator();
        this.wrapping = wrapping;
    }

    @Override
    public Object next() {
        if (jIterator.hasNext()) {
            return wrapping.wrap((In)jIterator.next());
        } else {
            return finished_.get_();
        }
    }
    
}