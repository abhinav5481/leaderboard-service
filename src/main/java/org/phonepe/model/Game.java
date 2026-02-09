package org.phonepe.model;

import lombok.Getter;

@Getter
public class Game {
    private final String id;
    private final String name;

    public Game(String id, String name) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("game id required");
        this.id = id;
        this.name = (name != null && !name.isBlank()) ? name : "";
    }
}
