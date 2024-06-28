# MqttBridge
![build](https://github.com/lucaci32u4/mqttbridge/actions/workflows/maven.yml/badge.svg)


MqttBridge is an application for connecting various smart devices to MQTT. It offers support base for exposing device's data to mqtt and syncing logic. 

The application is compatible with Home Assistant discovery protocol over MQTT.

# Supported devices 
 * Marantz SR6010 over IP (will probabily work with any other model)
 * Pipewire FilterChains (work in progress on `feat-pipewire` branch. Will be able to expose filter's parameters over MQTT)

# Status

 * Marantz support over IP is working properly. Has been running stable for 2 weeks on my SR6010. Some issues with surround mode setting due to bad documentation on marantz's side. Will be fixed in the future. 
 * Marantz support over RS232 is not yet implemented
 * Pipewire support is still being worked on

# Configuration and deployment

The config file is a json document with the following structure:

```json
{
  "mqtt": {
    "username": "yourMqttUsername",
    "password": "yourMqttPassword",
    "port": 1883,
    "qos": 2,
    "host": "ip-of-mqtt-broker",
    "publisherId": "SomeBridgeApplication",
    "baseTopic": "base/topic"
  },
  "discovery": {
    "entityName": "Home assistant device name",
    "discoveryBaseTopic": "homeassistant"
  },
  "device": {
    # device specific options
  },
  "deviceCodename": "insert-device-type"
}
```

For the `deviceCodename` field and device specific options, see the following documentation pages:
 * [Marantz](protocols/support/marantz.md)
 * [Epson Projector](protocols/support/epson-projector.md)
 * [Benq Projector](protocols/support/benq-projector.md)
 * [Pipewire Filter Chain](protocols/support/pipewire-filter-chain.md)


See the deployment document [here](deploy/readme.md)

## TODO:

* Allow user to choose between json and yaml configuration
* Surround mode parameters on Marantz AVR's
* Diagram of adapters
* How to define new devices
* Refactor modern Marantz AVR Support
* Add Marantz RS232 old protocol support
* Add Benq and Epson projector support over serial line
* Github pages docs


## License

This project is licensed under Affero GPL version 3