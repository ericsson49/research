package onotole.lib_defs

import onotole.typelib.TLConstDecl
import onotole.typelib.TLModuleDef
import onotole.typelib.TLTClass
import onotole.typelib.parseClassDescr
import onotole.typelib.parseFuncDecl

val phaseModuleDefs = listOf(
    TLModuleDef("phase0", listOf("pylib", "ssz", "bls"), listOf(
        parseFuncDecl("get_eth1_data(Eth1Block)->Eth1Data")
    )),
    TLModuleDef("altair", listOf("pylib", "ssz", "bls", "phase0")),
    TLModuleDef("bellatrix", listOf("pylib", "ssz", "bls", "phase0", "altair"), listOf(
        TLConstDecl("EXECUTION_ENGINE", TLTClass("ExecutionEngine", emptyList())),
        parseClassDescr("ExecutionEngine <: object",
            "get_payload" to "(PayloadId)->ExecutionPayload",
            "notify_new_payload" to "(ExecutionPayload)->bool",
            "notify_forkchoice_updated" to "(Hash32,Hash32,Optional[PayloadAttributes])->Optional[PayloadId]"
        ),
        parseFuncDecl("get_pow_block(Hash32)->PowBlock")
    ))
)
