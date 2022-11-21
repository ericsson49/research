package onotole.type_inference

import onotole.typelib.TLClassDecl

interface TypingCtx {
  fun getClassDecl(cls: String): TLClassDecl
  fun getBase(cls: FAtom): FAtom?
  fun getTypeParams(a: FAtom): Triple<List<Int>,List<Int>,List<Int>>
}