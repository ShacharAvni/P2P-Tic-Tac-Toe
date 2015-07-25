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

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;

import player.networking.messages.ParameterNames;

/*
 * Sender is a utility class that handles the transmission of
 * Messages (described in Message.java) and any other out-going network
 * transmissions.
 */

public final class Sender
{
   /*
    * Determines if the String s contains the message part of a
    * StrangeAxisException.
    */
   private static boolean isStrangeAxisException(String s)
   {
      return player.Parser.stringMatches(s, "(0).*null");
   }

   /*
    * Determines if the String s contains the message part of a
    * NonUniqueUserException (NonUniqueUserException is defined in
    * the Registry's source). This Exception is thrown by the Registry if
    * a user that is trying to connect has chosen a user name that is
    * already in use.
    */
   public static boolean isNonUniqueUserException(String s)
   {
      return player.Parser.stringMatches(s, "Non Unique User$");
   }

   /*
    * Determines if the String s contains the message part of a
    * UserDoesNotExistException (UserDoesNotExistException is defined in
    * the Registry's source). This Exception is thrown by the Registry if
    * a user requests the connection information of a user that is not
    * connected to the Registry.
    */
   public static boolean isUserDoesNotExistException(String s)
   {
      return player.Parser.stringMatches(s, "User Does Not Exist$");
   }

   /*
    * Calls a web service using Axis. endpoint is the location of the listening
    * party (in the form "IP:port"), functionName is the name of the web service
    * function, numParams is the number of parameters this function takes, returnType
    * is the return type of the web service function (in this system there are two
    * possibilities for this parameter: XSD_STRING for functions returning a String
    * and XSD_ANYTYPE for functions returning void), params are the parameters of
    * the web service function, and paramNames are the names of these parameters.
    * params and paramNames must be of equal length.
    *
    * All web service functions in this system take Strings as parameters. This was
    * just luck. This method could be extended to allow other parameter types by
    * taking in an array of type org.apache.axis.encoding.XMLType.QName.
    *
    * IMPORTANT NOTE: This method is a blocking call. This method should be
    * called such that the GUI or Receiver threads are not blocked.
    */
   public static Object callWebserviceFunction(String endpoint, String functionName, int numParams, final QName returnType, final Object[] params, String[] paramNames) throws WebServiceException, StrangeAxisException
   {
      Service service = new Service();
      Call call;

      try
      {
         // instantiate the Axis Call object
         call = (Call) service.createCall();
         call.setTargetEndpointAddress(new java.net.URL(endpoint));
      }
      catch (javax.xml.rpc.ServiceException e)
      {
         throw new WebServiceException(e.getMessage());
      }
      catch (java.net.MalformedURLException e)
      {
         throw new WebServiceException(e.getMessage());
      }

      call.setOperationName(functionName);

      // set all parameter names and types
      for (int iParam = 0; iParam < numParams; iParam++)
      {
         call.addParameter(paramNames[iParam], XMLType.XSD_STRING, ParameterMode.IN);
      }

      call.setReturnType(returnType);

      // call the web service function
      try
      {
         return call.invoke(params);
      }
      catch (java.rmi.RemoteException e)
      {
         if (isStrangeAxisException(e.getMessage()))
         {
            throw new StrangeAxisException();
         }
         else
         {
            throw new WebServiceException(e.getMessage());
         }
      }
   }

   /*
    * The following methods are network transmissions that I decided not to include in the
    * Message object hierarchy because their transactions are too different from those already
    * in the hierarchy. These methods could be reframed as Message objects with empty receive
    * methods (and we would need a special case for getConnectionInfo and getPlayerList as they
    * return Strings). Doing this would negate the need for the Sender class altogether, however
    * the Message hierarchy is already big enough as it is and there's not too much harm having
    * these methods here.
    */

    /*
     * The following methods all take a parameter called endpoint which is the URL of the Registry.
     */

    /*
     * Asks the Registry for the URL of the connected player with the user name uName. If the user
     * is not connected to the Registry, then the Registry returns the empty String.
     */
   public static String getConnectionInfo(String endpoint, String uName) throws WebServiceException, StrangeAxisException
   {
      return (String) callWebserviceFunction(endpoint, "getConnectionInfo", 1, XMLType.XSD_STRING, new Object[] {uName}, new String[] {ParameterNames.UserName});
   }

   /*
    * Asks the Registry for the list of players currently connected. The Registry returns the list as
    * an XML of the form:
    *
    * <players>
    *   <player>Player One</player>
    *   <player>Player Two</player>
    *   <player>Player Three</player>
    *   ...
    * </players>
    *
    * If there are no players connected to the Registry, then the XML returned is "<players></players>"
    */
   public static String getPlayerList(String endpoint) throws WebServiceException, StrangeAxisException
   {
      return (String) callWebserviceFunction(endpoint, "players", 0, XMLType.XSD_STRING, new Object[] {}, new String[] {});
   }

   /*
    * Asks the Registry to remove the player listening at the URL playerURL from both the "join" and "leave"
    * topics. See Subscriber.java for an explanation of this messaging pattern.
    */
   public static void removeSubscriber(String endpoint, String playerURL) throws WebServiceException, StrangeAxisException
   {
      String paramNames[] = { ParameterNames.Topic, ParameterNames.URL };

      // remove the player from the "join" topic subscriber list
      callWebserviceFunction(endpoint, "removeSubscriber", 2, XMLType.XSD_ANYTYPE, new Object[] {"join", playerURL}, paramNames);

      // remove the player from the "leave" topic subscriber list
      callWebserviceFunction(endpoint, "removeSubscriber", 2, XMLType.XSD_ANYTYPE, new Object[] {"leave", playerURL}, paramNames);
   }

   /*
    * Asks the Registry to add the player listening at the URL playerURL to both the "join" and "leave" topics.
    * See Subscriber.java for an explanation of this messaging pattern.
    */
   public static void addSubscriber(String endpoint, String playerURL) throws WebServiceException, StrangeAxisException
   {
      String paramNames[] = { ParameterNames.Topic, ParameterNames.URL };

      // add the player to the "join" topic subscriber list
      callWebserviceFunction(endpoint, "addSubscriber", 2, XMLType.XSD_ANYTYPE, new Object[] {"join", playerURL}, paramNames);

      // add the player to the "leave" topic subscriber list
      callWebserviceFunction(endpoint, "addSubscriber", 2, XMLType.XSD_ANYTYPE, new Object[] {"leave", playerURL}, paramNames);
   }

   /*
    * Tells the Registry that the player with the user name userName wants to join the Registry.
    */
   public static void sendJoin(String endpoint, String userName, String playerURL) throws WebServiceException, StrangeAxisException
   {
      callWebserviceFunction(endpoint, "join", 2, XMLType.XSD_ANYTYPE, new Object[] {userName, playerURL}, new String[] {ParameterNames.UserName, ParameterNames.URL});
   }

   /*
    * Tells the Registry that the player with the user name userName wants to leave the Registry.
    */
   public static void sendLeave(String endpoint, String userName) throws WebServiceException, StrangeAxisException
   {
      callWebserviceFunction(endpoint, "leave", 1, XMLType.XSD_ANYTYPE, new Object[] {userName}, new String[] {ParameterNames.UserName});
   }
}