package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

class TupleState {
  private int clientId = 0;
  private boolean locked = false;
  private String tuple;

  void lock(int clientId) throws SecurityException {
    if (this.locked == true && this.clientId != clientId)
      throw new SecurityException();
    else {
      this.clientId = clientId;
      this.locked = true;
    }
  }

  void unlock(int clientId) throws SecurityException {
    if (this.locked == true && this.clientId != clientId)
      throw new SecurityException();
    else {
      this.clientId = 0;
      this.locked = false;
    }
  }

  public TupleState(String tuple) {
    this.clientId = 0;
    this.locked = false;
    this.tuple = tuple;
  }

  int getClientId() {
    return this.clientId;
  }

  String getTuple() {
    return this.tuple;
  }

  boolean matches(String pattern) {
    return this.tuple.matches(pattern);
  }

  String myprint() {
    return "{ clientId=" + clientId +
    ", locked=" + locked +
    ", tuple=" + tuple + '}';
  }
}

public class ServerState {

  private List<TupleState> tuples;

  public ServerState() {
    this.tuples = new ArrayList<>();
  }

  public void put(String tuple) {
    TupleState newTupleState = new TupleState(tuple);
    tuples.add(newTupleState);
  }

  private String getMatchingTuple(String pattern) {
    for (TupleState tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple.getTuple();
      }
    }
    return null;
  }

  public String read(String pattern) {
    return getMatchingTuple(pattern);
  }

  public List<String> lock(String pattern, int clientId) {
    List<String> result = new ArrayList<>();

    Map<String, Boolean> uniqueStrings = new HashMap<>();

    for (TupleState tupleState : this.tuples) {

      if (tupleState.matches(pattern) && !uniqueStrings.containsKey(tupleState.getTuple())) {
        try {

          tupleState.lock(clientId);
          result.add(tupleState.getTuple());
          uniqueStrings.put(tupleState.getTuple(), true);
        } catch (SecurityException e) {
        }
      }
    }
    return result;
  }

  public void release(int clientId) {
    for (TupleState tupleState : this.tuples) {
      try {
        tupleState.unlock(clientId);
      } catch (SecurityException e) {
      }
    }
  }

  public void release(int clientId, String tuple) {
    for (TupleState tupleState : this.tuples) {
      if (tupleState.getTuple().equals(tuple)) {
        try {
          tupleState.unlock(clientId);
          // only one instance of the pattern should be unlocked
          break;
        } catch (SecurityException e) {
        }
      }
    }
  }

  public String take(String tupple, int clientId) {
    Iterator<TupleState> iterator = this.tuples.iterator();

    while (iterator.hasNext()) {
      TupleState tupleState = iterator.next();
      if (tupleState.matches(tupple) && tupleState.getClientId() == clientId) {
        iterator.remove();
        return tupleState.getTuple();
      }
    }
    return null;
  }

  public List<String> getTupleSpacesState() {
    List<String> result = new ArrayList<>();
    for (TupleState tupleState : this.tuples) {
      result.add(tupleState.getTuple());
    }
    return result;
  }

  public void myprint() {
    System.out.printf("TupleSpace State:\n");
    for (TupleState tupleState : this.tuples)
      System.out.println(tupleState.myprint());
  }

}
