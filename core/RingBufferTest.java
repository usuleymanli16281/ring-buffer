package core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ring Buffer – Comprehensive Unit Tests")
class RingBufferTest {

    @Nested
    @DisplayName("1. RingBuffer construction")
    class Construction {

        @Test
        @DisplayName("Valid capacity creates buffer without exception")
        void validCapacityCreatesBuffer() {
            assertDoesNotThrow(() -> new RingBuffer<Integer>(5));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 10, 100})
        @DisplayName("getReaderCount() returns 0 immediately after construction")
        void initialReaderCountIsZero(int capacity) {
            RingBuffer<Integer> buf = new RingBuffer<>(capacity);
            assertEquals(0, buf.getReaderCount());
        }

        @Test
        @DisplayName("Capacity 0 throws IllegalArgumentException")
        void zeroCapacityThrows() {
            assertThrows(IllegalArgumentException.class, () -> new RingBuffer<Integer>(0));
        }

        @Test
        @DisplayName("Negative capacity throws IllegalArgumentException")
        void negativeCapacityThrows() {
            assertThrows(IllegalArgumentException.class, () -> new RingBuffer<Integer>(-3));
        }
    }

    @Nested
    @DisplayName("2. Single-writer rule")
    class SingleWriterRule {

        @Test
        @DisplayName("First createWriter() returns a non-null Writer")
        void firstCreateWriterSucceeds() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Writer<Integer> w = assertDoesNotThrow(buf::createWriter);
            assertNotNull(w);
        }

        @Test
        @DisplayName("Second createWriter() throws IllegalStateException")
        void secondCreateWriterThrows() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            buf.createWriter();
            assertThrows(IllegalStateException.class, buf::createWriter);
        }

        @Test
        @DisplayName("Exception message from second createWriter() is not blank")
        void secondWriterExceptionMessage() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            buf.createWriter();
            IllegalStateException ex = assertThrows(IllegalStateException.class, buf::createWriter);
            assertNotNull(ex.getMessage());
            assertFalse(ex.getMessage().isBlank());
        }
    }

    @Nested
    @DisplayName("3. Reader creation and getReaderCount")
    class ReaderCreation {

        @Test
        @DisplayName("createReader() returns a non-null Reader")
        void createReaderReturnsNonNull() {
            assertNotNull(new RingBuffer<Integer>(4).createReader());
        }

        @Test
        @DisplayName("getReaderCount() increments with every createReader() call")
        void readerCountIncrements() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            assertEquals(0, buf.getReaderCount());
            buf.createReader(); assertEquals(1, buf.getReaderCount());
            buf.createReader(); assertEquals(2, buf.getReaderCount());
            buf.createReader(); assertEquals(3, buf.getReaderCount());
        }

        @Test
        @DisplayName("Two createReader() calls return distinct Reader objects")
        void readersAreDistinctObjects() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            assertNotSame(buf.createReader(), buf.createReader());
        }
    }

    @Nested
    @DisplayName("4. Basic write and read")
    class BasicWriteRead {

        @Test
        @DisplayName("Single write then read returns the written value")
        void singleWriteThenRead() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            w.write(42);
            assertEquals(42, r.read());
        }

        @Test
        @DisplayName("Multiple writes are read back in FIFO order")
        void multipleWritesReadInOrder() {
            RingBuffer<Integer> buf = new RingBuffer<>(8);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 1; i <= 5; i++) w.write(i);
            for (int i = 1; i <= 5; i++) assertEquals(i, r.read());
        }

        @Test
        @DisplayName("String elements round-trip correctly")
        void stringElements() {
            RingBuffer<String> buf = new RingBuffer<>(4);
            Writer<String> w = buf.createWriter();
            Reader<String> r = buf.createReader();
            w.write("hello");
            w.write("world");
            assertEquals("hello", r.read());
            assertEquals("world", r.read());
        }

        @Test
        @DisplayName("Exactly capacity elements written and fully read without data loss")
        void exactCapacityRoundTrip() {
            int cap = 5;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap; i++) w.write(i);
            for (int i = 0; i < cap; i++) assertEquals(i, r.read());
        }

        @Test
        @DisplayName("Reader created before any writes sees all subsequent writes")
        void readerCreatedBeforeWrites() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            w.write(7);
            w.write(8);
            assertEquals(7, r.read());
            assertEquals(8, r.read());
        }
    }

    @Nested
    @DisplayName("5. Null when no new data available")
    class NullWhenEmpty {

        @Test
        @DisplayName("read() returns null on a brand-new buffer with no writes")
        void readFromEmptyBufferReturnsNull() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            assertNull(buf.createReader().read());
        }

        @Test
        @DisplayName("read() returns null after all written items are consumed")
        void readAfterAllConsumedReturnsNull() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            w.write(1); w.write(2);
            r.read(); r.read();
            assertNull(r.read());
        }

        @Test
        @DisplayName("Repeated reads on an empty buffer keep returning null")
        void repeatedNullReads() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < 5; i++) assertNull(r.read());
        }

        @Test
        @DisplayName("read() returns null once reader catches up to writer mid-stream")
        void readerCatchesUpMidStream() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            w.write(99);
            r.read();
            assertNull(r.read());
        }
    }

    @Nested
    @DisplayName("6. Overwrite / wrap-around behavior")
    class OverwriteBehavior {

        @Test
        @DisplayName("capacity+1 writes: first item overwritten, reader sees item at index 1")
        void capacityPlusOneOverwritesOldest() {
            int cap = 3;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i <= cap; i++) w.write(i);
            assertEquals(1, r.read());
        }

        @Test
        @DisplayName("oldestAvailable = writeSequence - capacity after overflow")
        void oldestSequenceFormula() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap + 2; i++) w.write(i);
            assertEquals(2, r.read());
        }

        @Test
        @DisplayName("Buffer wraps correctly across multiple full cycles with in-sync reader")
        void multipleWrapAroundsInSync() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap * 3; i++) {
                w.write(i);
                assertEquals(i, r.read());
            }
        }

        @Test
        @DisplayName("capacity=1: second write overwrites first; reader sees second value")
        void capacityOneOverwrite() {
            RingBuffer<Integer> buf = new RingBuffer<>(1);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            w.write(1);
            w.write(2);
            assertEquals(2, r.read());
        }
    }

    @Nested
    @DisplayName("7. Slow reader – getMissedCount")
    class SlowReader {

        @Test
        @DisplayName("getMissedCount() is 0 when reader keeps pace with writer")
        void noMissedItemsWhenReaderKeepsUp() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap; i++) { w.write(i); r.read(); }
            assertEquals(0, r.getMissedCount());
        }

        @Test
        @DisplayName("getMissedCount() is positive after slow reader is lapped")
        void missedCountPositiveAfterLag() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap * 2; i++) w.write(i);
            r.read();
            assertTrue(r.getMissedCount() > 0);
        }

        @Test
        @DisplayName("missedCount equals number of overwritten items")
        void missedCountEqualsOverwrittenItems() {
            int cap = 4;
            int overflow = 3;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap + overflow; i++) w.write(i);
            r.read();
            assertEquals(overflow, r.getMissedCount());
        }

        @Test
        @DisplayName("Slow reader is fast-forwarded to oldestAvailableSequence")
        void slowReaderLandsAtOldestAvailable() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap + 2; i++) w.write(i);
            assertEquals(2, r.read());
        }

        @Test
        @DisplayName("getMissedCount() accumulates across multiple lag events")
        void missedCountAccumulatesAcrossMultipleLags() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap + 2; i++) w.write(i);
            r.read();
            for (int i = 0; i < cap + 1; i++) w.write(100 + i);
            r.read();
            assertTrue(r.getMissedCount() > 2);
        }
    }

    @Nested
    @DisplayName("8. Multiple independent readers")
    class MultipleReaders {

        @Test
        @DisplayName("Two readers independently consume the same data sequence")
        void twoReadersConsumeIndependently() {
            RingBuffer<Integer> buf = new RingBuffer<>(8);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r1 = buf.createReader();
            Reader<Integer> r2 = buf.createReader();
            w.write(10); w.write(20);
            assertEquals(10, r1.read()); assertEquals(20, r1.read());
            assertEquals(10, r2.read()); assertEquals(20, r2.read());
        }

        @Test
        @DisplayName("Fast reader does not advance slow reader's position")
        void fastReaderDoesNotMoveSlowReader() {
            RingBuffer<Integer> buf = new RingBuffer<>(8);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r1 = buf.createReader();
            Reader<Integer> r2 = buf.createReader();
            for (int i = 1; i <= 5; i++) w.write(i);
            for (int i = 1; i <= 5; i++) r1.read();
            assertEquals(1, r2.read());
        }

        @Test
        @DisplayName("Three readers each see null after consuming their own data")
        void threeReadersNullAfterConsumption() {
            RingBuffer<String> buf = new RingBuffer<>(4);
            Writer<String> w = buf.createWriter();
            Reader<String> r1 = buf.createReader();
            Reader<String> r2 = buf.createReader();
            Reader<String> r3 = buf.createReader();
            w.write("x");
            assertEquals("x", r1.read());
            assertEquals("x", r2.read());
            assertEquals("x", r3.read());
            assertNull(r1.read());
            assertNull(r2.read());
            assertNull(r3.read());
        }

        @Test
        @DisplayName("Two lagging readers accumulate identical missed counts")
        void twoSlowReadersHaveEqualMissedCounts() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r1 = buf.createReader();
            Reader<Integer> r2 = buf.createReader();
            for (int i = 0; i < cap * 2; i++) w.write(i);
            r1.read(); r2.read();
            assertEquals(r1.getMissedCount(), r2.getMissedCount());
            assertTrue(r1.getMissedCount() > 0);
        }

        @Test
        @DisplayName("getReaderCount() matches total createReader() calls")
        void readerCountMatchesTotalCalls() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            buf.createReader(); buf.createReader(); buf.createReader();
            assertEquals(3, buf.getReaderCount());
        }
    }

    @Nested
    @DisplayName("9. Generic type support")
    class GenericTypes {

        @Test
        @DisplayName("Buffer works with Double type")
        void doubleType() {
            RingBuffer<Double> buf = new RingBuffer<>(4);
            buf.createWriter().write(3.14);
            assertEquals(3.14, buf.createReader().read(), 1e-9);
        }

        @Test
        @DisplayName("Buffer works with Long type")
        void longType() {
            RingBuffer<Long> buf = new RingBuffer<>(4);
            Writer<Long> w = buf.createWriter();
            Reader<Long> r = buf.createReader();
            w.write(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, r.read());
        }

        @Test
        @DisplayName("Buffer works with array type")
        void arrayType() {
            RingBuffer<int[]> buf = new RingBuffer<>(4);
            Writer<int[]> w = buf.createWriter();
            Reader<int[]> r = buf.createReader();
            int[] payload = {1, 2, 3};
            w.write(payload);
            assertArrayEquals(payload, r.read());
        }
    }

    @Nested
    @DisplayName("10. Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Writing capacity-1 items: no overwrite, all readable, missedCount=0")
        void capacityMinusOneNoOverwrite() {
            int cap = 5;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap - 1; i++) w.write(i);
            for (int i = 0; i < cap - 1; i++) assertEquals(i, r.read());
            assertEquals(0, r.getMissedCount());
        }

        @Test
        @DisplayName("Writing exactly capacity items: item 0 still readable")
        void exactlyCapacityBoundary() {
            int cap = 5;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < cap; i++) w.write(i);
            assertEquals(0, r.read());
        }

        @Test
        @DisplayName("Writing capacity+1 items: item 0 gone, first read is item 1")
        void capacityPlusOneFirstItemGone() {
            int cap = 5;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i <= cap; i++) w.write(i);
            assertEquals(1, r.read());
        }

        @Test
        @DisplayName("Alternating write and read maintains correct ordering")
        void alternatingWriteRead() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            for (int i = 0; i < 20; i++) {
                w.write(i);
                assertEquals(i, r.read());
            }
        }

        @Test
        @DisplayName("Reader created after full buffer (no overflow) starts at item 0")
        void readerCreatedAfterFullBufferNoOverflow() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            for (int i = 0; i < cap; i++) w.write(i);
            Reader<Integer> r = buf.createReader();
            assertEquals(0, r.read());
        }

        @Test
        @DisplayName("Reader created after overflow starts at overwritten oldest sequence")
        void readerCreatedAfterOverflow() {
            int cap = 4;
            RingBuffer<Integer> buf = new RingBuffer<>(cap);
            Writer<Integer> w = buf.createWriter();
            for (int i = 0; i < cap + 2; i++) w.write(i);
            Reader<Integer> r = buf.createReader();
            assertEquals(2, r.read());
        }

        @Test
        @DisplayName("getNextSequence() increments by 1 after each successful read")
        void nextSequenceIncrementsOnRead() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Writer<Integer> w = buf.createWriter();
            Reader<Integer> r = buf.createReader();
            w.write(1);
            long before = r.getNextSequence();
            r.read();
            assertEquals(before + 1, r.getNextSequence());
        }

        @Test
        @DisplayName("getNextSequence() does not change on a null read")
        void nextSequenceUnchangedOnNullRead() {
            RingBuffer<Integer> buf = new RingBuffer<>(4);
            Reader<Integer> r = buf.createReader();
            long before = r.getNextSequence();
            r.read();
            assertEquals(before, r.getNextSequence());
        }
    }
}