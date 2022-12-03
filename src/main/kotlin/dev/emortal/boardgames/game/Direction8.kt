package dev.emortal.boardgames.game

enum class Direction8(val offX: Int, val offZ: Int) {
    // couldn't be arsed to name these
    D(0, -1),
    E(0, 1),
    G(1, 0),
    B(-1, 0),
    A(-1, -1),
    C(-1, 1),
    F(1, -1),
    H(1, 1),
}