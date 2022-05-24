# Formal Engineering in Ethereum R&D context

Formal verification of BFT protocols is notoriously difficult. However, it's critical in the Eth PoS context:
- huge incentives for attackers
- open source, so attackers can analyze code
- multiple ways how the system can fail: the specification, implementations, network layer, resource exhaustion attacks, etc
- bugs can be difficult to fix, after code is deployed

There are additional complications, specific to Ethereum development process, which is largely driven by engineers and oriented on a wide audience. In particular,
- the pyspecs are written using Python as a "specification" language, which lacks formal semantics and formal method tools
- the current pyspecs are rather a reference implementation, rather than an abstract specification, which makes it more difficult for formal analysis
- the modular structure of the specs is not suited for formal work, significantly increasing formal development efforts
- the pyspecs is evolving, thus synchronizing changes becomes a challenging task
- there are a number of protocol implementations, it's hardly possible that they will be formally verified

While this additionally complicates an already (very) challenging task, we accept that there are strong reasons for EF to do so, therefore the main goal of Formal Engineering in the context of Ethereum is to (help) overcome these challenges.

# Opportunities

First, EF recognizes the importance of application of F/M to Eth PoS protocol and is open to suggestions.

Second, specs are developed using a statically typed subset of Python. It can be tweaked, if necessary, within reasonable bounds. Formalizing the semantics of the subset is an achievable goal, though maybe involved (in particular, extensive testing of the semantics is necessary).

Third, there are patterns in the specs, which can greatly simplify formal reasoning - though it still expected to be very challenging:
- persistent data structures are dominant, while there are exceptions, the code can be transformed to a non-destructive from relatively easily with low overhead (e.g. loss of readability)
- most loops are parallel or close to such
- no floating point arithmetics, limited OO features, multi-threading

**NB** These patterns are more or less common to (specifications of) distributed protocols in general.

Fourth, implementations can be generated from an executable specification (reference implementation). It may be necessary to provide optimized versions of critical parts.

Fifth, lighter-weight formal methods like bounded model checking and/or concolic testing can be applied to facilitate specs and implementation development at early/mid stages.

Sixth, a "shadow" spec can be developed, which can be better suited for formal analysis. It's thus required to "synchronize" the official and the "shadow" specs.

# Goals

- equip the Python subset with formal/executable semantics
- make pyspecs the real specs
    - an abstract specification of a distibuted protocol
- make pyspecs more suitable for formal work
    - modularize according to formal method requirements
    - annotations (pre-/post-conditions, invariants)
- reduce formal development efforts
    - employ domain specifics
    - detect "easier" fragments, "free" theorems
    - infer properties via static analysis
    - etc

# Core principles

- the pyspecs are the first class object
    - it serves as the primary venue between specs developers and specs implementers, so changes in the pyspecs should be reflected in corresponding formal developments
- meta-programming
    - develop a transpiler from the Python subset to representations, which are more suitable for formal work
- custom semantics
    - employ domain specifics to develop customized semantics which reduce overall formal development efforts
- start with lighter-weight formal methods
    - type checking, bounded model checking

# Strategy

The core of the strategy is the "executable" semantics of the subset of the Python. "Executable" means here that the semantics are defined via a program which reduces Python bits to relatively simple intermediate representation, which possesses necessary properties:
- the representation can be equipped with a formal semantics (e.g. transformed to a typed lambda calculus, CEK machine, etc)
- static time ambiguities arising due to dynamic nature of Python are resolved:
    - variable declaration inference
    - name resolution order
    - type inference
    - implicit coercions
    - etc

After the pyspecs are transformed to this intermediate form, they can be further processed depending on goals.

Currently, the following processing steps are recognized:
- transformation of destructive updates to non-destructive ones - "pure" form
    - greatly simplifies formal reasoning, since aliasing becomes explicit
- transformation of loops to recursion
- transformation of exceptions to explicit outcome propagation
- translation to general programming languages: Kotlin, Javam, Rust, C, etc
- translation to input languages of formal method tools: Dafny, Coq, model checking tools, etc
- test generation, either direct or indirect
- code slicing
    - to obtain simplified versions of specs
    - to generate tests
    - to reduce costs of formal method application
    - to assist in developing a more abstract specification

# Scenarios

- fast generation of "up-to-date" research implementations
    - considered as not interesting right now
- generation of production-ready (partial) implementations
    - potentially interesting in future, e.g. as a way to mitigate resource exhaustion attacks
    - can be produced from an optimized spec version
- full formalization of the Python subset semantics
    - important in a long term, since any formal statement about protocol properties requires an appropriate formal foundation
- producing simplified versions of specs (slicing), to facilitate manual as well as mechanized analysis of the specs
- bridging "shadow" specs (e.g. expressed in Dafny) with the official pyspecs
    - e.g. support DDS team in their work on formal analysis of the Eth pyspecs
- model checking and test generation to find flaws in the pyspecs
- test generation to ensure conformance of implementations to the pyspecs

# Current priorities

- executable part of the semantics (resolve ambiguities due to dynamic nature of Python)
- generate Dafny implementation
    - help DDS team
    - prove auxiliary theorems, which can guide smart fuzzing (e.g. prune search space)
    - extract partial specifications (e.g. formulas, defining accepted vs rejected messages)
- Generate tests
    - via model checking tools (CPAChecker, CProver, CBMC/JBMC, etc)
    - via concolic testing
    - via SMT/SAT uniform-like sampling
