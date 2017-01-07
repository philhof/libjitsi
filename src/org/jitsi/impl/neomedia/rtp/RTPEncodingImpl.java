/*
 * Copyright @ 2015 Atlassian Pty Ltd
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
package org.jitsi.impl.neomedia.rtp;

import org.ice4j.util.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;
import org.jitsi.util.Logger;

import java.util.*;

/**
 * Keeps track of how many channels receive it, its subjective quality index,
 * its last stable bitrate and other useful things for adaptivity/routing.
 *
 * @author George Politis
 */
public class RTPEncodingImpl
    implements RTPEncoding
{
    /**
     * The {@link Logger} used by the {@link RTPEncodingImpl} class to print
     * debug information.
     */
    private static final Logger logger
        = Logger.getLogger(RTPEncodingImpl.class);

    /**
     * The default window size in ms for the bitrate estimation.
     *
     * TODO maybe make this configurable.
     */
    private static final int AVERAGE_BITRATE_WINDOW_MS = 5000;

    /**
     * The primary SSRC for this layering/encoding.
     */
    private final long primarySSRC;

    /**
     * The RTX SSRC for this layering/encoding.
     */
    private final long rtxSSRC;

    /**
     * The index of this instance in the track encodings array.
     */
    private final int idx;

    /**
     * The temporal ID of this instance.
     */
    private final int temporalId;

    /**
     * The {@link MediaStreamTrackImpl} that this {@link RTPEncodingImpl}
     * belongs to.
     */
    private final MediaStreamTrackImpl track;

    /**
     * The {@link RateStatistics} instance used to calculate the receiving
     * bitrate of this RTP encoding.
     */
    private final RateStatistics rateStatistics
        = new RateStatistics(AVERAGE_BITRATE_WINDOW_MS);

    /**
     * The {@link TreeMap} that holds the seen {@link FrameDesc}, keyed
     * by their RTP timestamps.
     */
    private final TreeMap<Long, FrameDesc> frames
        = new TreeMap<Long, FrameDesc>()
    {
        /**
         * A helper {@link LinkedList} that is used to cleanup the map.
         */
        private LinkedList<Long> tsl = new LinkedList<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public FrameDesc put(Long key, FrameDesc value)
        {
            FrameDesc previous = super.put(key, value);
            if (tsl.add(key) && tsl.size() > 300)
            {
                Long first = tsl.removeFirst();
                this.remove(first);
            }

            return previous;
        }
    };

    /**
     * The {@link RTPEncodingImpl} on which this layer depends.
     */
    private final RTPEncodingImpl[] dependencyEncodings;

    /**
     * A boolean flag that indicates whether or not this instance is streaming
     * or if it's suspended.
     */
    private boolean active = false;

    /**
     * The last stable bitrate (in bps) for this instance.
     */
    private long lastStableBitrateBps;

    /**
     * Ctor.
     *
     * @param track the {@link MediaStreamTrack} that this instance belongs to.
     * @param primarySSRC The primary SSRC for this layering/encoding.
     */
    public RTPEncodingImpl(MediaStreamTrackImpl track, long primarySSRC)
    {
        this(track, primarySSRC, -1 /* rtxSSRC */);
    }

    /**
     * Ctor.
     *
     * @param track the {@link MediaStreamTrack} that this instance belongs to.
     * @param primarySSRC The primary SSRC for this layering/encoding.
     * @param rtxSSRC The RTX SSRC for this layering/encoding.
     */
    public RTPEncodingImpl(
        MediaStreamTrackImpl track, long primarySSRC, long rtxSSRC)
    {
        this(track, 0, primarySSRC, rtxSSRC,
            -1 /* temporalId */, null /* dependencies */);
    }

    /**
     * Ctor.
     *
     * @param track the {@link MediaStreamTrack} that this instance belongs to.
     * @param idx the subjective quality index for this
     * layering/encoding.
     * @param primarySSRC The primary SSRC for this layering/encoding.
     * @param rtxSSRC The RTX SSRC for this layering/encoding.
     * @param temporalId temporal layer ID for this layering/encoding.
     * @param dependencyEncodings  The {@link RTPEncodingImpl} on which this
     * layer depends.
     */
    public RTPEncodingImpl(
        MediaStreamTrackImpl track, int idx,
        long primarySSRC, long rtxSSRC,
        int temporalId,
        RTPEncodingImpl[] dependencyEncodings)
    {
        this.primarySSRC = primarySSRC;
        this.rtxSSRC = rtxSSRC;
        this.track = track;
        this.idx = idx;
        this.temporalId = temporalId;
        this.dependencyEncodings = dependencyEncodings;
    }

    /**
     * Applies frame boundaries heuristics to frames a and b, assuming a
     * predates/is older than b.
     *
     * @param a the old {@link FrameDesc}.
     * @param b the new {@link FrameDesc}
     */
    private static void applyFrameBoundsHeuristics(FrameDesc a, FrameDesc b)
    {
        int end = a.getEnd(), start = b.getStart();
        if (end != -1 && start != -1)
        {
            // No need for heuristics.
            return;
        }

        long tsDiff = (b.getTimestamp() - a.getTimestamp()) & 0xFFFFFFFFL;
        if (tsDiff > (1L << 30) && tsDiff < (-(1L << 30) & 0xFFFFFFFFL))
        {
            // the distance (mod 32) between the two timestamps needs to be
            // less than half the timestamp space.
            return;
        }
        else if (tsDiff >= (-(1L << 30) & 0xFFFFFFFFL))
        {
            logger.warn("Frames that are out of order detected.");
        }
        else
        {
            int min = b.getMinSeen(), max = a.getMaxSeen();
            int snDiff = (max - min) & 0xFFFF;

            if (start != -1 || end != -1)
            {
                if (snDiff == 2)
                {
                    if (end == -1)
                    {
                        a.setEnd((max + 1) & 0xFFFF);
                    }
                    else
                    {
                        b.setStart((min - 1) & 0xFFFF);
                    }
                }
                else if (snDiff < 2 || snDiff > (-3 & 0xFFFF))
                {
                    logger.warn("Frame corruption or packets that are out of " +
                        "order detected.");
                }
            }
            else
            {
                if (snDiff == 3)
                {
                    a.setEnd((max + 1) & 0xFFFF);
                    b.setStart((min - 1) & 0xFFFF);
                }
                else if (snDiff < 3 || snDiff > (-4 & 0xFFFF))
                {
                    logger.warn("Frame corruption or packets that are out of" +
                        " order detected.");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPrimarySSRC()
    {
        return primarySSRC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRTXSSRC()
    {
        return rtxSSRC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive()
    {
        return active;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "subjective_quality=" + idx +
            ",primary_ssrc=" + getPrimarySSRC() +
            ",rtx_ssrc=" + getRTXSSRC() +
            ",temporal_id=" + temporalId +
            ",active=" + active +
            ",last_stable_bitrate_bps=" + lastStableBitrateBps;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaStreamTrackImpl getMediaStreamTrack()
    {
        return track;
    }

    /**
     * Gets the subjective quality index of this instance.
     *
     * @return the subjective quality index of this instance.
     */
    int getIndex()
    {
        return idx;
    }

    /**
     * Returns a boolean that indicates whether or not this
     * {@link RTPEncodingImpl} depends on the subjective quality index that is
     * passed as an argument.
     *
     * @param idx the index of this instance in the track encodings array.
     * @return true if this {@link RTPEncodingImpl} depends on the subjective
     * quality index that is passed as an argument, false otherwise.
     */
    boolean requires(int idx)
    {
        if (idx < 0)
        {
            return false;
        }

        if (idx == this.idx)
        {
            return true;
        }


        boolean requires = false;

        if (!ArrayUtils.isNullOrEmpty(dependencyEncodings))
        {
            for (RTPEncodingImpl enc : dependencyEncodings)
            {
                if (enc.requires(idx))
                {
                    requires = true;
                    break;
                }
            }
        }

        return requires;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(byte[] buf, int off, int len)
    {
        long ssrc = RawPacket.getSSRCAsLong(buf, off, len);

        if (primarySSRC != ssrc && rtxSSRC != ssrc)
        {
            return false;
        }

        if (temporalId == -1)
        {
            return true;
        }

        int tid = track.getMediaStreamTrackReceiver()
            .getStream().getTemporalID(buf, off, len);

        return tid == -1 && idx == 0 || tid == temporalId;
    }

    /**
     * Gets a boolean flag that indicates whether or not this instance is
     * streaming or if it's suspended.
     *
     * @param active true if this {@link RTPEncodingImpl} is active, otherwise
     * false.
     */
    void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     *
     * @param pkt
     * @param nowMs
     *
     * @return the {@link FrameDesc} that was updated, otherwise null.
     */
    FrameDesc update(RawPacket pkt, long nowMs)
    {
        // Update rate stats.
        rateStatistics.update(pkt.getLength(), nowMs);

        long ts = pkt.getTimestamp();
        FrameDesc frame = frames.get(ts);

        if (frame == null)
        {
            synchronized (frames)
            {
                frames.put(ts, frame = new FrameDesc(this, ts));
            }

            // We measure the stable bitrate on every new frame.
            lastStableBitrateBps = getBitrateBps(nowMs);
        }

        // Update the frame description.
        boolean frameChanged = frame.update(pkt);
        if (frameChanged)
        {
            // Frame boundaries heuristics.

            // Find the closest next frame.
            Map.Entry<Long, FrameDesc> ceilingEntry
                = frames.ceilingEntry((ts + 1) & 0xFFFFFFFFL);

            if (ceilingEntry != null)
            {
                applyFrameBoundsHeuristics(frame, ceilingEntry.getValue());
            }

            // Find the closest previous frame.
            Map.Entry<Long, FrameDesc> floorEntry
                = frames.floorEntry((ts - 1) & 0xFFFFFFFFL);

            if (floorEntry != null)
            {
                applyFrameBoundsHeuristics(floorEntry.getValue(), frame);
            }
        }

        return frameChanged ? frame : null;
    }


    /**
     * Gets the cumulative bitrate (in bps) of this {@link RTPEncodingImpl} and
     * its dependencies.
     *
     * @param nowMs
     * @return the cumulative bitrate (in bps) of this {@link RTPEncodingImpl}
     * and its dependencies.
     */
    private long getBitrateBps(long nowMs)
    {
        RTPEncodingImpl[] encodings = track.getRTPEncodings();
        if (ArrayUtils.isNullOrEmpty(encodings))
        {
            return 0;
        }

        long[] rates = new long[encodings.length];
        getBitrateBps(nowMs, rates);

        long bitrate = 0;
        for (int i = 0; i < rates.length; i++)
        {
            bitrate += rates[i];
        }

        return bitrate;
    }

    /**
     * Recursively adds the bitrate (in bps) of this {@link RTPEncodingImpl} and
     * its dependencies in the array passed in as an argument.
     *
     * @param nowMs
     */
    private void getBitrateBps(long nowMs, long[] rates)
    {
        if (rates[idx] == 0)
        {
            rates[idx] = rateStatistics.getRate(nowMs);
        }

        if (!ArrayUtils.isNullOrEmpty(dependencyEncodings))
        {
            for (RTPEncodingImpl dependency : dependencyEncodings)
            {
                dependency.getBitrateBps(nowMs, rates);
            }
        }
    }

    /**
     * Finds the {@link FrameDesc} that matches the RTP packet specified
     * in the buffer passed in as an argument.
     *
     * @param buf the <tt>byte</tt> array that contains the RTP packet data.
     * @param off the offset in <tt>buf</tt> at which the actual data starts.
     * @param len the number of <tt>byte</tt>s in <tt>buf</tt> which
     * constitute the actual data.
     *
     * @return the {@link FrameDesc} that matches the RTP packet specified
     * in the buffer passed in as a parameter, or null if there is no matching
     * {@link FrameDesc}.
     */
    public FrameDesc resolveFrameDesc(byte[] buf, int off, int len)
    {
        long ts = RawPacket.getTimestamp(buf, off, len);
        synchronized (frames)
        {
            return frames.get(ts);
        }
    }
}
