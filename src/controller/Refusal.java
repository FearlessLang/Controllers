package controller;

import java.util.List;

import userMessages.UserError;

/// A refusal of the reconcile: the error and the refused extensions, with their dot; no extension when more than one identity of the Fearless family is on the system.
@SuppressWarnings("serial") final class Refusal extends RuntimeException{
  final UserError error;
  final List<String> extensions;
  Refusal(UserError error, List<String> extensions){ super(error.getMessage(), error); this.error= error; this.extensions= extensions; }
}
