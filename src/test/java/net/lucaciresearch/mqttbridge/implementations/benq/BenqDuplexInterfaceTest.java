package net.lucaciresearch.mqttbridge.implementations.benq;

import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface.KeyValue;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.*;

public class BenqDuplexInterfaceTest {

    private FakeDuplexConnectionHolder fake;
    private BenqDuplexInterface iface;
    private List<VariableNode<?, String>> nodes;

    @Before
    public void setUp() throws Exception {
        fake = new FakeDuplexConnectionHolder();
        nodes = BenqW2700DeviceImplementation.buildNodes();
        iface = new BenqDuplexInterface(fake, nodes);
        iface.initializeConnection();
        long deadline = System.currentTimeMillis() + 2000;
        while (!iface.isOpen() && System.currentTimeMillis() < deadline)
            Thread.sleep(10);
        assertTrue("fake connection did not open", iface.isOpen());
    }

    @After
    public void tearDown() {
        iface.closeConnection();
    }

    @Test
    public void readPowerWritesExactCommand() throws Exception {
        fake.reply("\r*pow=?#\r", "*POW=ON#\r");
        assertEquals("ON", iface.readValue("pow", false));
        assertEquals(List.of("\r*pow=?#\r"), fake.written());
    }

    @Test
    public void writePowerOnWritesExactCommand() throws Exception {
        fake.reply("\r*pow=on#\r", "*POW=ON#\r");
        assertEquals("ON", iface.writeValue("pow", "on", false));
        assertEquals(List.of("\r*pow=on#\r"), fake.written());
    }

    @Test
    public void powerOnReplyParsesToOn() throws Exception {
        fake.reply("\r*pow=?#\r", "*POW=ON#\r");
        String reply = iface.readValue("pow", false);
        VariableNode<?, String> power = nodes.stream().filter(n -> n.deviceKey().equals("pow")).findFirst().orElseThrow();
        power.parseDevice(reply);
        assertEquals(Boolean.TRUE, power.value());
    }

    @Test
    public void echoAndEmptyLinesAreIgnored() throws Exception {
        fake.reply("\r*pow=?#\r", "\r\n>*pow=?#\r\n\r\n\n>\r*POW=OFF#\r\n");
        assertEquals("OFF", iface.readValue("pow", false));
    }

    @Test
    public void echoOfErrorTextIsIgnored() throws Exception {
        fake.reply("\r*pow=?#\r", ">*Block item#\r*POW=ON#\r");
        assertEquals("ON", iface.readValue("pow", false));
    }

    @Test
    public void replyForOtherKeyDoesNotAnswer() throws Exception {
        fake.reply("\r*pow=?#\r", "*MUTE=ON#\r*POW=ON#\r");
        assertEquals("ON", iface.readValue("pow", false));
    }

    @Test
    public void lfOnlyLineEndingsAreAccepted() throws Exception {
        fake.reply("\r*vol=?#\r", ">*vol=?#\n*VOL=5#\n");
        assertEquals("5", iface.readValue("vol", false));
    }

    @Test
    public void blockItemRaisesCallFail() {
        assertCallFails("*Block item#\r", "Block item");
    }

    @Test
    public void illegalFormatRaisesCallFail() {
        assertCallFails("*Illegal format#\r", "Illegal format");
    }

    @Test
    public void unsupportedItemRaisesCallFail() {
        assertCallFails("*Unsupported item#\r", "Unsupported item");
    }

    @Test
    public void errorWithoutMarkersAndOtherCaseRaisesCallFail() {
        assertCallFails("BLOCK ITEM\r", "BLOCK ITEM");
    }

    private void assertCallFails(String reply, String expectedMessage) {
        fake.reply("\r*pow=?#\r", reply);
        long start = System.currentTimeMillis();
        CallFailException e = assertThrows(CallFailException.class, () -> iface.readValue("pow", false));
        assertEquals(expectedMessage, e.getMessage());
        assertTrue("error must end the call before the timeout", System.currentTimeMillis() - start < BenqDuplexInterface.TIMEOUT_MS);
    }

    @Test
    public void noReplyWithin2000msRaisesCallFail() {
        long start = System.currentTimeMillis();
        CallFailException e = assertThrows(CallFailException.class, () -> iface.readValue("pow", false));
        long elapsed = System.currentTimeMillis() - start;
        assertEquals("Timeout", e.getMessage());
        assertTrue("waited only " + elapsed + " ms", elapsed >= 1900);
        assertTrue("waited " + elapsed + " ms", elapsed < 4000);
    }

    @Test
    public void callsAfterTimeoutStillWork() throws Exception {
        assertThrows(CallFailException.class, () -> iface.readValue("mute", false));
        fake.reply("\r*pow=?#\r", "*POW=ON#\r");
        assertEquals("ON", iface.readValue("pow", false));
    }

    @Test
    public void unsolicitedValueGoesToNotifyValue() throws Exception {
        List<KeyValue<String>> seen = new CopyOnWriteArrayList<>();
        iface.notifyValue().subscribe(seen::add);
        fake.sendFromDevice("*MUTE=ON#\r");
        fake.sendFromDevice("*UNKNOWNKEY=1#\r");
        fake.sendFromDevice(">*blank=?#\r");
        long deadline = System.currentTimeMillis() + 2000;
        while (seen.isEmpty() && System.currentTimeMillis() < deadline)
            Thread.sleep(10);
        Thread.sleep(100);
        assertEquals(List.of(new KeyValue<>("mute", "ON")), seen);
    }

    @Test
    public void parseReplyStripsMarkers() {
        assertEquals(new KeyValue<>("POW", "ON"), BenqDuplexInterface.parseReply("*POW=ON#"));
        assertEquals(new KeyValue<>("APPMOD", "D.CINEMA"), BenqDuplexInterface.parseReply("*APPMOD=D.CINEMA#"));
        assertEquals(new KeyValue<>("ASP", "16:9"), BenqDuplexInterface.parseReply("*ASP=16:9#"));
        assertNull(BenqDuplexInterface.parseReply("POW=ON"));
        assertNull(BenqDuplexInterface.parseReply("*Block item#"));
    }

    @Test
    public void parseErrorRecognisesErrorReplies() {
        assertEquals("Block item", BenqDuplexInterface.parseError("*Block item#"));
        assertEquals("illegal format", BenqDuplexInterface.parseError("illegal format"));
        assertEquals("Unsupported item", BenqDuplexInterface.parseError("*Unsupported item"));
        assertNull(BenqDuplexInterface.parseError("*POW=ON#"));
    }
}
