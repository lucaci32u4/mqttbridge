# MqttBridge
[![License](https://img.shields.io/badge/License-AGPL-blue)](#license)
[![Issues](https://img.shields.io/github/issues/lucaci32u4/mqttbridge)](https://github.com/lucaci32u4/mqttbridge/issues)
[![GitHub tag](https://img.shields.io/github/tag/lucaci32u4/mqttbridge?include_prereleases=&sort=semver)](https://github.com/lucaci32u4/mqttbridge/releases/)
![build](https://github.com/lucaci32u4/mqttbridge/actions/workflows/maven.yml/badge.svg)



MqttBridge is an application for connecting various smart devices to MQTT. It offers support base for exposing device's data to mqtt and syncing logic. 

The application is compatible with Home Assistant discovery protocol over MQTT.

# Supported devices 
 * Marantz SR6010 over IP (will probabily work with any other model)
 * BenQ W2700 / HT3550 projector over RS232 ([details](protocols/support/benq-projector.md))
 * Pipewire FilterChains

For adding support for other devices, see this document [here](protocols/support/supporting-new-devices.md)

# Status

 * Marantz support over IP is working properly. Has been running stable for years on my SR6010. Some issues with surround mode setting due to bad documentation on marantz's side. Will be fixed in the future. 
 * Marantz support over RS232 is not yet implemented
 * Pipewire support is still being worked on
 * BenQ W2700 / HT3550 support over RS232 covers the core controls (power, source, mute, picture mode, blank, freeze, aspect, lamp mode, lamp hours, model name; volume is read-only). The protocol handling is covered by unit tests but has not yet been tried on a real projector. Picture, color calibration and setup controls are planned.

# Configuration and deployment


See the deployment document [here](deploy/readme.md)


## License

This project is licensed under Affero GPL version 3
