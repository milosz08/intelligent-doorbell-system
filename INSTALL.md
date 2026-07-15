# Installation and provisioning

[TBD]

```powershell
usbipd list
usbipd bind --busid 16-2
usbipd attach --wsl --busid 16-2
```

```powershell
New-NetFirewallRule -DisplayName "MQTT Broker Inbound" -Direction Inbound -LocalPort 1883 -Protocol TCP -Action Allow
```
