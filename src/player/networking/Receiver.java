/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player.networking;

import player.networking.node.PlayerNode;

import player.*;
import player.networking.messages.*;

import java.net.*;
import java.io.*;

/*
 * The Receiver class is responsible for listening for data being sent to
 * the player over the network. The Receiver opens a socket and, within a loop
 * (called the "listening loop") in a separate thread, extracts the data and
 * forwards the data to the appropriate Message objects (which in turn update
 * the PlayerNode). The PlayerNode (described in PlayerNode.java) is responsible
 * for starting and killing the Receiver's listening loop.
 */

public final class Receiver implements Runnable
{
   //this reference is passed to the appropriate Message object so that the Message
   //can update the PlayerNode
   private PlayerNode playerNode;

   //the socket where the Receiver listens for incoming data
   private ServerSocket listener;

   //has the listening loop been terminated?
   private boolean killed;

   //The port to listen on is read from a file, "settings.ini".
   //If there is a problem reading from the file, then the Receiver
   //will listen on this port
   private final int DEFAULT_PORT = 9091;

   //the actual port the Receiver is listening on
   private int port;

   private String localHostIPAddress;

   /*
    * Receiver Constructor
    */
   public Receiver(PlayerNode p)
   {
      playerNode = p;
      killed = false;

      initializeListener();
   }

   /***********************************************************\
   *                        Setup                              *
   \***********************************************************/

   /*
    * Reads the port to listen on from a text file, "settings.ini" which is in the same
    * directory as the executable. The port should be on the first (and only) line of
    * the file.
    */
   private int readPortFromFile() throws IOException
   {
      //if there is a problem reading from the file, then use this port
      int portFromFile = DEFAULT_PORT;

      BufferedReader br = null;
      File file = new File(FileSystem.getExecutingDirectory(Receiver.class) + "settings.ini");

      try
      {
         br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

         //read the (only) line from the file
         String line = br.readLine().trim();

         //convert this line to an Integer (may cause a NumberFormatException)
         Integer integerFromFile = new Integer(line);
         portFromFile = integerFromFile.intValue();
        }
        catch(FileNotFoundException e)
        {
           //file not found
           Logger.logError("Error: settings file not found", null);
        }
        catch(NumberFormatException e)
        {
           //file is corrupted
           Logger.logError("Error: settings file is corrupt", null);
        }
        finally
        {
            if (br != null)
            {
                br.close(); //this call potentially throws an IOException
            }
        }

        return portFromFile;
   }

   /*
    * Sets up the Receiver's socket.
    */
   private void initializeListener()
   {
      obtainLocalHostIPAddress();

      port = DEFAULT_PORT;

      //first, read the port from "settings.ini"
      try
      {
         port = readPortFromFile();
      }
      catch (IOException e)
      {
         Logger.logError("Error: cannot read settings file", e);
      }

      //Next, try to listen at this port. If we can't, keep
      //adding one to the port number until we can successfully
      //listen there
      boolean canListen = false;

      do
      {
         try
         {
               listener = new ServerSocket(port);
               canListen = true;
         }
         catch (IOException e)
         {
            port++;
         }
      } while(!canListen);
   }

   /*
    * Obtains the IP Address of the localhost
    */
   private void obtainLocalHostIPAddress()
   {
      //will store the IP Address including http:// in front
      String fullAddress = "";

      try
      {
         fullAddress = java.net.InetAddress.getLocalHost().toString();
      }
      catch(java.net.UnknownHostException e)
      {
         Logger.fatalError("Cannot resolve localhost", e);
      }

      //get everything after the http://
      localHostIPAddress = Parser.findGroup(fullAddress, "/(.+)", 1);
   }

   /*
    * Closes the Receiver's socket. The listening loop detects this
    * by catching a SocketError Exception and stops as well.
    */
   public void kill()
   {
      killed = true;

      try
      {
         listener.close();
      }
      catch (IOException e)
      {
         Logger.logError("Error in closing receiver connection", e);
      }
   }

   /*
    * Obtains the URL where the Receiver is listening.
    */
   public String listeningURL()
   {
      return "http://" + localHostIPAddress + ":" + port;
   }

   /***********************************************************\
   *                       Getters                             *
   \***********************************************************/

   public int getPort()
   {
      return port;
   }

   public String getLocalHostIP()
   {
      return localHostIPAddress;
   }

   /***********************************************************\
   *                     Socket Reading                        *
   \***********************************************************/

   /*
    * This method starts the listening loop. Within the loop, the
    * socket accepts incoming connections. These connections may be
    * from another player or from the Registry. Both sources communicate
    * with SOAP, through Axis, so each connection is handled the same way.
    */
   public void run() //implementing Runnable
   {
      Socket s = null;

      while(!killed)
      {
         try
         {
            //accept an incoming connection
            s = listener.accept();

            //send back a response (otherwise, the transaction does not complete successfully)
            sendDummyResponse(s.getOutputStream());

            //process the data
            process(s, s.getInputStream());
         }
         catch (SocketException e)
         {
            //If killed is true, then the listener socket was closed through this program.
            //Otherwise, the socket was closed some other way.
            if(!killed)
            {
               Logger.fatalError("Receiver connection severed", e);
            }
         }
         catch (IOException e)
         {
            Logger.fatalError("Receiver connection severed", e);
         }
      }
   }

   /*
    * Sends a response back through an open connection. This is needed because, otherwise,
    * the transaction wouldn't complete successfully.
    */
   private void sendDummyResponse(final OutputStream os)
   {
      try
      {
         DataOutputStream dos = new DataOutputStream(os);
         dos.writeUTF("Response");
      }
      catch (IOException e)
      {
         Logger.logError("Error in sending response back to sender", e);
      }
   }

   /*
    * Takes the accepted connection, reads a SOAP message from it (including
    * the HTTP header) and passes the SOAP String to be parsed.
    */
   private void process(Socket s, InputStream is)
   {
      //the data read in will be converted to a String
      String response = "";

      DataInputStream dis = new DataInputStream(is);

      try
      {
         //the next byte read from the stream
         int next = 0;

         //Keep appending the next byte to the String until we find
         //the last tag of a SOAP call. When we find that tag, we
         //know we have read the full message
         while(!isCompletedSoap(response))
         {
            next = dis.read();
            response += (char) next;
         }

         s.close();

         //parse the response and receive the resulting Message
         process(response);
      }
      catch (IOException e)
      {
         Logger.fatalError("Cannot read SOAP response", e);
      }
   }

   /***********************************************************\
   *                   Message Processing                      *
   \***********************************************************/

   /*
    * Determines if a String ends with the last tag of a SOAP
    * call.
    */
   private boolean isCompletedSoap(String response)
   {
      return Parser.stringMatches(response, "</soapenv:Envelope>$");
   }

   /*
    * Takes the full message received from the socket and forwards
    * it to the appropriate Message object (described in Message.java),
    * which then updates the PlayerNode. This is done in another thread
    * so the Receiver is free to grab the next message.
    */
   private void process(final String soapMsg)
   {
      //Currently, there are six possible Messages that can
      //be received: Confirmation, Forfeit, Invitation, Move,
      //Response, and Update. See their respective .java files
      //for more information regarding each Message object.
      //It would be nice to use reflection and loop over all
      //the subclasses of Message, though for now we explicitly
      //check all of them.

      PlayerNode.startThread
      (
         new Runnable()
         {
            public void run()
            {
               if(Confirmation.isSelf(soapMsg))
               {
                  playerNode.acceptReceiveMessage(new Confirmation(soapMsg));
               }
               else if(Forfeit.isSelf(soapMsg))
               {
                  playerNode.acceptReceiveMessage(new Forfeit(soapMsg));
               }
               else if(Invitation.isSelf(soapMsg))
               {
                  playerNode.acceptReceiveMessage(new Invitation(soapMsg));
               }
               else if(Move.isSelf(soapMsg))
               {
                  playerNode.acceptReceiveMessage(new Move(soapMsg));
               }
               else if(Response.isSelf(soapMsg))
               {
                  playerNode.acceptReceiveMessage(new Response(soapMsg));
               }
               else if(Update.isSelf(soapMsg))
               {
                  playerNode.acceptReceiveMessage(new Update(soapMsg));
               }
            }
         }
      );
   }
}