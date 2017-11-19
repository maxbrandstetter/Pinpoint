# Pinpoint
Senior Project on indoor tracking with Bluetooth beacons

Pinpoint is an Android application that uses Bluetooth devices called beacons to map and track a user within a room.  The project itself is proof of concept an application that could similarly map and track users in malls, convention centers, and other large indoor areas.  Primarily, it aims to experiment with beacons, which have yet to see much public use.

The two main features of Pinpoint are the map setup and the location tracking.  Map setup encompasses a simple setup process, which instructs the user on selecting beacons, placing them, and scanning distances between the beacons.  Following the process generates a map of the room, which can then be used to track the user's location in.  The location tracking performs similarly.  It measure the user's distance to each of the configured beacons, calculates the distances to each beacon, and then inputs coordinates and the distances to a trilateration calculator.  The output is an approximate location, which is updated frequently to provide real-time tracking capabilities.

The project never reached completion, as I was unable to account for signal interference effectively.  There were some steps that I took to counteract interference, such as collecting a data set from each beacon and filtering out unusual values (outliers).  However, due to the nature of Bluetooth signals and how trilateration works, even the slightest fluctuation can cause inaccuracy.  The project is not a complete failure, as it works to some extent and was a learning experience for me.

See Pinpoint Project Report for more details
