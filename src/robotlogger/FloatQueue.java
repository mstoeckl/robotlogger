/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robotlogger;

import java.util.Iterator;

/**
 * Structure:
 *
 * Each segment contains:
 *
 * * 2-40K float[] (fixed size)
 *
 * * ~1-5K relative index block (grows (doubling)), an unsigned byte[]
 *
 * * pointers to next and previous blocks.
 *
 * The list itself is a linked list of segments.
 *
 * The goal is to have fast _iteration_/locality, and avoiding the annoying
 * almost 2x array overhead. We do not need random access, but linked lists are
 * way overkill for 100K elements
 *
 * A FloatQueue holds only floats.
 *
 * Short term, this may not exactly fill the above requirements.
 *
 */
public class FloatQueue implements Iterable<float[]> {

    public static final String TSV_SEPERATOR = "[ \t,;:]*";

    private float[][] store;
    private int tail;
    private int width;

    public FloatQueue() {
        store = new float[50][];
        tail = 0;
        width = 0;
    }

    /**
     * Returns true if the bunch was successful; any failures return false
     *
     * @param s
     * @return
     */
    public boolean add(String s) {
        String[] x = s.split(TSV_SEPERATOR);
        float[] values = new float[x.length];

        for (int j = 0; j < x.length; j++) {
            try {
                values[j] = Float.parseFloat(x[j]);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        tail++;
        if (tail == store.length) {
            float[][] t = store;
            store = new float[store.length * 2][];
            System.arraycopy(t, 0, store, 0, tail);
        }

        store[tail] = values;
        if (values.length > width) {
            width = values.length;
        }

        return true;
    }

    public int width() {
        return width;
    }

    private static class FloatIterator implements Iterator<float[]> {

        private final FloatQueue under;
        private int location;

        public FloatIterator(FloatQueue source) {
            under = source;
            location = under.tail;
        }

        @Override
        public boolean hasNext() {
            return location > 0;
        }

        @Override
        public float[] next() {
            location--;
            return under.store[location];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Hell no.");
        }
    }

    // key point: this iterates backwards
    @Override
    public Iterator<float[]> iterator() {
        return new FloatIterator(this);
    }

}
