package com.example.suburbanbot;

@lombok.Value
class TrainThread {
    Station fromStation;
    Station toStation;

    @Override
    public String toString() {
        return fromStation.getName() + " -> " + toStation.getName();
    }
}
