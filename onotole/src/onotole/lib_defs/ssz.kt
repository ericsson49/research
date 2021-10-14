package onotole.lib_defs

import onotole.*

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
      classDef("Container", "object") {
        ctor()
      }
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
      classDef("Bytes20", "Bytes")
      classDef("Bytes32", "Bytes")
      classDef("Bytes48", "Bytes")
      classDef("Bytes64", "Bytes")
      classDef("Bytes96", "Bytes")

      classDef("GeneralizedIndex", "int")

      val resolvers = listOf(
          "Bytes1" to mkSimpleResolver { NamedType("ssz.Bytes1") },
          "ByteList:get_backing" to SSZLib::treeNodeResolver,
          "ByteVector:get_backing" to SSZLib::treeNodeResolver,
          "merkle_tree.Node:get_left" to SSZLib::treeNodeResolver,
          "merkle_tree.Node:get_right" to SSZLib::treeNodeResolver,
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
