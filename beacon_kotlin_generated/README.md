This is a compilable [beacon chain spec phase1](https://github.com/ethereum/eth2.0-specs/tree/dev/specs/phase1) implementation in Kotlin, which is semi-automatically generated using [Onotole](../onotole) transpiler from pyspec.
It's a research prototype and has some limitations, though it's a great starting point for a JVM implementation of phase1 and I found some bugs in phase1 spec.

Current limitations, which are to be addressed in some near future:
- no SSZ, BLS, Merklization and hashing (TODO())
- no caching, so it can be slow
- due to lack of static analysis on Onotole, some manual work is required to solve compilation problem
- base component types are not harmonized with [Teku](https://github.com/PegaSysEng/teku) yet
- rudimentary [phase0](https://github.com/ethereum/eth2.0-specs/tree/dev/specs/phase0) (just to get phase1 compiled)