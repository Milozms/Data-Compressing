package gzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class gzip_test {
    //minimal match size = 3
	static int minmatch = 3;
    //window size 64kb
	static int windowsize = 64<<10;
    //max distance
	static int halfwindow = 32<<10;
    //buf size
    static int bufsize = 9<<20;
    static int min_lookahead = 3 ;
    static byte sfile[] = new byte[bufsize];
    static byte dfile[] =  new byte[bufsize];
    static byte window[] = new byte[windowsize];
    static byte l_buf[] = new byte[bufsize]; //non-matched byte or (matched length - 131)
    static short d_buf[] = new short[bufsize]; //matched distance
    static boolean flag_buf[] = new boolean[bufsize]; //true -> (matched length - 131); false -> non-matched byte
    static int hfm_buf[] = new int[bufsize*2];
    //debug
    //static int tmp_buf[] = new int[bufsize];
    static int lookahead;
    static int bufcount;
    static boolean eofile = false;
    static Vector<Character> hcode,hTree;
    static FileInputStream in;
    static Hashtable<String,Vector<pos_int> > hasht = new Hashtable<String,Vector<pos_int> >(windowsize);// String - Vector int
    //read first block
    static void gzip_init(){
        bufcount = 0;
        int b = 0;
        try{
            b = in.read(window,0,windowsize);
        }
        catch(IOException e){
            System.out.println("file read error" + e);
        }
        lookahead = b;
        if(b<windowsize){
            eofile = true;
        }
    }

    static int match_length(int apos, int bpos){
        int l=0;
        while(bpos+l<windowsize && l<258 && window[apos+l]==window[bpos+l]){
            l++;
        }
        return l;
    }

    static int deflate(int pos){
        int maxmatchlength = 0, maxmatchpos = 0;
        //insert String
        char chead[] = new char[minmatch];
        for(int i=0;i<minmatch;i++){
        	try{
        		chead[i]=(char)window[pos+i];
        	}catch(ArrayIndexOutOfBoundsException e){
        		System.out.println("error" + e);
        	}
        }
        String shead = new String(chead);
        if(hasht.containsKey(shead)){//exist match str
            //find longest match
            maxmatchlength = 0;
            Vector<pos_int> v = hasht.get(shead);
            for(int i=0;i<v.size();i++){
                if(pos - v.get(i).toInt()>=halfwindow){
                    break;
                }
                int tmp = match_length(v.get(i).toInt(),pos);
                if(tmp>maxmatchlength){
                    maxmatchlength = tmp;
                    maxmatchpos = v.get(i).toInt();
                }
            }
            //add to vector
            v.insertElementAt(new pos_int(pos), 0);
        }
        else{
            Vector<pos_int> v = new Vector<pos_int>();
            v.addElement(new pos_int(pos));
            hasht.put(shead, v);
        }
        if(maxmatchlength<3){
            flag_buf[bufcount] = false;
            l_buf[bufcount] = window[pos];
            d_buf[bufcount] = 0;
            bufcount++;
            return 1;
        }
        flag_buf[bufcount] = true;
        l_buf[bufcount] = (byte)(maxmatchlength - 131);
        d_buf[bufcount] = (short)(pos-maxmatchpos);
        if(d_buf[bufcount]==-32768){
        	System.out.printf("%d,%d",pos,maxmatchpos);
        }
        bufcount++;
        return maxmatchlength;
    }

    static void fill_window(){
        //move second window to first
        System.arraycopy(window,halfwindow,window,0,halfwindow);
        //read second window
        int b = 0;
        try{
            b = in.read(window,halfwindow,halfwindow);
        }
        catch(IOException e){
            System.out.println("file read error" + e);
        }
        if(b>=0) lookahead += b;
        if(b<halfwindow){
            eofile = true;
        }
        //update hashtable - string index
        Set s = hasht.keySet();
        for(Iterator<String> i = s.iterator();i.hasNext();){
            Vector<pos_int> v = hasht.get(i.next());
            for(int j=0;j<v.size();j++){
                if(v.get(j).toInt()>halfwindow){
                    //update index
                    v.setElementAt(new pos_int(v.get(j).toInt() - halfwindow), j);
                }
                else{
                    //remove elements from previous first window
                    while(j<v.size()){
                        v.removeElementAt(j);
                    }
                    break;
                }
            }
        }
    }
    static void encode(String infile){
        //open file
        try{
            File f = new File(infile);
            in = new FileInputStream(f);
        }
        catch(IOException e){
            System.out.println("file open error" + e);
        }
        int pos = 0;
        gzip_init();
        while(lookahead>=min_lookahead || !eofile){
            int move = deflate(pos);
            pos += move;
            lookahead -= move;
            if(lookahead<min_lookahead){
            	if(eofile){
            		for(int i=0;i<lookahead;i++){
            			l_buf[bufcount]=window[pos+i];
            			d_buf[bufcount]=0;
            			flag_buf[bufcount]=false;
            			bufcount++;
            		}
                    break;
            	}            	

                fill_window();
                pos -= halfwindow;
            }
        }
        //close file
        try{
            in.close();
        }
        catch(IOException e){
            System.out.println("file close error" + e);
        }
        //test
    	try{
            File f = new File(infile);
            in = new FileInputStream(f);
            in.read(sfile, 0, bufsize);
        }
        catch(IOException e){
            System.out.println("file open error" + e);
        }
    	int ipos = 0, opos = 0;
    	while(ipos<bufcount){
    		if(flag_buf[ipos]){
    			int matchl = l_buf[ipos] + 131;
    			int dis = (int)d_buf[ipos];
    			for(int t=0;t<matchl;t++){
    	        	try{
        				dfile[opos] = dfile[opos-dis];
    	        	}catch(ArrayIndexOutOfBoundsException e){
    	        		System.out.println("error" + e);
    	        	}
        			if(sfile[opos]!=dfile[opos]){
        				System.out.printf("%d,%c,%c\n", opos,sfile[opos],dfile[opos]);
        			}
    	        	if(opos >= 2361854 && opos<=2361864){
    	        		System.out.printf("%d:%d\n", opos, dfile[opos]);
    	        	}
    				opos++;
    			}
    		}
    		else{
    			dfile[opos] = l_buf[ipos];
    			if(sfile[opos]!=dfile[opos]){
    				System.out.printf("%d,%c,%c\n", opos,sfile[opos],dfile[opos]);
    			}
	        	if(opos >= 2361854 && opos<=2361864){
	        		System.out.printf("%d:%d\n", opos, dfile[opos]);
	        	}
    			opos++;
    		}
    		ipos++;
    		
    	}
    }
    /*this function is for test*/
    public static void main(String args[]){
        encode("2307kb.txt");
    }
}
