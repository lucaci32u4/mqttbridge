package net.lucaciresearch.mqttbridge.implementations.benq;

import net.lucaciresearch.mqttbridge.exceptions.InputIgnoredException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BenqAdaptersTest {

    // --- on/off ---

    @Test
    public void onOffParsesAnyCase() throws Exception {
        BenqOnOffAdapter a = new BenqOnOffAdapter();
        assertEquals(Boolean.TRUE, a.parseDevice("ON"));
        assertEquals(Boolean.TRUE, a.parseDevice("on"));
        assertEquals(Boolean.FALSE, a.parseDevice("OFF"));
        assertEquals(Boolean.FALSE, a.parseDevice(" Off "));
    }

    @Test
    public void onOffIgnoresUnknownValue() {
        assertThrows(InputIgnoredException.class, () -> new BenqOnOffAdapter().parseDevice("WARMING"));
    }

    @Test
    public void onOffFormatsLowercase() {
        BenqOnOffAdapter a = new BenqOnOffAdapter();
        assertEquals("on", a.toDevice(true));
        assertEquals("off", a.toDevice(false));
    }

    // --- enum ---

    private final BenqEnumAdapter aspect = new BenqEnumAdapter(List.of("AUTO", "4:3", "16:9", "REAL"));
    private final BenqEnumAdapter picture = new BenqEnumAdapter(List.of("bright", "cine", "d.cinema", "user1"));

    @Test
    public void enumParsesCaseInsensitivelyToOptionKey() throws Exception {
        assertEquals("cine", picture.parseDevice("CINE"));
        assertEquals("d.cinema", picture.parseDevice("D.Cinema"));
        assertEquals("AUTO", aspect.parseDevice("auto"));
        assertEquals("16:9", aspect.parseDevice("16:9"));
        assertEquals("REAL", aspect.parseDevice("Real"));
    }

    @Test
    public void enumIgnoresUnknownValue() {
        assertThrows(InputIgnoredException.class, () -> aspect.parseDevice("LBOX"));
    }

    @Test
    public void enumFormatsOptionKeyAsWritten() {
        assertEquals("cine", picture.toDevice("cine"));
        assertEquals("AUTO", aspect.toDevice("AUTO"));
        assertEquals("REAL", aspect.toDevice("REAL"));
    }

    // --- integer ---

    @Test
    public void integerParses() throws Exception {
        BenqIntegerAdapter a = new BenqIntegerAdapter();
        assertEquals(Integer.valueOf(1234), a.parseDevice("1234"));
        assertEquals(Integer.valueOf(0), a.parseDevice(" 0 "));
    }

    @Test
    public void integerIgnoresNonNumber() {
        assertThrows(InputIgnoredException.class, () -> new BenqIntegerAdapter().parseDevice("abc"));
    }

    @Test
    public void integerFormats() {
        assertEquals("7", new BenqIntegerAdapter().toDevice(7));
    }

    // --- string ---

    @Test
    public void stringParsesTrimmed() throws Exception {
        assertEquals("W2700", new BenqStringAdapter().parseDevice(" W2700 "));
    }

    @Test
    public void stringIgnoresEmpty() {
        assertThrows(InputIgnoredException.class, () -> new BenqStringAdapter().parseDevice(""));
    }

    @Test
    public void stringFormatsUnchanged() {
        assertEquals("HT3550", new BenqStringAdapter().toDevice("HT3550"));
    }
}
