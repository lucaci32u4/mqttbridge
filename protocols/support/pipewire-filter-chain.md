## Overview

| Model               | `deviceCodename` field  |
|---------------------|-------------------------|
| PipewireFilterChain | `PipewireFilterChain`   |

> **_Home assistant discovery is not supported because the data type is incompatible_**

This device type supports exposing a filter chain's `Float` parameters over MQTT. Communication with pipewire is implemented using shell commands, therefore **this device type is only compatible when installed directly on the host with pipewire, not in a docker container**.  
Required dependencies: Pipewire's `pw-cli` command line tool. 

## Device specific options

The configuration file contains a list of filter-chains whose individual plugins will be exposed each on their sub-topic.
A exact copy of the description field must be given to find the right filter chain inside pipewire.
A list of plugin names that will be exported needs to be provided.

```json
{
  ...
  "mqtt": {
    ...
    "baseTopic": "pipewire2mqtt"
  },
  "device": {
    "filterChains": [
      {
        "description": "Livingroom Eqializer",
        "subtopic": "livingroom-eq",
        "plugins": [ "eq-lr", "eq-fc", "eq-sd" ]
      }
    ]
  },
  "deviceCodename": "PipewireFilterChain"
}


```

The above configuration will yield the following MQTT structure:

```properties
pipewire2mqtt/livingroom-eq/eq-lr = { ... }
pipewire2mqtt/livingroom-eq/eq-fc = { ... }
pipewire2mqtt/livingroom-eq/eq-sd = { ... }
```

For example, if the plugins are instances of [LSP Parametric 32-Band Equalizer Stereo](https://lsp-plug.in/?page=manuals&section=para_equalizer_x32_stereo)
the json object will look like this:
```json
{"bal":0.0,"enabled":1.0,"f_0":20.0,"f_1":27.0,"f_10":332.0,"f_11":659.0,"f_12":837.0,"f_13":1500.0,"f_14":1500.0,"f_15":4000.0,"f_16":10300.0,"f_17":14100.0,"f_18":30.19722,"f_19":1250.0,"f_2":36.0,"f_20":1600.0,"f_21":2000.0,"f_22":2500.0,"f_23":3150.0,"f_24":4000.0,"f_25":5000.0,"f_26":6300.0,"f_27":8000.0,"f_28":10000.0,"f_29":12500.0,"f_3":58.0,"f_30":16000.0,"f_31":20000.0,"f_4":85.0,"f_5":102.0,"f_6":152.0,"f_7":186.0,"f_8":216.0,"f_9":295.0,"fm_0":0.0,"fm_1":0.0,"fm_10":6.0,"fm_11":6.0,"fm_12":6.0,"fm_13":6.0,"fm_14":6.0,"fm_15":6.0,"fm_16":6.0,"fm_17":6.0,"fm_18":0.0,"fm_19":0.0,"fm_2":6.0,"fm_20":0.0,"fm_21":0.0,"fm_22":0.0,"fm_23":0.0,"fm_24":0.0,"fm_25":0.0,"fm_26":0.0,"fm_27":0.0,"fm_28":0.0,"fm_29":0.0,"fm_3":6.0,"fm_30":0.0,"fm_31":0.0,"fm_4":6.0,"fm_5":6.0,"fm_6":6.0,"fm_7":6.0,"fm_8":6.0,"fm_9":6.0,"frqs":0.0,"fsel":2.0,"ft_0":1.0,"ft_1":1.0,"ft_10":1.0,"ft_11":1.0,"ft_12":1.0,"ft_13":1.0,"ft_14":1.0,"ft_15":1.0,"ft_16":1.0,"ft_17":1.0,"ft_18":5.0,"ft_19":0.0,"ft_2":1.0,"ft_20":0.0,"ft_21":0.0,"ft_22":0.0,"ft_23":0.0,"ft_24":0.0,"ft_25":0.0,"ft_26":0.0,"ft_27":0.0,"ft_28":0.0,"ft_29":0.0,"ft_3":1.0,"ft_30":0.0,"ft_31":0.0,"ft_4":1.0,"ft_5":1.0,"ft_6":1.0,"ft_7":1.0,"ft_8":1.0,"ft_9":1.0,"g_0":6.060382,"g_1":1.745822,"g_10":0.338844,"g_11":0.245471,"g_12":3.054921,"g_13":0.54325,"g_14":2.238721,"g_15":0.595662,"g_16":0.555904,"g_17":0.582103,"g_18":1.704935,"g_19":1.0,"g_2":0.051286,"g_20":1.0,"g_21":1.0,"g_22":1.0,"g_23":1.0,"g_24":1.0,"g_25":1.0,"g_26":1.0,"g_27":1.0,"g_28":1.0,"g_29":1.0,"g_3":0.316228,"g_30":1.0,"g_31":1.0,"g_4":0.301995,"g_5":0.301995,"g_6":0.169824,"g_7":2.60016,"g_8":0.188365,"g_9":3.162278,"g_in":1.0,"g_out":1.0,"hue_0":0.0,"hue_1":0.03125,"hue_10":0.3125,"hue_11":0.34375,"hue_12":0.375,"hue_13":0.40625,"hue_14":0.4375,"hue_15":0.46875,"hue_16":0.5,"hue_17":0.53125,"hue_18":0.5625,"hue_19":0.59375,"hue_2":0.0625,"hue_20":0.625,"hue_21":0.65625,"hue_22":0.6875,"hue_23":0.71875,"hue_24":0.75,"hue_25":0.78125,"hue_26":0.8125,"hue_27":0.84375,"hue_28":0.875,"hue_29":0.90625,"hue_3":0.09375,"hue_30":0.9375,"hue_31":0.96875,"hue_4":0.125,"hue_5":0.15625,"hue_6":0.1875,"hue_7":0.21875,"hue_8":0.25,"hue_9":0.28125,"ife_l":1.0,"ife_r":1.0,"insp_id":-1.0,"insp_on":0.0,"insp_r":1.0,"mode":0.0,"ofe_l":1.0,"ofe_r":1.0,"q_0":1.0,"q_1":6.0,"q_10":3.99,"q_11":1.08,"q_12":1.86,"q_13":1.24,"q_14":4.0,"q_15":1.0,"q_16":1.45,"q_17":2.28,"q_18":1.0,"q_19":0.0,"q_2":3.35,"q_20":0.0,"q_21":0.0,"q_22":0.0,"q_23":0.0,"q_24":0.0,"q_25":0.0,"q_26":0.0,"q_27":0.0,"q_28":0.0,"q_29":0.0,"q_3":1.41,"q_30":0.0,"q_31":0.0,"q_4":4.58,"q_5":4.85,"q_6":4.87,"q_7":4.21,"q_8":4.02,"q_9":1.01,"react":0.2,"s_0":0.0,"s_1":0.0,"s_10":0.0,"s_11":0.0,"s_12":0.0,"s_13":0.0,"s_14":0.0,"s_15":0.0,"s_16":0.0,"s_17":0.0,"s_18":0.0,"s_19":0.0,"s_2":0.0,"s_20":0.0,"s_21":0.0,"s_22":0.0,"s_23":0.0,"s_24":0.0,"s_25":0.0,"s_26":0.0,"s_27":0.0,"s_28":0.0,"s_29":0.0,"s_3":0.0,"s_30":0.0,"s_31":0.0,"s_4":0.0,"s_5":0.0,"s_6":0.0,"s_7":0.0,"s_8":0.0,"s_9":0.0,"shift":1.0,"w_0":0.0,"w_1":0.0,"w_10":4.0,"w_11":4.0,"w_12":4.0,"w_13":4.0,"w_14":4.0,"w_15":4.0,"w_16":4.0,"w_17":4.0,"w_18":4.0,"w_19":4.0,"w_2":0.0,"w_20":4.0,"w_21":4.0,"w_22":4.0,"w_23":4.0,"w_24":4.0,"w_25":4.0,"w_26":4.0,"w_27":4.0,"w_28":4.0,"w_29":4.0,"w_3":0.0,"w_30":4.0,"w_31":4.0,"w_4":0.0,"w_5":0.0,"w_6":0.0,"w_7":0.0,"w_8":4.0,"w_9":4.0,"xm_0":0.0,"xm_1":0.0,"xm_10":0.0,"xm_11":0.0,"xm_12":0.0,"xm_13":0.0,"xm_14":0.0,"xm_15":0.0,"xm_16":0.0,"xm_17":0.0,"xm_18":0.0,"xm_19":0.0,"xm_2":0.0,"xm_20":0.0,"xm_21":0.0,"xm_22":0.0,"xm_23":0.0,"xm_24":0.0,"xm_25":0.0,"xm_26":0.0,"xm_27":0.0,"xm_28":0.0,"xm_29":0.0,"xm_3":0.0,"xm_30":0.0,"xm_31":0.0,"xm_4":0.0,"xm_5":0.0,"xm_6":0.0,"xm_7":0.0,"xm_8":0.0,"xm_9":0.0,"xs_0":0.0,"xs_1":0.0,"xs_10":0.0,"xs_11":0.0,"xs_12":0.0,"xs_13":0.0,"xs_14":0.0,"xs_15":0.0,"xs_16":0.0,"xs_17":0.0,"xs_18":0.0,"xs_19":0.0,"xs_2":0.0,"xs_20":0.0,"xs_21":0.0,"xs_22":0.0,"xs_23":0.0,"xs_24":0.0,"xs_25":0.0,"xs_26":0.0,"xs_27":0.0,"xs_28":0.0,"xs_29":0.0,"xs_3":0.0,"xs_30":0.0,"xs_31":0.0,"xs_4":0.0,"xs_5":0.0,"xs_6":0.0,"xs_7":0.0,"xs_8":0.0,"xs_9":0.0,"zoom":1.0}
```

## Utilities

If using LSP plugins, here is a script that converts from the native `.cfg` (here named `saved-file.cfg`) file saved from LSP GUI to a json object accepted into MQTT:

```shell
#!/usr/bin/bash

cat saved-file.cfg \
    | sed 's/false/0/g' \
    | sed 's/true/1/g' \
    | sed 's/d[bB]//g' \
    | sed 's/^#.*$//g' \
    | sed -r '/^\s*$/d' \
    | sort \
    | sed -r 's/([a-zA-Z0-9_]+)\s*=\s*(-?[.0-9]*)/"\1": \2,/g' \
    | sed -rz 's/(.*),/\{ \n\1 }/' \
    | tee output.json
```