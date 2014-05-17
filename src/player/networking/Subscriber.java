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
 * An object implementing the Subscriber interface is a subscriber
 * in a publish-subscribe relationship. "Publish-subscribe" is a
 * messaging pattern. Essentially, a "publisher" has a selection of
 * "topics" that "subscribers" may be interested in. In our case, the
 * Registry is the publisher and the PlayerNodes (described in
 * PlayerNode.java) are the subscribers. There are two topics: "join"
 * and "leave". When a PlayerNode connects to the Registry, it subscribes
 * to both these topics. Once the PlayerNode is connected, the Registry
 * updates all PlayerNodes who subscribe to the "join" topic. When a PlayerNode
 * disconnects from the Registry, the Registry updates all PlayerNodes who
 * subscribe to the "leave" topic.
 */

public interface Subscriber
{
   /*
    * The update method gets called when the publisher, i.e. the
     * Registry, updates for a given topic. Here, topic can be
    * either "join" or "leave" and "userName" is the user name of the
    * player either joining or leaving the network.
    */
   public void update(String topic, String userName);
}