package org.vadim;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;




class NodeTest implements NodeInterface{
	public int keyNum;
	public int[] key;
	public boolean leaf;
	public NodeTest[] children;
	public NodeTest(int keyNum,boolean leaf,int t){
		this.keyNum=keyNum;
		this.leaf=leaf;
		this.key=new int[2*t-1];
		this.children=new NodeTest[2*t];
	}
}

/*
 * NodeIndex нужен только чтобы вернуть объект с полями Node и int при конце поиска.
 * Посчитал это лучшим способом реализовать особеннсоти функции поиска
 * (Нужно возвращать два значения одновременно при удачном нахождении)
 */
class NodeIndexTest implements NodeInterface{
	private NodeTest node;
	private int index;
	public NodeIndexTest(NodeTest node,int index){
		this.node=node;
		this.index=index;
	}
	public String toString(){
		if(node==null)
			return "The key is not found";
		return "The key is found "+node.key[index];
	}
}

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class TreeTest {
	private static final int t=100;
	private int height=0;
	public NodeTest root;
	private ExecutorService search=Executors.newFixedThreadPool(4);//Потоки для поиска
	private ExecutorService insert=Executors.newFixedThreadPool(1);
	/*
	 * Поток для добавления. Создаю один поток для добавления, так как ипользовать большое кол-во считаю ненужным
	 * (они будут мешать друг другу)
	 */
	ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	@Setup
    public void setup() {
		root=new NodeTest(0,true,t);
		for (int j = 1; j < 1000000; j++) {// В цикле добавляю данные
			this.treeInsert(j);
		}
    }

	public TreeTest(){
		this.root=new NodeTest(0,true,t);
	}


	public void treeSplitChild(NodeTest x,int i){
		NodeTest y=x.children[i];
		NodeTest z=new NodeTest(t-1,y.leaf,t);
		for(int j=0;j<t-1;j++){
			z.key[j]=y.key[j+t];
			y.key[j+t]=0;
		}
		if(!y.leaf){
			for(int j=0;j<t;j++){
				z.children[j]=y.children[j+t];
				y.children[j+t]=null;
			}
		}
		y.keyNum=t-1;
		for(int j=x.keyNum;j>=i+1;j--){
			x.children[j+1]=x.children[j];
		}
		x.children[i+1]=z;
		for(int j=x.keyNum-1;j>=i;j--){
			x.key[j+1]=x.key[j];
		}
		x.key[i]=y.key[t-1];
		y.key[t-1]=0;
		x.keyNum++;
	}

	@Benchmark
	public void InsertNew(){
		treeInsert(1000);
	}



	public void treeInsert(int key){//При вызове этой функции задача добавляется в пул потоков для добавления
		Runnable obj=()-> {
			readWriteLock.writeLock().lock();//Надеюсь это работает так как написано в интернете
			if(root.keyNum==2*t-1){
				NodeTest s=new NodeTest(0,false,t);
				s.children[0]=root;
				root=s;
				height++;
				treeSplitChild(s,0);
				treeInsertNonfull(s,key);
			}
			else
				treeInsertNonfull(root,key);
			readWriteLock.writeLock().unlock();
		};
		insert.submit(obj);
	}

	public void treeInsertNonfull(NodeTest x,int key){
		int i=x.keyNum-1;

		if(x.leaf){
			if(i==-1){
				x.key[0]=key;
				x.keyNum++;
				return;
			}
			while(i>=0 && key<x.key[i] ){
				x.key[i+1]=x.key[i];
				i--;
			}

			x.key[i+1]=key;
			x.keyNum++;
		}
		else{
			while(i>=0 && key<x.key[i])
				i--;
			i++;
			if(x.children[i].keyNum==2*t-1){
				treeSplitChild(x,i);
				if(key>x.key[i]){
					i++;
				}
			}
			treeInsertNonfull(x.children[i],key);
		}
	}

	@Benchmark
	public void search(){
		treeSearch(222);
	}
	public Future<NodeInterface> treeSearch(int key){//При вызове этой функции задача добавляется в пул потоков для поиска
		Callable<NodeInterface> obj=()-> {
					readWriteLock.readLock().lock();//Опять же, надеюсь это работает так как написано в интернете
					NodeInterface objNode=treeSearch(root,key);
					readWriteLock.readLock().unlock();
					return objNode;
				};
		return search.submit(obj);
	}


	public NodeInterface treeSearch(NodeTest x,int key){
		int i=0;

		while(i<x.keyNum && key>x.key[i])
			i++;
		if(i<x.keyNum && key==x.key[i]){
			NodeIndexTest objBuf=new NodeIndexTest(x,i);
			return objBuf;
		}
		else if(x.leaf)
		{
			NodeIndexTest objBuf=new NodeIndexTest(null,0);
			return objBuf;
		}
		else
			return treeSearch(x.children[i],key);
	}

	public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TreeTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
	}
}







