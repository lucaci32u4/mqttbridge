# BenQ projectors

## Overview

| Model          | `deviceCodename` field                         | Supported parameters                                                                                       |
|----------------|------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| W2700 / HT3550 | `BenqW2700Serial` or `BenqHT3550Serial` (RS232) | Power, source, mute, volume (read-only), picture mode, blank, freeze, aspect, lamp mode, lamp hours, model name |

The W2700 (sold as HT3550 in some markets) is one device: both codenames build exactly the same entities.
Only the RS232 serial port is supported (no RS232 over LAN or HDBaseT).

The command set comes from [BenQ's RS232 command table](../Benq%20W2700%20HT3550.pdf), pages 8 to 16, rows marked YES.

## Configuration

The `device` section holds the serial port path and the baud rate:

```json
{
  ...
  "device": {
    "port": "/dev/ttyUSB0",
    "baud": 9600
  },
  "deviceCodename": "BenqW2700Serial"
}
```

| Field  | Meaning                                                                                                              |
|--------|----------------------------------------------------------------------------------------------------------------------|
| `port` | Serial port path. A stable path such as `/dev/serial/by-id/usb-FTDI_...-if00-port0` survives reboots and replugging. |
| `baud` | Must match the projector. The W2700 / HT3550 uses 9600.                                                              |

A complete example is in [`deploy/config-benq.json`](../../deploy/config-benq.json).

The user running mqttbridge needs read/write access to the port (on most Linux distributions, membership of the `dialout` group).

## Cable and serial settings

* Projector side: D-Sub 9 pin **male** RS232 port. Computer side: D-Sub 9 pin female (or a USB-to-RS232 adapter).
* Use a **crossover** (null-modem) cable: pin 2 (RXD) and pin 3 (TXD) are swapped between the two ends, pin 5 is ground.
* Projector pinout: 2 RXD, 3 TXD, 5 GND, 7 RTS, 8 CTS, other pins not connected.
* Serial settings: 9600 baud, 8 data bits, no parity, 1 stop bit, no flow control.

## Protocol

* Read: `<CR>*key=?#<CR>`, write: `<CR>*key=value#<CR>`. Keys and values are sent in lowercase as in BenQ's table, except `AUTO` and `REAL` (aspect).
* The projector echoes the command on a line starting with `>`, then answers `*KEY=VALUE#`. Echo lines and empty lines are ignored; a reply is matched to the pending call by key, ignoring case.
* `Block item`, `Illegal format` and `Unsupported item` end the call with an error. The projector answers `Block item` to most commands while it is in standby, so apart from Power most entities keep their last value while the projector is off (a warning is logged).
* A call with no answer within 2000 ms fails with a timeout.
* Values the projector sends on its own (`*KEY=VALUE#` for a known key) update the matching entity.

## Entities

| Entity      | Home Assistant type | MQTT subtopic  | Command key | Values (Home Assistant = projector)                                                                                                                      | Polling      |
|-------------|---------------------|----------------|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| Power       | switch              | `power`        | `pow`       | `ON` = `on`, `OFF` = `off`                                                                                                                                 | normal       |
| Mute        | switch              | `mute`         | `mute`      | `ON` = `on`, `OFF` = `off`                                                                                                                                 | slow         |
| Blank       | switch              | `blank`        | `blank`     | `ON` = `on`, `OFF` = `off`                                                                                                                                 | slow         |
| Freeze      | switch              | `freeze`       | `freeze`    | `ON` = `on`, `OFF` = `off`                                                                                                                                 | slow         |
| Source      | select              | `source`       | `sour`      | HDMI 1 = `hdmi`, HDMI 2 = `hdmi2`, USB Reader = `usbreader`                                                                                                | slow         |
| Picture Mode | select              | `picture-mode` | `appmod`    | Bright = `bright`, Vivid = `vivid`, Cinema = `cine`, D. Cinema = `d.cinema`, User 1 = `user1`, ISF Day = `isfday`, ISF Night = `isfnight`, 3D = `threed` | slow         |
| Aspect      | select              | `aspect`       | `asp`       | Auto = `AUTO`, 4:3 = `4:3`, 16:9 = `16:9`, Real = `REAL`                                                                                                   | slow         |
| Lamp Mode   | select              | `lamp-mode`    | `lampm`     | Normal = `lnor`, Eco = `eco`, SmartEco = `seco`                                                                                                            | slow         |
| Volume      | sensor (read-only)  | `volume`       | `vol`       | integer                                                                                                                                                  | slow         |
| Lamp Hours  | sensor (read-only)  | `lamp-hours`   | `ltim`      | integer, unit `h`                                                                                                                                        | almost never |
| Model Name  | sensor (read-only)  | `model-name`   | `modelname` | text                                                                                                                                                     | almost never |

Volume is read-only because BenQ's W2700 / HT3550 user manual does not state the numeric volume range, so a Home Assistant number slider with correct limits cannot be offered yet.

Not exposed: menu navigation (`menu`, `up`, `down`, `left`, `right`, `enter`) and the `+`/`-` step commands, since mqttbridge syncs state only.
Picture, color calibration and setup controls are planned as a follow-up.
