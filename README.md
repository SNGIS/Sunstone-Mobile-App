# Sunstone RTLS
### Sunstone RTLS application was created to update Sunstone Tags through Bluetooth using smartphone.

---

## App requirements
* Min Android version is 6.0
* Application requires Internet connection and Bluetooth.
* Auth0 credentials



## Features
- Upgrade Tags with new firmware
- Change selected LoLaN variables
- Read current configuration of Tags



## Usage

- Add Auth0 credentials to app string resources:
  - ```<string name="com_auth0_client_id">client_id_here</string>```
  - ```<string name="com_auth0_domain">auth0_domain_here</string>```

- Login using your credentials

- Read LoLaN variables

- Update chosen LoLaN variables 

- Firmware update



## Troubleshooting
- ### Tags are not listed or some of them are missing
If scanner doesn't show your devices, try turning off and on Bluetooth on your smartphone.

- ### Bugs
Please report bugs [here](https://github.com/flow2code-com/sunstone-rtls/issues/new)

---

## License
Open source
