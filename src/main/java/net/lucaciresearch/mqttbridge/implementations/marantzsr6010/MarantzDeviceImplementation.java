package net.lucaciresearch.mqttbridge.implementations.marantzsr6010;


import lombok.Getter;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.BooleanHAMqttAdapter;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.EnumHACompatibleMqttAdapter;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.IntervalNumberDoubleSteppingHAMqttAdapter;
import net.lucaciresearch.mqttbridge.implementations.homeassistant.IntervalNumberIntegerSteppingHAMqttAdapter;
import net.lucaciresearch.mqttbridge.implementations.marantz.*;
import net.lucaciresearch.mqttbridge.implementations.util.DuplexConnectionHolder;
import net.lucaciresearch.mqttbridge.mqtt.HAClass;
import net.lucaciresearch.mqttbridge.util.PollSpeed;

import java.util.List;
import java.util.Map;

@Getter
public class MarantzDeviceImplementation implements DevicePropertiesInterface<String, MarantzTelnetConfig> {

    // SR or NR
    private final String series;

    // 4-digit code: 6010, 5011, 5004 etc
    private final String deviceCode;

    private final DuplexConnectionHolder connectionHolder;

    private final MarantzDuplexInterface marantzDuplexInterface;

    public MarantzDeviceImplementation(DuplexConnectionHolder connectionHolder, String deviceCode, String series) throws IllegalArgumentException {
        this.connectionHolder = connectionHolder;
        this.deviceCode = deviceCode;
        this.series = series;

        // for now only SR6010 is supported
        if (!series.equals("SR") || !deviceCode.equals("6010"))
            throw new IllegalArgumentException("Device not supported");

        marantzDuplexInterface = new MarantzDuplexInterface(connectionHolder, List.of(
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new OnOffAdapter(), "MU",
                        new BooleanHAMqttAdapter("Mute", HAClass.SWITCH),
                        "mute",  false, false, null),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new MasterVolumeHalvesMarantzAdapter(), "MV",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Volume", HAClass.NUMBER, +18, -80, 0.5, "dB"),
                        "master-volume", false, false, null),

                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVC",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Center Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "center-volume", true, false, "CV", "CVEND"), // implement CVEND for this
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVFL",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Front-Left Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "front-left-volume", true, false, "CV", "CVEND"),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVFR",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Front-Right Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "front-right-volume", true, false, "CV", "CVEND"),

                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVSL",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Side-Left Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "side-left-volume", true, false, "CV", "CVEND"),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVSR",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Side-Right Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "side-right-volume", true, false, "CV", "CVEND"),

                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVSBL",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Back-Left Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "back-left-volume", true, false, "CV", "CVEND"),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVSBR",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Back-Right Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "back-right-volume", true, false, "CV", "CVEND"),

                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVFHL",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Front-Height-Left Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "front-height-left-volume", true, false, "CV", "CVEND"),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVFHR",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Front-Height-Right Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "front-height-right-volume", true, false, "CV", "CVEND"),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVFWL",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Front-Wide-Left Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "front-wide-left-volume", true, false, "CV", "CVEND"),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVFWR",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Front-Wide-Right Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "front-wide-right-volume", true, false, "CV", "CVEND"),

                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVSW",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Subwoofer 1 Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "subwoofer-1-volume", true, false, "CV", "CVEND"),
                new MarantzVariableNode<>(PollSpeed.VERY_SLOW, new ChannelVolumeHalvesMarantzAdapter(), "CVSW2",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Subwoofer 2 Channel", HAClass.NUMBER, +12, -12, 0.5, "dB"),
                        "subwoofer-2-volume", true, false, "CV", "CVEND"),

                new MarantzVariableNode<>(PollSpeed.ALMOST_NEVER, new EnumStringMarantzAdapter(false), "DIM",
                        new EnumHACompatibleMqttAdapter("Screen Brightness", HAClass.SELECT, Map.of(
                                "BRI",  "Bright",
                                "DIM", "Medium",
                                "DAR", "Dark",
                                "OFF", "Off"
                        )),
                        "screen-brightness", true, true, null),

                new MarantzVariableNode<>(PollSpeed.ALMOST_NEVER, new EnumStringMarantzAdapter(false), "SI",
                        new EnumHACompatibleMqttAdapter("Source", HAClass.SELECT, Map.ofEntries(
                                Map.entry("SAT",  "SAT/CBL"),
                                Map.entry("TV", "TV"),
                                Map.entry("MPLAY", "Media Player"),
                                Map.entry("GAME", "Game"),
                                Map.entry("PHONO", "Phono"),
                                Map.entry("CD", "CD"),
                                Map.entry("DVD", "DVD"),
                                Map.entry("BD", "Blu-ray"),
                                Map.entry("AUX1", "AUX1"),
                                Map.entry("AUX2", "AUX2"),
                                Map.entry("TUNER",  "Tuner"),
                                Map.entry("NET",  "Online Music/Audio"),
                                Map.entry("BT",  "Bluetooth"),
                                Map.entry("USB/IPOD",  "USB/IPOD")
                        )),
                        "source", false, false, null),

                new MarantzVariableNode<>(PollSpeed.ALMOST_NEVER, new EnumStringMarantzAdapter(false), "PSDEH",
                        new EnumHACompatibleMqttAdapter("Dialog Enhancer", HAClass.SELECT, Map.ofEntries(
                                Map.entry("OFF",  "Off"),
                                Map.entry("LOW", "Low"),
                                Map.entry("MED", "Medium"),
                                Map.entry("HIGH", "High")
                        )),
                        "dialog-enhancer", true, true, null),
                new MarantzVariableNode<>(PollSpeed.ALMOST_NEVER, new ChannelVolumeHalvesMarantzAdapter(), "PSDIL",
                        new IntervalNumberDoubleSteppingHAMqttAdapter("Dialog Enhancer Level", HAClass.NUMBER, -12, +12, 0.5, "dB"),
                        "dialog-enhancer-level", true, true, null),

                new MarantzVariableNode<>(PollSpeed.SLOW, new EnumStringMarantzAdapter(false), "PSMULTEQ:",
                        new EnumHACompatibleMqttAdapter("Audissey MultiEQ", HAClass.SELECT, Map.ofEntries(
                                Map.entry("AUDISSEY",  "Reference"),
                                Map.entry("BYP.LR", "Bypass L/R"),
                                Map.entry("FLAT", "Flat"),
                                Map.entry("Off", "OFF")
                        )),
                        "multi-eq", false, true, null),
                new MarantzVariableNode<>(PollSpeed.SLOW, new OnOffAdapter(), "PSDYNEQ",
                        new BooleanHAMqttAdapter("Audissey DynamicEQ", HAClass.SWITCH),
                        "dynamic-eq", true, true, null),
                new MarantzVariableNode<>(PollSpeed.SLOW, new EnumStringMarantzAdapter(false), "PSREFLEV",
                        new EnumHACompatibleMqttAdapter("Audissey DynamicEQ Offset", HAClass.SELECT, Map.ofEntries(
                                Map.entry("0",  "0 dB"),
                                Map.entry("5", "5 dB"),
                                Map.entry("10", "10 dB"),
                                Map.entry("15", "15 dB")
                        )), "dynamic-eq-offset", true, true, null),
                new MarantzVariableNode<>(PollSpeed.SLOW, new EnumStringMarantzAdapter(false), "PSDYNVOL",
                        new EnumHACompatibleMqttAdapter("Audissey Dynamic Volume", HAClass.SELECT, Map.ofEntries(
                                Map.entry("OFF",  "Off"),
                                Map.entry("LIT", "Light"),
                                Map.entry("MED", "Medium"),
                                Map.entry("HEV", "Heavy")
                        )), "dynamic-volume", true, true, null),
                new MarantzVariableNode<>(PollSpeed.SLOW, new OnOffAdapter(), "PSLFC",
                        new BooleanHAMqttAdapter("Audissey LFC", HAClass.SWITCH),
                        "audissey-lfc", true, true, null),
                new MarantzVariableNode<>(PollSpeed.SLOW, new NumberLevelMarantzAdapter(2), "PSCNTAMT",
                        new IntervalNumberIntegerSteppingHAMqttAdapter("Audissey LFC Containment Level", HAClass.NUMBER, 7, 0, 1, null),
                        "lfc-cntamt", true, true, null)

//                new MarantzVariableNode<>(PollSpeed.ALMOST_NEVER, new EnumStringMarantzAdapter(false), "MS",
//                        new EnumHACompatibleMqttAdapter("Surround Mode", HAClass.SELECT, Map.ofEntries(
//                                Map.entry("DIRECT",  "Direct"),
//                                Map.entry("STEREO",  "Stereo"),
//
//                                /*
//                                   The following surround modes does not seem to work on my config
//                                   This might be due to combination of inputs and modes.
//                                   Marantz does not specify in their protocol documentation when these can be enabled
//                                 */
//
////                                Map.entry("DOLBY PRO LOGIC", "Dolby PRO LOGIC"),
////                                Map.entry("DOLBY PL2 C", "Dolby PL2 Movie"),
////                                Map.entry("DOLBY PL2 M", "Dolby PL2 Music"),
////                                Map.entry("DOLBY PL2 G", "Dolby PL2 Game"),
////                                Map.entry("DOLBY PL2X C", "Dolby PL2X Movie"),
////                                Map.entry("DOLBY PL2X M", "Dolby PL2X Music"),
////                                Map.entry("DOLBY PL2X G", "Dolby PL2X Game"),
////                                Map.entry("DOLBY PL2Z H", "Dolby PL2Z H"),
////                                Map.entry("DOLBY SURROUND", "Dolby Surround"),
////                                Map.entry("DOLBY ATMOS", "Dolby Atmos"),
////                                Map.entry("DOLBY DIGITAL", "Dolby Digital"),
////                                Map.entry("DOLBY D EX", "Dolby D EX"),
////                                Map.entry("DOLBY D+PL2X C", "Dolby D+PL2X Movie"),
////                                Map.entry("DOLBY D+PL2X M", "Dolby D+PL2X Music"),
////                                Map.entry("DOLBY D+PL2Z H", "Dolby D+PL2Z H"),
////                                Map.entry("DOLBY D+DS", "Dolby D+DS"),
////                                Map.entry("DOLBY D+NEO:X C", "Dolby D+NEO:X Movie"),
////                                Map.entry("DOLBY D+NEO:X M", "Dolby D+NEO:X Music"),
////                                Map.entry("DOLBY D+NEO:X G", "Dolby D+NEO:X Game"),
////                                Map.entry("DOLBY D+", "Dolby D+"),
////                                Map.entry("DOLBY D+ +EX", "Dolby D+ +EX"),
////                                Map.entry("DOLBY D+ +PL2X C", "Dolby D+ +PL2X Movie"),
////                                Map.entry("DOLBY D+ +PL2X M", "Dolby D+ +PL2X Music"),
////                                Map.entry("DOLBY D+ +PL2Z H", "Dolby D+ +PL2Z H"),
////                                Map.entry("DOLBY D+ +DS", "Dolby D+ +DS"),
////                                Map.entry("DOLBY D+ +NEO:X C", "Dolby D+ +NEO:X Movie"),
////                                Map.entry("DOLBY D+ +NEO:X M", "Dolby D+ +NEO:X Music"),
////                                Map.entry("DOLBY D+ +NEO:X G", "Dolby D+ +NEO:X Game"),
////                                Map.entry("DOLBY HD", "Dolby HD"),
////                                Map.entry("DOLBY HD+EX", "Dolby HD+EX"),
////                                Map.entry("DOLBY HD+PL2X C", "Dolby HD+PL2X Movie"),
////                                Map.entry("DOLBY HD+PL2X M", "Dolby HD+PL2X Music"),
////                                Map.entry("DOLBY HD+PL2Z H", "Dolby HD+PL2Z H"),
////                                Map.entry("DOLBY HD+DS", "Dolby HD+DS"),
////                                Map.entry("DOLBY HD+NEO:X C", "Dolby HD+NEO:X Movie"),
////                                Map.entry("DOLBY HD+NEO:X M", "Dolby HD+NEO:X Music"),
////                                Map.entry("DOLBY HD+NEO:X G", "Dolby HD+NEO:X Game"),
////                                Map.entry("DTS NEO:6 C", "DTS NEO:6 Movie"),
////                                Map.entry("DTS NEO:6 M", "DTS NEO:6 Music"),
////                                Map.entry("DTS NEO:X C", "DTS NEO:X Movie"),
////                                Map.entry("DTS NEO:X M", "DTS NEO:X Music"),
////                                Map.entry("DTS NEO:X G", "DTS NEO:X Game"),
////                                Map.entry("DTS SURROUND", "DTS Surround"),
////                                Map.entry("DTS ES DSCRT6.1", "DTS ES DSCRT6.1"),
////                                Map.entry("DTS ES MTRX6.1", "DTS ES MTRX6.1"),
////                                Map.entry("DTS+PL2X C", "DTS+PL2X Movie"),
////                                Map.entry("DTS+PL2X M", "DTS+PL2X Music"),
////                                Map.entry("DTS+PL2Z H", "DTS+PL2Z H"),
////                                Map.entry("DTS+DS", "DTS+DS"),
////                                Map.entry("DTS+NEO:6", "DTS+NEO:6"),
////                                Map.entry("DTS+NEO:X C", "DTS+NEO:X Movie"),
////                                Map.entry("DTS+NEO:X M", "DTS+NEO:X Music"),
////                                Map.entry("DTS+NEO:X G", "DTS+NEO:X Game"),
////                                Map.entry("DTS96/24", "DTS96/24"),
////                                Map.entry("DTS96 ES MTRX", "DTS96 ES MTRX"),
////                                Map.entry("DTS HD", "DTS HD"),
////                                Map.entry("DTS HD MSTR", "DTS HD MSTR"),
////                                Map.entry("DTS HD+PL2X C", "DTS HD+PL2X Movie"),
////                                Map.entry("DTS HD+PL2X M", "DTS HD+PL2X Music"),
////                                Map.entry("DTS HD+PL2Z H", "DTS HD+PL2Z H"),
////                                Map.entry("DTS HD+DS", "DTS HD+DS"),
////                                Map.entry("DTS HD+NEO:6", "DTS HD+NEO:6"),
////                                Map.entry("DTS HD+NEO:X C", "DTS HD+NEO:X Movie"),
////                                Map.entry("DTS HD+NEO:X M", "DTS HD+NEO:X Music"),
////                                Map.entry("DTS HD+NEO:X G", "DTS HD+NEO:X Game"),
////                                Map.entry("DTS EXPRESS", "DTS EXPRESS"),
////                                Map.entry("DTS ES 8CH DSCRT", "DTS ES 8CH DSCRT"),
////                                Map.entry("MPEG2 AAC", "MPEG2 AAC"),
////                                Map.entry("AAC+DOLBY EX", "AAC+DOLBY EX"),
////                                Map.entry("AAC+PL2X C", "AAC+PL2X Movie"),
////                                Map.entry("AAC+PL2X M", "AAC+PL2X Music"),
////                                Map.entry("AAC+PL2Z H", "AAC+PL2Z H"),
////                                Map.entry("AAC+DS", "AAC+DS"),
////                                Map.entry("AAC+NEO:X C", "AAC+NEO:X Movie"),
////                                Map.entry("AAC+NEO:X M", "AAC+NEO:X Music"),
////                                Map.entry("AAC+NEO:X G", "AAC+NEO:X Game"),
////                                Map.entry("PL DSX", "PL DSX"),
////                                Map.entry("PL2 C DSX", "PL2 Movie DSX"),
////                                Map.entry("PL2 M DSX", "PL2 Music DSX"),
////                                Map.entry("PL2 G DSX", "PL2 Game DSX"),
////                                Map.entry("NEO:6 C DSX", "NEO:6 Movie DSX"),
////                                Map.entry("NEO:6 M DSX", "NEO:6 Music DSX"),
////                                Map.entry("AUDYSSEY DSX", "AUDYSSEY DSX"),
//                                Map.entry("MCH STEREO", "Multichannel Stereo"),
//                                //Map.entry("M CH IN+DOLBY EX", "M CH IN+DOLBY EX"),
//                                //Map.entry("M CH IN+PL2X C", "M CH IN+PL2X Movie"),
//                                //Map.entry("M CH IN+PL2X M", "M CH IN+PL2X Music"),
//                                //Map.entry("M CH IN+PL2Z H", "M CH IN+PL2Z H"),
//                                Map.entry("M CH IN+DS", "M CH IN+DS"),
//                                //Map.entry("M CH IN+NEO:X C", "M CH IN+NEO:X Movie"),
//                                //Map.entry("M CH IN+NEO:X M", "M CH IN+NEO:X Music"),
//                                //Map.entry("M CH IN+NEO:X G", "M CH IN+NEO:X Game"),
//                                //Map.entry("MULTI CH IN 7.1", "MULTI CH IN 7.1"),
//                                Map.entry("MULTI CH IN", "Multichannel")
//                        )),
//                        "surround-mode", false, false, null)
        ));
    }

    @Override
    public List<DeviceCallInterface<String>> getCallInterface() {
        return List.of(marantzDuplexInterface);
    }

    @Override
    public String getManufacturer() {
        return "Marantz";
    }

    @Override
    public String getModel() {
        return series + deviceCode;
    }

}
