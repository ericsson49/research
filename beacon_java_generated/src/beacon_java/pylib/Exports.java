package beacon_java.pylib;

import beacon_java.phase0.data.BLSPubkey;
import beacon_java.ssz.Bytes32;
import beacon_java.ssz.uint;
import beacon_java.ssz.uint64;

import java.util.function.Function;

import static beacon_java.util.Exports.TODO;

/**
 * This is a dummy implementation. The spec is assumed to be executed symbolically.
 */
public class Exports {
  public static void pyassert(pybool a) {
    pyassert(a.equals(pybool.create(true)));
  }
  public static void pyassert(boolean a) {
    assert a;
  }

  public static <T extends pyint> T max(Sequence<T> a) { return null; }
  public static <T extends pyint> T max(T a, T b) { return null; }
  public static <E, T> E max(Sequence<E> a, Function<E,T> b) { return null; }
  public static <E, T> E max(Set<E> a, Function<E,T> b) { return null; }
  public static <E, T> E max(Sequence<E> a, Function<E,T> b, E def) { return null; }
  public static <T extends pyint> T min(T a, T b) { return null; }
  public static <T, E> T min(Sequence<T> a, Function<T,E> key) { return null; }
  public static Sequence<pyint> range(pyint b) { return new PyList<>(); }
  public static <T extends pyint> Sequence<T> range(T a, T b) { return new PyList<>(); }
  public static <T extends pyint> Sequence<T> range(T a, T b, T c) { return new PyList<>(); }


  public static <T> PyList<T> list(Iterable<T> a) { return null; }
  public static <T> Set<T> set(Iterable<T> s) { return null; }
  public static <T> PyList<T> sorted(Set<T> s) { return null; }
  public static <T> PyList<T> sorted(Sequence<T> s) { return null; }
  public static <T,E> PyList<T> sorted(Sequence<T> s, Function<T,E> key) { return null; }
  public static <T,E> PyList<T> sorted(Set<T> s, Function<T,E> key) { return null; }
  public static <T> pyint len(Sequence<T> a) { return null; }
  public static <T> pyint len(Set<T> a) { return null; }
  public static <A,B> Iterable<B> map(Function<A,B> f, Iterable<A> m) { return null; }
  public static <A> Iterable<A> filter(Function<A,pyint> f, Iterable<A> m) { return null; }

  public static <E> Sequence<Pair<uint64,E>> enumerate(Sequence<E> s) { return null; }

  public static <T extends pyint> T sum(Sequence<T> s) { return null; }
  public static pyint sum(Set<pyint> s) { return pyint.ZERO; }

  public static <E> pybool any(Sequence<E> s) { return TODO(pybool.class); }
  public static <E> pybool any(Set<E> s) { return TODO(pybool.class); }
  public static <E> pybool all(Sequence<E> s) { return TODO(pybool.class); }
  public static <E> pybool all(Set<E> s) { return TODO(pybool.class); }

  public static <A,B> Sequence<Pair<A,B>> zip(Sequence<A> a, Sequence<B> b) { return null; }
  public static <A,B,C> Sequence<Triple<A,B,C>> zip(Sequence<A> a, Sequence<B> b, Sequence<C> c) { return null; }

  public static pybytes uint_to_bytes(uint v) { return null; }

  public static pybool pybool(pyint v) { return null; }

  public static Bytes32 hash(Object o) { return null; }



  public static pybool eq(Object a, Object b) { return TODO(pybool.class); }
  public static <T> pybool contains(Set<T> s, T o) { return TODO(pybool.class); }
  public static <T> pybool contains(Sequence<T> s, T o) { return TODO(pybool.class); }
  public static <K,V> pybool contains(Dictionary<K,V> s, K o) { return TODO(pybool.class); }
  public static <A> pybool contains(Pair<A,A> s, A o) { return TODO(pybool.class); }

  public static <T extends pyint> T uminus(T a) { return null; }

  public static <T extends pyint> T plus(T a, pyint b) { return null; }
  public static <T extends pyint> T rplus(pyint a, T b) { return null; }
  public static <T extends pyint> T minus(T a, pyint b) { return null; }
  public static <T extends pyint> T multiply(T a, pyint b) { return null; }
  public static <T extends pyint> T divide(T a, pyint b) { return null; }
  public static <T extends pyint> T modulo(T a, pyint b) { return null; }
  public static <T extends pyint> T pow(T a, pyint b) { return null; }
  public static <T extends pyint> T pow(T a, pyint b, pyint c) { return null; }

  public static <T extends pyint> T rightShift(T a, pyint b) { return null; }
  public static <T extends pyint> T leftShift(T a, pyint b) { return null; }

  public static <T extends pyint> T bitAnd(T a, pyint b) { return null; }
  public static <T extends pyint> T bitOr(T a, pyint b) { return null; }
  public static <T extends pyint> T bitXor(T a, pyint b) { return null; }

  public static <T extends pyint> pybool less(T a, T b) { return TODO(pybool.class); }
  public static <T extends pyint> pybool lessOrEqual(T a, T b) { return TODO(pybool.class); }
  public static <T extends pyint> pybool greater(T a, T b) { return TODO(pybool.class); }
  public static <T extends pyint> pybool greaterOrEqual(T a, T b) { return TODO(pybool.class); }

  public static pybool or(pyint... args) { return null; }
  public static pybool and(pyint... args) { return null; }
  public static pybool not(pyint a) { return null; }

  public static pybytes plus(pybytes a, pybytes b) { return null; }
  public static <A extends pybytes, B extends pyint> A multiply(A a, B b) { return null; }
  public static String multiply(String a, pyint b) { return null; }

  public static <A> Sequence<A> plus(Sequence<A> a, Sequence<A> b) { return null; }
  public static <A extends Sequence<?>> A multiply(A a, pyint b) { return null; }

  public static BLSPubkey bls_plus(BLSPubkey a, BLSPubkey b) { return null; }
}
