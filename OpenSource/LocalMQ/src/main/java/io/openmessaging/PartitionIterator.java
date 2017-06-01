package io.openmessaging;

import io.openmessaging.exception.OMSRuntimeException;

/**
 * A {@code PartitionIterator} over a partition of queue, created by
 * {@link PartitionConsumer#partitionIterator(String)}, supports consume
 * messages bilaterally.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 * @version OMS 1.0
 * @since OMS 1.0
 */
public interface PartitionIterator {
    /**
     * Fetches the current offset of this partition iterator.
     *
     * @return the current offset, return -1 if the iterator is first created.
     */
    long currentOffset();

    /**
     * Fetches the first offset of this partition iterator.
     *
     * @return the first offset, return -1 if the partition has no message.
     */
    long firstOffset();

    /**
     * Fetches the last offset of this partition iterator.
     *
     * @return the last offset, return 0 if the iterator is first created.
     */
    long lastOffset();

    /**
     * Moves the current offset to the specified timestamp.
     * <p>
     * Moves the current offset to the first offset, if the given timestamp
     * is earlier than the first message's store timestamp in this partition iterator.
     * <p>
     * Moves the current offset to the last offset, if the given timestamp
     * is later than the last message's store timestamp in this partition iterator.
     *
     * @param timestamp the specified timestamp
     */
    void seekByTime(long timestamp);

    /**
     * Moves the current offset to the specified offset.
     *
     * @param offset the specified offset
     */
    void seekByOffset(long offset);

    /**
     * Persist this iterator to local or remote server, that depends on specified
     * implementation of {@link PartitionConsumer}.
     */
    void persist();

    /**
     * Returns {@code true} if this partition iterator has more messages when
     * traversing the iterator in the forward direction.
     *
     * @return {@code true} if the partition iterator has more messages when
     *         traversing the iterator in the forward direction
     */
    boolean hasNext();

    /**
     * Returns the next message in the iteration and advances the offset position.
     * <p>
     * This method may be called repeatedly to iterate through the iteration,
     * or intermixed with calls to {@link #previous} to go back and forth.
     *
     * @return the next message in the list
     * @throws OMSRuntimeException if the iteration has no more message
     */
    Message next();

    /**
     * Returns {@code true} if this partition iterator has more messages when
     * traversing the iterator in the reverse direction.
     *
     * @return {@code true} if the partition iterator has more messages when
     *         traversing the iterator in the reverse direction
     */
    boolean hasPrevious();

    /**
     * Returns the previous message in the iteration and moves the offset
     * position backwards.
     * <p>
     * This method may be called repeatedly to iterate through the iteration backwards,
     * or intermixed with calls to {@link #next} to go back and forth.
     *
     * @return the previous message in the list
     * @throws OMSRuntimeException if the iteration has no previous message
     */
    Message previous();
}
