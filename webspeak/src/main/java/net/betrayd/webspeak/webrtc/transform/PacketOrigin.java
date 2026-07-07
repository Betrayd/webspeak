package net.betrayd.webspeak.webrtc.transform;
/**
 * The origin of a packet in the system; used to track outgoing packets in [OutgoingStatisticsTracker]
 * to measure the bitrates of each type of data.
 *
 * Currently only used for RTP, so RTCP, SCTP, and Datachannel will all be either Routed or Misc.
 */
public enum PacketOrigin {
    Routed,
    Retransmission,
    Probing,
    Padding,
    Synthesized,
    Misc
    /* TODO: Add RTCP, SCTP, and datachannel if needed */
}