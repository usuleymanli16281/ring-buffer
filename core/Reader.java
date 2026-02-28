package core;

public class Reader<T> {
    private final RingBuffer<T> buffer;
    private long nextSequence;
    private long missedCount;

    Reader(RingBuffer<T> buffer) {
        this.buffer = buffer;
        this.nextSequence = buffer.oldestAvailableSequence();
        this.missedCount = 0;
    }

    public T read() {
        long oldest = buffer.oldestAvailableSequence();
        if (nextSequence < oldest) {
            missedCount += (oldest - nextSequence);
            nextSequence = oldest;
        }

        long writeSeq = buffer.currentWriteSequence();
        if (nextSequence >= writeSeq) return null;

        T item = buffer.getAt(nextSequence);
        nextSequence++;
        return item;
    }

    public long getMissedCount() {
        return missedCount;
    }

    public long getNextSequence() {
        return nextSequence;
    }
}