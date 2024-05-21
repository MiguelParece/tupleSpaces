package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class ServerState {

  private List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<>();
  }

  public void put(String tuple) {
    this.tuples.add(tuple);
    System.out.println("Entrou: " + tuple);

  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public String read(String pattern) {
    System.out.println("Leu: " + pattern);

    return getMatchingTuple(pattern);
  }

  public String take(String tupple) {
    Iterator<String> iterator = this.tuples.iterator();

    while (iterator.hasNext()) {
      String tuple = iterator.next();
      if (tuple.matches(tupple)) {
        iterator.remove();
        System.out.println("Removeu: " + tuple);
        return tuple;
      }
    }
    return null;
  }

  public List<String> getTupleSpacesState() {
    List<String> result = new ArrayList<>();
    for (String tupleState : this.tuples) {
      result.add(tupleState);
    }
    return result;
  }

  public void myprint() {
    System.out.printf("TupleSpace State:\n");
    for (String tupleState : this.tuples)
      System.out.println(tupleState);
  }

}
