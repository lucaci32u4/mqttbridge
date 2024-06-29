## Overview

| Model  | `deviceCodename` field                                                      | Supported parameters                                                                |
|--------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| SR6010 | `SR6010Telnet` using telnet<br/> `SR6010Serial` using RS232 line (upcoming) | Mute, master volume, per-channel volume, screen brightness, source, dialog enhancer |
| SR5004 | `SR5004Serial` using RS232 (upcoming)                                       | Mute, master volume, screen brightness, source                                      |


## Configuration

### Telnet

The configuration contains the hostname (or IP) of the Marantz AVR:

```json
{
  ...
  "device": {
    "host": "192.168.1.45"
  },
  "deviceCodename": "SR6010Telnet"
}
```

### Serial port

The configuration contains the serial port path and baud rate:

Note: not yet implemented

```json
{
  ...
  "device": {
    "port": "/dev/serial/by-id/usb-FTDI_FT232R_USB_UART_A10MMGWJ-if00-port0",
    "baud": 9600
  },
  "deviceCodename": "SR6010Serial"
}
```

