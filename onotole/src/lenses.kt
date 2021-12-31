import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class Lens<S,V>(val get: (S)->V, val set: (S, V)->S)

class ILens<S,I,V>(val get: (S,I)->V, val set: (S,I,V)->S) {
  fun at(i: I): Lens<S,V> = Lens({ s -> get(s, i) }, { s,v -> set(s,i, v)})
}

class AA() {
  val f: String = ""
  val l: List<String> = mutableListOf()
}

