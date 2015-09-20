Code for reading raw data from Lidar

Output data format:

```
<angle> <distance> <warning>
<angle> <distance> <warning>
...
```

* angle: An integer between 0-356 inclusive, that gives the angle measurement.

* distance: An integer that details the distance away.

* warning: "True" or "False", depending on if there is a strength warning (meaning distance measurement may not be accurate).
