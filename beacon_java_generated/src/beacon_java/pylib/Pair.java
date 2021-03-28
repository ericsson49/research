package beacon_java.pylib;

public class Pair<A, B> {
  public final A first;
  public final B second;

  public Pair(A a, B b) {
    this.first = a;
    this.second = b;
  }

  public static <T> Sequence<T> of(T a, T b) { return PyList.of(a,b); }
}
