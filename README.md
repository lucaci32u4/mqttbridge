# MqttBridge
![build](https://github.com/lucaci32u4/mqttbridge/actions/workflows/maven.yml/badge.svg)


MqttBridge is an application for connecting various smart devices to MQTT. It offers support base for exposing device's data to mqtt and syncing logic. 

The application is compatible with Home Assistant dsicovery protocol over MQTT.

# Supported devices 
 * Marantz SR6010 over IP (will probabily work with any other model)
 * Pipewire FilterChains (work in progress on `feat-pipewire` branch. Will be able to expose filter's parameters over MQTT)

# Status

 * Marantz support over IP is working properly. Has been running stable for 2 weeks on my SR6010. Some issues with surround mode setting due to bad documentation on marantz's side. Will be fixed in the future. 
 * Marantz support over RS232 is not yet implemented
 * Pipewire support is still being worked on

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