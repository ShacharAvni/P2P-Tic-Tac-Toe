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

import java.io.Serializable;

/*
 * A WebServiceException gets thrown if a web service call through Axis
 * (which occurs in the callWebserviceFunction method in Sender.java) throws
 * either a javax.xml.rpc.ServiceException, a java.net.MalformedURLException,
 * or a java.rmi.RemoteException. Thus, a WebServiceException may be either of
 * those. In the case of a java.rmi.RemoteException, this Exception is a subset
 * of those thrown, namely those that are not a StrangeAxisException
 * (See: StrangeAxisException.java). Essentially, this Exception class exists so
 * that any method issuing a web service call does not have to deal with three
 * different Exception cases. try {} catch (Exception) {} is an alternative of
 * course, though I chose not to go that route.
 */

public class WebServiceException extends Exception implements Serializable
{
   private static final long serialVersionUID = 1L;

   /*
    * WebServiceException constructor
    */
   public WebServiceException(String msg)
   {
      super(msg);
   }
}