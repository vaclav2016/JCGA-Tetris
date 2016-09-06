# Tetris Video Game in CGA-style graphic

This is a example for JCGA framework - Tetris Video Game.

Features:

* Good perfomance - work at low-end ARM without delay (like Pocket C.H.I.P or Raspberry Pi 1)
* Onscreen buttons - alow to use touch-screen interface

![Tetris Video Game](screenshot-tetris.png)

## Build

You need download JCGA framework and install it into local maven repository:

    $ git clone https://github.com/vaclav2016/JCGA
    $ cd JCGA
    $ mvn install
    $ cd ..

At second step, You clone Tetris repository and build it:

    $ git clone https://github.com/vaclav2016/JCGA-Tetris
    $ cd JCGA-Tetris
    $ mvn install
    $ cd ..

## Run

    $ cd JCGA-Tetris
    $ java -jar target/cga-tetris-0.0.1-jar-with-dependencies.jar

## Licensing

(c) 2016 Copyright Vaclav2016 https://github.com/vaclav2016, jabber id vaclav2016@jabber.cz

is licensed under the Boost License, Version 1.0.

<http://www.boost.org/LICENSE_1_0.txt>
