package onotole.lib_defs

import onotole.*

data class FunDecl(val name: String, val args: List<FArg>, val retType: RTType)

fun mkSimpleResolver(ret: () -> RTType) = run {
  fun resolver(a: List<RTType>) = ret()
  ::resolver
}

object PyLib {
  fun init() {
    classDef("Nothing")
    classDef("object")
    classDef("None", "object")
    classDef("int", "object", comparable = true) {
      staticMeth("from_bytes")
      meth("bit_length")
      meth("to_bytes")
    }
    classDef("bool", "int")

    classDef("Optional", "object", nTParms = 1)
    classDef("Iterator", "object", nTParms = 1)
    classDef("Iterable", "object", nTParms = 1) {
      meth("__contains__")
    }
    classDef("Collection", "Iterable", nTParms = 1, baseParams = listOf(TVIndex(0))) {
      meth("__iter__")
      meth("__len__")
    }
    classDef("Mapping", "Collection", nTParms = 2, baseParams = listOf(TVIndex(0))) {
      meth("__getitem__")
      meth("keys")
      meth("values")
      meth("items")
    }
    classDef("Sequence", "Collection", nTParms = 1, baseParams = listOf(TVIndex(0))) {
      meth("__getitem__")
      meth("__reversed__")
      meth("index")
      meth("count")
    }
    classDef("Set", "Collection", nTParms = 1, baseParams = listOf(TVIndex(0))) {
      meth("union")
      meth("intersection")
      meth("difference")
      meth("add")
    }
    TypeResolver.register(MetaClassTInfo("Tuple", null, 0, emptyList()) { tParams ->
      val elemType = if (tParams.isNotEmpty()) getCommonSuperType(tParams)
      else parseType(TypeResolver.topLevelTyper, "ValidatorIndex")
      TPySequence(elemType)
    })
    classDef("MutableSequence", "Sequence", nTParms = 1, baseParams = listOf(TVIndex(0))) {
      meth("__setitem__")
      meth("__delitem__")
      meth("__iadd__")
      meth("append")
      meth("reverse")
      meth("extend")
      meth("pop")
      meth("remove")
    }
    classDef("PyList", "MutableSequence", nTParms = 1, baseParams = listOf(TVIndex(0)))
    classDef("Dict", "Mapping", nTParms = 2, baseParams = listOf(TVIndex(0), TVIndex(1)))
    //classDef("PyDict", "Mapping", nTParms = 2, baseParams = listOf(0,1))

    classDef("bytes", baseType = TPySequence(TPyInt)) {
      meth("join")
    }
    classDef("str", "object", comparable = true)

    val resolvers = listOf(
        "bool" to PyLib::boolResolver,
        "sum" to PyLib::sumResolver,
        "range" to PyLib::rangeResolver,
        "len" to mkSimpleResolver { TPyInt },
        "zip" to PyLib::zipResolver,
        "enumerate" to PyLib::enumerateResolver,
        "filter" to PyLib::fitlerResolver,
        "list" to PyLib::listResolver,
        "iter" to PyLib::iterResolver,
        "has_next" to PyLib::hasNextResolver,
        "next" to PyLib::nextResolver,
        "set" to PyLib::setResolver,
        "copy" to PyLib::copyResolver,
        "Set:union" to PyLib::Set_opResolver,
        "Set:intersection" to PyLib::Set_opResolver,
        "Set:difference" to PyLib::Set_opResolver,
        "Set:add" to PyLib::Set_addResolver,
        "Mapping:keys" to PyLib::Mapping_keys,
        "Mapping:values" to PyLib::Mapping_values,
        "Mapping:items" to PyLib::Dict_items,
        "Sequence:index" to PyLib::sequenceIndexResolver,
        "Sequence:count" to PyLib::sequenceIndexResolver,
        "MutableSequence:append" to PyLib::mutableSequenceAppend,
        "MutableSequence:reverse" to PyLib::mutableSequenceReverse,
        "int:bit_length" to PyLib::bitLengthResolver,
        "int:to_bytes" to PyLib::intToBytesResolver,
        "bytes:join" to PyLib::bytesJoinResolver,
        "pow" to PyLib::powResolver,
        "hash" to mkSimpleResolver { NamedType("ssz.Bytes32") },
        "hash_tree_root" to mkSimpleResolver { parseType(TypeResolver.topLevelTyper, "Root") },
    )
    val resolvers2 = listOf(
        "max" to PyLib::maxMinResolver,
        "min" to PyLib::maxMinResolver,
        "sorted" to PyLib::sortedResolver
    )
    val resolvers3 = listOf(
        "map" to PyLib::mapResolver,
    )
    resolvers.forEach {
      TypeResolver.registerFuncResolver(it.first, it.second)
    }
    resolvers2.forEach {
      TypeResolver.registerFuncResolver(it.first, it.second)
    }
    resolvers3.forEach {
      TypeResolver.registerFuncResolver(it.first, it.second)
    }
    funDef("any", listOf(TPySequence(TPyObject)), TPyBool)
    funDef("all", listOf(TPySequence(TPyObject)), TPyBool)

    funDef("uint_to_bytes", listOf("ssz.uint"),"bytes")
    funDef("int.from_bytes", listOf("bytes:bytes","endiannes:str"), "int")
    funDef("ceillog2", listOf("int"), "ssz.uint64")
    funDef("floorlog2", listOf("int"), "ssz.uint64")
  }

  fun powResolver(argTypes: List<RTType>): RTType {
    if (argTypes.size == 2 || argTypes.size == 3) {
      if (argTypes.all { it == TPyInt })
        return TPyInt
      else
        TODO()
    } else {
      fail("not supported")
    }
  }

  fun copyResolver(argTypes: List<RTType>): RTType {
    if (argTypes.size != 1)
      fail("unsupported")
    return argTypes[0]
  }

  fun boolResolver(argTypes: List<RTType>): RTType {
    if (argTypes.size <= 1)
      return TPyBool
    else
      fail("unsupported")
  }

  fun Set_opResolver(argTypes: List<RTType>): RTType {
    return TPySet(getIterableElemType(argTypes[0]))
  }

  fun Set_addResolver(argTypes: List<RTType>): RTType {
    if (argTypes.size < 2) fail()
    if (!isSubType(argTypes[1], getIterableElemType(argTypes[0]))) fail()
    return TPyNone
  }

  fun Mapping_keys(argTypes: List<RTType>): RTType {
    return TPySequence(getMappingTypeParams(argTypes[0]).first)
  }

  fun Mapping_values(argTypes: List<RTType>): RTType {
    return TPySequence(getMappingTypeParams(argTypes[0]).second)
  }

  fun Dict_items(argTypes: List<RTType>): RTType {
    return TPySequence(TPyTuple(getMappingTypeParams(argTypes[0])))
  }

  private fun rangeResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size <= 3) {
      TPySequence(getCommonSuperType(argTypes))
    } else {
      fail("not supported")
    }
  }

  fun iterResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1 && isSubType(argTypes[0], TPyIterable(TPyObject))) {
      return TPyIterator(getIterableElemType(argTypes[0]))
    } else fail("not supported")
  }

  fun hasNextResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1 && isSubType(argTypes[0], TPyIterator(TPyObject))) {
      return TPyBool
    } else fail("not supported")
  }

  fun nextResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1 && isSubType(argTypes[0], TPyIterator(TPyObject))) {
      return getNamedAncestorF(argTypes[0], "Iterator").tParams[0]
    } else fail("not supported")
  }

  private fun sumResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1)
      getIterableElemType(argTypes[0])
    else
      fail("not supported")
  }

  private fun maxMinResolver(argTypes: List<RTType>, kwds: List<Pair<String, RTType>>): RTType {
    val elemType = if (argTypes.size == 1)
      getIterableElemType(argTypes[0])
    else
      getCommonSuperType(argTypes)
    if (kwds.isNotEmpty()) {
      val kwdArgs = kwds.toMap()
      val extraArgs = kwdArgs.keys.subtract(setOf("key", "default"))
      if (extraArgs.isNotEmpty())
        fail("unsupported kwd args ${extraArgs}")
      val lam = kwdArgs["key"]
      if (lam != null) {
        if (lam !is FunType) {
          fail("key should be a function")
        } else {
          if (lam.argTypes.size != 1)
            fail("key func should accept one argument")
          val argType = lam.argTypes[0]
          if (argType is TypeVar)
            throw UnificationException(listOf(argType to elemType))
          else if (!canAssignOrCoerceTo(elemType, argType)) {
            fail("type error")
          }
        }
      }
      val default = kwdArgs["default"]
      if (default != null) {
        if (!canAssignOrCoerceTo(default, elemType))
          fail("type error")
      }
    }
    return elemType
  }

  private fun sortedResolver(argTypes: List<RTType>, kwds: List<Pair<String, RTType>>): RTType {
    return if (argTypes.size == 1) {
      TPyList(getIterableElemType(argTypes[0]))
    } else {
      fail("unsupported")
    }
  }

  private fun setResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1) {
      TPySet(getIterableElemType(argTypes[0]))
    } else if (argTypes.size == 0) {
      // dirty hack
      TPySet(parseType(TypeResolver.topLevelTyper, "ValidatorIndex"))
    } else {
      fail("unsupported")
    }
  }

  private fun listResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1) {
      TPyList(getIterableElemType(argTypes[0]))
    } else {
      fail("unsupported")
    }
  }

  private fun fitlerResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 2 && argTypes[0] is FunType) {
      val funTyp = argTypes[0] as FunType
      val elemTyp = getIterableElemType(argTypes[1])
      if (funTyp.argTypes.size == 1) {
        val fArg0 = funTyp.argTypes[0]
        if (fArg0 is TypeVar) {
          throw UnificationException(listOf(fArg0 to elemTyp))
        } else if (canAssignOrCoerceTo(elemTyp, fArg0)) {
          TPySequence(elemTyp)
        } else {
          fail("type error")
        }
      } else {
        fail("unsupported")
      }
    } else {
      fail("unsupported")
    }
  }

  private fun mapResolver(ctx: NameResolver<Sort>, argTypes: List<RTType>): RTType {
    return if (argTypes.size == 2) {
      val elemTyp = getIterableElemType(argTypes[1])
      TPySequence(argTypes[0].resolveReturnType(ctx, listOf(elemTyp), emptyList()).first.retType)
    } else {
      fail("unsupported")
    }
  }

  private fun enumerateResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1) {
      TPySequence(TPyTuple(TPyInt, getIterableElemType(argTypes[0])))
    } else {
      fail("too many args for enumerate")
    }
  }

  private fun zipResolver(argTypes: List<RTType>) = TPySequence(TPyTuple(argTypes.map(::getIterableElemType)))

  fun sequenceIndexResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 2
            && isSubType(argTypes[1], getSeqElemTyp(argTypes[0]))) {
      TPyInt
    } else {
      fail("not supported")
    }
  }

  fun mutableSequenceAppend(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 2 && isSubType(argTypes[1], getSeqElemTyp(argTypes[0]))) {
      TPyNone
    } else {
      fail("not supported")
    }
  }

  fun mutableSequenceReverse(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1) {
      getSeqElemTyp(argTypes[0])
      TPyNone
    } else {
      fail("not supported")
    }
  }

  fun listIndexResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1) {
      TPyInt
    } else {
      fail("not supported")
    }
  }

  fun bitLengthResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 0)
      TPyInt
    else
      fail("not supported")
  }

  fun intToBytesResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 2 && isSubType(argTypes[0], TPyInt) && argTypes[1] == TPyStr) {
      TPyBytes
    } else {
      fail("not supported")
    }
  }

  fun bytesJoinResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1 && isSubType(getIterableElemType(argTypes[0]), TPyBytes)) {
      TPyBytes
    } else {
      fail("not supported")
    }
  }
}



object Additional {
  fun init(pkg: String) {
    funDef("get_eth1_data", listOf(pkg + ".Eth1Block"), pkg + ".Eth1Data")
    if (pkg in setOf("merge", "sharding")) {
      TypeResolver.registerDelayed("ExecutionEngine") {
        classDef("ExecutionEngine", baseType = TPyObject) {
          meth("new_block")
          meth("assemble_block")
          meth("on_payload")
        }
        funDef("ExecutionEngine:new_block", listOf("merge.ExecutionPayload"), "bool")
        funDef("ExecutionEngine:assemble_block", listOf("phase0.Hash32", "ssz.uint64", "ssz.Bytes32"), "merge.ExecutionPayload")
        funDef("ExecutionEngine:on_payload", listOf("merge.ExecutionPayload"), "bool")
      }
      TypeResolver.registerDelayed("EXECUTION_ENGINE") {
        TypeResolver.registerTopLevelAssign("EXECUTION_ENGINE", NamedType("ExecutionEngine"))
      }


      //funDef("produce_execution_payload", listOf("parent_hash:phase0.Hash32", "timestamp:ssz.uint64"), "merge.ExecutionPayload")
      //funDef("verify_execution_state_transition", listOf("merge.ExecutionPayload"), "bool")
      funDef("get_pow_block", listOf("block_hash:phase0.Hash32"), "merge.PowBlock")
      funDef("get_pow_chain_head", listOf(), "merge.PowBlock")
    }
  }
}