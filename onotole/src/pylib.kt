
data class FunDecl(val name: String, val args: List<FArg>, val retType: RTType)

fun mkSimpleResolver(ret: () -> RTType) = run {
  fun resolver(a: List<RTType>) = ret()
  ::resolver
}

object PyLib {
  fun init() {
    classDef("object")
    classDef("None", "object")
    classDef("int", "object", comparable = true) {
      staticMeth("from_bytes")
      meth("bit_length")
      meth("to_bytes")
    }
    classDef("bool", "int")

    classDef("Optional", "object", nTParms = 1)
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
    }
    TypeResolver.register(MetaClassTInfo("Tuple", null, 0, emptyList()) { tParams ->
      val elemType = if (tParams.isNotEmpty()) getCommonSuperType(tParams)
      else parseType("ValidatorIndex")
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
    classDef("Dict", "Mapping", nTParms = 2, baseParams = listOf(TVIndex(0),TVIndex(1)))
    //classDef("PyDict", "Mapping", nTParms = 2, baseParams = listOf(0,1))

    classDef("bytes", baseType = TPySequence(TPyInt)) {
      meth("join")
    }
    classDef("str", "object", comparable = true)

    val resolvers = listOf(
        "bool" to ::boolResolver,
        "sum" to ::sumResolver,
        "range" to ::rangeResolver,
        "len" to mkSimpleResolver { TPyInt },
        "zip" to ::zipResolver,
        "enumerate" to ::enumerateResolver,
        "map" to ::mapResolver,
        "filter" to ::fitlerResolver,
        "list" to ::listResolver,
        "set" to ::setResolver,
        "copy" to ::copyResolver,
        "Set:union" to ::Set_opResolver,
        "Set:intersection" to ::Set_opResolver,
        "Set:difference" to ::Set_opResolver,
        "Mapping:keys" to ::Mapping_keys,
        "Mapping:values" to ::Mapping_values,
        "Mapping:items" to ::Dict_items,
        "Sequence:index" to ::sequenceIndexResolver,
        "Sequence:count" to ::sequenceIndexResolver,
        "MutableSequence:append" to ::mutableSequenceAppend,
        "MutableSequence:reverse" to ::mutableSequenceReverse,
        "int:bit_length" to ::bitLengthResolver,
        "int:to_bytes" to ::intToBytesResolver,
        "bytes:join" to ::bytesJoinResolver,
        "pow" to ::powResolver,
        "hash" to mkSimpleResolver { NamedType("ssz.Bytes32") },
        "hash_tree_root" to mkSimpleResolver { parseType("Root") },
    )
    val resolvers2 = listOf(
        "max" to ::maxMinResolver,
        "min" to ::maxMinResolver,
        "sorted" to ::sortedResolver
    )
    resolvers.forEach {
      TypeResolver.registerFuncResolver(it.first, it.second)
    }
    resolvers2.forEach {
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

  private fun sumResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 1)
      getIterableElemType(argTypes[0])
    else
      fail("not supported")
  }

  private fun maxMinResolver(argTypes: List<RTType>, kwds: List<Pair<String,RTType>>): RTType {
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

  private fun sortedResolver(argTypes: List<RTType>, kwds: List<Pair<String,RTType>>): RTType {
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
      TPySet(parseType("ValidatorIndex"))
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

  private fun mapResolver(argTypes: List<RTType>): RTType {
    return if (argTypes.size == 2) {
      val elemTyp = getIterableElemType(argTypes[1])
      TPySequence(argTypes[0].resolveReturnType(listOf(elemTyp), emptyList()).first.retType)
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

object SSZLib {
  fun init() {
    packageDef("ssz") {
      classDef("List", "MutableSequence", nTParms = 1, nEParams = 1, baseParams = listOf(TVIndex(0)))
      classDef("Vector", "MutableSequence", nTParms = 1, nEParams = 1, baseParams = listOf(TVIndex(0)))
      classDef("Bitlist", "MutableSequence", nTParms = 0, nEParams = 1, baseParams = listOf(TVConcrete(NamedType("ssz.boolean"))))
      classDef("ByteList", "bytes", nTParms = 0, nEParams = 1)
      classDef("Bitvector", "MutableSequence", nTParms = 0, nEParams = 1, baseParams = listOf(TVConcrete(NamedType("ssz.boolean"))))
      classDef("ByteVector", "bytes", nTParms = 0, nEParams = 1)

      // ???
      classDef("Container", "object")
      // SSZ
      //classDef("Bitlist", "Sequence")
      //classDef("Bitvector", "Sequence")
      classDef("merkle_tree.Node", "bytes") {
        meth("merkle_tree.Node:get_left")
        meth("merkle_tree.Node:get_right")
        meth("merkle_tree.Node:merkle_root")
      }
      classDef("ByteVector", "bytes") {
        meth("get_backing")
      }
      classDef("ByteList", "bytes") {
        meth("get_backing")
      }
      classDef("uint", "int")
      classDef("boolean", "uint") {
        meth("__bool__")
      }
      classDef("bit", "boolean")
      classDef("uint8", "uint")
      classDef("uint32", "uint")
      classDef("uint64", "uint")
      classDef("uint256", "uint")
      classDef("Bytes", "bytes")
      classDef("Bytes1", "Bytes")
      classDef("Bytes4", "Bytes")
      classDef("Bytes32", "Bytes")
      classDef("Bytes48", "Bytes")
      classDef("Bytes64", "Bytes")
      classDef("Bytes96", "Bytes")

      classDef("GeneralizedIndex", "int")

      val resolvers = listOf(
              "Bytes1" to mkSimpleResolver { NamedType("ssz.Bytes1") },
              "ByteList:get_backing" to ::treeNodeResolver,
              "ByteVector:get_backing" to ::treeNodeResolver,
              "merkle_tree.Node:get_left" to ::treeNodeResolver,
              "merkle_tree.Node:get_right" to ::treeNodeResolver,
              "merkle_tree.Node:merkle_root" to mkSimpleResolver { TPyBytes },
              "boolean:__bool__" to mkSimpleResolver { TPyBool },
              "get_generalized_index" to mkSimpleResolver { NamedType("ssz.GeneralizedIndex") },
      )
      resolvers.forEach {
        TypeResolver.registerFuncResolver("ssz." + it.first, it.second)
        registerName(it.first)
      }
    }
  }


  fun treeNodeResolver(args: List<RTType>): RTType {
    return if (args.size == 0) {
      NamedType("ssz.merkle_tree.Node")
    } else {
      fail("arguments are not supported")
    }
  }

  fun hashTreeRootResolver(args: List<RTType>): RTType {
    if (args.size == 1) {
      return NamedType("ssz.Bytes32")
    } else {
      fail("unsupported")
    }
  }
}

object BLS {
  fun init() {
    packageDef("bls") {
      val TSignature = NamedType("ssz.Bytes96")
      val TPubKey = NamedType("ssz.Bytes48")
      val fq2Type = NamedType("FQ2")
      val TG2 = TPySequence(fq2Type)

      val msgType = NamedType("ssz.Bytes32")

      funDef("signature_to_G2", listOf(TSignature), TG2)
      funDef("Sign", listOf(TPyInt, msgType), TSignature)
      funDef("Verify", listOf(TPubKey, msgType, TSignature), TPyBool)
      funDef("FastAggregateVerify", listOf(TPySequence(TPubKey), msgType, TSignature), TPyBool)
      funDef("AggregateVerify", listOf(TPySequence(TPubKey), TPySequence(msgType), TSignature), TPyBool)
      funDef("Aggregate", listOf(TPySequence(TSignature)), TSignature)
      funDef("AggregatePKs", listOf(TPySequence(TPubKey)), TPubKey)

      classDef("FQ1", baseType = TPyObject) {
        attr("coeffs", TPySequence(TPyInt))
      }
      classDef("FQ2", baseType = TPyObject) {
        attr("coeffs", TPySequence(TPyInt))
      }
    }
  }
}

object Additional {
  fun init(pkg: String) {
    funDef("get_eth1_data", listOf(pkg + ".Eth1Block"), pkg + ".Eth1Data")
  }
}