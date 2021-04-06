
data class FunDecl(val name: String, val args: List<FArg>, val retType: RTType)

fun mkSimpleResolver(ret: RTType) = run {
  fun resolver(a: List<RTType>) = ret
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
    classDef("Collection", "Iterable", nTParms = 1, baseParams = listOf(0)) {
      meth("__iter__")
      meth("__len__")
    }
    classDef("Mapping", "Collection", nTParms = 2, baseParams = listOf(0)) {
      meth("__getitem__")
      meth("keys")
      meth("values")
      meth("items")
    }
    classDef("Sequence", "Collection", nTParms = 1, baseParams = listOf(0)) {
      meth("__getitem__")
      meth("__reversed__")
      meth("index")
      meth("count")
    }
    classDef("Set", "Collection", nTParms = 1, baseParams = listOf(0)) {
      meth("union")
      meth("intersection")
      meth("difference")
    }
    TypeResolver.register(MetaClassTInfo("Tuple", null, 0, emptyList()) { tParams ->
      val elemType = if (tParams.isNotEmpty()) getCommonSuperType(tParams)
      else NamedType("ValidatorIndex")
      TPySequence(elemType)
    })
    classDef("MutableSequence", "Sequence", nTParms = 1, baseParams = listOf(0)) {
      meth("__setitem__")
      meth("__delitem__")
      meth("__iadd__")
      meth("append")
      meth("reverse")
      meth("extend")
      meth("pop")
      meth("remove")
    }
    classDef("PyList", "MutableSequence", nTParms = 1, baseParams = listOf(0))
    classDef("Dict", "Mapping", nTParms = 2, baseParams = listOf(0,1))
    //classDef("PyDict", "Mapping", nTParms = 2, baseParams = listOf(0,1))

    classDef("bytes", baseType = TPySequence(TPyInt)) {
      meth("join")
    }
    classDef("str", "object", comparable = true)

    val resolvers = listOf(
        "bool" to ::boolResolver,
        "sum" to ::sumResolver,
        "range" to ::rangeResolver,
        "len" to mkSimpleResolver(TPyInt),
        "zip" to ::zipResolver,
        "enumerate" to ::enumerateResolver,
        "map" to ::mapResolver,
        "filter" to ::fitlerResolver,
        "list" to ::listResolver,
        "set" to ::setResolver,
        "copy" to ::copyResolver,
        "hash" to mkSimpleResolver(NamedType("Bytes32")),
        "hash_tree_root" to mkSimpleResolver(NamedType("Root")),
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
    val funDefs = listOf(
            funDef("any", listOf(TPySequence(TPyObject)), TPyBool),
            funDef("all", listOf(TPySequence(TPyObject)), TPyBool),

            funDef("uint_to_bytes", listOf("uint"),"bytes"),
            funDef("get_eth1_data", listOf("Eth1Block"), "Eth1Data"),
            funDef("int.from_bytes", listOf("bytes:bytes","endiannes:str"), "int"),
            funDef("ceillog2", listOf("int"), "uint64"),
            funDef("floorlog2", listOf("int"), "uint64"),
    )
    funDefs.forEach(TypeResolver::registerFunc)
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
      TPySet(NamedType("ValidatorIndex"))
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
    classDef("List", "MutableSequence", nTParms = 1, nEParams = 1, baseParams = listOf(0))
    classDef("Vector", "MutableSequence", nTParms = 1, nEParams = 1, baseParams = listOf(0))
    TypeResolver.register(MetaClassTInfo("Bitlist", 0, 1) { TPySequence(NamedType("boolean")) })
    TypeResolver.register(MetaClassTInfo("ByteList", 0, 1) { TPyBytes })
    TypeResolver.register(MetaClassTInfo("Bitvector", 0, 1) { TPySequence(NamedType("boolean")) })
    TypeResolver.register(MetaClassTInfo("ByteVector", 0, 1) { TPyBytes })

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
        "Bytes1" to mkSimpleResolver(NamedType("Bytes1")),
        "ByteList:get_backing" to ::treeNodeResolver,
        "ByteVector:get_backing" to ::treeNodeResolver,
        "merkle_tree.Node:get_left" to ::treeNodeResolver,
        "merkle_tree.Node:get_right" to ::treeNodeResolver,
        "merkle_tree.Node:merkle_root" to mkSimpleResolver(TPyBytes),
        "boolean:__bool__" to mkSimpleResolver(TPyBool),
        "get_generalized_index" to mkSimpleResolver(NamedType("GeneralizedIndex"))
        //, "hash_tree_root" to ::hashTreeRootResolver
    )
    resolvers.forEach {
      TypeResolver.registerFuncResolver(it.first, it.second)
    }
  }


  fun treeNodeResolver(args: List<RTType>): RTType {
    return if (args.size == 0) {
      NamedType("merkle_tree.Node")
    } else {
      fail("arguments are not supported")
    }
  }

  fun hashTreeRootResolver(args: List<RTType>): RTType {
    if (args.size == 1) {
      return NamedType("Bytes32")
    } else {
      fail("unsupported")
    }
  }
}

object BLS {
  fun init() {
    val TSignature = NamedType("BLSSignature")
    val TPubKey = NamedType("BLSPubkey")
    val fq2Type = NamedType("FQ2")
    val TG2 = TPySequence(fq2Type)

    val msgType = NamedType("Bytes32")

    val props: Map<String, Pair<List<Pair<String, RTType>>, RTType>> = mapOf(
        "signature_to_G2" to Pair(listOf("signature" to TSignature), TG2),
        "Sign" to Pair(listOf("SK" to TPyInt, "message" to msgType), TSignature),
        "Verify" to Pair(listOf("PK" to TPubKey, "message" to msgType, "signature" to TSignature), TPyBool),
        "FastAggregateVerify" to Pair(listOf("pubkeys" to TPySequence(TPubKey), "message" to msgType, "signature" to TSignature), TPyBool),
        "AggregateVerify" to Pair(listOf("pubkeys" to TPySequence(TPubKey), "messages" to TPySequence(msgType), "signature" to TSignature), TPyBool),
        "Aggregate" to Pair(listOf("signatures" to TPySequence(TSignature)), TSignature),
        "AggregatePKs" to Pair(listOf("keys" to TPySequence(TPubKey)), TPubKey)
    )
    TypeResolver.registerPackage("bls", props.map { it.key to NamedFuncRef("bls:${it.key}") }.toMap())
    props.forEach { k, (args, retType) ->
      val funDecl = FunDecl("bls:$k", args.map { t -> FArg(t.first, t.second) }, retType)
      TypeResolver.registerFunc(funDecl)
    }

    TypeResolver.register(PackageTI("bls",
            props.map { it.key to FuncRefTI("bls:${it.key}").type }))

    val fq1attrs = mapOf("coeffs" to TPySequence(TPyInt))
    val fq2attrs = mapOf("coeffs" to TPySequence(TPyInt))
    TypeResolver.register(DataTInfo("FQ1", baseType = TPyObject, attrs = fq1attrs.toList()))
    TypeResolver.register(DataTInfo("FQ2", baseType = TPyObject, attrs = fq2attrs.toList()))
  }
}