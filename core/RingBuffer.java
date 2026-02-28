package core;

import java.util.ArrayList;
import java.util.List;

public class RingBuffer<T> {
    private final List<T> data;
    private final int capacity;
    private long writeSequence;

    private Writer<T> writer;
    private final List<Reader<T>> readers;

    public RingBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");

        this.capacity = capacity;
        this.data = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) data.add(null);

        this.writeSequence = 0;
        this.writer = null;
        this.readers = new ArrayList<>();
    }

    public synchronized Writer<T> createWriter() {
        if (writer != null) throw new IllegalStateException("Writer already exists");
        writer = new Writer<>(this);
        return writer;
    }

    public synchronized Reader<T> createReader() {
        Reader<T> reader = new Reader<>(this);
        readers.add(reader);
        return reader;
    }

    public synchronized int getReaderCount() {
        return readers.size();
    }

    synchronized long currentWriteSequence() {
        return writeSequence;
    }

    synchronized long oldestAvailableSequence() {
        long oldest = writeSequence - capacity;
        return Math.max(0, oldest);
    }

    synchronized void putAt(long sequence, T item) {
        int index = (int) (sequence % capacity);
        data.set(index, item);
    }

    synchronized T getAt(long sequence) {
        int index = (int) (sequence % capacity);
        return data.get(index);
    }

    synchronized void advanceWriteSequence() {
        writeSequence++;
    }
}