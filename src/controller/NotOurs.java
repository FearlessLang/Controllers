package controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import userMessages.UserError;
import userMessages.Violation;

/// A refusal of the reconcile: the error and the refused extensions, with their dot.
@SuppressWarnings("serial") final class NotOurs extends RuntimeException{
  final UserError error;
  final List<String> extensions;
  NotOurs(Map<String,List<String>> held, Function<String,String> claimedBy){ error= Violation.associationNotOurs(held, claimedBy); extensions= List.copyOf(held.keySet()); }
}
