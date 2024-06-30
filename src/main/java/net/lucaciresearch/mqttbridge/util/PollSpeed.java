package net.lucaciresearch.mqttbridge.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PollSpeed {

    // Intervals are prime numbers to prevent collisions
    FAST(1163), NORMAL(5399), SLOW(19391), VERY_SLOW(115249), ALMOST_NEVER(1014989), NONE(0);

    private final int milliseconds;

}
