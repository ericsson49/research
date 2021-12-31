package onotole

import dk.brics.automaton.Automaton
import dk.brics.automaton.RegExp
import dk.brics.automaton.State
import dk.brics.automaton.StatePair
import dk.brics.automaton.Transition


fun main() {
  val a = Automaton.makeEmpty()
  val s0 = State()
  s0.isAccept = true
  a.initialState.addTransition(Transition('a', s0))
  val s1 = State()
  s1.isAccept = true
  val s2 = State()
  s2.isAccept = true
  s1.addTransition(Transition('b', s2))
  val s3 = State()
  s3.isAccept = true
  s1.addTransition(Transition('d', s3))
  val s4 = State()
  s4.isAccept = true

  a.addEpsilons(mutableListOf(StatePair(s0, s1), StatePair(s2, s4), StatePair(s3, s4), StatePair(s4, s1)))

  a.minimize()
  println(a.getStrings(5))
  println(a.numberOfStates)
  println(a.numberOfTransitions)
}