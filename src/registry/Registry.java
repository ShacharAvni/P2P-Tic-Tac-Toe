/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package registry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;

/*
 * The Registry is the server in the Tic-Tac-Toe network. The Registry stores
 * the user names and URLs of the players that are currently connected, and
 * sends updates to the other players when a player joins or leaves the network.
 * The Registry updates players through the publish-subscribe messaging pattern.
 * When a player connects to the Registry, it subscribes to two "topics", which
 * are "join" and "leave". When a player joins the network, the Registry updates
 * all the players which have subscribed to the "join" topic. When a player leaves
 * the network, the Registry updates all the players which have subscribed to the
 * "leave" topic. Note that players only use the Registry to obtain the URLs of
 * other players through their user names. The actual Tic-Tac-Toe games are played
 * between players in P2P fashion. This simplifies the Registry code as it does not
 * have to act as a mediator.
 *
 * Note that all Exceptions thrown by methods of this class are absorbed on the player
 * side as java.rmi.RemoteExceptions.
 */

public final class Registry
{
   // the topics that the Registry supports
   private final String[] validTopics = { "join", "leave" };

   // the topics HashMap maps the topic name to a Vector containing the URLs of its subscribers
   private HashMap<String, Vector<URL>> topics;

   // the players HashMap maps players' user names to their URLs
   private HashMap<String, URL> players;

   // the subscriberMutexes HashMap maps the topic name to a Mutex that locks that topic's
   // subscribers Vector. We need to synchronize access to the subscribers Vectors so that
   // the Registry publishes to the join and leave subscribers correctly. For instance, we
   // don't want to have a case where the Registry publishes to a player that is in the
   // process of being removed from the subscriber list. Similarly, we don't want to have a
   // case where a player is in the process of being added to the subscriber list and the
   // Registry does not publish to that player.
   private HashMap<String, Semaphore> subscriberMutexes;

   // the playersMutex locks the players HashMap. We need to synchronize access to the players
   // HashMap to ensure that when a player queries the state of the other players (user names or
   // connection info) that the result is consistent with the state of the Registry. For instance, we
   // don't want to have a case where a player queries for the list of players while another player is
   // in the process of leaving the network (or similarly, when a player is in the process of joining
   // the network). In the first case, the player would have a list containing a player that had already
   // left and in the second case, the player would have an incomplete list of players. We also don't want
   // the case where a player queries for the connection information of a player who is just about to leave
   // the network.
   private Semaphore playersMutex;

   /*
    * Registry Constructor. Initializes the topics and players HashMaps to the empty state, as well as
    * initializes the synchronization objects. This constructor is called when
    * org.apache.axis.transport.http.SimpleAxisServer is started.
    */
   public Registry()
   {
      topics = new HashMap<String, Vector<URL>>();
      players = new HashMap<String, URL>();

      subscriberMutexes = new HashMap<String, Semaphore>();
      playersMutex = new Semaphore(1);

      // create subscribers and mutex mappings for each topic
      for (String topic : validTopics)
      {
         topics.put(topic, new Vector<URL>());
         subscriberMutexes.put(topic, new Semaphore(1));
      }
   }

   /*
    * Determines if the String "topic" is a topic name supported by the Registry.
    */
   private boolean isValidTopic(String topic)
   {
      // only have two topics for now
      return ( validTopics[0].equals(topic) || validTopics[1].equals(topic) );
   }

   /*
    * Tells the Registry that the player with the user name userName and listening at the URL subscriptionURL
    * wants to join the Registry. If a player is already connected with the same user name, then a
    * NonUniqueUsernameException is thrown. If the subscriptionURL is not valid, then a MalformedURLException
    * is thrown.
    */
   public void join(String userName, String subscriptionURL) throws NonUniqueUsernameException, MalformedURLException, InterruptedException
   {
      playersMutex.acquire();

      try
      {
         // check if this user name is already being used
         if (players.containsKey(userName))
         {
            throw new NonUniqueUsernameException();
         }

         // add this player to the list of connected players
         players.put(userName, new URL(subscriptionURL));
      }
      finally
      {
         playersMutex.release();
      }

      // tell all subscribers to the "join" topic that userName has joined
      // the network
      publish("join", userName);
   }

   /*
    * Tells the Registry that the player with the user name userName wants to leave the Registry. The user name
    * is the key in the players HashMap so no need to send the player's URL.
    */
   public void leave(String userName) throws InterruptedException
   {
      boolean removed = false;
      playersMutex.acquire();

      try
      {
         removed = (players.remove(userName) != null);
      }
      finally
      {
         playersMutex.release();
      }

      if (removed)
      {
         // tell all subscribers to the "leave" topic that userName has left
         // the network
         publish("leave", userName);
      }
   }

   /*
    * Queries the Registry for the list of currently connected players.
    *
    * The list is returned as XML of the form:
    * <players>
    *   <player>User Name 1</player>
    *   <player>User Name 2</player>
    *   <player>User Name 3</player>
    *   ...
    * </players>
    */
   public String players() throws InterruptedException
   {
      String playersXML = "<players>";

      playersMutex.acquire();

      try
      {
         // Iterate through the user names, appending a "<player>" tag
         // to the XML for each player
         Iterator<String> userNames = players.keySet().iterator();

         while (userNames.hasNext())
         {
            playersXML += "<player>" + userNames.next() + "</player>";
         }
      }
      finally
      {
         playersMutex.release();
      }

      playersXML += "</players>";

      return playersXML;
   }

   /*
    * Queries the Registry for the URL of a particular player. If the player with the user name userName is not
    * connected to the network, then a UserDoesNotExistException is thrown.
    */
   public String getConnectionInfo(String userName) throws UserDoesNotExistException, InterruptedException
   {
      URL playerURL;
      playersMutex.acquire();

      try
      {
         // get the URL of the player with the given user name
         playerURL = players.get(userName);
      }
      finally
      {
         playersMutex.release();
      }

      if (playerURL == null)
      {
         throw new UserDoesNotExistException();
      }

      return playerURL.toString();
   }

   /*
    * Adds the player listening at the URL subscriptionURL to the subscribers list for the given topic. If the
    * topic is not "join" or "leave" then an InvalidTopicException is thrown. If the subscriptionURL is invalid,
    * then a MalformedURLException is thrown.
    */
   public void addSubscriber(String topic, String subscriptionURL) throws InvalidTopicException, MalformedURLException, InterruptedException
   {
      if (!isValidTopic(topic))
      {
         throw new InvalidTopicException();
      }

      // throws MalformedURLException if this URL is invalid
      URL subscriptionURLAsURL = new URL(subscriptionURL);

      subscriberMutexes.get(topic).acquire();

      try
      {
         Vector<URL> subscribers = topics.get(topic);

         // add the player listening at subscriptionURL to the subscribers list
         // for this topic (if they haven't been added already).
         if (!subscribers.contains(subscriptionURLAsURL))
         {
            subscribers.add(subscriptionURLAsURL);
            System.out.println("Added " + subscriptionURL + " to " + topic);
         }
      }
      finally
      {
         subscriberMutexes.get(topic).release();
      }
   }

   /*
    * Removes the player listening at the URL subscriptionURL from the subscribers list for the given topic. If
    * the topic is not "join" or "leave" then an InvalidTopicException is thrown. If the subscriptionURL is invalid,
    * then a MalformedURLException is thrown.
    */
   public void removeSubscriber(String topic, String subscriptionURL) throws InvalidTopicException, MalformedURLException, InterruptedException
   {
      if (!isValidTopic(topic))
      {
         throw new InvalidTopicException();
      }

      subscriberMutexes.get(topic).acquire();

      try
      {
         Vector<URL> subscribers = topics.get(topic);

         // remove the player listening at subscriptionURL from the subscribers list
         // for this topic (if need be). The call to new URL(...) may throw a
         // MalformedURLException.
         if (subscribers.remove( new URL(subscriptionURL) ))
         {
            System.out.println(subscriptionURL + " successfully removed from: " + topic);
         }
      }
      finally
      {
         subscriberMutexes.get(topic).release();
      }
   }

   /*
    * Updates all the subscribers for the given topic that a user (whose name is stored in
    * the Object data) has either joined (if topic is "join") or left (if topic is "leave")
    * the Registry. An Object is passed, as opposed to a String, for the user name to keep
    * this method extensible in case other Objects are sent as an update.
    */
   private void publish(final String topic, final Object data) throws InterruptedException
   {
      subscriberMutexes.get(topic).acquire();

      try
      {
         Vector<URL> subscribers = topics.get(topic);

         if (subscribers != null)
         {
            // Iterate over all the subscribers, updating each one in a separate thread
            for (URL subscriberURL : subscribers)
            {
               // URL needs to be final because it's used in another thread
               final URL finalSubscriberURL = subscriberURL;

               Runnable r = new Runnable()
               {
                  @Override
                  public void run()
                  {
                     try
                     {
                        // call the web service function "update" with topic and data as the parameters.
                        Service service = new Service();
                        Call call = (Call) service.createCall();

                        call.setTargetEndpointAddress(finalSubscriberURL);
                        call.setOperationName("update");

                        // the two parameters are called "Topic" and "UserName". The receiving
                        // end needs to know this in order to parse these parameters out of the
                        // resulting SOAP call.
                        call.addParameter("Topic", XMLType.XSD_STRING, ParameterMode.IN);
                        call.addParameter("UserName", XMLType.XSD_STRING, ParameterMode.IN);
                        call.setReturnType(XMLType.XSD_ANYTYPE);

                        call.invoke(new Object[] { topic, data });
                     }
                     catch (javax.xml.rpc.ServiceException e)
                     {
                        System.err.println("ServiceException occurred while publishing " + topic + " to " + finalSubscriberURL);
                     }
                     catch (java.rmi.RemoteException e)
                     {
                        if(!e.getMessage().matches(".*(0).*null.*")) // if this is not a StrangeAxisException (see: StrangeAxisException.java in Player's source for an explanation)
                        {
                           System.err.println("RemoteException occurred while publishing " + topic + " to " + finalSubscriberURL);
                        }
                     }
                  }
               };

               Thread t = new Thread(r);
               t.start();
            }
         }
      }
      finally
      {
         subscriberMutexes.get(topic).release();
      }
   }
}