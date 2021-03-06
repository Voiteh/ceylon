/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
import ceylon.language.meta.declaration {
  ValueConstructorDeclaration, Package,
  ClassDeclaration, Module, OpenType
}
import ceylon.language.meta.model {
  ValueConstructor, MemberClassValueConstructor, Type
}
import ceylon.language { AnnotationType=Annotation }

shared native class OpenValueConstructor(containingPackage, shared Anything meta) satisfies ValueConstructorDeclaration {
  shared actual native ClassDeclaration container;
  shared actual native ValueConstructor<Result> apply<Result=Anything>();
  shared actual native MemberClassValueConstructor<Container, Result> memberApply<Container=Nothing, Result=Anything>(Type<Object> containerType);
  shared actual native Anything staticGet(Type<Object> containerType);

  shared actual native Boolean shared;
  shared actual native Boolean formal;
  shared actual native Boolean default;
  shared actual native Boolean actual;
  shared actual native Boolean static;
  //Contained
  toplevel=false;
  shared actual Package containingPackage;
  shared actual native Module containingModule => containingPackage.container;
  //TypedDeclaration
  shared actual native OpenType openType;
  //AnnotatedDeclaration
  shared actual native Annotation[] annotations<out Annotation>()
        given Annotation satisfies AnnotationType;
  shared actual native Boolean annotated<Annotation>()
          given Annotation satisfies AnnotationType;
  //Declaration
  shared actual native String name;
  shared actual native String qualifiedName;
  shared actual String string => "new ``qualifiedName``";
  shared actual native Boolean equals(Object other);
}
