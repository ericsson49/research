package onotole.lib_defs

import onotole.*

object BLS {
  fun init() {
    packageDef("bls") {
      val TSignature = NamedType("ssz.Bytes96")
      val TPubKey = NamedType("ssz.Bytes48")
      val fq2Type = NamedType("bls.FQ2")
      val TG2 = TPySequence(fq2Type)

      val msgType = NamedType("ssz.Bytes32")

      funDef("signature_to_G2", listOf(TSignature), TG2)
      funDef("Sign", listOf(TPyInt, msgType), TSignature)
      funDef("Verify", listOf(TPubKey, msgType, TSignature), TPyBool)
      funDef("FastAggregateVerify", listOf(TPySequence(TPubKey), msgType, TSignature), TPyBool)
      funDef("AggregateVerify", listOf(TPySequence(TPubKey), TPySequence(msgType), TSignature), TPyBool)
      funDef("Aggregate", listOf(TPySequence(TSignature)), TSignature)
      funDef("AggregatePKs", listOf(TPySequence(TPubKey)), TPubKey)
      funDef("Pairing", listOf(NamedType("sharding.BLSCommitment"), TG2), TPyObject)
      funDef("KeyValidate", listOf(TPubKey), TPyBool)

      classDef("FQ", baseType = TPyObject) {
        attr("coeffs", TPySequence(TPyInt))
      }
      classDef("FQ2", baseType = TPyObject) {
        attr("coeffs", TPySequence(TPyInt))
      }
    }
  }
}
