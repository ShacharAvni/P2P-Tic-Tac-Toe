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

/*
 * The Endpoint class is a basic storage class that encapsulates
 * an endpoint of the Tic-Tac-Toe network.
 */

public final class Endpoint implements Cloneable
{
   public String url; //the url where this endpoint resides
   public String userName; //the user name of the player listening at url

   /*
    * Endpoint Constructor.
    */
   public Endpoint(String endpointURL, String endpointUserName)
   {
      url = endpointURL;
      userName = endpointUserName;
   }

   /*
    * Endpoint Constructor.
    */
   public Endpoint(String endpointURL)
   {
      url = endpointURL;
      userName = "";
   }

   /*
    * Endpoint Constructor.
    */
   public Endpoint()
   {
      url = "";
      userName = "";
   }

   /*
    * Standard clone method.
    */
   public Endpoint clone()
   {
      try
      {
         return (Endpoint) super.clone(); //shallow copy is OK because we're dealing with Strings
      }
      catch(CloneNotSupportedException e)
      {
         //this should never happen
         player.Logger.logError("Unexpected Error in cloning Endpoint", e);
         return new Endpoint(url, userName);
      }
   }

   /*
    * Standard equals method.
    */
   public boolean equals(Object object)
   {
      if(this == object)
      {
         return true;
      }
      else if( !(object instanceof Endpoint) || (object == null) )
      {
         return false;
      }
      else
      {
         Endpoint endpoint = (Endpoint) object;

         return url.equals(endpoint.url) && userName.equals(endpoint.userName);
      }
   }
}