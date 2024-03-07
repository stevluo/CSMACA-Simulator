The program can be run from command prompt using “java Starter” from the folder it is located in.

The design of this program is based on two objects – the CSMACA access point and stations. They follow the Observable and Observer design patterns. The access point prompts for the inputs # of stations, slot time, SIFS, and CWmin. These inputs are used to create a list of stations with unique random back-off timers ranging from CWmin to CWmax.

When the program is run, it waits until a station back-off timer reaches 0 and then receives an RTS, sens NAV to all other stations, waits SIFS, receives frame, and sends ACK. If the back-off timer of other stations reach 0 when the access point is busy, their back-off is incremented exponentially with a cap of 1023. If collisions occur, the Contention Window is doubled + 1 and the stations are given new backoff timers. When a station completes transmission, the Contention Window is reset back to CWmin.
