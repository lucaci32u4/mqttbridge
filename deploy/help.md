# Deploy

## Use ansible to install as a systemd service on a remote host

Copy the `config-template.json` to `config.json` and edit to match your configuration

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

```docker-compose
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