package net.quantum6.mediacodec;

import net.quantum6.kit.Log;

public class H264SpsParser
{
    private final static String TAG = H264SpsParser.class.getCanonicalName();
    private final static int NAL_HEADER = 4;
    
    private static int nStartBit = 0;
    

    private static int Ue(byte[] pBuff, int nLen)
    {
        int nZeroNum = 0;
        while (nStartBit < nLen * 8)
        {
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                break;
            }
            nZeroNum++;
            nStartBit++;
        }
        nStartBit ++;
    
        int dwRet = 0;
        for (int i=0; i<nZeroNum; i++)
        {
            dwRet <<= 1;
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                dwRet += 1;
            }
            nStartBit++;
        }
        return (1 << nZeroNum) - 1 + dwRet;
    }

    private static int Se(byte[] pBuff, int nLen)
    {
        int UeVal =Ue(pBuff,nLen);
        double k  =UeVal;
        int nValue=(int)Math.ceil(k/2);
        if (UeVal % 2==0)
        {    
            nValue=-nValue;
        }
        return nValue;
    }
    
    private static int u(int BitCount, byte[] buf)
    {
        int dwRet = 0;
        for (int i=0; i<BitCount; i++)
        {
            dwRet <<= 1;
            if ((buf[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                dwRet += 1;
            }
            nStartBit++;
        }
        return dwRet;
    }
    
    private static boolean h264_decode_seq_parameter_set(byte[] buf, int nLen, int[] size)
    {
        nStartBit = NAL_HEADER*8;
        int forbidden_zero_bit=u(1, buf);
        int nal_ref_idc       =u(2, buf);
        int nal_unit_type     =u(5, buf);
        if(nal_unit_type==7)
        {
            int profile_idc         =u(8, buf);
            int constraint_set0_flag=u(1, buf);//(buf[1] & 0x80)>>7;
            int constraint_set1_flag=u(1, buf);//(buf[1] & 0x40)>>6;
            int constraint_set2_flag=u(1, buf);//(buf[1] & 0x20)>>5;
            int constraint_set3_flag=u(1, buf);//(buf[1] & 0x10)>>4;
            int reserved_zero_4bits =u(4, buf);
            int level_idc           =u(8, buf);
    
            int seq_parameter_set_id=Ue(buf, nLen);
    
            if ( profile_idc == 100 || profile_idc == 110 ||
                 profile_idc == 122 || profile_idc == 144 )
            {
                int chroma_format_idc=Ue(buf, nLen);
                if ( chroma_format_idc == 3 )
                {
                    int residual_colour_transform_flag=u(1,buf);
                }
                int bit_depth_luma_minus8               =Ue(buf, nLen);
                int bit_depth_chroma_minus8             =Ue(buf, nLen);
                int qpprime_y_zero_transform_bypass_flag=u(1, buf);
                int seq_scaling_matrix_present_flag     =u(1, buf);
    
                int[] seq_scaling_list_present_flag = new int[8];
                if ( seq_scaling_matrix_present_flag != 0)
                {
                    for( int i = 0; i < 8; i++ )
                    {
                        seq_scaling_list_present_flag[i]=u(1, buf);
                    }
                }
            }
            int log2_max_frame_num_minus4=Ue(buf, nLen);
            int pic_order_cnt_type       =Ue(buf, nLen);
            if ( pic_order_cnt_type == 0 )
            {
                int log2_max_pic_order_cnt_lsb_minus4=Ue(buf, nLen);
            }
            else if( pic_order_cnt_type == 1 )
            {
                int delta_pic_order_always_zero_flag     =u(1,     buf);
                int offset_for_non_ref_pic               =Se(buf, nLen);
                int offset_for_top_to_bottom_field       =Se(buf, nLen);
                int num_ref_frames_in_pic_order_cnt_cycle=Ue(buf, nLen);
    
                int[] offset_for_ref_frame=new int[num_ref_frames_in_pic_order_cnt_cycle];
                for( int i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++ )
                {
                    offset_for_ref_frame[i]=Se(buf, nLen);
                }
            }
            int num_ref_frames                      =Ue(buf, nLen);
            int gaps_in_frame_num_value_allowed_flag=u(1,     buf);
            int pic_width_in_mbs_minus1             =Ue(buf, nLen);
            int pic_height_in_map_units_minus1      =Ue(buf, nLen);
    
            size[0]=(pic_width_in_mbs_minus1       +1)*16;
            size[1]=(pic_height_in_map_units_minus1+1)*16;
    
            return true;
        }
        return false;
    }

    public static int[] getSizeFromSps(byte[] data)
    {
        if (data[0]==0 && data[1]==0 && data[2]==0 && data[3]==1 && data[4]==0x67)
        {
            int[] size = new int[2];
            h264_decode_seq_parameter_set(data, data.length, size);
            Log.d(TAG, "Sps=("+size[0]+", "+size[1]+")");
            return size;
        }
        return null;
    }
    
}
