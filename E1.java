/**  E1 demultiplexer, revision 2
  *  Created reference implementation
  *  Added test and measurement code
  */

import java.util.Random;

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

    static void measure (Demux demux)
    {
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

    public static void main (String [] args) 
    {
        measure (new Reference ());
    }
}
