Code for analyzing raw Lidar Data into background and human location data

== Object Identifier Output Format

=== Background Identification

It is guaranteed that an identified background will first be written before any object data is printed out. A background flush is as follows:

```
BACKGROUND BEGIN
BACKGROUND POLY1 <angle> <distance>
BACKGROUND POLY2 <angle> <distance>
BACKGROUND POLY3 <angle> <distance>
...
BACKGROUND POLY1 <angle> <distance>
BACKGROUND POLY2 <angle> <distance>
...
BACKGROUND END
```

* angle: The angle in degrees (integer) between 0-359
* distance: The distance of the polygon vertex in millimeters

The drawing process is as follows. Given POLY(N) and POLY(N + 1), always draw a line between the two points. Otherwise, no additional drawing is performed.

Additional background writes may occur throughout the process of operation, but it is not guaranteed.

=== Object Identification

After the first background write, identified objects will be written in the following format:

```
OBJECT <angle> <distance>
OBJECT <angle> <distance>
...
```

* angle: The angle (in degrees) of the object center between 0-359
* distance: The distance (in millimeters) of the object center 

