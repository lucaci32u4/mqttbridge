# Deploy to a server

Copy the `config-template.json` to `config.json` and edit to match your configuration

You should have passwordless ssh key already configured for your host, otherwise look into ansible documentation on how to configure inventories.
Rum the ansible script, editing your destination host:

```shell
ansible-playbook deploy.yaml -i moonspawn.lr, --extra-vars 'conffile=config-marantz.json progname=mqttbridge'
```

To deploy multiple instances on the same machine, use the `progname` parameter to install them as different systemd units:

```shell
ansible-playbook deploy.yaml -i moonspawn.lr, --extra-vars 'conffile=config-marantz.json progname=Marantz2Mqtt'
ansible-playbook deploy.yaml -i moonspawn.lr, --extra-vars 'conffile=config-pipewire.json progname=Pipewire2Mqtt'
```