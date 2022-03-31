package onotole.typelib

val pylib = mkLibraryModule("pylib", emptySet(), listOf(
    parseClassDescr("object"),
    parseClassDescr("None <: object"),
    parseClassDescr("int <: object",
        "__add__" to "(int)->int",
        "__radd__" to "(int)->int",
        "__sub__" to "(int)->int",
        "__mult__" to "(int)->int",
        "__floordiv__" to "(int)->int",
        "__mod__" to "(int)->int",
        "__pow__" to "(int)->int",
        "__bitxor__" to "(int)->int",
        "__rshift__" to "(int)->int",
        "__rrshift__" to "(int)->int",
        "__neg__" to "()->int",
    ),
    parseClassDescr("str <: object"),
    parseClassDescr("bytes <: Sequence[int]",
        "__add__" to "(bytes)->bytes",
        "__radd__" to "(bytes)->bytes",
        "__getitem__" to "(int)->int"
    ),
    parseClassDescr("bool <: int"),
    parseClassDescr("Tuple[A,B] <: object"),
    parseClassDescr("Optional[T] <: object"),
    parseClassDescr("Iterable[T] <: object"),
    parseClassDescr("Iterator[T] <: object"),
    parseClassDescr("Sequence[T] <: Iterable[T]",
        "__mult__" to "(int)->Sequence[T]",
        "__getitem__" to "(int)->T",
        "count" to "(T)->int",
        "index" to "(T)->int"
    ),
    parseClassDescr("Set[T] <: Sequence[T]",
        "union" to "(Sequence[T])->Set[T]",
        "intersection" to "(Sequence[T])->Set[T]"
    ),
    parseClassDescr("PyList[T] <: Sequence[T]",
        "__add__" to "(Sequence[T])->Sequence[T]",
        "append" to "(T)->None"
    ),
    parseClassDescr("Dict[K,V] <: object",
        "__getitem__" to "(K)->V",
        "keys" to "()->Set[K]",
        "values" to "()->Sequence[V]"
    ),
    parseClassDescr("Container <: object"),
    parseFuncDecl("zip[A,B](Sequence[A],Sequence[B])->Sequence[Tuple[A,B]]"),
    parseFuncDecl("len[A](Sequence[A])->int"),
    parseFuncDecl("sorted[A](Sequence[A])->Sequence[A]\nsorted[A,B](Sequence[A],key:(A)->B)->Sequence[A]"),
    parseFuncDecl("set[A]()->Set[A]\nset[A](Sequence[A])->Set[A]"),
    parseFuncDecl("iter[A](Iterable[A])->Iterator[A]"),
    parseFuncDecl("has_next[A](Iterator[A])->bool"),
    parseFuncDecl("next[A](Iterator[A])->A"),
    parseFuncDecl("range(int)->Sequence[int]\nrange(int,int)->Sequence[int]"),
    parseFuncDecl("min[A](A,A)->A\nmin[A,B](Sequence[A],key:(A)->B,default:A)->A"),
    parseFuncDecl("max[A](Sequence[A])->A\nmax[A](A,A)->A\nmax[A,B](Sequence[A],key:(A)->B,default:A)->A"),
    parseFuncDecl("enumerate[A](Iterable[A])->Sequence[Tuple[int,A]]"),
    parseFuncDecl("sum(Sequence[int])->int"),
    parseFuncDecl("list[A]()->PyList[A]\nlist[A](Sequence[A])->PyList[A]"),
    parseFuncDecl("map[A,B]((A)->B,Sequence[A])->Sequence[B]"),
    parseFuncDecl("filter[A]((A)->bool,Sequence[A])->Sequence[A]"),
    parseFuncDecl("all(Sequence[int])->bool"),
    parseFuncDecl("copy[A](A)->A"),
    parseFuncDecl("any(Sequence[object])->bool"),
    parseFuncDecl("int#from_bytes(bytes,str)->int")
))

fun ssz_uintN(t: String, base: String) = parseClassDescr(
    "$t <: $base" to mapOf(
        "__add__" to "(int)->$t",
        "__radd__" to "(int)->$t",
        "__sub__" to "(int)->$t",
        "__rsub__" to "(int)->$t",
        "__mult__" to "(int)->$t",
        "__rmult__" to "(int)->$t",
        "__floordiv__" to "(int)->$t",
        "__rfloordiv__" to "(int)->$t",
        "__pow__" to "(int)->$t",
        "__rpow__" to "(int)->$t",
        "__mod__" to "(int)->$t",
        "__rshift__" to "(int)->$t",
        "__bitand__" to "(int)->$t",
        "__bitor__" to "(int)->$t",
        "__bitxor__" to "(int)->$t"
    )
)

val ssz = mkLibraryModule("ssz", listOf("pylib"), listOf(
    ssz_uintN("uint", "int"),
    ssz_uintN("uint8", "uint"),
    ssz_uintN("uint32", "uint"),
    ssz_uintN("uint64", "uint"),
    ssz_uintN("uint256", "uint"),
    parseClassDescr("boolean <: uint"),
    parseClassDescr("Bitlist[n] <: Sequence[int]"),
    parseClassDescr("Bitvector[n] <: Sequence[int]"),
    parseClassDescr("ByteList[n] <: Sequence[int]"),
    parseClassDescr("ByteVector[n] <: Sequence[int]"),
    parseClassDescr("List[T,n] <: Sequence[T]",
        "append" to "(T)->None"
    ),
    parseClassDescr("Vector[T,n] <: Sequence[T]"),
    parseClassDescr("Bytes1 <: bytes"),
    parseClassDescr("Bytes4 <: bytes"),
    parseClassDescr("Bytes8 <: bytes"),
    parseClassDescr("Bytes20 <: bytes"),
    parseClassDescr("Bytes32 <: bytes"),
    parseClassDescr("Bytes48 <: bytes"),
    parseClassDescr("Bytes96 <: bytes"),
    parseClassDescr("Hash32 <: Bytes32"),
    parseClassDescr("GeneralizedIndex <: int"),

    parseFuncDecl("hash(object)->Hash32"),
    parseFuncDecl("hash_tree_root(object)->Hash32"),
    parseFuncDecl("uint_to_bytes(uint)->bytes"),
    parseFuncDecl("ceillog2(int)->uint64"),
    parseFuncDecl("floorlog2(int)->uint64"),
    parseFuncDecl("get_generalized_index(object,object)->GeneralizedIndex\nget_generalized_index(object,object,object)->GeneralizedIndex")
))

val bls = mkLibraryModule("bls", listOf("pylib","ssz"), listOf(
    parseFuncDecl("Sign(int,Bytes32)->Bytes96"),
    parseFuncDecl("FastAggregateVerify(Sequence[Bytes48],Bytes32,Bytes96)->bool"),
    parseFuncDecl("Verify(Bytes48,Bytes32,Bytes96)->bool"),
    parseFuncDecl("Aggregate(Sequence[Bytes96])->Bytes96"),
    parseFuncDecl("KeyValidate(Bytes48)->bool")
))
