package core;

public class Writer<T> {
    private final RingBuffer<T> buffer;

    Writer(RingBuffer<T> buffer) {
        this.buffer = buffer;
    }

    public void write(T item) {
        long sequence = buffer.currentWriteSequence();
        buffer.putAt(sequence, item);
        buffer.advanceWriteSequence();
    }
}