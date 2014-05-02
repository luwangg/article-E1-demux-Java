/**  E1 demultiplexer, revision 12
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
  *    Added Unrolled_2_Full: Both loops unrolled in one huge method
  *    Added Unrolled_3:      Both loops unrolled; each iteration of the outer loop made into separate method
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
                d[ 0] = src[j+32* 0]; d[ 1] = src[j+32* 1]; d[ 2] = src[j+32* 2]; d[ 3] = src[j+32* 3];
                d[ 4] = src[j+32* 4]; d[ 5] = src[j+32* 5]; d[ 6] = src[j+32* 6]; d[ 7] = src[j+32* 7];
                d[ 8] = src[j+32* 8]; d[ 9] = src[j+32* 9]; d[10] = src[j+32*10]; d[11] = src[j+32*11];
                d[12] = src[j+32*12]; d[13] = src[j+32*13]; d[14] = src[j+32*14]; d[15] = src[j+32*15];
                d[16] = src[j+32*16]; d[17] = src[j+32*17]; d[18] = src[j+32*18]; d[19] = src[j+32*19];
                d[20] = src[j+32*20]; d[21] = src[j+32*21]; d[22] = src[j+32*22]; d[23] = src[j+32*23];
                d[24] = src[j+32*24]; d[25] = src[j+32*25]; d[26] = src[j+32*26]; d[27] = src[j+32*27];
                d[28] = src[j+32*28]; d[29] = src[j+32*29]; d[30] = src[j+32*30]; d[31] = src[j+32*31];
                d[32] = src[j+32*32]; d[33] = src[j+32*33]; d[34] = src[j+32*34]; d[35] = src[j+32*35];
                d[36] = src[j+32*36]; d[37] = src[j+32*37]; d[38] = src[j+32*38]; d[39] = src[j+32*39];
                d[40] = src[j+32*40]; d[41] = src[j+32*41]; d[42] = src[j+32*42]; d[43] = src[j+32*43];
                d[44] = src[j+32*44]; d[45] = src[j+32*45]; d[46] = src[j+32*46]; d[47] = src[j+32*47];
                d[48] = src[j+32*48]; d[49] = src[j+32*49]; d[50] = src[j+32*50]; d[51] = src[j+32*51];
                d[52] = src[j+32*52]; d[53] = src[j+32*53]; d[54] = src[j+32*54]; d[55] = src[j+32*55];
                d[56] = src[j+32*56]; d[57] = src[j+32*57]; d[58] = src[j+32*58]; d[59] = src[j+32*59];
                d[60] = src[j+32*60]; d[61] = src[j+32*61]; d[62] = src[j+32*62]; d[63] = src[j+32*63];
            }
        }
    }

    static final class Unrolled_2_Full implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert NUM_TIMESLOTS == 32;
            assert DST_SIZE == 64;
            assert src.length == NUM_TIMESLOTS * DST_SIZE;

            byte[] d;
            d = dst[0];
            d = dst[0];
            d[ 0] = src[   0]; d[ 1] = src[  32]; d[ 2] = src[  64]; d[ 3] = src[  96];
            d[ 4] = src[ 128]; d[ 5] = src[ 160]; d[ 6] = src[ 192]; d[ 7] = src[ 224];
            d[ 8] = src[ 256]; d[ 9] = src[ 288]; d[10] = src[ 320]; d[11] = src[ 352];
            d[12] = src[ 384]; d[13] = src[ 416]; d[14] = src[ 448]; d[15] = src[ 480];
            d[16] = src[ 512]; d[17] = src[ 544]; d[18] = src[ 576]; d[19] = src[ 608];
            d[20] = src[ 640]; d[21] = src[ 672]; d[22] = src[ 704]; d[23] = src[ 736];
            d[24] = src[ 768]; d[25] = src[ 800]; d[26] = src[ 832]; d[27] = src[ 864];
            d[28] = src[ 896]; d[29] = src[ 928]; d[30] = src[ 960]; d[31] = src[ 992];
            d[32] = src[1024]; d[33] = src[1056]; d[34] = src[1088]; d[35] = src[1120];
            d[36] = src[1152]; d[37] = src[1184]; d[38] = src[1216]; d[39] = src[1248];
            d[40] = src[1280]; d[41] = src[1312]; d[42] = src[1344]; d[43] = src[1376];
            d[44] = src[1408]; d[45] = src[1440]; d[46] = src[1472]; d[47] = src[1504];
            d[48] = src[1536]; d[49] = src[1568]; d[50] = src[1600]; d[51] = src[1632];
            d[52] = src[1664]; d[53] = src[1696]; d[54] = src[1728]; d[55] = src[1760];
            d[56] = src[1792]; d[57] = src[1824]; d[58] = src[1856]; d[59] = src[1888];
            d[60] = src[1920]; d[61] = src[1952]; d[62] = src[1984]; d[63] = src[2016];
            d = dst[1];
            d[ 0] = src[   1]; d[ 1] = src[  33]; d[ 2] = src[  65]; d[ 3] = src[  97];
            d[ 4] = src[ 129]; d[ 5] = src[ 161]; d[ 6] = src[ 193]; d[ 7] = src[ 225];
            d[ 8] = src[ 257]; d[ 9] = src[ 289]; d[10] = src[ 321]; d[11] = src[ 353];
            d[12] = src[ 385]; d[13] = src[ 417]; d[14] = src[ 449]; d[15] = src[ 481];
            d[16] = src[ 513]; d[17] = src[ 545]; d[18] = src[ 577]; d[19] = src[ 609];
            d[20] = src[ 641]; d[21] = src[ 673]; d[22] = src[ 705]; d[23] = src[ 737];
            d[24] = src[ 769]; d[25] = src[ 801]; d[26] = src[ 833]; d[27] = src[ 865];
            d[28] = src[ 897]; d[29] = src[ 929]; d[30] = src[ 961]; d[31] = src[ 993];
            d[32] = src[1025]; d[33] = src[1057]; d[34] = src[1089]; d[35] = src[1121];
            d[36] = src[1153]; d[37] = src[1185]; d[38] = src[1217]; d[39] = src[1249];
            d[40] = src[1281]; d[41] = src[1313]; d[42] = src[1345]; d[43] = src[1377];
            d[44] = src[1409]; d[45] = src[1441]; d[46] = src[1473]; d[47] = src[1505];
            d[48] = src[1537]; d[49] = src[1569]; d[50] = src[1601]; d[51] = src[1633];
            d[52] = src[1665]; d[53] = src[1697]; d[54] = src[1729]; d[55] = src[1761];
            d[56] = src[1793]; d[57] = src[1825]; d[58] = src[1857]; d[59] = src[1889];
            d[60] = src[1921]; d[61] = src[1953]; d[62] = src[1985]; d[63] = src[2017];
            d = dst[2];
            d[ 0] = src[   2]; d[ 1] = src[  34]; d[ 2] = src[  66]; d[ 3] = src[  98];
            d[ 4] = src[ 130]; d[ 5] = src[ 162]; d[ 6] = src[ 194]; d[ 7] = src[ 226];
            d[ 8] = src[ 258]; d[ 9] = src[ 290]; d[10] = src[ 322]; d[11] = src[ 354];
            d[12] = src[ 386]; d[13] = src[ 418]; d[14] = src[ 450]; d[15] = src[ 482];
            d[16] = src[ 514]; d[17] = src[ 546]; d[18] = src[ 578]; d[19] = src[ 610];
            d[20] = src[ 642]; d[21] = src[ 674]; d[22] = src[ 706]; d[23] = src[ 738];
            d[24] = src[ 770]; d[25] = src[ 802]; d[26] = src[ 834]; d[27] = src[ 866];
            d[28] = src[ 898]; d[29] = src[ 930]; d[30] = src[ 962]; d[31] = src[ 994];
            d[32] = src[1026]; d[33] = src[1058]; d[34] = src[1090]; d[35] = src[1122];
            d[36] = src[1154]; d[37] = src[1186]; d[38] = src[1218]; d[39] = src[1250];
            d[40] = src[1282]; d[41] = src[1314]; d[42] = src[1346]; d[43] = src[1378];
            d[44] = src[1410]; d[45] = src[1442]; d[46] = src[1474]; d[47] = src[1506];
            d[48] = src[1538]; d[49] = src[1570]; d[50] = src[1602]; d[51] = src[1634];
            d[52] = src[1666]; d[53] = src[1698]; d[54] = src[1730]; d[55] = src[1762];
            d[56] = src[1794]; d[57] = src[1826]; d[58] = src[1858]; d[59] = src[1890];
            d[60] = src[1922]; d[61] = src[1954]; d[62] = src[1986]; d[63] = src[2018];
            d = dst[3];
            d[ 0] = src[   3]; d[ 1] = src[  35]; d[ 2] = src[  67]; d[ 3] = src[  99];
            d[ 4] = src[ 131]; d[ 5] = src[ 163]; d[ 6] = src[ 195]; d[ 7] = src[ 227];
            d[ 8] = src[ 259]; d[ 9] = src[ 291]; d[10] = src[ 323]; d[11] = src[ 355];
            d[12] = src[ 387]; d[13] = src[ 419]; d[14] = src[ 451]; d[15] = src[ 483];
            d[16] = src[ 515]; d[17] = src[ 547]; d[18] = src[ 579]; d[19] = src[ 611];
            d[20] = src[ 643]; d[21] = src[ 675]; d[22] = src[ 707]; d[23] = src[ 739];
            d[24] = src[ 771]; d[25] = src[ 803]; d[26] = src[ 835]; d[27] = src[ 867];
            d[28] = src[ 899]; d[29] = src[ 931]; d[30] = src[ 963]; d[31] = src[ 995];
            d[32] = src[1027]; d[33] = src[1059]; d[34] = src[1091]; d[35] = src[1123];
            d[36] = src[1155]; d[37] = src[1187]; d[38] = src[1219]; d[39] = src[1251];
            d[40] = src[1283]; d[41] = src[1315]; d[42] = src[1347]; d[43] = src[1379];
            d[44] = src[1411]; d[45] = src[1443]; d[46] = src[1475]; d[47] = src[1507];
            d[48] = src[1539]; d[49] = src[1571]; d[50] = src[1603]; d[51] = src[1635];
            d[52] = src[1667]; d[53] = src[1699]; d[54] = src[1731]; d[55] = src[1763];
            d[56] = src[1795]; d[57] = src[1827]; d[58] = src[1859]; d[59] = src[1891];
            d[60] = src[1923]; d[61] = src[1955]; d[62] = src[1987]; d[63] = src[2019];
            d = dst[4];
            d[ 0] = src[   4]; d[ 1] = src[  36]; d[ 2] = src[  68]; d[ 3] = src[ 100];
            d[ 4] = src[ 132]; d[ 5] = src[ 164]; d[ 6] = src[ 196]; d[ 7] = src[ 228];
            d[ 8] = src[ 260]; d[ 9] = src[ 292]; d[10] = src[ 324]; d[11] = src[ 356];
            d[12] = src[ 388]; d[13] = src[ 420]; d[14] = src[ 452]; d[15] = src[ 484];
            d[16] = src[ 516]; d[17] = src[ 548]; d[18] = src[ 580]; d[19] = src[ 612];
            d[20] = src[ 644]; d[21] = src[ 676]; d[22] = src[ 708]; d[23] = src[ 740];
            d[24] = src[ 772]; d[25] = src[ 804]; d[26] = src[ 836]; d[27] = src[ 868];
            d[28] = src[ 900]; d[29] = src[ 932]; d[30] = src[ 964]; d[31] = src[ 996];
            d[32] = src[1028]; d[33] = src[1060]; d[34] = src[1092]; d[35] = src[1124];
            d[36] = src[1156]; d[37] = src[1188]; d[38] = src[1220]; d[39] = src[1252];
            d[40] = src[1284]; d[41] = src[1316]; d[42] = src[1348]; d[43] = src[1380];
            d[44] = src[1412]; d[45] = src[1444]; d[46] = src[1476]; d[47] = src[1508];
            d[48] = src[1540]; d[49] = src[1572]; d[50] = src[1604]; d[51] = src[1636];
            d[52] = src[1668]; d[53] = src[1700]; d[54] = src[1732]; d[55] = src[1764];
            d[56] = src[1796]; d[57] = src[1828]; d[58] = src[1860]; d[59] = src[1892];
            d[60] = src[1924]; d[61] = src[1956]; d[62] = src[1988]; d[63] = src[2020];
            d = dst[5];
            d[ 0] = src[   5]; d[ 1] = src[  37]; d[ 2] = src[  69]; d[ 3] = src[ 101];
            d[ 4] = src[ 133]; d[ 5] = src[ 165]; d[ 6] = src[ 197]; d[ 7] = src[ 229];
            d[ 8] = src[ 261]; d[ 9] = src[ 293]; d[10] = src[ 325]; d[11] = src[ 357];
            d[12] = src[ 389]; d[13] = src[ 421]; d[14] = src[ 453]; d[15] = src[ 485];
            d[16] = src[ 517]; d[17] = src[ 549]; d[18] = src[ 581]; d[19] = src[ 613];
            d[20] = src[ 645]; d[21] = src[ 677]; d[22] = src[ 709]; d[23] = src[ 741];
            d[24] = src[ 773]; d[25] = src[ 805]; d[26] = src[ 837]; d[27] = src[ 869];
            d[28] = src[ 901]; d[29] = src[ 933]; d[30] = src[ 965]; d[31] = src[ 997];
            d[32] = src[1029]; d[33] = src[1061]; d[34] = src[1093]; d[35] = src[1125];
            d[36] = src[1157]; d[37] = src[1189]; d[38] = src[1221]; d[39] = src[1253];
            d[40] = src[1285]; d[41] = src[1317]; d[42] = src[1349]; d[43] = src[1381];
            d[44] = src[1413]; d[45] = src[1445]; d[46] = src[1477]; d[47] = src[1509];
            d[48] = src[1541]; d[49] = src[1573]; d[50] = src[1605]; d[51] = src[1637];
            d[52] = src[1669]; d[53] = src[1701]; d[54] = src[1733]; d[55] = src[1765];
            d[56] = src[1797]; d[57] = src[1829]; d[58] = src[1861]; d[59] = src[1893];
            d[60] = src[1925]; d[61] = src[1957]; d[62] = src[1989]; d[63] = src[2021];
            d = dst[6];
            d[ 0] = src[   6]; d[ 1] = src[  38]; d[ 2] = src[  70]; d[ 3] = src[ 102];
            d[ 4] = src[ 134]; d[ 5] = src[ 166]; d[ 6] = src[ 198]; d[ 7] = src[ 230];
            d[ 8] = src[ 262]; d[ 9] = src[ 294]; d[10] = src[ 326]; d[11] = src[ 358];
            d[12] = src[ 390]; d[13] = src[ 422]; d[14] = src[ 454]; d[15] = src[ 486];
            d[16] = src[ 518]; d[17] = src[ 550]; d[18] = src[ 582]; d[19] = src[ 614];
            d[20] = src[ 646]; d[21] = src[ 678]; d[22] = src[ 710]; d[23] = src[ 742];
            d[24] = src[ 774]; d[25] = src[ 806]; d[26] = src[ 838]; d[27] = src[ 870];
            d[28] = src[ 902]; d[29] = src[ 934]; d[30] = src[ 966]; d[31] = src[ 998];
            d[32] = src[1030]; d[33] = src[1062]; d[34] = src[1094]; d[35] = src[1126];
            d[36] = src[1158]; d[37] = src[1190]; d[38] = src[1222]; d[39] = src[1254];
            d[40] = src[1286]; d[41] = src[1318]; d[42] = src[1350]; d[43] = src[1382];
            d[44] = src[1414]; d[45] = src[1446]; d[46] = src[1478]; d[47] = src[1510];
            d[48] = src[1542]; d[49] = src[1574]; d[50] = src[1606]; d[51] = src[1638];
            d[52] = src[1670]; d[53] = src[1702]; d[54] = src[1734]; d[55] = src[1766];
            d[56] = src[1798]; d[57] = src[1830]; d[58] = src[1862]; d[59] = src[1894];
            d[60] = src[1926]; d[61] = src[1958]; d[62] = src[1990]; d[63] = src[2022];
            d = dst[7];
            d[ 0] = src[   7]; d[ 1] = src[  39]; d[ 2] = src[  71]; d[ 3] = src[ 103];
            d[ 4] = src[ 135]; d[ 5] = src[ 167]; d[ 6] = src[ 199]; d[ 7] = src[ 231];
            d[ 8] = src[ 263]; d[ 9] = src[ 295]; d[10] = src[ 327]; d[11] = src[ 359];
            d[12] = src[ 391]; d[13] = src[ 423]; d[14] = src[ 455]; d[15] = src[ 487];
            d[16] = src[ 519]; d[17] = src[ 551]; d[18] = src[ 583]; d[19] = src[ 615];
            d[20] = src[ 647]; d[21] = src[ 679]; d[22] = src[ 711]; d[23] = src[ 743];
            d[24] = src[ 775]; d[25] = src[ 807]; d[26] = src[ 839]; d[27] = src[ 871];
            d[28] = src[ 903]; d[29] = src[ 935]; d[30] = src[ 967]; d[31] = src[ 999];
            d[32] = src[1031]; d[33] = src[1063]; d[34] = src[1095]; d[35] = src[1127];
            d[36] = src[1159]; d[37] = src[1191]; d[38] = src[1223]; d[39] = src[1255];
            d[40] = src[1287]; d[41] = src[1319]; d[42] = src[1351]; d[43] = src[1383];
            d[44] = src[1415]; d[45] = src[1447]; d[46] = src[1479]; d[47] = src[1511];
            d[48] = src[1543]; d[49] = src[1575]; d[50] = src[1607]; d[51] = src[1639];
            d[52] = src[1671]; d[53] = src[1703]; d[54] = src[1735]; d[55] = src[1767];
            d[56] = src[1799]; d[57] = src[1831]; d[58] = src[1863]; d[59] = src[1895];
            d[60] = src[1927]; d[61] = src[1959]; d[62] = src[1991]; d[63] = src[2023];
            d = dst[8];
            d[ 0] = src[   8]; d[ 1] = src[  40]; d[ 2] = src[  72]; d[ 3] = src[ 104];
            d[ 4] = src[ 136]; d[ 5] = src[ 168]; d[ 6] = src[ 200]; d[ 7] = src[ 232];
            d[ 8] = src[ 264]; d[ 9] = src[ 296]; d[10] = src[ 328]; d[11] = src[ 360];
            d[12] = src[ 392]; d[13] = src[ 424]; d[14] = src[ 456]; d[15] = src[ 488];
            d[16] = src[ 520]; d[17] = src[ 552]; d[18] = src[ 584]; d[19] = src[ 616];
            d[20] = src[ 648]; d[21] = src[ 680]; d[22] = src[ 712]; d[23] = src[ 744];
            d[24] = src[ 776]; d[25] = src[ 808]; d[26] = src[ 840]; d[27] = src[ 872];
            d[28] = src[ 904]; d[29] = src[ 936]; d[30] = src[ 968]; d[31] = src[1000];
            d[32] = src[1032]; d[33] = src[1064]; d[34] = src[1096]; d[35] = src[1128];
            d[36] = src[1160]; d[37] = src[1192]; d[38] = src[1224]; d[39] = src[1256];
            d[40] = src[1288]; d[41] = src[1320]; d[42] = src[1352]; d[43] = src[1384];
            d[44] = src[1416]; d[45] = src[1448]; d[46] = src[1480]; d[47] = src[1512];
            d[48] = src[1544]; d[49] = src[1576]; d[50] = src[1608]; d[51] = src[1640];
            d[52] = src[1672]; d[53] = src[1704]; d[54] = src[1736]; d[55] = src[1768];
            d[56] = src[1800]; d[57] = src[1832]; d[58] = src[1864]; d[59] = src[1896];
            d[60] = src[1928]; d[61] = src[1960]; d[62] = src[1992]; d[63] = src[2024];
            d = dst[9];
            d[ 0] = src[   9]; d[ 1] = src[  41]; d[ 2] = src[  73]; d[ 3] = src[ 105];
            d[ 4] = src[ 137]; d[ 5] = src[ 169]; d[ 6] = src[ 201]; d[ 7] = src[ 233];
            d[ 8] = src[ 265]; d[ 9] = src[ 297]; d[10] = src[ 329]; d[11] = src[ 361];
            d[12] = src[ 393]; d[13] = src[ 425]; d[14] = src[ 457]; d[15] = src[ 489];
            d[16] = src[ 521]; d[17] = src[ 553]; d[18] = src[ 585]; d[19] = src[ 617];
            d[20] = src[ 649]; d[21] = src[ 681]; d[22] = src[ 713]; d[23] = src[ 745];
            d[24] = src[ 777]; d[25] = src[ 809]; d[26] = src[ 841]; d[27] = src[ 873];
            d[28] = src[ 905]; d[29] = src[ 937]; d[30] = src[ 969]; d[31] = src[1001];
            d[32] = src[1033]; d[33] = src[1065]; d[34] = src[1097]; d[35] = src[1129];
            d[36] = src[1161]; d[37] = src[1193]; d[38] = src[1225]; d[39] = src[1257];
            d[40] = src[1289]; d[41] = src[1321]; d[42] = src[1353]; d[43] = src[1385];
            d[44] = src[1417]; d[45] = src[1449]; d[46] = src[1481]; d[47] = src[1513];
            d[48] = src[1545]; d[49] = src[1577]; d[50] = src[1609]; d[51] = src[1641];
            d[52] = src[1673]; d[53] = src[1705]; d[54] = src[1737]; d[55] = src[1769];
            d[56] = src[1801]; d[57] = src[1833]; d[58] = src[1865]; d[59] = src[1897];
            d[60] = src[1929]; d[61] = src[1961]; d[62] = src[1993]; d[63] = src[2025];
            d = dst[10];
            d[ 0] = src[  10]; d[ 1] = src[  42]; d[ 2] = src[  74]; d[ 3] = src[ 106];
            d[ 4] = src[ 138]; d[ 5] = src[ 170]; d[ 6] = src[ 202]; d[ 7] = src[ 234];
            d[ 8] = src[ 266]; d[ 9] = src[ 298]; d[10] = src[ 330]; d[11] = src[ 362];
            d[12] = src[ 394]; d[13] = src[ 426]; d[14] = src[ 458]; d[15] = src[ 490];
            d[16] = src[ 522]; d[17] = src[ 554]; d[18] = src[ 586]; d[19] = src[ 618];
            d[20] = src[ 650]; d[21] = src[ 682]; d[22] = src[ 714]; d[23] = src[ 746];
            d[24] = src[ 778]; d[25] = src[ 810]; d[26] = src[ 842]; d[27] = src[ 874];
            d[28] = src[ 906]; d[29] = src[ 938]; d[30] = src[ 970]; d[31] = src[1002];
            d[32] = src[1034]; d[33] = src[1066]; d[34] = src[1098]; d[35] = src[1130];
            d[36] = src[1162]; d[37] = src[1194]; d[38] = src[1226]; d[39] = src[1258];
            d[40] = src[1290]; d[41] = src[1322]; d[42] = src[1354]; d[43] = src[1386];
            d[44] = src[1418]; d[45] = src[1450]; d[46] = src[1482]; d[47] = src[1514];
            d[48] = src[1546]; d[49] = src[1578]; d[50] = src[1610]; d[51] = src[1642];
            d[52] = src[1674]; d[53] = src[1706]; d[54] = src[1738]; d[55] = src[1770];
            d[56] = src[1802]; d[57] = src[1834]; d[58] = src[1866]; d[59] = src[1898];
            d[60] = src[1930]; d[61] = src[1962]; d[62] = src[1994]; d[63] = src[2026];
            d = dst[11];
            d[ 0] = src[  11]; d[ 1] = src[  43]; d[ 2] = src[  75]; d[ 3] = src[ 107];
            d[ 4] = src[ 139]; d[ 5] = src[ 171]; d[ 6] = src[ 203]; d[ 7] = src[ 235];
            d[ 8] = src[ 267]; d[ 9] = src[ 299]; d[10] = src[ 331]; d[11] = src[ 363];
            d[12] = src[ 395]; d[13] = src[ 427]; d[14] = src[ 459]; d[15] = src[ 491];
            d[16] = src[ 523]; d[17] = src[ 555]; d[18] = src[ 587]; d[19] = src[ 619];
            d[20] = src[ 651]; d[21] = src[ 683]; d[22] = src[ 715]; d[23] = src[ 747];
            d[24] = src[ 779]; d[25] = src[ 811]; d[26] = src[ 843]; d[27] = src[ 875];
            d[28] = src[ 907]; d[29] = src[ 939]; d[30] = src[ 971]; d[31] = src[1003];
            d[32] = src[1035]; d[33] = src[1067]; d[34] = src[1099]; d[35] = src[1131];
            d[36] = src[1163]; d[37] = src[1195]; d[38] = src[1227]; d[39] = src[1259];
            d[40] = src[1291]; d[41] = src[1323]; d[42] = src[1355]; d[43] = src[1387];
            d[44] = src[1419]; d[45] = src[1451]; d[46] = src[1483]; d[47] = src[1515];
            d[48] = src[1547]; d[49] = src[1579]; d[50] = src[1611]; d[51] = src[1643];
            d[52] = src[1675]; d[53] = src[1707]; d[54] = src[1739]; d[55] = src[1771];
            d[56] = src[1803]; d[57] = src[1835]; d[58] = src[1867]; d[59] = src[1899];
            d[60] = src[1931]; d[61] = src[1963]; d[62] = src[1995]; d[63] = src[2027];
            d = dst[12];
            d[ 0] = src[  12]; d[ 1] = src[  44]; d[ 2] = src[  76]; d[ 3] = src[ 108];
            d[ 4] = src[ 140]; d[ 5] = src[ 172]; d[ 6] = src[ 204]; d[ 7] = src[ 236];
            d[ 8] = src[ 268]; d[ 9] = src[ 300]; d[10] = src[ 332]; d[11] = src[ 364];
            d[12] = src[ 396]; d[13] = src[ 428]; d[14] = src[ 460]; d[15] = src[ 492];
            d[16] = src[ 524]; d[17] = src[ 556]; d[18] = src[ 588]; d[19] = src[ 620];
            d[20] = src[ 652]; d[21] = src[ 684]; d[22] = src[ 716]; d[23] = src[ 748];
            d[24] = src[ 780]; d[25] = src[ 812]; d[26] = src[ 844]; d[27] = src[ 876];
            d[28] = src[ 908]; d[29] = src[ 940]; d[30] = src[ 972]; d[31] = src[1004];
            d[32] = src[1036]; d[33] = src[1068]; d[34] = src[1100]; d[35] = src[1132];
            d[36] = src[1164]; d[37] = src[1196]; d[38] = src[1228]; d[39] = src[1260];
            d[40] = src[1292]; d[41] = src[1324]; d[42] = src[1356]; d[43] = src[1388];
            d[44] = src[1420]; d[45] = src[1452]; d[46] = src[1484]; d[47] = src[1516];
            d[48] = src[1548]; d[49] = src[1580]; d[50] = src[1612]; d[51] = src[1644];
            d[52] = src[1676]; d[53] = src[1708]; d[54] = src[1740]; d[55] = src[1772];
            d[56] = src[1804]; d[57] = src[1836]; d[58] = src[1868]; d[59] = src[1900];
            d[60] = src[1932]; d[61] = src[1964]; d[62] = src[1996]; d[63] = src[2028];
            d = dst[13];
            d[ 0] = src[  13]; d[ 1] = src[  45]; d[ 2] = src[  77]; d[ 3] = src[ 109];
            d[ 4] = src[ 141]; d[ 5] = src[ 173]; d[ 6] = src[ 205]; d[ 7] = src[ 237];
            d[ 8] = src[ 269]; d[ 9] = src[ 301]; d[10] = src[ 333]; d[11] = src[ 365];
            d[12] = src[ 397]; d[13] = src[ 429]; d[14] = src[ 461]; d[15] = src[ 493];
            d[16] = src[ 525]; d[17] = src[ 557]; d[18] = src[ 589]; d[19] = src[ 621];
            d[20] = src[ 653]; d[21] = src[ 685]; d[22] = src[ 717]; d[23] = src[ 749];
            d[24] = src[ 781]; d[25] = src[ 813]; d[26] = src[ 845]; d[27] = src[ 877];
            d[28] = src[ 909]; d[29] = src[ 941]; d[30] = src[ 973]; d[31] = src[1005];
            d[32] = src[1037]; d[33] = src[1069]; d[34] = src[1101]; d[35] = src[1133];
            d[36] = src[1165]; d[37] = src[1197]; d[38] = src[1229]; d[39] = src[1261];
            d[40] = src[1293]; d[41] = src[1325]; d[42] = src[1357]; d[43] = src[1389];
            d[44] = src[1421]; d[45] = src[1453]; d[46] = src[1485]; d[47] = src[1517];
            d[48] = src[1549]; d[49] = src[1581]; d[50] = src[1613]; d[51] = src[1645];
            d[52] = src[1677]; d[53] = src[1709]; d[54] = src[1741]; d[55] = src[1773];
            d[56] = src[1805]; d[57] = src[1837]; d[58] = src[1869]; d[59] = src[1901];
            d[60] = src[1933]; d[61] = src[1965]; d[62] = src[1997]; d[63] = src[2029];
            d = dst[14];
            d[ 0] = src[  14]; d[ 1] = src[  46]; d[ 2] = src[  78]; d[ 3] = src[ 110];
            d[ 4] = src[ 142]; d[ 5] = src[ 174]; d[ 6] = src[ 206]; d[ 7] = src[ 238];
            d[ 8] = src[ 270]; d[ 9] = src[ 302]; d[10] = src[ 334]; d[11] = src[ 366];
            d[12] = src[ 398]; d[13] = src[ 430]; d[14] = src[ 462]; d[15] = src[ 494];
            d[16] = src[ 526]; d[17] = src[ 558]; d[18] = src[ 590]; d[19] = src[ 622];
            d[20] = src[ 654]; d[21] = src[ 686]; d[22] = src[ 718]; d[23] = src[ 750];
            d[24] = src[ 782]; d[25] = src[ 814]; d[26] = src[ 846]; d[27] = src[ 878];
            d[28] = src[ 910]; d[29] = src[ 942]; d[30] = src[ 974]; d[31] = src[1006];
            d[32] = src[1038]; d[33] = src[1070]; d[34] = src[1102]; d[35] = src[1134];
            d[36] = src[1166]; d[37] = src[1198]; d[38] = src[1230]; d[39] = src[1262];
            d[40] = src[1294]; d[41] = src[1326]; d[42] = src[1358]; d[43] = src[1390];
            d[44] = src[1422]; d[45] = src[1454]; d[46] = src[1486]; d[47] = src[1518];
            d[48] = src[1550]; d[49] = src[1582]; d[50] = src[1614]; d[51] = src[1646];
            d[52] = src[1678]; d[53] = src[1710]; d[54] = src[1742]; d[55] = src[1774];
            d[56] = src[1806]; d[57] = src[1838]; d[58] = src[1870]; d[59] = src[1902];
            d[60] = src[1934]; d[61] = src[1966]; d[62] = src[1998]; d[63] = src[2030];
            d = dst[15];
            d[ 0] = src[  15]; d[ 1] = src[  47]; d[ 2] = src[  79]; d[ 3] = src[ 111];
            d[ 4] = src[ 143]; d[ 5] = src[ 175]; d[ 6] = src[ 207]; d[ 7] = src[ 239];
            d[ 8] = src[ 271]; d[ 9] = src[ 303]; d[10] = src[ 335]; d[11] = src[ 367];
            d[12] = src[ 399]; d[13] = src[ 431]; d[14] = src[ 463]; d[15] = src[ 495];
            d[16] = src[ 527]; d[17] = src[ 559]; d[18] = src[ 591]; d[19] = src[ 623];
            d[20] = src[ 655]; d[21] = src[ 687]; d[22] = src[ 719]; d[23] = src[ 751];
            d[24] = src[ 783]; d[25] = src[ 815]; d[26] = src[ 847]; d[27] = src[ 879];
            d[28] = src[ 911]; d[29] = src[ 943]; d[30] = src[ 975]; d[31] = src[1007];
            d[32] = src[1039]; d[33] = src[1071]; d[34] = src[1103]; d[35] = src[1135];
            d[36] = src[1167]; d[37] = src[1199]; d[38] = src[1231]; d[39] = src[1263];
            d[40] = src[1295]; d[41] = src[1327]; d[42] = src[1359]; d[43] = src[1391];
            d[44] = src[1423]; d[45] = src[1455]; d[46] = src[1487]; d[47] = src[1519];
            d[48] = src[1551]; d[49] = src[1583]; d[50] = src[1615]; d[51] = src[1647];
            d[52] = src[1679]; d[53] = src[1711]; d[54] = src[1743]; d[55] = src[1775];
            d[56] = src[1807]; d[57] = src[1839]; d[58] = src[1871]; d[59] = src[1903];
            d[60] = src[1935]; d[61] = src[1967]; d[62] = src[1999]; d[63] = src[2031];
            d = dst[16];
            d[ 0] = src[  16]; d[ 1] = src[  48]; d[ 2] = src[  80]; d[ 3] = src[ 112];
            d[ 4] = src[ 144]; d[ 5] = src[ 176]; d[ 6] = src[ 208]; d[ 7] = src[ 240];
            d[ 8] = src[ 272]; d[ 9] = src[ 304]; d[10] = src[ 336]; d[11] = src[ 368];
            d[12] = src[ 400]; d[13] = src[ 432]; d[14] = src[ 464]; d[15] = src[ 496];
            d[16] = src[ 528]; d[17] = src[ 560]; d[18] = src[ 592]; d[19] = src[ 624];
            d[20] = src[ 656]; d[21] = src[ 688]; d[22] = src[ 720]; d[23] = src[ 752];
            d[24] = src[ 784]; d[25] = src[ 816]; d[26] = src[ 848]; d[27] = src[ 880];
            d[28] = src[ 912]; d[29] = src[ 944]; d[30] = src[ 976]; d[31] = src[1008];
            d[32] = src[1040]; d[33] = src[1072]; d[34] = src[1104]; d[35] = src[1136];
            d[36] = src[1168]; d[37] = src[1200]; d[38] = src[1232]; d[39] = src[1264];
            d[40] = src[1296]; d[41] = src[1328]; d[42] = src[1360]; d[43] = src[1392];
            d[44] = src[1424]; d[45] = src[1456]; d[46] = src[1488]; d[47] = src[1520];
            d[48] = src[1552]; d[49] = src[1584]; d[50] = src[1616]; d[51] = src[1648];
            d[52] = src[1680]; d[53] = src[1712]; d[54] = src[1744]; d[55] = src[1776];
            d[56] = src[1808]; d[57] = src[1840]; d[58] = src[1872]; d[59] = src[1904];
            d[60] = src[1936]; d[61] = src[1968]; d[62] = src[2000]; d[63] = src[2032];
            d = dst[17];
            d[ 0] = src[  17]; d[ 1] = src[  49]; d[ 2] = src[  81]; d[ 3] = src[ 113];
            d[ 4] = src[ 145]; d[ 5] = src[ 177]; d[ 6] = src[ 209]; d[ 7] = src[ 241];
            d[ 8] = src[ 273]; d[ 9] = src[ 305]; d[10] = src[ 337]; d[11] = src[ 369];
            d[12] = src[ 401]; d[13] = src[ 433]; d[14] = src[ 465]; d[15] = src[ 497];
            d[16] = src[ 529]; d[17] = src[ 561]; d[18] = src[ 593]; d[19] = src[ 625];
            d[20] = src[ 657]; d[21] = src[ 689]; d[22] = src[ 721]; d[23] = src[ 753];
            d[24] = src[ 785]; d[25] = src[ 817]; d[26] = src[ 849]; d[27] = src[ 881];
            d[28] = src[ 913]; d[29] = src[ 945]; d[30] = src[ 977]; d[31] = src[1009];
            d[32] = src[1041]; d[33] = src[1073]; d[34] = src[1105]; d[35] = src[1137];
            d[36] = src[1169]; d[37] = src[1201]; d[38] = src[1233]; d[39] = src[1265];
            d[40] = src[1297]; d[41] = src[1329]; d[42] = src[1361]; d[43] = src[1393];
            d[44] = src[1425]; d[45] = src[1457]; d[46] = src[1489]; d[47] = src[1521];
            d[48] = src[1553]; d[49] = src[1585]; d[50] = src[1617]; d[51] = src[1649];
            d[52] = src[1681]; d[53] = src[1713]; d[54] = src[1745]; d[55] = src[1777];
            d[56] = src[1809]; d[57] = src[1841]; d[58] = src[1873]; d[59] = src[1905];
            d[60] = src[1937]; d[61] = src[1969]; d[62] = src[2001]; d[63] = src[2033];
            d = dst[18];
            d[ 0] = src[  18]; d[ 1] = src[  50]; d[ 2] = src[  82]; d[ 3] = src[ 114];
            d[ 4] = src[ 146]; d[ 5] = src[ 178]; d[ 6] = src[ 210]; d[ 7] = src[ 242];
            d[ 8] = src[ 274]; d[ 9] = src[ 306]; d[10] = src[ 338]; d[11] = src[ 370];
            d[12] = src[ 402]; d[13] = src[ 434]; d[14] = src[ 466]; d[15] = src[ 498];
            d[16] = src[ 530]; d[17] = src[ 562]; d[18] = src[ 594]; d[19] = src[ 626];
            d[20] = src[ 658]; d[21] = src[ 690]; d[22] = src[ 722]; d[23] = src[ 754];
            d[24] = src[ 786]; d[25] = src[ 818]; d[26] = src[ 850]; d[27] = src[ 882];
            d[28] = src[ 914]; d[29] = src[ 946]; d[30] = src[ 978]; d[31] = src[1010];
            d[32] = src[1042]; d[33] = src[1074]; d[34] = src[1106]; d[35] = src[1138];
            d[36] = src[1170]; d[37] = src[1202]; d[38] = src[1234]; d[39] = src[1266];
            d[40] = src[1298]; d[41] = src[1330]; d[42] = src[1362]; d[43] = src[1394];
            d[44] = src[1426]; d[45] = src[1458]; d[46] = src[1490]; d[47] = src[1522];
            d[48] = src[1554]; d[49] = src[1586]; d[50] = src[1618]; d[51] = src[1650];
            d[52] = src[1682]; d[53] = src[1714]; d[54] = src[1746]; d[55] = src[1778];
            d[56] = src[1810]; d[57] = src[1842]; d[58] = src[1874]; d[59] = src[1906];
            d[60] = src[1938]; d[61] = src[1970]; d[62] = src[2002]; d[63] = src[2034];
            d = dst[19];
            d[ 0] = src[  19]; d[ 1] = src[  51]; d[ 2] = src[  83]; d[ 3] = src[ 115];
            d[ 4] = src[ 147]; d[ 5] = src[ 179]; d[ 6] = src[ 211]; d[ 7] = src[ 243];
            d[ 8] = src[ 275]; d[ 9] = src[ 307]; d[10] = src[ 339]; d[11] = src[ 371];
            d[12] = src[ 403]; d[13] = src[ 435]; d[14] = src[ 467]; d[15] = src[ 499];
            d[16] = src[ 531]; d[17] = src[ 563]; d[18] = src[ 595]; d[19] = src[ 627];
            d[20] = src[ 659]; d[21] = src[ 691]; d[22] = src[ 723]; d[23] = src[ 755];
            d[24] = src[ 787]; d[25] = src[ 819]; d[26] = src[ 851]; d[27] = src[ 883];
            d[28] = src[ 915]; d[29] = src[ 947]; d[30] = src[ 979]; d[31] = src[1011];
            d[32] = src[1043]; d[33] = src[1075]; d[34] = src[1107]; d[35] = src[1139];
            d[36] = src[1171]; d[37] = src[1203]; d[38] = src[1235]; d[39] = src[1267];
            d[40] = src[1299]; d[41] = src[1331]; d[42] = src[1363]; d[43] = src[1395];
            d[44] = src[1427]; d[45] = src[1459]; d[46] = src[1491]; d[47] = src[1523];
            d[48] = src[1555]; d[49] = src[1587]; d[50] = src[1619]; d[51] = src[1651];
            d[52] = src[1683]; d[53] = src[1715]; d[54] = src[1747]; d[55] = src[1779];
            d[56] = src[1811]; d[57] = src[1843]; d[58] = src[1875]; d[59] = src[1907];
            d[60] = src[1939]; d[61] = src[1971]; d[62] = src[2003]; d[63] = src[2035];
            d = dst[20];
            d[ 0] = src[  20]; d[ 1] = src[  52]; d[ 2] = src[  84]; d[ 3] = src[ 116];
            d[ 4] = src[ 148]; d[ 5] = src[ 180]; d[ 6] = src[ 212]; d[ 7] = src[ 244];
            d[ 8] = src[ 276]; d[ 9] = src[ 308]; d[10] = src[ 340]; d[11] = src[ 372];
            d[12] = src[ 404]; d[13] = src[ 436]; d[14] = src[ 468]; d[15] = src[ 500];
            d[16] = src[ 532]; d[17] = src[ 564]; d[18] = src[ 596]; d[19] = src[ 628];
            d[20] = src[ 660]; d[21] = src[ 692]; d[22] = src[ 724]; d[23] = src[ 756];
            d[24] = src[ 788]; d[25] = src[ 820]; d[26] = src[ 852]; d[27] = src[ 884];
            d[28] = src[ 916]; d[29] = src[ 948]; d[30] = src[ 980]; d[31] = src[1012];
            d[32] = src[1044]; d[33] = src[1076]; d[34] = src[1108]; d[35] = src[1140];
            d[36] = src[1172]; d[37] = src[1204]; d[38] = src[1236]; d[39] = src[1268];
            d[40] = src[1300]; d[41] = src[1332]; d[42] = src[1364]; d[43] = src[1396];
            d[44] = src[1428]; d[45] = src[1460]; d[46] = src[1492]; d[47] = src[1524];
            d[48] = src[1556]; d[49] = src[1588]; d[50] = src[1620]; d[51] = src[1652];
            d[52] = src[1684]; d[53] = src[1716]; d[54] = src[1748]; d[55] = src[1780];
            d[56] = src[1812]; d[57] = src[1844]; d[58] = src[1876]; d[59] = src[1908];
            d[60] = src[1940]; d[61] = src[1972]; d[62] = src[2004]; d[63] = src[2036];
            d = dst[21];
            d[ 0] = src[  21]; d[ 1] = src[  53]; d[ 2] = src[  85]; d[ 3] = src[ 117];
            d[ 4] = src[ 149]; d[ 5] = src[ 181]; d[ 6] = src[ 213]; d[ 7] = src[ 245];
            d[ 8] = src[ 277]; d[ 9] = src[ 309]; d[10] = src[ 341]; d[11] = src[ 373];
            d[12] = src[ 405]; d[13] = src[ 437]; d[14] = src[ 469]; d[15] = src[ 501];
            d[16] = src[ 533]; d[17] = src[ 565]; d[18] = src[ 597]; d[19] = src[ 629];
            d[20] = src[ 661]; d[21] = src[ 693]; d[22] = src[ 725]; d[23] = src[ 757];
            d[24] = src[ 789]; d[25] = src[ 821]; d[26] = src[ 853]; d[27] = src[ 885];
            d[28] = src[ 917]; d[29] = src[ 949]; d[30] = src[ 981]; d[31] = src[1013];
            d[32] = src[1045]; d[33] = src[1077]; d[34] = src[1109]; d[35] = src[1141];
            d[36] = src[1173]; d[37] = src[1205]; d[38] = src[1237]; d[39] = src[1269];
            d[40] = src[1301]; d[41] = src[1333]; d[42] = src[1365]; d[43] = src[1397];
            d[44] = src[1429]; d[45] = src[1461]; d[46] = src[1493]; d[47] = src[1525];
            d[48] = src[1557]; d[49] = src[1589]; d[50] = src[1621]; d[51] = src[1653];
            d[52] = src[1685]; d[53] = src[1717]; d[54] = src[1749]; d[55] = src[1781];
            d[56] = src[1813]; d[57] = src[1845]; d[58] = src[1877]; d[59] = src[1909];
            d[60] = src[1941]; d[61] = src[1973]; d[62] = src[2005]; d[63] = src[2037];
            d = dst[22];
            d[ 0] = src[  22]; d[ 1] = src[  54]; d[ 2] = src[  86]; d[ 3] = src[ 118];
            d[ 4] = src[ 150]; d[ 5] = src[ 182]; d[ 6] = src[ 214]; d[ 7] = src[ 246];
            d[ 8] = src[ 278]; d[ 9] = src[ 310]; d[10] = src[ 342]; d[11] = src[ 374];
            d[12] = src[ 406]; d[13] = src[ 438]; d[14] = src[ 470]; d[15] = src[ 502];
            d[16] = src[ 534]; d[17] = src[ 566]; d[18] = src[ 598]; d[19] = src[ 630];
            d[20] = src[ 662]; d[21] = src[ 694]; d[22] = src[ 726]; d[23] = src[ 758];
            d[24] = src[ 790]; d[25] = src[ 822]; d[26] = src[ 854]; d[27] = src[ 886];
            d[28] = src[ 918]; d[29] = src[ 950]; d[30] = src[ 982]; d[31] = src[1014];
            d[32] = src[1046]; d[33] = src[1078]; d[34] = src[1110]; d[35] = src[1142];
            d[36] = src[1174]; d[37] = src[1206]; d[38] = src[1238]; d[39] = src[1270];
            d[40] = src[1302]; d[41] = src[1334]; d[42] = src[1366]; d[43] = src[1398];
            d[44] = src[1430]; d[45] = src[1462]; d[46] = src[1494]; d[47] = src[1526];
            d[48] = src[1558]; d[49] = src[1590]; d[50] = src[1622]; d[51] = src[1654];
            d[52] = src[1686]; d[53] = src[1718]; d[54] = src[1750]; d[55] = src[1782];
            d[56] = src[1814]; d[57] = src[1846]; d[58] = src[1878]; d[59] = src[1910];
            d[60] = src[1942]; d[61] = src[1974]; d[62] = src[2006]; d[63] = src[2038];
            d = dst[23];
            d[ 0] = src[  23]; d[ 1] = src[  55]; d[ 2] = src[  87]; d[ 3] = src[ 119];
            d[ 4] = src[ 151]; d[ 5] = src[ 183]; d[ 6] = src[ 215]; d[ 7] = src[ 247];
            d[ 8] = src[ 279]; d[ 9] = src[ 311]; d[10] = src[ 343]; d[11] = src[ 375];
            d[12] = src[ 407]; d[13] = src[ 439]; d[14] = src[ 471]; d[15] = src[ 503];
            d[16] = src[ 535]; d[17] = src[ 567]; d[18] = src[ 599]; d[19] = src[ 631];
            d[20] = src[ 663]; d[21] = src[ 695]; d[22] = src[ 727]; d[23] = src[ 759];
            d[24] = src[ 791]; d[25] = src[ 823]; d[26] = src[ 855]; d[27] = src[ 887];
            d[28] = src[ 919]; d[29] = src[ 951]; d[30] = src[ 983]; d[31] = src[1015];
            d[32] = src[1047]; d[33] = src[1079]; d[34] = src[1111]; d[35] = src[1143];
            d[36] = src[1175]; d[37] = src[1207]; d[38] = src[1239]; d[39] = src[1271];
            d[40] = src[1303]; d[41] = src[1335]; d[42] = src[1367]; d[43] = src[1399];
            d[44] = src[1431]; d[45] = src[1463]; d[46] = src[1495]; d[47] = src[1527];
            d[48] = src[1559]; d[49] = src[1591]; d[50] = src[1623]; d[51] = src[1655];
            d[52] = src[1687]; d[53] = src[1719]; d[54] = src[1751]; d[55] = src[1783];
            d[56] = src[1815]; d[57] = src[1847]; d[58] = src[1879]; d[59] = src[1911];
            d[60] = src[1943]; d[61] = src[1975]; d[62] = src[2007]; d[63] = src[2039];
            d = dst[24];
            d[ 0] = src[  24]; d[ 1] = src[  56]; d[ 2] = src[  88]; d[ 3] = src[ 120];
            d[ 4] = src[ 152]; d[ 5] = src[ 184]; d[ 6] = src[ 216]; d[ 7] = src[ 248];
            d[ 8] = src[ 280]; d[ 9] = src[ 312]; d[10] = src[ 344]; d[11] = src[ 376];
            d[12] = src[ 408]; d[13] = src[ 440]; d[14] = src[ 472]; d[15] = src[ 504];
            d[16] = src[ 536]; d[17] = src[ 568]; d[18] = src[ 600]; d[19] = src[ 632];
            d[20] = src[ 664]; d[21] = src[ 696]; d[22] = src[ 728]; d[23] = src[ 760];
            d[24] = src[ 792]; d[25] = src[ 824]; d[26] = src[ 856]; d[27] = src[ 888];
            d[28] = src[ 920]; d[29] = src[ 952]; d[30] = src[ 984]; d[31] = src[1016];
            d[32] = src[1048]; d[33] = src[1080]; d[34] = src[1112]; d[35] = src[1144];
            d[36] = src[1176]; d[37] = src[1208]; d[38] = src[1240]; d[39] = src[1272];
            d[40] = src[1304]; d[41] = src[1336]; d[42] = src[1368]; d[43] = src[1400];
            d[44] = src[1432]; d[45] = src[1464]; d[46] = src[1496]; d[47] = src[1528];
            d[48] = src[1560]; d[49] = src[1592]; d[50] = src[1624]; d[51] = src[1656];
            d[52] = src[1688]; d[53] = src[1720]; d[54] = src[1752]; d[55] = src[1784];
            d[56] = src[1816]; d[57] = src[1848]; d[58] = src[1880]; d[59] = src[1912];
            d[60] = src[1944]; d[61] = src[1976]; d[62] = src[2008]; d[63] = src[2040];
            d = dst[25];
            d[ 0] = src[  25]; d[ 1] = src[  57]; d[ 2] = src[  89]; d[ 3] = src[ 121];
            d[ 4] = src[ 153]; d[ 5] = src[ 185]; d[ 6] = src[ 217]; d[ 7] = src[ 249];
            d[ 8] = src[ 281]; d[ 9] = src[ 313]; d[10] = src[ 345]; d[11] = src[ 377];
            d[12] = src[ 409]; d[13] = src[ 441]; d[14] = src[ 473]; d[15] = src[ 505];
            d[16] = src[ 537]; d[17] = src[ 569]; d[18] = src[ 601]; d[19] = src[ 633];
            d[20] = src[ 665]; d[21] = src[ 697]; d[22] = src[ 729]; d[23] = src[ 761];
            d[24] = src[ 793]; d[25] = src[ 825]; d[26] = src[ 857]; d[27] = src[ 889];
            d[28] = src[ 921]; d[29] = src[ 953]; d[30] = src[ 985]; d[31] = src[1017];
            d[32] = src[1049]; d[33] = src[1081]; d[34] = src[1113]; d[35] = src[1145];
            d[36] = src[1177]; d[37] = src[1209]; d[38] = src[1241]; d[39] = src[1273];
            d[40] = src[1305]; d[41] = src[1337]; d[42] = src[1369]; d[43] = src[1401];
            d[44] = src[1433]; d[45] = src[1465]; d[46] = src[1497]; d[47] = src[1529];
            d[48] = src[1561]; d[49] = src[1593]; d[50] = src[1625]; d[51] = src[1657];
            d[52] = src[1689]; d[53] = src[1721]; d[54] = src[1753]; d[55] = src[1785];
            d[56] = src[1817]; d[57] = src[1849]; d[58] = src[1881]; d[59] = src[1913];
            d[60] = src[1945]; d[61] = src[1977]; d[62] = src[2009]; d[63] = src[2041];
            d = dst[26];
            d[ 0] = src[  26]; d[ 1] = src[  58]; d[ 2] = src[  90]; d[ 3] = src[ 122];
            d[ 4] = src[ 154]; d[ 5] = src[ 186]; d[ 6] = src[ 218]; d[ 7] = src[ 250];
            d[ 8] = src[ 282]; d[ 9] = src[ 314]; d[10] = src[ 346]; d[11] = src[ 378];
            d[12] = src[ 410]; d[13] = src[ 442]; d[14] = src[ 474]; d[15] = src[ 506];
            d[16] = src[ 538]; d[17] = src[ 570]; d[18] = src[ 602]; d[19] = src[ 634];
            d[20] = src[ 666]; d[21] = src[ 698]; d[22] = src[ 730]; d[23] = src[ 762];
            d[24] = src[ 794]; d[25] = src[ 826]; d[26] = src[ 858]; d[27] = src[ 890];
            d[28] = src[ 922]; d[29] = src[ 954]; d[30] = src[ 986]; d[31] = src[1018];
            d[32] = src[1050]; d[33] = src[1082]; d[34] = src[1114]; d[35] = src[1146];
            d[36] = src[1178]; d[37] = src[1210]; d[38] = src[1242]; d[39] = src[1274];
            d[40] = src[1306]; d[41] = src[1338]; d[42] = src[1370]; d[43] = src[1402];
            d[44] = src[1434]; d[45] = src[1466]; d[46] = src[1498]; d[47] = src[1530];
            d[48] = src[1562]; d[49] = src[1594]; d[50] = src[1626]; d[51] = src[1658];
            d[52] = src[1690]; d[53] = src[1722]; d[54] = src[1754]; d[55] = src[1786];
            d[56] = src[1818]; d[57] = src[1850]; d[58] = src[1882]; d[59] = src[1914];
            d[60] = src[1946]; d[61] = src[1978]; d[62] = src[2010]; d[63] = src[2042];
            d = dst[27];
            d[ 0] = src[  27]; d[ 1] = src[  59]; d[ 2] = src[  91]; d[ 3] = src[ 123];
            d[ 4] = src[ 155]; d[ 5] = src[ 187]; d[ 6] = src[ 219]; d[ 7] = src[ 251];
            d[ 8] = src[ 283]; d[ 9] = src[ 315]; d[10] = src[ 347]; d[11] = src[ 379];
            d[12] = src[ 411]; d[13] = src[ 443]; d[14] = src[ 475]; d[15] = src[ 507];
            d[16] = src[ 539]; d[17] = src[ 571]; d[18] = src[ 603]; d[19] = src[ 635];
            d[20] = src[ 667]; d[21] = src[ 699]; d[22] = src[ 731]; d[23] = src[ 763];
            d[24] = src[ 795]; d[25] = src[ 827]; d[26] = src[ 859]; d[27] = src[ 891];
            d[28] = src[ 923]; d[29] = src[ 955]; d[30] = src[ 987]; d[31] = src[1019];
            d[32] = src[1051]; d[33] = src[1083]; d[34] = src[1115]; d[35] = src[1147];
            d[36] = src[1179]; d[37] = src[1211]; d[38] = src[1243]; d[39] = src[1275];
            d[40] = src[1307]; d[41] = src[1339]; d[42] = src[1371]; d[43] = src[1403];
            d[44] = src[1435]; d[45] = src[1467]; d[46] = src[1499]; d[47] = src[1531];
            d[48] = src[1563]; d[49] = src[1595]; d[50] = src[1627]; d[51] = src[1659];
            d[52] = src[1691]; d[53] = src[1723]; d[54] = src[1755]; d[55] = src[1787];
            d[56] = src[1819]; d[57] = src[1851]; d[58] = src[1883]; d[59] = src[1915];
            d[60] = src[1947]; d[61] = src[1979]; d[62] = src[2011]; d[63] = src[2043];
            d = dst[28];
            d[ 0] = src[  28]; d[ 1] = src[  60]; d[ 2] = src[  92]; d[ 3] = src[ 124];
            d[ 4] = src[ 156]; d[ 5] = src[ 188]; d[ 6] = src[ 220]; d[ 7] = src[ 252];
            d[ 8] = src[ 284]; d[ 9] = src[ 316]; d[10] = src[ 348]; d[11] = src[ 380];
            d[12] = src[ 412]; d[13] = src[ 444]; d[14] = src[ 476]; d[15] = src[ 508];
            d[16] = src[ 540]; d[17] = src[ 572]; d[18] = src[ 604]; d[19] = src[ 636];
            d[20] = src[ 668]; d[21] = src[ 700]; d[22] = src[ 732]; d[23] = src[ 764];
            d[24] = src[ 796]; d[25] = src[ 828]; d[26] = src[ 860]; d[27] = src[ 892];
            d[28] = src[ 924]; d[29] = src[ 956]; d[30] = src[ 988]; d[31] = src[1020];
            d[32] = src[1052]; d[33] = src[1084]; d[34] = src[1116]; d[35] = src[1148];
            d[36] = src[1180]; d[37] = src[1212]; d[38] = src[1244]; d[39] = src[1276];
            d[40] = src[1308]; d[41] = src[1340]; d[42] = src[1372]; d[43] = src[1404];
            d[44] = src[1436]; d[45] = src[1468]; d[46] = src[1500]; d[47] = src[1532];
            d[48] = src[1564]; d[49] = src[1596]; d[50] = src[1628]; d[51] = src[1660];
            d[52] = src[1692]; d[53] = src[1724]; d[54] = src[1756]; d[55] = src[1788];
            d[56] = src[1820]; d[57] = src[1852]; d[58] = src[1884]; d[59] = src[1916];
            d[60] = src[1948]; d[61] = src[1980]; d[62] = src[2012]; d[63] = src[2044];
            d = dst[29];
            d[ 0] = src[  29]; d[ 1] = src[  61]; d[ 2] = src[  93]; d[ 3] = src[ 125];
            d[ 4] = src[ 157]; d[ 5] = src[ 189]; d[ 6] = src[ 221]; d[ 7] = src[ 253];
            d[ 8] = src[ 285]; d[ 9] = src[ 317]; d[10] = src[ 349]; d[11] = src[ 381];
            d[12] = src[ 413]; d[13] = src[ 445]; d[14] = src[ 477]; d[15] = src[ 509];
            d[16] = src[ 541]; d[17] = src[ 573]; d[18] = src[ 605]; d[19] = src[ 637];
            d[20] = src[ 669]; d[21] = src[ 701]; d[22] = src[ 733]; d[23] = src[ 765];
            d[24] = src[ 797]; d[25] = src[ 829]; d[26] = src[ 861]; d[27] = src[ 893];
            d[28] = src[ 925]; d[29] = src[ 957]; d[30] = src[ 989]; d[31] = src[1021];
            d[32] = src[1053]; d[33] = src[1085]; d[34] = src[1117]; d[35] = src[1149];
            d[36] = src[1181]; d[37] = src[1213]; d[38] = src[1245]; d[39] = src[1277];
            d[40] = src[1309]; d[41] = src[1341]; d[42] = src[1373]; d[43] = src[1405];
            d[44] = src[1437]; d[45] = src[1469]; d[46] = src[1501]; d[47] = src[1533];
            d[48] = src[1565]; d[49] = src[1597]; d[50] = src[1629]; d[51] = src[1661];
            d[52] = src[1693]; d[53] = src[1725]; d[54] = src[1757]; d[55] = src[1789];
            d[56] = src[1821]; d[57] = src[1853]; d[58] = src[1885]; d[59] = src[1917];
            d[60] = src[1949]; d[61] = src[1981]; d[62] = src[2013]; d[63] = src[2045];
            d = dst[30];
            d[ 0] = src[  30]; d[ 1] = src[  62]; d[ 2] = src[  94]; d[ 3] = src[ 126];
            d[ 4] = src[ 158]; d[ 5] = src[ 190]; d[ 6] = src[ 222]; d[ 7] = src[ 254];
            d[ 8] = src[ 286]; d[ 9] = src[ 318]; d[10] = src[ 350]; d[11] = src[ 382];
            d[12] = src[ 414]; d[13] = src[ 446]; d[14] = src[ 478]; d[15] = src[ 510];
            d[16] = src[ 542]; d[17] = src[ 574]; d[18] = src[ 606]; d[19] = src[ 638];
            d[20] = src[ 670]; d[21] = src[ 702]; d[22] = src[ 734]; d[23] = src[ 766];
            d[24] = src[ 798]; d[25] = src[ 830]; d[26] = src[ 862]; d[27] = src[ 894];
            d[28] = src[ 926]; d[29] = src[ 958]; d[30] = src[ 990]; d[31] = src[1022];
            d[32] = src[1054]; d[33] = src[1086]; d[34] = src[1118]; d[35] = src[1150];
            d[36] = src[1182]; d[37] = src[1214]; d[38] = src[1246]; d[39] = src[1278];
            d[40] = src[1310]; d[41] = src[1342]; d[42] = src[1374]; d[43] = src[1406];
            d[44] = src[1438]; d[45] = src[1470]; d[46] = src[1502]; d[47] = src[1534];
            d[48] = src[1566]; d[49] = src[1598]; d[50] = src[1630]; d[51] = src[1662];
            d[52] = src[1694]; d[53] = src[1726]; d[54] = src[1758]; d[55] = src[1790];
            d[56] = src[1822]; d[57] = src[1854]; d[58] = src[1886]; d[59] = src[1918];
            d[60] = src[1950]; d[61] = src[1982]; d[62] = src[2014]; d[63] = src[2046];
            d = dst[31];
            d[ 0] = src[  31]; d[ 1] = src[  63]; d[ 2] = src[  95]; d[ 3] = src[ 127];
            d[ 4] = src[ 159]; d[ 5] = src[ 191]; d[ 6] = src[ 223]; d[ 7] = src[ 255];
            d[ 8] = src[ 287]; d[ 9] = src[ 319]; d[10] = src[ 351]; d[11] = src[ 383];
            d[12] = src[ 415]; d[13] = src[ 447]; d[14] = src[ 479]; d[15] = src[ 511];
            d[16] = src[ 543]; d[17] = src[ 575]; d[18] = src[ 607]; d[19] = src[ 639];
            d[20] = src[ 671]; d[21] = src[ 703]; d[22] = src[ 735]; d[23] = src[ 767];
            d[24] = src[ 799]; d[25] = src[ 831]; d[26] = src[ 863]; d[27] = src[ 895];
            d[28] = src[ 927]; d[29] = src[ 959]; d[30] = src[ 991]; d[31] = src[1023];
            d[32] = src[1055]; d[33] = src[1087]; d[34] = src[1119]; d[35] = src[1151];
            d[36] = src[1183]; d[37] = src[1215]; d[38] = src[1247]; d[39] = src[1279];
            d[40] = src[1311]; d[41] = src[1343]; d[42] = src[1375]; d[43] = src[1407];
            d[44] = src[1439]; d[45] = src[1471]; d[46] = src[1503]; d[47] = src[1535];
            d[48] = src[1567]; d[49] = src[1599]; d[50] = src[1631]; d[51] = src[1663];
            d[52] = src[1695]; d[53] = src[1727]; d[54] = src[1759]; d[55] = src[1791];
            d[56] = src[1823]; d[57] = src[1855]; d[58] = src[1887]; d[59] = src[1919];
            d[60] = src[1951]; d[61] = src[1983]; d[62] = src[2015]; d[63] = src[2047];
        }
    }

    static final class Unrolled_3 implements Demux
    {
        public void demux (byte[] src, byte[][] dst)
        {
            assert NUM_TIMESLOTS == 32;
            assert DST_SIZE == 64;
            assert src.length == NUM_TIMESLOTS * DST_SIZE;
            
            demux_00 (src, dst[ 0]);
            demux_01 (src, dst[ 1]);
            demux_02 (src, dst[ 2]);
            demux_03 (src, dst[ 3]);
            demux_04 (src, dst[ 4]);
            demux_05 (src, dst[ 5]);
            demux_06 (src, dst[ 6]);
            demux_07 (src, dst[ 7]);
            demux_08 (src, dst[ 8]);
            demux_09 (src, dst[ 9]);
            demux_10 (src, dst[10]);
            demux_11 (src, dst[11]);
            demux_12 (src, dst[12]);
            demux_13 (src, dst[13]);
            demux_14 (src, dst[14]);
            demux_15 (src, dst[15]);
            demux_16 (src, dst[16]);
            demux_17 (src, dst[17]);
            demux_18 (src, dst[18]);
            demux_19 (src, dst[19]);
            demux_20 (src, dst[20]);
            demux_21 (src, dst[21]);
            demux_22 (src, dst[22]);
            demux_23 (src, dst[23]);
            demux_24 (src, dst[24]);
            demux_25 (src, dst[25]);
            demux_26 (src, dst[26]);
            demux_27 (src, dst[27]);
            demux_28 (src, dst[28]);
            demux_29 (src, dst[29]);
            demux_30 (src, dst[30]);
            demux_31 (src, dst[31]);
        }

        private static void demux_00 (byte[] src, byte[] d)
        {
            d[ 0] = src[   0]; d[ 1] = src[  32]; d[ 2] = src[  64]; d[ 3] = src[  96];
            d[ 4] = src[ 128]; d[ 5] = src[ 160]; d[ 6] = src[ 192]; d[ 7] = src[ 224];
            d[ 8] = src[ 256]; d[ 9] = src[ 288]; d[10] = src[ 320]; d[11] = src[ 352];
            d[12] = src[ 384]; d[13] = src[ 416]; d[14] = src[ 448]; d[15] = src[ 480];
            d[16] = src[ 512]; d[17] = src[ 544]; d[18] = src[ 576]; d[19] = src[ 608];
            d[20] = src[ 640]; d[21] = src[ 672]; d[22] = src[ 704]; d[23] = src[ 736];
            d[24] = src[ 768]; d[25] = src[ 800]; d[26] = src[ 832]; d[27] = src[ 864];
            d[28] = src[ 896]; d[29] = src[ 928]; d[30] = src[ 960]; d[31] = src[ 992];
            d[32] = src[1024]; d[33] = src[1056]; d[34] = src[1088]; d[35] = src[1120];
            d[36] = src[1152]; d[37] = src[1184]; d[38] = src[1216]; d[39] = src[1248];
            d[40] = src[1280]; d[41] = src[1312]; d[42] = src[1344]; d[43] = src[1376];
            d[44] = src[1408]; d[45] = src[1440]; d[46] = src[1472]; d[47] = src[1504];
            d[48] = src[1536]; d[49] = src[1568]; d[50] = src[1600]; d[51] = src[1632];
            d[52] = src[1664]; d[53] = src[1696]; d[54] = src[1728]; d[55] = src[1760];
            d[56] = src[1792]; d[57] = src[1824]; d[58] = src[1856]; d[59] = src[1888];
            d[60] = src[1920]; d[61] = src[1952]; d[62] = src[1984]; d[63] = src[2016];
        }

        private static void demux_01 (byte[] src, byte[] d)
        {
            d[ 0] = src[   1]; d[ 1] = src[  33]; d[ 2] = src[  65]; d[ 3] = src[  97];
            d[ 4] = src[ 129]; d[ 5] = src[ 161]; d[ 6] = src[ 193]; d[ 7] = src[ 225];
            d[ 8] = src[ 257]; d[ 9] = src[ 289]; d[10] = src[ 321]; d[11] = src[ 353];
            d[12] = src[ 385]; d[13] = src[ 417]; d[14] = src[ 449]; d[15] = src[ 481];
            d[16] = src[ 513]; d[17] = src[ 545]; d[18] = src[ 577]; d[19] = src[ 609];
            d[20] = src[ 641]; d[21] = src[ 673]; d[22] = src[ 705]; d[23] = src[ 737];
            d[24] = src[ 769]; d[25] = src[ 801]; d[26] = src[ 833]; d[27] = src[ 865];
            d[28] = src[ 897]; d[29] = src[ 929]; d[30] = src[ 961]; d[31] = src[ 993];
            d[32] = src[1025]; d[33] = src[1057]; d[34] = src[1089]; d[35] = src[1121];
            d[36] = src[1153]; d[37] = src[1185]; d[38] = src[1217]; d[39] = src[1249];
            d[40] = src[1281]; d[41] = src[1313]; d[42] = src[1345]; d[43] = src[1377];
            d[44] = src[1409]; d[45] = src[1441]; d[46] = src[1473]; d[47] = src[1505];
            d[48] = src[1537]; d[49] = src[1569]; d[50] = src[1601]; d[51] = src[1633];
            d[52] = src[1665]; d[53] = src[1697]; d[54] = src[1729]; d[55] = src[1761];
            d[56] = src[1793]; d[57] = src[1825]; d[58] = src[1857]; d[59] = src[1889];
            d[60] = src[1921]; d[61] = src[1953]; d[62] = src[1985]; d[63] = src[2017];
        }

        private static void demux_02 (byte[] src, byte[] d)
        {
            d[ 0] = src[   2]; d[ 1] = src[  34]; d[ 2] = src[  66]; d[ 3] = src[  98];
            d[ 4] = src[ 130]; d[ 5] = src[ 162]; d[ 6] = src[ 194]; d[ 7] = src[ 226];
            d[ 8] = src[ 258]; d[ 9] = src[ 290]; d[10] = src[ 322]; d[11] = src[ 354];
            d[12] = src[ 386]; d[13] = src[ 418]; d[14] = src[ 450]; d[15] = src[ 482];
            d[16] = src[ 514]; d[17] = src[ 546]; d[18] = src[ 578]; d[19] = src[ 610];
            d[20] = src[ 642]; d[21] = src[ 674]; d[22] = src[ 706]; d[23] = src[ 738];
            d[24] = src[ 770]; d[25] = src[ 802]; d[26] = src[ 834]; d[27] = src[ 866];
            d[28] = src[ 898]; d[29] = src[ 930]; d[30] = src[ 962]; d[31] = src[ 994];
            d[32] = src[1026]; d[33] = src[1058]; d[34] = src[1090]; d[35] = src[1122];
            d[36] = src[1154]; d[37] = src[1186]; d[38] = src[1218]; d[39] = src[1250];
            d[40] = src[1282]; d[41] = src[1314]; d[42] = src[1346]; d[43] = src[1378];
            d[44] = src[1410]; d[45] = src[1442]; d[46] = src[1474]; d[47] = src[1506];
            d[48] = src[1538]; d[49] = src[1570]; d[50] = src[1602]; d[51] = src[1634];
            d[52] = src[1666]; d[53] = src[1698]; d[54] = src[1730]; d[55] = src[1762];
            d[56] = src[1794]; d[57] = src[1826]; d[58] = src[1858]; d[59] = src[1890];
            d[60] = src[1922]; d[61] = src[1954]; d[62] = src[1986]; d[63] = src[2018];
        }

        private static void demux_03 (byte[] src, byte[] d)
        {
            d[ 0] = src[   3]; d[ 1] = src[  35]; d[ 2] = src[  67]; d[ 3] = src[  99];
            d[ 4] = src[ 131]; d[ 5] = src[ 163]; d[ 6] = src[ 195]; d[ 7] = src[ 227];
            d[ 8] = src[ 259]; d[ 9] = src[ 291]; d[10] = src[ 323]; d[11] = src[ 355];
            d[12] = src[ 387]; d[13] = src[ 419]; d[14] = src[ 451]; d[15] = src[ 483];
            d[16] = src[ 515]; d[17] = src[ 547]; d[18] = src[ 579]; d[19] = src[ 611];
            d[20] = src[ 643]; d[21] = src[ 675]; d[22] = src[ 707]; d[23] = src[ 739];
            d[24] = src[ 771]; d[25] = src[ 803]; d[26] = src[ 835]; d[27] = src[ 867];
            d[28] = src[ 899]; d[29] = src[ 931]; d[30] = src[ 963]; d[31] = src[ 995];
            d[32] = src[1027]; d[33] = src[1059]; d[34] = src[1091]; d[35] = src[1123];
            d[36] = src[1155]; d[37] = src[1187]; d[38] = src[1219]; d[39] = src[1251];
            d[40] = src[1283]; d[41] = src[1315]; d[42] = src[1347]; d[43] = src[1379];
            d[44] = src[1411]; d[45] = src[1443]; d[46] = src[1475]; d[47] = src[1507];
            d[48] = src[1539]; d[49] = src[1571]; d[50] = src[1603]; d[51] = src[1635];
            d[52] = src[1667]; d[53] = src[1699]; d[54] = src[1731]; d[55] = src[1763];
            d[56] = src[1795]; d[57] = src[1827]; d[58] = src[1859]; d[59] = src[1891];
            d[60] = src[1923]; d[61] = src[1955]; d[62] = src[1987]; d[63] = src[2019];
        }

        private static void demux_04 (byte[] src, byte[] d)
        {
            d[ 0] = src[   4]; d[ 1] = src[  36]; d[ 2] = src[  68]; d[ 3] = src[ 100];
            d[ 4] = src[ 132]; d[ 5] = src[ 164]; d[ 6] = src[ 196]; d[ 7] = src[ 228];
            d[ 8] = src[ 260]; d[ 9] = src[ 292]; d[10] = src[ 324]; d[11] = src[ 356];
            d[12] = src[ 388]; d[13] = src[ 420]; d[14] = src[ 452]; d[15] = src[ 484];
            d[16] = src[ 516]; d[17] = src[ 548]; d[18] = src[ 580]; d[19] = src[ 612];
            d[20] = src[ 644]; d[21] = src[ 676]; d[22] = src[ 708]; d[23] = src[ 740];
            d[24] = src[ 772]; d[25] = src[ 804]; d[26] = src[ 836]; d[27] = src[ 868];
            d[28] = src[ 900]; d[29] = src[ 932]; d[30] = src[ 964]; d[31] = src[ 996];
            d[32] = src[1028]; d[33] = src[1060]; d[34] = src[1092]; d[35] = src[1124];
            d[36] = src[1156]; d[37] = src[1188]; d[38] = src[1220]; d[39] = src[1252];
            d[40] = src[1284]; d[41] = src[1316]; d[42] = src[1348]; d[43] = src[1380];
            d[44] = src[1412]; d[45] = src[1444]; d[46] = src[1476]; d[47] = src[1508];
            d[48] = src[1540]; d[49] = src[1572]; d[50] = src[1604]; d[51] = src[1636];
            d[52] = src[1668]; d[53] = src[1700]; d[54] = src[1732]; d[55] = src[1764];
            d[56] = src[1796]; d[57] = src[1828]; d[58] = src[1860]; d[59] = src[1892];
            d[60] = src[1924]; d[61] = src[1956]; d[62] = src[1988]; d[63] = src[2020];
        }

        private static void demux_05 (byte[] src, byte[] d)
        {
            d[ 0] = src[   5]; d[ 1] = src[  37]; d[ 2] = src[  69]; d[ 3] = src[ 101];
            d[ 4] = src[ 133]; d[ 5] = src[ 165]; d[ 6] = src[ 197]; d[ 7] = src[ 229];
            d[ 8] = src[ 261]; d[ 9] = src[ 293]; d[10] = src[ 325]; d[11] = src[ 357];
            d[12] = src[ 389]; d[13] = src[ 421]; d[14] = src[ 453]; d[15] = src[ 485];
            d[16] = src[ 517]; d[17] = src[ 549]; d[18] = src[ 581]; d[19] = src[ 613];
            d[20] = src[ 645]; d[21] = src[ 677]; d[22] = src[ 709]; d[23] = src[ 741];
            d[24] = src[ 773]; d[25] = src[ 805]; d[26] = src[ 837]; d[27] = src[ 869];
            d[28] = src[ 901]; d[29] = src[ 933]; d[30] = src[ 965]; d[31] = src[ 997];
            d[32] = src[1029]; d[33] = src[1061]; d[34] = src[1093]; d[35] = src[1125];
            d[36] = src[1157]; d[37] = src[1189]; d[38] = src[1221]; d[39] = src[1253];
            d[40] = src[1285]; d[41] = src[1317]; d[42] = src[1349]; d[43] = src[1381];
            d[44] = src[1413]; d[45] = src[1445]; d[46] = src[1477]; d[47] = src[1509];
            d[48] = src[1541]; d[49] = src[1573]; d[50] = src[1605]; d[51] = src[1637];
            d[52] = src[1669]; d[53] = src[1701]; d[54] = src[1733]; d[55] = src[1765];
            d[56] = src[1797]; d[57] = src[1829]; d[58] = src[1861]; d[59] = src[1893];
            d[60] = src[1925]; d[61] = src[1957]; d[62] = src[1989]; d[63] = src[2021];
        }

        private static void demux_06 (byte[] src, byte[] d)
        {
            d[ 0] = src[   6]; d[ 1] = src[  38]; d[ 2] = src[  70]; d[ 3] = src[ 102];
            d[ 4] = src[ 134]; d[ 5] = src[ 166]; d[ 6] = src[ 198]; d[ 7] = src[ 230];
            d[ 8] = src[ 262]; d[ 9] = src[ 294]; d[10] = src[ 326]; d[11] = src[ 358];
            d[12] = src[ 390]; d[13] = src[ 422]; d[14] = src[ 454]; d[15] = src[ 486];
            d[16] = src[ 518]; d[17] = src[ 550]; d[18] = src[ 582]; d[19] = src[ 614];
            d[20] = src[ 646]; d[21] = src[ 678]; d[22] = src[ 710]; d[23] = src[ 742];
            d[24] = src[ 774]; d[25] = src[ 806]; d[26] = src[ 838]; d[27] = src[ 870];
            d[28] = src[ 902]; d[29] = src[ 934]; d[30] = src[ 966]; d[31] = src[ 998];
            d[32] = src[1030]; d[33] = src[1062]; d[34] = src[1094]; d[35] = src[1126];
            d[36] = src[1158]; d[37] = src[1190]; d[38] = src[1222]; d[39] = src[1254];
            d[40] = src[1286]; d[41] = src[1318]; d[42] = src[1350]; d[43] = src[1382];
            d[44] = src[1414]; d[45] = src[1446]; d[46] = src[1478]; d[47] = src[1510];
            d[48] = src[1542]; d[49] = src[1574]; d[50] = src[1606]; d[51] = src[1638];
            d[52] = src[1670]; d[53] = src[1702]; d[54] = src[1734]; d[55] = src[1766];
            d[56] = src[1798]; d[57] = src[1830]; d[58] = src[1862]; d[59] = src[1894];
            d[60] = src[1926]; d[61] = src[1958]; d[62] = src[1990]; d[63] = src[2022];
        }

        private static void demux_07 (byte[] src, byte[] d)
        {
            d[ 0] = src[   7]; d[ 1] = src[  39]; d[ 2] = src[  71]; d[ 3] = src[ 103];
            d[ 4] = src[ 135]; d[ 5] = src[ 167]; d[ 6] = src[ 199]; d[ 7] = src[ 231];
            d[ 8] = src[ 263]; d[ 9] = src[ 295]; d[10] = src[ 327]; d[11] = src[ 359];
            d[12] = src[ 391]; d[13] = src[ 423]; d[14] = src[ 455]; d[15] = src[ 487];
            d[16] = src[ 519]; d[17] = src[ 551]; d[18] = src[ 583]; d[19] = src[ 615];
            d[20] = src[ 647]; d[21] = src[ 679]; d[22] = src[ 711]; d[23] = src[ 743];
            d[24] = src[ 775]; d[25] = src[ 807]; d[26] = src[ 839]; d[27] = src[ 871];
            d[28] = src[ 903]; d[29] = src[ 935]; d[30] = src[ 967]; d[31] = src[ 999];
            d[32] = src[1031]; d[33] = src[1063]; d[34] = src[1095]; d[35] = src[1127];
            d[36] = src[1159]; d[37] = src[1191]; d[38] = src[1223]; d[39] = src[1255];
            d[40] = src[1287]; d[41] = src[1319]; d[42] = src[1351]; d[43] = src[1383];
            d[44] = src[1415]; d[45] = src[1447]; d[46] = src[1479]; d[47] = src[1511];
            d[48] = src[1543]; d[49] = src[1575]; d[50] = src[1607]; d[51] = src[1639];
            d[52] = src[1671]; d[53] = src[1703]; d[54] = src[1735]; d[55] = src[1767];
            d[56] = src[1799]; d[57] = src[1831]; d[58] = src[1863]; d[59] = src[1895];
            d[60] = src[1927]; d[61] = src[1959]; d[62] = src[1991]; d[63] = src[2023];
        }

        private static void demux_08 (byte[] src, byte[] d)
        {
            d[ 0] = src[   8]; d[ 1] = src[  40]; d[ 2] = src[  72]; d[ 3] = src[ 104];
            d[ 4] = src[ 136]; d[ 5] = src[ 168]; d[ 6] = src[ 200]; d[ 7] = src[ 232];
            d[ 8] = src[ 264]; d[ 9] = src[ 296]; d[10] = src[ 328]; d[11] = src[ 360];
            d[12] = src[ 392]; d[13] = src[ 424]; d[14] = src[ 456]; d[15] = src[ 488];
            d[16] = src[ 520]; d[17] = src[ 552]; d[18] = src[ 584]; d[19] = src[ 616];
            d[20] = src[ 648]; d[21] = src[ 680]; d[22] = src[ 712]; d[23] = src[ 744];
            d[24] = src[ 776]; d[25] = src[ 808]; d[26] = src[ 840]; d[27] = src[ 872];
            d[28] = src[ 904]; d[29] = src[ 936]; d[30] = src[ 968]; d[31] = src[1000];
            d[32] = src[1032]; d[33] = src[1064]; d[34] = src[1096]; d[35] = src[1128];
            d[36] = src[1160]; d[37] = src[1192]; d[38] = src[1224]; d[39] = src[1256];
            d[40] = src[1288]; d[41] = src[1320]; d[42] = src[1352]; d[43] = src[1384];
            d[44] = src[1416]; d[45] = src[1448]; d[46] = src[1480]; d[47] = src[1512];
            d[48] = src[1544]; d[49] = src[1576]; d[50] = src[1608]; d[51] = src[1640];
            d[52] = src[1672]; d[53] = src[1704]; d[54] = src[1736]; d[55] = src[1768];
            d[56] = src[1800]; d[57] = src[1832]; d[58] = src[1864]; d[59] = src[1896];
            d[60] = src[1928]; d[61] = src[1960]; d[62] = src[1992]; d[63] = src[2024];
        }

        private static void demux_09 (byte[] src, byte[] d)
        {
            d[ 0] = src[   9]; d[ 1] = src[  41]; d[ 2] = src[  73]; d[ 3] = src[ 105];
            d[ 4] = src[ 137]; d[ 5] = src[ 169]; d[ 6] = src[ 201]; d[ 7] = src[ 233];
            d[ 8] = src[ 265]; d[ 9] = src[ 297]; d[10] = src[ 329]; d[11] = src[ 361];
            d[12] = src[ 393]; d[13] = src[ 425]; d[14] = src[ 457]; d[15] = src[ 489];
            d[16] = src[ 521]; d[17] = src[ 553]; d[18] = src[ 585]; d[19] = src[ 617];
            d[20] = src[ 649]; d[21] = src[ 681]; d[22] = src[ 713]; d[23] = src[ 745];
            d[24] = src[ 777]; d[25] = src[ 809]; d[26] = src[ 841]; d[27] = src[ 873];
            d[28] = src[ 905]; d[29] = src[ 937]; d[30] = src[ 969]; d[31] = src[1001];
            d[32] = src[1033]; d[33] = src[1065]; d[34] = src[1097]; d[35] = src[1129];
            d[36] = src[1161]; d[37] = src[1193]; d[38] = src[1225]; d[39] = src[1257];
            d[40] = src[1289]; d[41] = src[1321]; d[42] = src[1353]; d[43] = src[1385];
            d[44] = src[1417]; d[45] = src[1449]; d[46] = src[1481]; d[47] = src[1513];
            d[48] = src[1545]; d[49] = src[1577]; d[50] = src[1609]; d[51] = src[1641];
            d[52] = src[1673]; d[53] = src[1705]; d[54] = src[1737]; d[55] = src[1769];
            d[56] = src[1801]; d[57] = src[1833]; d[58] = src[1865]; d[59] = src[1897];
            d[60] = src[1929]; d[61] = src[1961]; d[62] = src[1993]; d[63] = src[2025];
        }

        private static void demux_10 (byte[] src, byte[] d)
        {
            d[ 0] = src[  10]; d[ 1] = src[  42]; d[ 2] = src[  74]; d[ 3] = src[ 106];
            d[ 4] = src[ 138]; d[ 5] = src[ 170]; d[ 6] = src[ 202]; d[ 7] = src[ 234];
            d[ 8] = src[ 266]; d[ 9] = src[ 298]; d[10] = src[ 330]; d[11] = src[ 362];
            d[12] = src[ 394]; d[13] = src[ 426]; d[14] = src[ 458]; d[15] = src[ 490];
            d[16] = src[ 522]; d[17] = src[ 554]; d[18] = src[ 586]; d[19] = src[ 618];
            d[20] = src[ 650]; d[21] = src[ 682]; d[22] = src[ 714]; d[23] = src[ 746];
            d[24] = src[ 778]; d[25] = src[ 810]; d[26] = src[ 842]; d[27] = src[ 874];
            d[28] = src[ 906]; d[29] = src[ 938]; d[30] = src[ 970]; d[31] = src[1002];
            d[32] = src[1034]; d[33] = src[1066]; d[34] = src[1098]; d[35] = src[1130];
            d[36] = src[1162]; d[37] = src[1194]; d[38] = src[1226]; d[39] = src[1258];
            d[40] = src[1290]; d[41] = src[1322]; d[42] = src[1354]; d[43] = src[1386];
            d[44] = src[1418]; d[45] = src[1450]; d[46] = src[1482]; d[47] = src[1514];
            d[48] = src[1546]; d[49] = src[1578]; d[50] = src[1610]; d[51] = src[1642];
            d[52] = src[1674]; d[53] = src[1706]; d[54] = src[1738]; d[55] = src[1770];
            d[56] = src[1802]; d[57] = src[1834]; d[58] = src[1866]; d[59] = src[1898];
            d[60] = src[1930]; d[61] = src[1962]; d[62] = src[1994]; d[63] = src[2026];
        }

        private static void demux_11 (byte[] src, byte[] d)
        {
            d[ 0] = src[  11]; d[ 1] = src[  43]; d[ 2] = src[  75]; d[ 3] = src[ 107];
            d[ 4] = src[ 139]; d[ 5] = src[ 171]; d[ 6] = src[ 203]; d[ 7] = src[ 235];
            d[ 8] = src[ 267]; d[ 9] = src[ 299]; d[10] = src[ 331]; d[11] = src[ 363];
            d[12] = src[ 395]; d[13] = src[ 427]; d[14] = src[ 459]; d[15] = src[ 491];
            d[16] = src[ 523]; d[17] = src[ 555]; d[18] = src[ 587]; d[19] = src[ 619];
            d[20] = src[ 651]; d[21] = src[ 683]; d[22] = src[ 715]; d[23] = src[ 747];
            d[24] = src[ 779]; d[25] = src[ 811]; d[26] = src[ 843]; d[27] = src[ 875];
            d[28] = src[ 907]; d[29] = src[ 939]; d[30] = src[ 971]; d[31] = src[1003];
            d[32] = src[1035]; d[33] = src[1067]; d[34] = src[1099]; d[35] = src[1131];
            d[36] = src[1163]; d[37] = src[1195]; d[38] = src[1227]; d[39] = src[1259];
            d[40] = src[1291]; d[41] = src[1323]; d[42] = src[1355]; d[43] = src[1387];
            d[44] = src[1419]; d[45] = src[1451]; d[46] = src[1483]; d[47] = src[1515];
            d[48] = src[1547]; d[49] = src[1579]; d[50] = src[1611]; d[51] = src[1643];
            d[52] = src[1675]; d[53] = src[1707]; d[54] = src[1739]; d[55] = src[1771];
            d[56] = src[1803]; d[57] = src[1835]; d[58] = src[1867]; d[59] = src[1899];
            d[60] = src[1931]; d[61] = src[1963]; d[62] = src[1995]; d[63] = src[2027];
        }

        private static void demux_12 (byte[] src, byte[] d)
        {
            d[ 0] = src[  12]; d[ 1] = src[  44]; d[ 2] = src[  76]; d[ 3] = src[ 108];
            d[ 4] = src[ 140]; d[ 5] = src[ 172]; d[ 6] = src[ 204]; d[ 7] = src[ 236];
            d[ 8] = src[ 268]; d[ 9] = src[ 300]; d[10] = src[ 332]; d[11] = src[ 364];
            d[12] = src[ 396]; d[13] = src[ 428]; d[14] = src[ 460]; d[15] = src[ 492];
            d[16] = src[ 524]; d[17] = src[ 556]; d[18] = src[ 588]; d[19] = src[ 620];
            d[20] = src[ 652]; d[21] = src[ 684]; d[22] = src[ 716]; d[23] = src[ 748];
            d[24] = src[ 780]; d[25] = src[ 812]; d[26] = src[ 844]; d[27] = src[ 876];
            d[28] = src[ 908]; d[29] = src[ 940]; d[30] = src[ 972]; d[31] = src[1004];
            d[32] = src[1036]; d[33] = src[1068]; d[34] = src[1100]; d[35] = src[1132];
            d[36] = src[1164]; d[37] = src[1196]; d[38] = src[1228]; d[39] = src[1260];
            d[40] = src[1292]; d[41] = src[1324]; d[42] = src[1356]; d[43] = src[1388];
            d[44] = src[1420]; d[45] = src[1452]; d[46] = src[1484]; d[47] = src[1516];
            d[48] = src[1548]; d[49] = src[1580]; d[50] = src[1612]; d[51] = src[1644];
            d[52] = src[1676]; d[53] = src[1708]; d[54] = src[1740]; d[55] = src[1772];
            d[56] = src[1804]; d[57] = src[1836]; d[58] = src[1868]; d[59] = src[1900];
            d[60] = src[1932]; d[61] = src[1964]; d[62] = src[1996]; d[63] = src[2028];
        }

        private static void demux_13 (byte[] src, byte[] d)
        {
            d[ 0] = src[  13]; d[ 1] = src[  45]; d[ 2] = src[  77]; d[ 3] = src[ 109];
            d[ 4] = src[ 141]; d[ 5] = src[ 173]; d[ 6] = src[ 205]; d[ 7] = src[ 237];
            d[ 8] = src[ 269]; d[ 9] = src[ 301]; d[10] = src[ 333]; d[11] = src[ 365];
            d[12] = src[ 397]; d[13] = src[ 429]; d[14] = src[ 461]; d[15] = src[ 493];
            d[16] = src[ 525]; d[17] = src[ 557]; d[18] = src[ 589]; d[19] = src[ 621];
            d[20] = src[ 653]; d[21] = src[ 685]; d[22] = src[ 717]; d[23] = src[ 749];
            d[24] = src[ 781]; d[25] = src[ 813]; d[26] = src[ 845]; d[27] = src[ 877];
            d[28] = src[ 909]; d[29] = src[ 941]; d[30] = src[ 973]; d[31] = src[1005];
            d[32] = src[1037]; d[33] = src[1069]; d[34] = src[1101]; d[35] = src[1133];
            d[36] = src[1165]; d[37] = src[1197]; d[38] = src[1229]; d[39] = src[1261];
            d[40] = src[1293]; d[41] = src[1325]; d[42] = src[1357]; d[43] = src[1389];
            d[44] = src[1421]; d[45] = src[1453]; d[46] = src[1485]; d[47] = src[1517];
            d[48] = src[1549]; d[49] = src[1581]; d[50] = src[1613]; d[51] = src[1645];
            d[52] = src[1677]; d[53] = src[1709]; d[54] = src[1741]; d[55] = src[1773];
            d[56] = src[1805]; d[57] = src[1837]; d[58] = src[1869]; d[59] = src[1901];
            d[60] = src[1933]; d[61] = src[1965]; d[62] = src[1997]; d[63] = src[2029];
        }

        private static void demux_14 (byte[] src, byte[] d)
        {
            d[ 0] = src[  14]; d[ 1] = src[  46]; d[ 2] = src[  78]; d[ 3] = src[ 110];
            d[ 4] = src[ 142]; d[ 5] = src[ 174]; d[ 6] = src[ 206]; d[ 7] = src[ 238];
            d[ 8] = src[ 270]; d[ 9] = src[ 302]; d[10] = src[ 334]; d[11] = src[ 366];
            d[12] = src[ 398]; d[13] = src[ 430]; d[14] = src[ 462]; d[15] = src[ 494];
            d[16] = src[ 526]; d[17] = src[ 558]; d[18] = src[ 590]; d[19] = src[ 622];
            d[20] = src[ 654]; d[21] = src[ 686]; d[22] = src[ 718]; d[23] = src[ 750];
            d[24] = src[ 782]; d[25] = src[ 814]; d[26] = src[ 846]; d[27] = src[ 878];
            d[28] = src[ 910]; d[29] = src[ 942]; d[30] = src[ 974]; d[31] = src[1006];
            d[32] = src[1038]; d[33] = src[1070]; d[34] = src[1102]; d[35] = src[1134];
            d[36] = src[1166]; d[37] = src[1198]; d[38] = src[1230]; d[39] = src[1262];
            d[40] = src[1294]; d[41] = src[1326]; d[42] = src[1358]; d[43] = src[1390];
            d[44] = src[1422]; d[45] = src[1454]; d[46] = src[1486]; d[47] = src[1518];
            d[48] = src[1550]; d[49] = src[1582]; d[50] = src[1614]; d[51] = src[1646];
            d[52] = src[1678]; d[53] = src[1710]; d[54] = src[1742]; d[55] = src[1774];
            d[56] = src[1806]; d[57] = src[1838]; d[58] = src[1870]; d[59] = src[1902];
            d[60] = src[1934]; d[61] = src[1966]; d[62] = src[1998]; d[63] = src[2030];
        }

        private static void demux_15 (byte[] src, byte[] d)
        {
            d[ 0] = src[  15]; d[ 1] = src[  47]; d[ 2] = src[  79]; d[ 3] = src[ 111];
            d[ 4] = src[ 143]; d[ 5] = src[ 175]; d[ 6] = src[ 207]; d[ 7] = src[ 239];
            d[ 8] = src[ 271]; d[ 9] = src[ 303]; d[10] = src[ 335]; d[11] = src[ 367];
            d[12] = src[ 399]; d[13] = src[ 431]; d[14] = src[ 463]; d[15] = src[ 495];
            d[16] = src[ 527]; d[17] = src[ 559]; d[18] = src[ 591]; d[19] = src[ 623];
            d[20] = src[ 655]; d[21] = src[ 687]; d[22] = src[ 719]; d[23] = src[ 751];
            d[24] = src[ 783]; d[25] = src[ 815]; d[26] = src[ 847]; d[27] = src[ 879];
            d[28] = src[ 911]; d[29] = src[ 943]; d[30] = src[ 975]; d[31] = src[1007];
            d[32] = src[1039]; d[33] = src[1071]; d[34] = src[1103]; d[35] = src[1135];
            d[36] = src[1167]; d[37] = src[1199]; d[38] = src[1231]; d[39] = src[1263];
            d[40] = src[1295]; d[41] = src[1327]; d[42] = src[1359]; d[43] = src[1391];
            d[44] = src[1423]; d[45] = src[1455]; d[46] = src[1487]; d[47] = src[1519];
            d[48] = src[1551]; d[49] = src[1583]; d[50] = src[1615]; d[51] = src[1647];
            d[52] = src[1679]; d[53] = src[1711]; d[54] = src[1743]; d[55] = src[1775];
            d[56] = src[1807]; d[57] = src[1839]; d[58] = src[1871]; d[59] = src[1903];
            d[60] = src[1935]; d[61] = src[1967]; d[62] = src[1999]; d[63] = src[2031];
        }

        private static void demux_16 (byte[] src, byte[] d)
        {
            d[ 0] = src[  16]; d[ 1] = src[  48]; d[ 2] = src[  80]; d[ 3] = src[ 112];
            d[ 4] = src[ 144]; d[ 5] = src[ 176]; d[ 6] = src[ 208]; d[ 7] = src[ 240];
            d[ 8] = src[ 272]; d[ 9] = src[ 304]; d[10] = src[ 336]; d[11] = src[ 368];
            d[12] = src[ 400]; d[13] = src[ 432]; d[14] = src[ 464]; d[15] = src[ 496];
            d[16] = src[ 528]; d[17] = src[ 560]; d[18] = src[ 592]; d[19] = src[ 624];
            d[20] = src[ 656]; d[21] = src[ 688]; d[22] = src[ 720]; d[23] = src[ 752];
            d[24] = src[ 784]; d[25] = src[ 816]; d[26] = src[ 848]; d[27] = src[ 880];
            d[28] = src[ 912]; d[29] = src[ 944]; d[30] = src[ 976]; d[31] = src[1008];
            d[32] = src[1040]; d[33] = src[1072]; d[34] = src[1104]; d[35] = src[1136];
            d[36] = src[1168]; d[37] = src[1200]; d[38] = src[1232]; d[39] = src[1264];
            d[40] = src[1296]; d[41] = src[1328]; d[42] = src[1360]; d[43] = src[1392];
            d[44] = src[1424]; d[45] = src[1456]; d[46] = src[1488]; d[47] = src[1520];
            d[48] = src[1552]; d[49] = src[1584]; d[50] = src[1616]; d[51] = src[1648];
            d[52] = src[1680]; d[53] = src[1712]; d[54] = src[1744]; d[55] = src[1776];
            d[56] = src[1808]; d[57] = src[1840]; d[58] = src[1872]; d[59] = src[1904];
            d[60] = src[1936]; d[61] = src[1968]; d[62] = src[2000]; d[63] = src[2032];
        }

        private static void demux_17 (byte[] src, byte[] d)
        {
            d[ 0] = src[  17]; d[ 1] = src[  49]; d[ 2] = src[  81]; d[ 3] = src[ 113];
            d[ 4] = src[ 145]; d[ 5] = src[ 177]; d[ 6] = src[ 209]; d[ 7] = src[ 241];
            d[ 8] = src[ 273]; d[ 9] = src[ 305]; d[10] = src[ 337]; d[11] = src[ 369];
            d[12] = src[ 401]; d[13] = src[ 433]; d[14] = src[ 465]; d[15] = src[ 497];
            d[16] = src[ 529]; d[17] = src[ 561]; d[18] = src[ 593]; d[19] = src[ 625];
            d[20] = src[ 657]; d[21] = src[ 689]; d[22] = src[ 721]; d[23] = src[ 753];
            d[24] = src[ 785]; d[25] = src[ 817]; d[26] = src[ 849]; d[27] = src[ 881];
            d[28] = src[ 913]; d[29] = src[ 945]; d[30] = src[ 977]; d[31] = src[1009];
            d[32] = src[1041]; d[33] = src[1073]; d[34] = src[1105]; d[35] = src[1137];
            d[36] = src[1169]; d[37] = src[1201]; d[38] = src[1233]; d[39] = src[1265];
            d[40] = src[1297]; d[41] = src[1329]; d[42] = src[1361]; d[43] = src[1393];
            d[44] = src[1425]; d[45] = src[1457]; d[46] = src[1489]; d[47] = src[1521];
            d[48] = src[1553]; d[49] = src[1585]; d[50] = src[1617]; d[51] = src[1649];
            d[52] = src[1681]; d[53] = src[1713]; d[54] = src[1745]; d[55] = src[1777];
            d[56] = src[1809]; d[57] = src[1841]; d[58] = src[1873]; d[59] = src[1905];
            d[60] = src[1937]; d[61] = src[1969]; d[62] = src[2001]; d[63] = src[2033];
        }

        private static void demux_18 (byte[] src, byte[] d)
        {
            d[ 0] = src[  18]; d[ 1] = src[  50]; d[ 2] = src[  82]; d[ 3] = src[ 114];
            d[ 4] = src[ 146]; d[ 5] = src[ 178]; d[ 6] = src[ 210]; d[ 7] = src[ 242];
            d[ 8] = src[ 274]; d[ 9] = src[ 306]; d[10] = src[ 338]; d[11] = src[ 370];
            d[12] = src[ 402]; d[13] = src[ 434]; d[14] = src[ 466]; d[15] = src[ 498];
            d[16] = src[ 530]; d[17] = src[ 562]; d[18] = src[ 594]; d[19] = src[ 626];
            d[20] = src[ 658]; d[21] = src[ 690]; d[22] = src[ 722]; d[23] = src[ 754];
            d[24] = src[ 786]; d[25] = src[ 818]; d[26] = src[ 850]; d[27] = src[ 882];
            d[28] = src[ 914]; d[29] = src[ 946]; d[30] = src[ 978]; d[31] = src[1010];
            d[32] = src[1042]; d[33] = src[1074]; d[34] = src[1106]; d[35] = src[1138];
            d[36] = src[1170]; d[37] = src[1202]; d[38] = src[1234]; d[39] = src[1266];
            d[40] = src[1298]; d[41] = src[1330]; d[42] = src[1362]; d[43] = src[1394];
            d[44] = src[1426]; d[45] = src[1458]; d[46] = src[1490]; d[47] = src[1522];
            d[48] = src[1554]; d[49] = src[1586]; d[50] = src[1618]; d[51] = src[1650];
            d[52] = src[1682]; d[53] = src[1714]; d[54] = src[1746]; d[55] = src[1778];
            d[56] = src[1810]; d[57] = src[1842]; d[58] = src[1874]; d[59] = src[1906];
            d[60] = src[1938]; d[61] = src[1970]; d[62] = src[2002]; d[63] = src[2034];
        }

        private static void demux_19 (byte[] src, byte[] d)
        {
            d[ 0] = src[  19]; d[ 1] = src[  51]; d[ 2] = src[  83]; d[ 3] = src[ 115];
            d[ 4] = src[ 147]; d[ 5] = src[ 179]; d[ 6] = src[ 211]; d[ 7] = src[ 243];
            d[ 8] = src[ 275]; d[ 9] = src[ 307]; d[10] = src[ 339]; d[11] = src[ 371];
            d[12] = src[ 403]; d[13] = src[ 435]; d[14] = src[ 467]; d[15] = src[ 499];
            d[16] = src[ 531]; d[17] = src[ 563]; d[18] = src[ 595]; d[19] = src[ 627];
            d[20] = src[ 659]; d[21] = src[ 691]; d[22] = src[ 723]; d[23] = src[ 755];
            d[24] = src[ 787]; d[25] = src[ 819]; d[26] = src[ 851]; d[27] = src[ 883];
            d[28] = src[ 915]; d[29] = src[ 947]; d[30] = src[ 979]; d[31] = src[1011];
            d[32] = src[1043]; d[33] = src[1075]; d[34] = src[1107]; d[35] = src[1139];
            d[36] = src[1171]; d[37] = src[1203]; d[38] = src[1235]; d[39] = src[1267];
            d[40] = src[1299]; d[41] = src[1331]; d[42] = src[1363]; d[43] = src[1395];
            d[44] = src[1427]; d[45] = src[1459]; d[46] = src[1491]; d[47] = src[1523];
            d[48] = src[1555]; d[49] = src[1587]; d[50] = src[1619]; d[51] = src[1651];
            d[52] = src[1683]; d[53] = src[1715]; d[54] = src[1747]; d[55] = src[1779];
            d[56] = src[1811]; d[57] = src[1843]; d[58] = src[1875]; d[59] = src[1907];
            d[60] = src[1939]; d[61] = src[1971]; d[62] = src[2003]; d[63] = src[2035];
        }

        private static void demux_20 (byte[] src, byte[] d)
        {
            d[ 0] = src[  20]; d[ 1] = src[  52]; d[ 2] = src[  84]; d[ 3] = src[ 116];
            d[ 4] = src[ 148]; d[ 5] = src[ 180]; d[ 6] = src[ 212]; d[ 7] = src[ 244];
            d[ 8] = src[ 276]; d[ 9] = src[ 308]; d[10] = src[ 340]; d[11] = src[ 372];
            d[12] = src[ 404]; d[13] = src[ 436]; d[14] = src[ 468]; d[15] = src[ 500];
            d[16] = src[ 532]; d[17] = src[ 564]; d[18] = src[ 596]; d[19] = src[ 628];
            d[20] = src[ 660]; d[21] = src[ 692]; d[22] = src[ 724]; d[23] = src[ 756];
            d[24] = src[ 788]; d[25] = src[ 820]; d[26] = src[ 852]; d[27] = src[ 884];
            d[28] = src[ 916]; d[29] = src[ 948]; d[30] = src[ 980]; d[31] = src[1012];
            d[32] = src[1044]; d[33] = src[1076]; d[34] = src[1108]; d[35] = src[1140];
            d[36] = src[1172]; d[37] = src[1204]; d[38] = src[1236]; d[39] = src[1268];
            d[40] = src[1300]; d[41] = src[1332]; d[42] = src[1364]; d[43] = src[1396];
            d[44] = src[1428]; d[45] = src[1460]; d[46] = src[1492]; d[47] = src[1524];
            d[48] = src[1556]; d[49] = src[1588]; d[50] = src[1620]; d[51] = src[1652];
            d[52] = src[1684]; d[53] = src[1716]; d[54] = src[1748]; d[55] = src[1780];
            d[56] = src[1812]; d[57] = src[1844]; d[58] = src[1876]; d[59] = src[1908];
            d[60] = src[1940]; d[61] = src[1972]; d[62] = src[2004]; d[63] = src[2036];
        }

        private static void demux_21 (byte[] src, byte[] d)
        {
            d[ 0] = src[  21]; d[ 1] = src[  53]; d[ 2] = src[  85]; d[ 3] = src[ 117];
            d[ 4] = src[ 149]; d[ 5] = src[ 181]; d[ 6] = src[ 213]; d[ 7] = src[ 245];
            d[ 8] = src[ 277]; d[ 9] = src[ 309]; d[10] = src[ 341]; d[11] = src[ 373];
            d[12] = src[ 405]; d[13] = src[ 437]; d[14] = src[ 469]; d[15] = src[ 501];
            d[16] = src[ 533]; d[17] = src[ 565]; d[18] = src[ 597]; d[19] = src[ 629];
            d[20] = src[ 661]; d[21] = src[ 693]; d[22] = src[ 725]; d[23] = src[ 757];
            d[24] = src[ 789]; d[25] = src[ 821]; d[26] = src[ 853]; d[27] = src[ 885];
            d[28] = src[ 917]; d[29] = src[ 949]; d[30] = src[ 981]; d[31] = src[1013];
            d[32] = src[1045]; d[33] = src[1077]; d[34] = src[1109]; d[35] = src[1141];
            d[36] = src[1173]; d[37] = src[1205]; d[38] = src[1237]; d[39] = src[1269];
            d[40] = src[1301]; d[41] = src[1333]; d[42] = src[1365]; d[43] = src[1397];
            d[44] = src[1429]; d[45] = src[1461]; d[46] = src[1493]; d[47] = src[1525];
            d[48] = src[1557]; d[49] = src[1589]; d[50] = src[1621]; d[51] = src[1653];
            d[52] = src[1685]; d[53] = src[1717]; d[54] = src[1749]; d[55] = src[1781];
            d[56] = src[1813]; d[57] = src[1845]; d[58] = src[1877]; d[59] = src[1909];
            d[60] = src[1941]; d[61] = src[1973]; d[62] = src[2005]; d[63] = src[2037];
        }

        private static void demux_22 (byte[] src, byte[] d)
        {
            d[ 0] = src[  22]; d[ 1] = src[  54]; d[ 2] = src[  86]; d[ 3] = src[ 118];
            d[ 4] = src[ 150]; d[ 5] = src[ 182]; d[ 6] = src[ 214]; d[ 7] = src[ 246];
            d[ 8] = src[ 278]; d[ 9] = src[ 310]; d[10] = src[ 342]; d[11] = src[ 374];
            d[12] = src[ 406]; d[13] = src[ 438]; d[14] = src[ 470]; d[15] = src[ 502];
            d[16] = src[ 534]; d[17] = src[ 566]; d[18] = src[ 598]; d[19] = src[ 630];
            d[20] = src[ 662]; d[21] = src[ 694]; d[22] = src[ 726]; d[23] = src[ 758];
            d[24] = src[ 790]; d[25] = src[ 822]; d[26] = src[ 854]; d[27] = src[ 886];
            d[28] = src[ 918]; d[29] = src[ 950]; d[30] = src[ 982]; d[31] = src[1014];
            d[32] = src[1046]; d[33] = src[1078]; d[34] = src[1110]; d[35] = src[1142];
            d[36] = src[1174]; d[37] = src[1206]; d[38] = src[1238]; d[39] = src[1270];
            d[40] = src[1302]; d[41] = src[1334]; d[42] = src[1366]; d[43] = src[1398];
            d[44] = src[1430]; d[45] = src[1462]; d[46] = src[1494]; d[47] = src[1526];
            d[48] = src[1558]; d[49] = src[1590]; d[50] = src[1622]; d[51] = src[1654];
            d[52] = src[1686]; d[53] = src[1718]; d[54] = src[1750]; d[55] = src[1782];
            d[56] = src[1814]; d[57] = src[1846]; d[58] = src[1878]; d[59] = src[1910];
            d[60] = src[1942]; d[61] = src[1974]; d[62] = src[2006]; d[63] = src[2038];
        }

        private static void demux_23 (byte[] src, byte[] d)
        {
            d[ 0] = src[  23]; d[ 1] = src[  55]; d[ 2] = src[  87]; d[ 3] = src[ 119];
            d[ 4] = src[ 151]; d[ 5] = src[ 183]; d[ 6] = src[ 215]; d[ 7] = src[ 247];
            d[ 8] = src[ 279]; d[ 9] = src[ 311]; d[10] = src[ 343]; d[11] = src[ 375];
            d[12] = src[ 407]; d[13] = src[ 439]; d[14] = src[ 471]; d[15] = src[ 503];
            d[16] = src[ 535]; d[17] = src[ 567]; d[18] = src[ 599]; d[19] = src[ 631];
            d[20] = src[ 663]; d[21] = src[ 695]; d[22] = src[ 727]; d[23] = src[ 759];
            d[24] = src[ 791]; d[25] = src[ 823]; d[26] = src[ 855]; d[27] = src[ 887];
            d[28] = src[ 919]; d[29] = src[ 951]; d[30] = src[ 983]; d[31] = src[1015];
            d[32] = src[1047]; d[33] = src[1079]; d[34] = src[1111]; d[35] = src[1143];
            d[36] = src[1175]; d[37] = src[1207]; d[38] = src[1239]; d[39] = src[1271];
            d[40] = src[1303]; d[41] = src[1335]; d[42] = src[1367]; d[43] = src[1399];
            d[44] = src[1431]; d[45] = src[1463]; d[46] = src[1495]; d[47] = src[1527];
            d[48] = src[1559]; d[49] = src[1591]; d[50] = src[1623]; d[51] = src[1655];
            d[52] = src[1687]; d[53] = src[1719]; d[54] = src[1751]; d[55] = src[1783];
            d[56] = src[1815]; d[57] = src[1847]; d[58] = src[1879]; d[59] = src[1911];
            d[60] = src[1943]; d[61] = src[1975]; d[62] = src[2007]; d[63] = src[2039];
        }

        private static void demux_24 (byte[] src, byte[] d)
        {
            d[ 0] = src[  24]; d[ 1] = src[  56]; d[ 2] = src[  88]; d[ 3] = src[ 120];
            d[ 4] = src[ 152]; d[ 5] = src[ 184]; d[ 6] = src[ 216]; d[ 7] = src[ 248];
            d[ 8] = src[ 280]; d[ 9] = src[ 312]; d[10] = src[ 344]; d[11] = src[ 376];
            d[12] = src[ 408]; d[13] = src[ 440]; d[14] = src[ 472]; d[15] = src[ 504];
            d[16] = src[ 536]; d[17] = src[ 568]; d[18] = src[ 600]; d[19] = src[ 632];
            d[20] = src[ 664]; d[21] = src[ 696]; d[22] = src[ 728]; d[23] = src[ 760];
            d[24] = src[ 792]; d[25] = src[ 824]; d[26] = src[ 856]; d[27] = src[ 888];
            d[28] = src[ 920]; d[29] = src[ 952]; d[30] = src[ 984]; d[31] = src[1016];
            d[32] = src[1048]; d[33] = src[1080]; d[34] = src[1112]; d[35] = src[1144];
            d[36] = src[1176]; d[37] = src[1208]; d[38] = src[1240]; d[39] = src[1272];
            d[40] = src[1304]; d[41] = src[1336]; d[42] = src[1368]; d[43] = src[1400];
            d[44] = src[1432]; d[45] = src[1464]; d[46] = src[1496]; d[47] = src[1528];
            d[48] = src[1560]; d[49] = src[1592]; d[50] = src[1624]; d[51] = src[1656];
            d[52] = src[1688]; d[53] = src[1720]; d[54] = src[1752]; d[55] = src[1784];
            d[56] = src[1816]; d[57] = src[1848]; d[58] = src[1880]; d[59] = src[1912];
            d[60] = src[1944]; d[61] = src[1976]; d[62] = src[2008]; d[63] = src[2040];
        }

        private static void demux_25 (byte[] src, byte[] d)
        {
            d[ 0] = src[  25]; d[ 1] = src[  57]; d[ 2] = src[  89]; d[ 3] = src[ 121];
            d[ 4] = src[ 153]; d[ 5] = src[ 185]; d[ 6] = src[ 217]; d[ 7] = src[ 249];
            d[ 8] = src[ 281]; d[ 9] = src[ 313]; d[10] = src[ 345]; d[11] = src[ 377];
            d[12] = src[ 409]; d[13] = src[ 441]; d[14] = src[ 473]; d[15] = src[ 505];
            d[16] = src[ 537]; d[17] = src[ 569]; d[18] = src[ 601]; d[19] = src[ 633];
            d[20] = src[ 665]; d[21] = src[ 697]; d[22] = src[ 729]; d[23] = src[ 761];
            d[24] = src[ 793]; d[25] = src[ 825]; d[26] = src[ 857]; d[27] = src[ 889];
            d[28] = src[ 921]; d[29] = src[ 953]; d[30] = src[ 985]; d[31] = src[1017];
            d[32] = src[1049]; d[33] = src[1081]; d[34] = src[1113]; d[35] = src[1145];
            d[36] = src[1177]; d[37] = src[1209]; d[38] = src[1241]; d[39] = src[1273];
            d[40] = src[1305]; d[41] = src[1337]; d[42] = src[1369]; d[43] = src[1401];
            d[44] = src[1433]; d[45] = src[1465]; d[46] = src[1497]; d[47] = src[1529];
            d[48] = src[1561]; d[49] = src[1593]; d[50] = src[1625]; d[51] = src[1657];
            d[52] = src[1689]; d[53] = src[1721]; d[54] = src[1753]; d[55] = src[1785];
            d[56] = src[1817]; d[57] = src[1849]; d[58] = src[1881]; d[59] = src[1913];
            d[60] = src[1945]; d[61] = src[1977]; d[62] = src[2009]; d[63] = src[2041];
        }

        private static void demux_26 (byte[] src, byte[] d)
        {
            d[ 0] = src[  26]; d[ 1] = src[  58]; d[ 2] = src[  90]; d[ 3] = src[ 122];
            d[ 4] = src[ 154]; d[ 5] = src[ 186]; d[ 6] = src[ 218]; d[ 7] = src[ 250];
            d[ 8] = src[ 282]; d[ 9] = src[ 314]; d[10] = src[ 346]; d[11] = src[ 378];
            d[12] = src[ 410]; d[13] = src[ 442]; d[14] = src[ 474]; d[15] = src[ 506];
            d[16] = src[ 538]; d[17] = src[ 570]; d[18] = src[ 602]; d[19] = src[ 634];
            d[20] = src[ 666]; d[21] = src[ 698]; d[22] = src[ 730]; d[23] = src[ 762];
            d[24] = src[ 794]; d[25] = src[ 826]; d[26] = src[ 858]; d[27] = src[ 890];
            d[28] = src[ 922]; d[29] = src[ 954]; d[30] = src[ 986]; d[31] = src[1018];
            d[32] = src[1050]; d[33] = src[1082]; d[34] = src[1114]; d[35] = src[1146];
            d[36] = src[1178]; d[37] = src[1210]; d[38] = src[1242]; d[39] = src[1274];
            d[40] = src[1306]; d[41] = src[1338]; d[42] = src[1370]; d[43] = src[1402];
            d[44] = src[1434]; d[45] = src[1466]; d[46] = src[1498]; d[47] = src[1530];
            d[48] = src[1562]; d[49] = src[1594]; d[50] = src[1626]; d[51] = src[1658];
            d[52] = src[1690]; d[53] = src[1722]; d[54] = src[1754]; d[55] = src[1786];
            d[56] = src[1818]; d[57] = src[1850]; d[58] = src[1882]; d[59] = src[1914];
            d[60] = src[1946]; d[61] = src[1978]; d[62] = src[2010]; d[63] = src[2042];
        }

        private static void demux_27 (byte[] src, byte[] d)
        {
            d[ 0] = src[  27]; d[ 1] = src[  59]; d[ 2] = src[  91]; d[ 3] = src[ 123];
            d[ 4] = src[ 155]; d[ 5] = src[ 187]; d[ 6] = src[ 219]; d[ 7] = src[ 251];
            d[ 8] = src[ 283]; d[ 9] = src[ 315]; d[10] = src[ 347]; d[11] = src[ 379];
            d[12] = src[ 411]; d[13] = src[ 443]; d[14] = src[ 475]; d[15] = src[ 507];
            d[16] = src[ 539]; d[17] = src[ 571]; d[18] = src[ 603]; d[19] = src[ 635];
            d[20] = src[ 667]; d[21] = src[ 699]; d[22] = src[ 731]; d[23] = src[ 763];
            d[24] = src[ 795]; d[25] = src[ 827]; d[26] = src[ 859]; d[27] = src[ 891];
            d[28] = src[ 923]; d[29] = src[ 955]; d[30] = src[ 987]; d[31] = src[1019];
            d[32] = src[1051]; d[33] = src[1083]; d[34] = src[1115]; d[35] = src[1147];
            d[36] = src[1179]; d[37] = src[1211]; d[38] = src[1243]; d[39] = src[1275];
            d[40] = src[1307]; d[41] = src[1339]; d[42] = src[1371]; d[43] = src[1403];
            d[44] = src[1435]; d[45] = src[1467]; d[46] = src[1499]; d[47] = src[1531];
            d[48] = src[1563]; d[49] = src[1595]; d[50] = src[1627]; d[51] = src[1659];
            d[52] = src[1691]; d[53] = src[1723]; d[54] = src[1755]; d[55] = src[1787];
            d[56] = src[1819]; d[57] = src[1851]; d[58] = src[1883]; d[59] = src[1915];
            d[60] = src[1947]; d[61] = src[1979]; d[62] = src[2011]; d[63] = src[2043];
        }

        private static void demux_28 (byte[] src, byte[] d)
        {
            d[ 0] = src[  28]; d[ 1] = src[  60]; d[ 2] = src[  92]; d[ 3] = src[ 124];
            d[ 4] = src[ 156]; d[ 5] = src[ 188]; d[ 6] = src[ 220]; d[ 7] = src[ 252];
            d[ 8] = src[ 284]; d[ 9] = src[ 316]; d[10] = src[ 348]; d[11] = src[ 380];
            d[12] = src[ 412]; d[13] = src[ 444]; d[14] = src[ 476]; d[15] = src[ 508];
            d[16] = src[ 540]; d[17] = src[ 572]; d[18] = src[ 604]; d[19] = src[ 636];
            d[20] = src[ 668]; d[21] = src[ 700]; d[22] = src[ 732]; d[23] = src[ 764];
            d[24] = src[ 796]; d[25] = src[ 828]; d[26] = src[ 860]; d[27] = src[ 892];
            d[28] = src[ 924]; d[29] = src[ 956]; d[30] = src[ 988]; d[31] = src[1020];
            d[32] = src[1052]; d[33] = src[1084]; d[34] = src[1116]; d[35] = src[1148];
            d[36] = src[1180]; d[37] = src[1212]; d[38] = src[1244]; d[39] = src[1276];
            d[40] = src[1308]; d[41] = src[1340]; d[42] = src[1372]; d[43] = src[1404];
            d[44] = src[1436]; d[45] = src[1468]; d[46] = src[1500]; d[47] = src[1532];
            d[48] = src[1564]; d[49] = src[1596]; d[50] = src[1628]; d[51] = src[1660];
            d[52] = src[1692]; d[53] = src[1724]; d[54] = src[1756]; d[55] = src[1788];
            d[56] = src[1820]; d[57] = src[1852]; d[58] = src[1884]; d[59] = src[1916];
            d[60] = src[1948]; d[61] = src[1980]; d[62] = src[2012]; d[63] = src[2044];
        }

        private static void demux_29 (byte[] src, byte[] d)
        {
            d[ 0] = src[  29]; d[ 1] = src[  61]; d[ 2] = src[  93]; d[ 3] = src[ 125];
            d[ 4] = src[ 157]; d[ 5] = src[ 189]; d[ 6] = src[ 221]; d[ 7] = src[ 253];
            d[ 8] = src[ 285]; d[ 9] = src[ 317]; d[10] = src[ 349]; d[11] = src[ 381];
            d[12] = src[ 413]; d[13] = src[ 445]; d[14] = src[ 477]; d[15] = src[ 509];
            d[16] = src[ 541]; d[17] = src[ 573]; d[18] = src[ 605]; d[19] = src[ 637];
            d[20] = src[ 669]; d[21] = src[ 701]; d[22] = src[ 733]; d[23] = src[ 765];
            d[24] = src[ 797]; d[25] = src[ 829]; d[26] = src[ 861]; d[27] = src[ 893];
            d[28] = src[ 925]; d[29] = src[ 957]; d[30] = src[ 989]; d[31] = src[1021];
            d[32] = src[1053]; d[33] = src[1085]; d[34] = src[1117]; d[35] = src[1149];
            d[36] = src[1181]; d[37] = src[1213]; d[38] = src[1245]; d[39] = src[1277];
            d[40] = src[1309]; d[41] = src[1341]; d[42] = src[1373]; d[43] = src[1405];
            d[44] = src[1437]; d[45] = src[1469]; d[46] = src[1501]; d[47] = src[1533];
            d[48] = src[1565]; d[49] = src[1597]; d[50] = src[1629]; d[51] = src[1661];
            d[52] = src[1693]; d[53] = src[1725]; d[54] = src[1757]; d[55] = src[1789];
            d[56] = src[1821]; d[57] = src[1853]; d[58] = src[1885]; d[59] = src[1917];
            d[60] = src[1949]; d[61] = src[1981]; d[62] = src[2013]; d[63] = src[2045];
        }

        private static void demux_30 (byte[] src, byte[] d)
        {
            d[ 0] = src[  30]; d[ 1] = src[  62]; d[ 2] = src[  94]; d[ 3] = src[ 126];
            d[ 4] = src[ 158]; d[ 5] = src[ 190]; d[ 6] = src[ 222]; d[ 7] = src[ 254];
            d[ 8] = src[ 286]; d[ 9] = src[ 318]; d[10] = src[ 350]; d[11] = src[ 382];
            d[12] = src[ 414]; d[13] = src[ 446]; d[14] = src[ 478]; d[15] = src[ 510];
            d[16] = src[ 542]; d[17] = src[ 574]; d[18] = src[ 606]; d[19] = src[ 638];
            d[20] = src[ 670]; d[21] = src[ 702]; d[22] = src[ 734]; d[23] = src[ 766];
            d[24] = src[ 798]; d[25] = src[ 830]; d[26] = src[ 862]; d[27] = src[ 894];
            d[28] = src[ 926]; d[29] = src[ 958]; d[30] = src[ 990]; d[31] = src[1022];
            d[32] = src[1054]; d[33] = src[1086]; d[34] = src[1118]; d[35] = src[1150];
            d[36] = src[1182]; d[37] = src[1214]; d[38] = src[1246]; d[39] = src[1278];
            d[40] = src[1310]; d[41] = src[1342]; d[42] = src[1374]; d[43] = src[1406];
            d[44] = src[1438]; d[45] = src[1470]; d[46] = src[1502]; d[47] = src[1534];
            d[48] = src[1566]; d[49] = src[1598]; d[50] = src[1630]; d[51] = src[1662];
            d[52] = src[1694]; d[53] = src[1726]; d[54] = src[1758]; d[55] = src[1790];
            d[56] = src[1822]; d[57] = src[1854]; d[58] = src[1886]; d[59] = src[1918];
            d[60] = src[1950]; d[61] = src[1982]; d[62] = src[2014]; d[63] = src[2046];
        }

        private static void demux_31 (byte[] src, byte[] d)
        {
            d[ 0] = src[  31]; d[ 1] = src[  63]; d[ 2] = src[  95]; d[ 3] = src[ 127];
            d[ 4] = src[ 159]; d[ 5] = src[ 191]; d[ 6] = src[ 223]; d[ 7] = src[ 255];
            d[ 8] = src[ 287]; d[ 9] = src[ 319]; d[10] = src[ 351]; d[11] = src[ 383];
            d[12] = src[ 415]; d[13] = src[ 447]; d[14] = src[ 479]; d[15] = src[ 511];
            d[16] = src[ 543]; d[17] = src[ 575]; d[18] = src[ 607]; d[19] = src[ 639];
            d[20] = src[ 671]; d[21] = src[ 703]; d[22] = src[ 735]; d[23] = src[ 767];
            d[24] = src[ 799]; d[25] = src[ 831]; d[26] = src[ 863]; d[27] = src[ 895];
            d[28] = src[ 927]; d[29] = src[ 959]; d[30] = src[ 991]; d[31] = src[1023];
            d[32] = src[1055]; d[33] = src[1087]; d[34] = src[1119]; d[35] = src[1151];
            d[36] = src[1183]; d[37] = src[1215]; d[38] = src[1247]; d[39] = src[1279];
            d[40] = src[1311]; d[41] = src[1343]; d[42] = src[1375]; d[43] = src[1407];
            d[44] = src[1439]; d[45] = src[1471]; d[46] = src[1503]; d[47] = src[1535];
            d[48] = src[1567]; d[49] = src[1599]; d[50] = src[1631]; d[51] = src[1663];
            d[52] = src[1695]; d[53] = src[1727]; d[54] = src[1759]; d[55] = src[1791];
            d[56] = src[1823]; d[57] = src[1855]; d[58] = src[1887]; d[59] = src[1919];
            d[60] = src[1951]; d[61] = src[1983]; d[62] = src[2015]; d[63] = src[2047];
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
//      measure (new Unrolled_1 ());
//      measure (new Unrolled_2_Full ());
//      measure (new Unrolled_3 ());
    }
}
