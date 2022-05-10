What is this?
=====

A program which receives OCR text via a ZeroMQ connection, on a configurable port.
It then tries to parse these as wow-chat messages, noting the timestamp, user, and message.
Afterwards it parses the message part for any of the configured commands and presses the matching key via a Robot.


Requirements
-----

Java 8 or higher has to be installed and be on the Path.

Starting the program
-----

If you have downloaded the .jar, simply launch it via `java -jar OCR-Receiver-1.0-SNAPSHOT.jar`

If you have downloaded the whole repo, open it in your favorite IDE, let gradle do its thing and launch Main.

Configuration
-----

Inside config.properties you can specify the port on which ZeroMQ is listening.
You can specify the pattern from which the command is captured (it simply takes the first capture group of the regex).
You can also insert all command mappings there. The format is `command=keycode`, 
where keycode is the hexnumber which is written in java.awt.event.KeyEvent.