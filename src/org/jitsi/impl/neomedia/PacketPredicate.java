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

package org.jitsi.impl.neomedia;

/**
 * Represents a predicate (boolean-valued function) of a <tt>RawPacket</tt>.
 *
 * The idea is to have <tt>PacketTransformer</tt> implementations strictly
 * associated with a <tt>PacketPredicate</tt> so that they only process packets
 * that they're supposed to process. For example, transformers that transform
 * RTP packets should not transform RTCP packets, if, by mistake, they happen
 * to be passed RTCP packets.
 *
 * @author George Politis
 */
public interface PacketPredicate
{
    /**
     * Evaluates the predicate on the given <tt>RawPacket</tt>.
     */
    boolean test(RawPacket pkt);
}