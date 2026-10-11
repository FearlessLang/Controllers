package controller;

@SuppressWarnings("serial")
public final class Refused extends RuntimeException{
  public Refused(String message){ super(message); }
}
