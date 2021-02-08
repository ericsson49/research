This is a compilable [beacon chain spec phase1](https://github.com/ethereum/eth2.0-specs/tree/dev/specs/phase1) implementation in Java, which is semi-automatically generated using [Onotole](../onotole) transpiler from pyspec.
It's a research prototype and has many limitations. It's currently intended to be used along with some formal method, i.e. assumes some form of symbolic execution (so, its runtime lib is dummy). It's to be changed in some future.

Current limitations, which are to be addressed in some near future:
- no SSZ, BLS, Merklization and hashing (TODO())
- no caching, so it can be slow
- due to bugs in static analysis of Onotole, some manual work was still required to solve compilation problem
- base component types are not going to match [Teku](https://github.com/PegaSysEng/teku)