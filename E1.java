/**  E1 demultiplexer, revision 10
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
  *    Added Dst_First_2: Dst_First_1 manually optimised
  *    Added Dst_First_3: Dst_First_1 with hard-coded array size
  *
  *  Started unrolled family (based on Dst_First_1)
  *    Added Unrolled_1:      Inner loop unrolled fully
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

    static final class Dst_First_2 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert src.length % NUM_TIMESLOTS == 0;

            int dst_size = src.length / NUM_TIMESLOTS;
            for (int dst_num = 0; dst_num < NUM_TIMESLOTS; ++ dst_num) {
                byte [] d = dst [dst_num];
                int src_pos = dst_num;
                for (int dst_pos = 0; dst_pos < dst_size; ++ dst_pos) {
                    d[dst_pos] = src[src_pos];
                    src_pos += NUM_TIMESLOTS;
                }
            }
        }
    }

    static final class Dst_First_3 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert src.length == NUM_TIMESLOTS * DST_SIZE;

            for (int dst_num = 0; dst_num < NUM_TIMESLOTS; ++ dst_num) {
                for (int dst_pos = 0; dst_pos < DST_SIZE; ++ dst_pos) {
                    dst [dst_num][dst_pos] = src [dst_pos * NUM_TIMESLOTS + dst_num];
                }
            }
        }
    }

    static final class Unrolled_1 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert NUM_TIMESLOTS == 32;
            assert DST_SIZE == 64;
            assert src.length == NUM_TIMESLOTS * DST_SIZE;

            for (int j = 0; j < NUM_TIMESLOTS; j++) {
                final byte[] d = dst[j];
                d[ 0] = src[j+32* 0]; d[ 1] = src[j+32* 1];
                d[ 2] = src[j+32* 2]; d[ 3] = src[j+32* 3];
                d[ 4] = src[j+32* 4]; d[ 5] = src[j+32* 5];
                d[ 6] = src[j+32* 6]; d[ 7] = src[j+32* 7];
                d[ 8] = src[j+32* 8]; d[ 9] = src[j+32* 9];
                d[10] = src[j+32*10]; d[11] = src[j+32*11];
                d[12] = src[j+32*12]; d[13] = src[j+32*13];
                d[14] = src[j+32*14]; d[15] = src[j+32*15];
                d[16] = src[j+32*16]; d[17] = src[j+32*17];
                d[18] = src[j+32*18]; d[19] = src[j+32*19];
                d[20] = src[j+32*20]; d[21] = src[j+32*21];
                d[22] = src[j+32*22]; d[23] = src[j+32*23];
                d[24] = src[j+32*24]; d[25] = src[j+32*25];
                d[26] = src[j+32*26]; d[27] = src[j+32*27];
                d[28] = src[j+32*28]; d[29] = src[j+32*29];
                d[30] = src[j+32*30]; d[31] = src[j+32*31];
                d[32] = src[j+32*32]; d[33] = src[j+32*33];
                d[34] = src[j+32*34]; d[35] = src[j+32*35];
                d[36] = src[j+32*36]; d[37] = src[j+32*37];
                d[38] = src[j+32*38]; d[39] = src[j+32*39];
                d[40] = src[j+32*40]; d[41] = src[j+32*41];
                d[42] = src[j+32*42]; d[43] = src[j+32*43];
                d[44] = src[j+32*44]; d[45] = src[j+32*45];
                d[46] = src[j+32*46]; d[47] = src[j+32*47];
                d[48] = src[j+32*48]; d[49] = src[j+32*49];
                d[50] = src[j+32*50]; d[51] = src[j+32*51];
                d[52] = src[j+32*52]; d[53] = src[j+32*53];
                d[54] = src[j+32*54]; d[55] = src[j+32*55];
                d[56] = src[j+32*56]; d[57] = src[j+32*57];
                d[58] = src[j+32*58]; d[59] = src[j+32*59];
                d[60] = src[j+32*60]; d[61] = src[j+32*61];
                d[62] = src[j+32*62]; d[63] = src[j+32*63];
            }
        }
    }

    public static void main (String [] args) 
    {
//      measure (new Reference ());
//      measure (new Src_First_1 ());
//      measure (new Src_First_2 ());
//      measure (new Src_First_3 ());
//      measure (new Dst_First_1 ());
//      measure (new Dst_First_2 ());
//      measure (new Dst_First_3 ());
        measure (new Unrolled_1 ());
    }
}
