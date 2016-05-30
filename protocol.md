# Polyjam App Communication Protocol

Written by Tal Kirshboim

### Actors and Terminology

- Polyjam app = app
- Polygod app = god
- Ableton Live machine = Live (doesn't have to be Ableton live, could be any other machine that handles the OSC messages)

## Protocol

### Step 1: Where is god?

- app listens on port 4444 for a UDP packet from god with the content "hi"
- once this message arrives the app keeps the IP that it came from (so we know where god lives and we can talk to god)

let's say for the examples that god's address is : 192.168.0.101

### Step 2: Registration

- app sends a HTTP GET request to god at http://[GOD IP]:8080/hi?name=[user's name]

For example if the player is called "gadi", the app should call:
http://192.168.0.101:8080/hi?name=gadi

When this request succeeds the app start polling god for the player's status

### Step 3: Status Polling

- app polls the player status by sending a HTTP GET requests in the following format:
http://[GOD IP]:8080/status

God can respond with:
1. 'wait' - which means: player needs to wait to be called (which could also be after playing)
2. '[Port] [IP]' - IP of Live AKA "Server IP" in god app. Port is the port where the OSC messages should be sent to.
example response: '5550 192.168.0.12' means that the player can now play using the app by sending OSC messages on port 5550 to Live on 192.168.0.12

- app keep polling god for the status while the player is playing. when god decides that game is over they will send again 'wait' and the app should not let the user play (until next time).
- app shouldn't poll too often - every 2 seconds should be enough

## Comments
Text data is sent in UTF-8 encoding
