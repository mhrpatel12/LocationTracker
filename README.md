# LocationTracker

This project tracks location of user in background. 
Upon change in user location, activity gets notified with updated location and polyline is drawn on the map. This is continued untill user ends the shift, which stops the background service.


EventBus from GreenRobot is used for managing location events.
