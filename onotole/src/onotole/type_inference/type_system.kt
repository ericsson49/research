package onotole.type_inference

interface TypingCtx {
  fun getBase(cls: FAtom): FAtom?
  fun getTypeParams(a: FAtom): Triple<List<Int>,List<Int>,List<Int>>
}