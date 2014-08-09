P2P Tic-Tac-Toe
=================

##Description

This software is a peer-to-peer Tic-Tac-Toe network where players can log on, play rounds of Tic-Tac-Toe with each other and log off.

##Code Review

If you wish to review the code for P2P Tic-Tac-Toe, please see README_CODE_REVIEW.txt.

##License

Use of this software is governed by a BSD-style license (see the accompanying LICENSE.txt file). This software also makes use of
third-party software. All third-party software binaries are located in the "third-party/lib" folder. Their licenses are located in the
"third-party/license" folder. The relationships between the binaries and their respective licenses are outlined in LICENSE.txt.

##Compilation

The final goal of compiling the source is to produce two jar files:
* Player.jar, an executable jar file, to be placed in bin/player
* Registry.jar, to be placed in bin/registry

This project uses the [Apache Ant build system](http://ant.apache.org/ "Ant Homepage").
[This](http://ant.apache.org/manual/tutorial-HelloWorldWithAnt.html "Ant tutorial") is a tutorial for using Ant.

Note that to properly install Ant, __two environment variables must be added__, JAVA_HOME which stores the path to the JRE installation
directory, and ANT_HOME which stores the path to Ant's installation directory.

__To build the project, run the command "ant" from the command line/shell in this project's upper level directory__. This will clean the
previous built files, compile the source, create the jar files and place them in the appropriate folders, and run the player program.

* To simply clean the previous built files, run "ant clean".
* To simply compile the source, run "ant compile".
* To create the two jar files from the compiled source, run "ant jar".
* To run the Player program, run "ant run" (or follow the instructions in the next section).

To compile this project without Ant, know that the source is in the "src" directory and the classpath should be set to all the jar files
found in the "third-party/lib" directory.

##Starting Tic-Tac-Toe

To play Tic-Tac-Toe you must first run the "Player" program. You should have the JRE bin folder on the system path.

Starting the Player program on Windows:
* Navigate to bin/player and run the executable jar file Player.jar. If that doen't work, run StartPlayer.bat which is in the
  same directory.

Starting the Player program on Linux and OS X:
* Navigate to bin/player and run StartPlayer.sh.

You may be prompted to give the program special security privileges due to the networking component. Note that the Player
program requires the files in the third-party/lib folder to run.

After starting the Player program, You will first notice a message in the Message Log stating that you are not connected to the
Registry as well as a message stating the URL where the program is listening. Starting the Registry is described in the next
section. Note your URL as other players will need to know it to connect with you directly.

Should the default port (stored in bin/player/settings.ini) be unusable, the program will keep selecting higher and higher port
numbers until one is free. Many players can play on the same computer though the intended scenario is one player per computer.

##Starting the Registry

The Registry stores the connection information of all currently connected players. Players may connect with each other directly
(if the other player's URL is known) or they may connect through the Registry.

You should have the JRE bin folder on the system path.

On Windows:
* Navigate to bin/registry and run StartRegistry.bat.

On Linux and OS X:
* Navigate to bin/registry and run StartRegistry.sh.

Note that the Registry program requires the files in the lib folder to run.

The Registry's port can be changed by changing the -p option in the StartRegistry file. You may be prompted to give the
program special security privileges due to the networking component.

##Connecting with Players and the Registry

To play a match of Tic-Tac-Toe, you will first have to invite another player to play. If you're not connected to the
Registry (or the player you want to play against is not connected to the Registry), select Game->Send Invitation from the
menu. Then, enter the IP address and port of the player as "IP:PORT". You will be asked to enter a user name if you have
not already done so. After which, a dialog box will appear on the other player's screen, asking if they want to play with you.
If they select Yes, then a match starts and one of you will get the first move. If you changed your mind after sending a
connection request, or if the player is taking too long to respond, you can cancel the request by selecting Game->Cancel
Invitation from the menu.

If you're connected to the Registry, you can play a game by simply selecting a player from the "Online" list, and pressing
the "Play" button. In a similar fashion to the above, you can cancel your request by selecting Game->Cancel Invitation
from the menu.

To connect to the Registry, select Registry->connect... from the menu. You will have to enter the IP address and port of the
Registry as "IP:PORT" and you will have to enter a user name. To disconnect from the Registry, select
Registry->disconnect... from the menu.

##Playing Tic-Tac-Toe

A Tic-Tac-Toe match is played with players alternating turns until someone wins or until the board is filled and it's a
draw (a Cat's Game). The default playing piece is "X" though you can change your piece at any time by selecting from the
"Weapon" menu. If you wish, to stop playing, you may select Game->Forfeit from the menu.

##Directory Setup

| File/Folder   | Description   |
| ------------- | ------------- |
| LICENSE.txt   | information about the license agreement for using this software |
| NOTICE.txt | file for compliance with the Apache 2.0 license pertaining to the use and distribution of Apache Axis binaries | 
| third-party/lib | folder containing the third-party binaries necessary for running this software |
| third-party/license | folder containing the license files and other accompanying materials for the third-party software included with this software |
| bin | folder containing this software's executables and accompanying files. Partitioned into executables for the Player and Registry programs |
| src | folder containing this software's source code. Partitioned into source files for the Player and Registry programs. For the Player, the main method is in Main.java. For the Registry, the main source file of interest is Registry.java. |

##Credits

P2P Tic-Tac-Toe Copyright (c) 2014 Shachar Avni. All rights reserved.

##Third-Party Software

P2P Tic-Tac-Toe makes use of the following third-party software:

* [Apache Axis](http://axis.apache.org/axis/ "Axis Homepage")
  Copyright (c) 2000-2005 The Apache Software Foundation

* [Apache Commons libraries](http://commons.apache.org/ "Apache Commons Homepage")
  Copyright (c) 2014 The Apache Software Foundation. All Rights Reserved

* [Apache XML Commons](http://xerces.apache.org/xml-commons/ "Apache XML Commons")
  Copyright (c) 2001-2009 The Apache Software Foundation. All Rights Reserved

* [Apache Xerces](http://xerces.apache.org/ "Apache Xerces Homepage")
  Copyright (c) 1999-2012 The Apache Software Foundation. All Rights Reserved

* [Log4J](http://logging.apache.org/log4j/1.2/ "Log4J Homepage")
  Copyright (c) 1999-2012 Apache Software Foundation. All Rights Reserved

* [Activation Framework](http://www.oracle.com/technetwork/java/jaf11-139815.html "Activation Homepage") 1.1.1
  Copyright (c) Sun Microsystems

* [JavaMail](http://www.oracle.com/technetwork/java/javamail/index.html "JavaMail Homepage")
  Copyright (c) 1997-2011, Oracle and/or its affiliates. All rights reserved.