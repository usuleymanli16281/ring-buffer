package core;

public class Main {
    public static void main(String[] args) {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);

        Writer<Integer> writer = buffer.createWriter();

        Reader<Integer> r1 = buffer.createReader();
        Reader<Integer> r2 = buffer.createReader();

        writer.write(20);
        writer.write(40);
        writer.write(60);

        System.out.println("r1: " + r1.read());
        System.out.println("r2: " + r2.read());

        writer.write(80);
        writer.write(100);

        System.out.println("r1: " + r1.read());
        System.out.println("r1: " + r1.read());

        System.out.println("r2: " + r2.read());
        System.out.println("r2 missed: " + r2.getMissedCount());

        System.out.println("r1 (no new data): " + r1.read());

        try {
            buffer.createWriter();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        System.out.println("reader count: " + buffer.getReaderCount());
    }
}