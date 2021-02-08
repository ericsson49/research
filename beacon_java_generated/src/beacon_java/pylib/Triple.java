package beacon_java.pylib;

public class Triple<A, B, C> {
  public final A first;
  public final B second;
  public final C third;

  public Triple(A a, B b, C c) {
    this.first = a;
    this.second = b;
    this.third = c;
  }
}
