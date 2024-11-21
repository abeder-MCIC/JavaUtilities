package com.mcic.util;


public class Tripple<T extends Comparable, U extends Comparable, V extends Comparable> implements Comparable<Tripple>{
	public final T pri;
	public final U sec;
	public final V ter;
	
	public Tripple(T left, U right, V ter){
		this.pri = left;
		this.sec = right;
		this.ter = ter;
	}

	public int compareTo(Tripple o) {
		int res = (pri).compareTo(o.pri);
		if (res == 0) {
			res = (sec).compareTo(o.sec);
		}
		if (res == 0) {
			res = (ter).compareTo(o.ter);
		}
		return res;
	}

	@Override
	public String toString() {
		return "[" + pri.toString() + ", " + sec.toString() + ", " + ter.toString() + "]";
	}
	
}
	
