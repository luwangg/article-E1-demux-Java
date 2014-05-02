/**  E1 demultiplexer, revision 7
  *  Created reference implementation
  *  Added test and measurement code
  *  Added correctness test
  *
  *  Started source-first family of solutions:
  *    Added Src_First_1: Reference with refactored inner loop
  *    Added Src_First_2: Multiplication in the inner loop
  *    Fixed a bug in Src_First_2: double increment of an outer loop variable
  *    Added Src_First_3: Loop along source with division and modulo operations
  *
  *  Started destination-first family of solutions:
  *    Added Dst_First_1: Similar to Src_First_2 but different order of loops
  */

import java.util.Random;
import java.util.Arrays;

public final class E1
{
    public static final int NUM_TIMESLOTS = 32;
    public static final int DST_SIZE = 64;
    public static final int SRC_SIZE = NUM_TIMESLOTS * DST_SIZE;

    public static final int ITERATIONS = 1000000;
    public static final int REPETITIONS = 5;

    interface Demux
    {
        public void demux (byte[] src, byte[][] dst);
    }

    static byte[] generate ()
    {
        byte [] buf = new byte [SRC_SIZE];
        Random r = new Random (0);
        r.nextBytes (buf);
        return buf;
    }
    
    static byte[][] allocate_dst ()
    {
        return new byte [NUM_TIMESLOTS][DST_SIZE];
    }

    static void check (Demux demux)
    {
        byte[] src = generate ();
        byte[][] dst0 = allocate_dst ();
        byte[][] dst = allocate_dst ();
        new Reference ().demux (src, dst0);
        demux.demux (src, dst);
        for (int i = 0; i < NUM_TIMESLOTS; i++) {
            if (! Arrays.equals (dst0[i], dst[i])) {
                throw new java.lang.RuntimeException ("Results not equal");
            }
        }
    }

    static void measure (Demux demux)
    {
        check (demux);

        byte[] src = generate ();
        byte[][] dst = allocate_dst ();

        System.out.print (demux.getClass ().getCanonicalName () + ":");
     
        for (int loop = 0; loop < REPETITIONS; loop ++) {
            long t0 = System.currentTimeMillis ();
            for (int i = 0; i < ITERATIONS; i++) {
                demux.demux (src, dst);
            }
            long t = System.currentTimeMillis () - t0;
            System.out.print (" " + t);
        }
        System.out.println ();
    }

    static final class Reference implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert src.length % NUM_TIMESLOTS == 0;

            int dst_pos = 0;
            int dst_num = 0;
            for (byte b : src) {
                dst [dst_num][dst_pos] = b;
                if (++ dst_num == NUM_TIMESLOTS) {
                    dst_num = 0;
                    ++ dst_pos;
                }
            }
        }
    }

    static final class Src_First_1 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert src.length % NUM_TIMESLOTS == 0;

            int src_pos = 0;
            int dst_pos = 0;
            while (src_pos < src.length) {
                for (int dst_num = 0; dst_num < NUM_TIMESLOTS; ++ dst_num) {
                    dst [dst_num][dst_pos] = src [src_pos ++];
                }
                ++ dst_pos;
            }
        }
    }

    static final class Src_First_2 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert src.length % NUM_TIMESLOTS == 0;
            
            for (int dst_pos = 0; dst_pos < src.length / NUM_TIMESLOTS; ++ dst_pos) {
                for (int dst_num = 0; dst_num < NUM_TIMESLOTS; ++ dst_num) {
                    dst [dst_num][dst_pos] = src [dst_pos * NUM_TIMESLOTS + dst_num];
                }
            }
        }
    }

    static final class Src_First_3 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert src.length % NUM_TIMESLOTS == 0;

            for (int i = 0; i < src.length; i++) {
                dst [i % NUM_TIMESLOTS][i / NUM_TIMESLOTS] = src [i];
            }
        }
    }

    static final class Dst_First_1 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert src.length % NUM_TIMESLOTS == 0;

            for (int dst_num = 0; dst_num < NUM_TIMESLOTS; ++ dst_num) {
                for (int dst_pos = 0; dst_pos < src.length / NUM_TIMESLOTS; ++ dst_pos) {
                    dst [dst_num][dst_pos] = src [dst_pos * NUM_TIMESLOTS + dst_num];
                }
            }
        }
    }

    public static void main (String [] args) 
    {
//      measure (new Reference ());
//      measure (new Src_First_1 ());
//      measure (new Src_First_2 ());
//      measure (new Src_First_3 ());
        measure (new Dst_First_1 ());
    }
}
