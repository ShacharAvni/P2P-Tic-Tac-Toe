P2P Tic-Tac-Toe - Code Review
=================

1) Introduction

Thank you for taking the time to review the P2P Tic-Tac-Toe code.

The purpose of this document is to help you become familiar with
the structure of the P2P Tic-Tac-Toe code base.

Links to cited code can be found at the bottom of the document.


2) Code Structure

All first-party code is contained in the src directory.


2.1) The Registry Package

This package contains code for the Registry. This is the server that
players can optionally connect to so that they can connect to other
players in a Skype-like fashion. Note that the Registry only forwards
the connection information. No matter how players connect, the games
are always peer-to-peer.

-> The Registry updates players through the publish-subscribe messaging
   pattern. When a player connects to the Registry, it subscribes to two
   "topics", which are "join" and "leave". When a player joins the network,
   the Registry updates all the players which have subscribed to the "join"
   topic. When a player leaves the network, the Registry updates all the
   players which have subscribed to the "leave" topic.

-> The main file of interest is registry/Registry.java [1]. Please see that
   file for comments.


2.2) The Player Package

This package contains code for the main game client. This is split up into
subpackages for game logic (game), networking, and UI.

-> The main method is located in player/Main.java [2].

-> The program begins by spawning two threads, the GUI thread which gets
   spawned when the window opens, and the receiver thread which connects
   to a socket and listens for incoming messages from other players or
   the Registry.

-> The main decision maker of the player program is the PlayerNode object
   This object maintains the player's connectivity state, receives messages
   from the Receiver, and forwards relevant updates to the GUI. See
   player/networking/node/PlayerNode.java [3] for details.

-> Please see player/networking/Receiver.java [4] to see how the player program
   receives incoming messages.

-> The player.networking.messages subpackage [5] contains a class hierarchy which
   describe the different messages that the player program can receive (e.g.
   Move, Forfeit, Confirmation). The abstract base class for this hierarchy
   is in player/networking/messages/Message.java [6]. This hierachy was implemented
   to take responsibility away from the PlayerNode and is an example of the
   Visitor pattern.


3) Links

[1] https://github.com/ShacharAvni/P2P-Tic-Tac-Toe/blob/master/src/registry/Registry.java
[2] https://github.com/ShacharAvni/P2P-Tic-Tac-Toe/blob/master/src/player/Main.java
[3] https://github.com/ShacharAvni/P2P-Tic-Tac-Toe/blob/master/src/player/networking/node/PlayerNode.java
[4] https://github.com/ShacharAvni/P2P-Tic-Tac-Toe/blob/master/src/player/networking/Receiver.java
[5] https://github.com/ShacharAvni/P2P-Tic-Tac-Toe/tree/master/src/player/networking/messages
[6] https://github.com/ShacharAvni/P2P-Tic-Tac-Toe/blob/master/src/player/networking/messages/Message.java