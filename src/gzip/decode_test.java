package gzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class decode_test {
    //minimal match size = 3
	static int minmatch = 3;
    //window size 64kb
	static int maxwindowsize = 70<<10;
	static int fsize = 9<<20;
	static int dfcount = 0;
    //max distance
	static int halfwindow = 32<<10;
    //buf size
    static int bufsize = 80<<10;
    static int min_lookahead = 3 ;
    static byte window[] = new byte[maxwindowsize];
    static byte sfile[] = new byte[fsize];
    static byte l_buf[] = new byte[bufsize]; //non-matched byte or (matched length - 131)
    static short d_buf[] = new short[bufsize]; //matched distance
    static boolean flag_buf[] = new boolean[bufsize]; //true -> (matched length - 131); false -> non-matched byte
    static int hfm_buf[] = new int[bufsize*2];
    static int lookahead;
    static int bufcount;
    static int textpos = 0;
    static boolean eoflag = false;
    static Vector<Character> text = new Vector<Character>();
    //static FileInputStream in;
    static FileOutputStream out;
    static Hashtable<String,Vector<pos_int> > hasht = new Hashtable<String,Vector<pos_int> >(maxwindowsize);// String - Vector int
    static int readblock(inbits ib){
    	ib.textin(text, textpos);
    	int length = ib.read_int16(text);
    	if(length==0){
    		eoflag=true;
    		return 0;
    	}
    	textpos = 0;
    	ib.textin(text, textpos);
    	if(text.size()<ib.maxtextsize){
    		eoflag=true;
    	}
    	Huffman hf = new Huffman();
    	hfm_buf = new int[bufsize];
    	int pos = hf.Decode(text, hfm_buf, length, 9);
    	for(int i=0;i<length;++i){
    		if((hfm_buf[i]&0x100) == 0){
    			flag_buf[i]=false;
    		}
    		else{
    			flag_buf[i]=true;
    		}
    		l_buf[i] = (byte)(hfm_buf[i]&0xff);
    	}
    	textpos = pos;
    	ib.textin(text, textpos);
    	if(text.size()<ib.maxtextsize){
    		eoflag=true;
    	}
    	hf = new Huffman();
    	hfm_buf = new int[bufsize*2];
    	pos = hf.Decode(text, hfm_buf, length*2, 8);
    	for(int i=0;i<length;++i){
    		d_buf[i]=(short)((hfm_buf[2*i]<<8)|hfm_buf[2*i+1]);
    	}
    	textpos = pos;
    	return length;
    }
    static void putwindow(int opos){
    	/*
    	try{
    		out.write(window, 0, l);
    	}catch(IOException e){
            System.out.println("write error" + e);
    	}*/
    	/*for(int i=0;i<halfwindow;i++){   		
    		if(sfile[dfcount]!=window[i]){
				System.out.printf("%d,%c,%c\n", dfcount,sfile[dfcount],window[i]);
			}
    		dfcount++;
    	}*/
    	int l = opos - halfwindow;
    	for(int i=0;i<l;i++){   
    		window[i]=window[i+halfwindow];
    	}
    }
    static void putrestwindow(int l){
    	/*for(int i=0;i<l;i++){   		
    		if(sfile[dfcount]!=window[i]){
				System.out.printf("%d,%d,%d\n", dfcount,sfile[dfcount],window[i]);
			}
    		dfcount++;
    	}*/
    }
    static void decode(String ifilename, String sfilename){   
    	try{
            File f = new File(sfilename);
            FileInputStream in = new FileInputStream(f);
            in.read(sfile, 0, fsize);
        }
        catch(IOException e){
            System.out.println("file open error" + e);
        }
        int ipos = 0, opos = 0, ilength = 0, aipos = 0;
        inbits ib = new inbits(ifilename);
        while(true){  ////
    		/*if(aipos == 491520){
    			System.out.printf("%d\n",d_buf[ipos]);
    		}*/
        	ilength = readblock(ib);
        	if(ilength==0){
        		break;
        	}
        	//Huffman.Decode
        	ipos = 0;
        	while(ipos<ilength){
        		/*if(aipos == 502781){
        			System.out.printf("%d\n",d_buf[ipos]);
        		}*/
        		if(flag_buf[ipos]){
        			int matchl = l_buf[ipos] + 131;
        			int dis = (int)d_buf[ipos];
        			for(int t=0;t<matchl;t++){
        	        	try{
            				window[opos] = window[opos-dis];
        	        	}catch(ArrayIndexOutOfBoundsException e){
        	        		System.out.println("error" + e);
        	        	}
        	        	if(sfile[dfcount]!=window[opos]){
        					System.out.printf("%d,%d,%d\n", dfcount,sfile[dfcount],window[opos]);
        				}

        				opos++;
        				dfcount++;
        			}
        		}
        		else{
        			window[opos] = l_buf[ipos];
        			if(sfile[dfcount]!=window[opos]){
    					System.out.printf("%d,%d,%d\n", dfcount,sfile[dfcount],window[opos]);
    				}
        			opos++;
        			dfcount++;
        		}
        		ipos++;
        		aipos++;
        		if(opos>(64<<10)){
        			putwindow(opos);
        			opos -= halfwindow;
        		}
        	}
        }   
        putrestwindow(opos);  
    }
    /*this function is for test*/
    public static void main(String args[]){
        decode("2307kb.gzip","2307kb.txt");
    }
}
