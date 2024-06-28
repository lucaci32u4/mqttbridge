# Deploy

## Create a configuration file

The config file is a json document with the following structure:

```json
{
  "mqtt": {
    "username": "yourMqttUsername",
    "password": "yourMqttPassword",
    "port": 1883,
    "qos": 2,
    "host": "ip-or-hostname-of-mqtt-broker",
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

The `mqtt` section contains connection details for the MQTT broker.

The `discovery` section contains details for exposing devices to Home Assistant using MQTT Discovery protocol. To disable Home Assistant discovery, set this object to `null`.
* `entityName` controls the name display name of the entity. The entity id is obtained by lowercasing and replacing spaces with underscores.
* `discoveryBaseTopic` must be the same topic Home Assistant is configured to listen on for discovery messages. The default is `homeassistant`

For the `deviceCodename` field and device specific options, see the following documentation pages:
* [Marantz](../protocols/support/marantz.md)
* [Epson Projector](../protocols/support/epson-projector.md)
* [Benq Projector](../protocols/support/benq-projector.md)
* [Pipewire Filter Chain](../protocols/support/pipewire-filter-chain.md)



## Use ansible to install as a systemd service on a remote host

This ansible playbook will create a user-level systemd service on your remote host. You **must** have java (at least 17) installed on the remote host to properly run the application:
```text
$ java --version
openjdk 22 2024-03-19
OpenJDK Runtime Environment (build 22)
OpenJDK 64-Bit Server VM (build 22, mixed mode, sharing)
```
The jar will be placed in `~/.local/share/{{progname}}/{{jar}}`  
The configuration file will be placed at `~/.config/{{progname}}/config.json`
Use the `conffile` variable to pass the local path to the configuration file.

You should have passwordless ssh key already configured for your host, otherwise look into ansible documentation on how to configure inventories.
Run the ansible script, editing your destination host:

```shell
ansible-playbook deploy.yaml -i moonspawn.lr, --extra-vars 'conffile=config-marantz.json progname=mqttbridge'
```

To deploy multiple instances on the same machine, use the `progname` parameter to install them as different systemd units:

```shell
ansible-playbook deploy.yaml -i moonspawn.lr, --extra-vars 'conffile=config-marantz.json progname=Marantz2Mqtt'
ansible-playbook deploy.yaml -i moonspawn.lr, --extra-vars 'conffile=config-pipewire.json progname=Pipewire2Mqtt'
```

## Docker container

Running as a docker container is the easiest method to get up and running, but will not work with some devices that need access to OS resources (like PipewireFilterChain or RS232 projector's serial port).
A workaround for RS232 is to pass the device in the container. 

Docker compose script:

```yaml
version: '3'

services:
  mqttbridge:
    container_name: mqttbridge
    image: ghcr.io/lucaci32u4/mqttbridge:latest
    volumes: 
      - ./config.json:/config/config.json
      # Uncomment this if you're connecting to your device using a serial port and replace with your serial port path
      # Use /dev/mqttbridge_serial as the port in the config file
      # - /dev/serial/by-id/usb-FTDI_FT232R_USB_UART_A10MMGWJ-if00-port0:/dev/mqttbridge_serial
    restart: unless-stopped
```

## Run manually (for development or testing)

Clone the project on your development machine and ensure that you have maven and java (minumum 1.17) installed.
Compile the project, then run the resulting jar.
```shell
mvn package
java -jar target/mqttbridge-1.0-SNAPSHOT.jar --config config.json
```