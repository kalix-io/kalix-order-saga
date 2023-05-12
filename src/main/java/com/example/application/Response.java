package com.example.application;

public sealed interface Response {

  record Success(String message) implements Response {
    public static Success of(String message) {
      return new Success(message);
    }
  }

  record Failure(String message) implements Response {
    public static Failure of(String message) {
      return new Failure(message);
    }
  }
}
