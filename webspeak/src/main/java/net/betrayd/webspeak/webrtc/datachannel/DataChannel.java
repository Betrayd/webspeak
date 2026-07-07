//from jitsi-videobridge dirrectly
/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.betrayd.webspeak.webrtc.datachannel;

import net.betrayd.webspeak.webrtc.datachannel.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.*;
import java.util.HexFormat;

/**
 * Models a WebRTC Data Channel
 *
 * @author Brian Baldino
 */
public class DataChannel
{
    public static final Logger LOGGER = LoggerFactory.getLogger(DataChannel.class);

    private final DataChannelStack.DataChannelDataSender dataChannelDataSender;
    protected final int channelType;
    protected final int priority;
    protected final long reliability;
    protected final int sid;
    protected final String label;

    protected boolean ready = false;

    private DataChannelStack.DataChannelEventListener eventListener;
    private DataChannelStack.DataChannelMessageListener messageListener;

    /**
     * Initializes a new {@link DataChannel} instance.
     */
    public DataChannel(
            DataChannelStack.DataChannelDataSender dataChannelDataSender,
            int channelType, int priority, long reliability, int sid, String label
    )
    {
        this.dataChannelDataSender = dataChannelDataSender;
        this.channelType = channelType;
        this.priority = priority;
        this.reliability = reliability;
        this.sid = sid;
        this.label = label;
    }

    public void open()
    {
        OpenChannelMessage openMessage = new OpenChannelMessage(
                channelType,
                priority,
                reliability,
                label,
                DataChannelProtocolConstants.PROTOCOL_STRING);

        ByteBuffer msg = openMessage.getBuffer();
        if (dataChannelDataSender.send(msg, sid, DataChannelProtocolConstants.WEBRTC_DCEP_PPID) < 0)
        {
            LOGGER.error("Error sending data channel open message");
        }
    }

    /**
     * Checks if this data channel is ready.
     */
    public boolean isReady()
    {
        return ready;
    }

    /**
     * Sets the listener for data channel events.
     * @param listener
     */
    public void onDataChannelEvents(
            DataChannelStack.DataChannelEventListener listener)
    {
        this.eventListener = listener;
    }

    /**
     * Sets the message listener.
     */
    public void onDataChannelMessage(
            DataChannelStack.DataChannelMessageListener
                    dataChannelMessageListener)
    {
        this.messageListener = dataChannelMessageListener;
    }

    /**
     * Handles an incoming message.
     * @param message
     */
    public void onIncomingMsg(DataChannelMessage message)
    {
        if (message instanceof OpenChannelAckMessage)
        {
            ready = true;
            eventListener.onDataChannelOpened();
        }
        else if (message instanceof DataChannelStringMessage)
        {
            DataChannelStringMessage dataChannelStringMessage = (DataChannelStringMessage)message;
            LOGGER.debug("Received data channel string message: {}", dataChannelStringMessage.data);
        }
        else if (message instanceof DataChannelBinaryMessage)
        {
            DataChannelBinaryMessage dataChannelBinaryMessage = (DataChannelBinaryMessage)message;
            LOGGER.debug("Received data channel binary message: {}", HexFormat.of().formatHex(dataChannelBinaryMessage.data));
        }

        messageListener.onDataChannelMessage(message);
    }

    /**
     * Sends data through the sender.
     */
    protected int sendData(ByteBuffer data, int sid, int ppid)
    {
        return dataChannelDataSender.send(data, sid, ppid);
    }

    /**
     * Sends a string through this data channel.
     * @param message the string to send.
     */
    public void sendString(String message)
    {
        LOGGER.debug("Sending string data channel message: '{}'", message);
        DataChannelStringMessage stringMessage = new DataChannelStringMessage(message);
        dataChannelDataSender.send(stringMessage.getBuffer(), sid, DataChannelProtocolConstants.WEBRTC_PPID_STRING);
    }
}