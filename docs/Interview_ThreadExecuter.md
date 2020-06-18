线程池的作用：
	1. 降低资源消耗
		1. 通过重复利用已经创建的线程，降低线程的创建和销毁造成的资源消耗。
	2. 提高响应速度
		1. 当任务到达时，任务可以不需要等待线程的创建，就能立即执行
	3. 更方便的管理线程
		1. 线程属于稀缺资源，如果无限制的创建，会消耗系统资源，降低系统的稳定性，
		    使用线程池的话，可以进行统一的分配，调优和监控

JAVA中提供了4中类型的线程池：
	1.FixedThreadPool, 创建一个固定线程数量的线程池，线程池中线程数量不变，有新任务提交时，如果线程池中有空闲线程，则使用空闲的线程
			,没有的话，就加入队列中，等待

	2.CachedThreadPool 创建一个不固定线程数量的线程池；线程数量不固定，有新任务提交时，若线程池中有空闲线程的话，则使用空闲线程；
			若没有的话，则会创建一个新的线程；可以进行线程的复用

	3.SingleThreadExecuter，创建一个只有一个线程的线程池；若多余一个任务被提交时，会被加入到队列中进行等待，先进先出等待线程空闲

	4.SechedualThreadfPoolExecuter： 主要就是用来创建延迟一段时间后执行的任务的线程池，也可以说是定期执行任务的线程；
				主要分为: SchedualThreadPoolExceutor（多个线程）和SingleSchedualThreadPoolExecutor(单线程)

JAVA中创建线程池的方式：
	1.使用Executors创建，<阿里巴巴JAVA开发手册>中不允许直接使用Executors进行创建，而是推荐使用ThreadPoolExecutor
				构造函数的方式来进行创建
			使用Executors创建以上4中类型的线程池的话，存在一个重大问题便是：可能会出现OOM现象；
			1) FixedThreadPool和SingleThreadExecuter，允许请求的队列长度最大为Integer.MAX_Value,可能会出现堆积大量的请求
			导致OOM；
			2) CachedThreadPool和SchedualThreadPoolExecutor  这种的话，可以创建的线程数量为Integer.Max_Value,可能会创建大
			量线程导致OOM
	2.ThreadPoolExecutor的构造函数来创建，创建的同时，指定BlockQueue(队列)的容量就可以了
			存在的问题是：BlockQueue中使用的是有边界的队列，一旦当提交的线程数量超过可用的线程数量，就会抛出Exception
			队列已满，无法处理请求
	3.使用开源类库创建，使用Apache和guava来进行创建线程池

Object 类：所有类的父类;提供了一下方法：
	1. final getClass()	返回 当前运行时对象的Class对象
	2. equals()	比较对象的内存地址
	3. Clone()	clone当前对象
	4. toString()	
	5. final notify()	唤醒所有等待线程中的任意一个
	6. final notifyAll()	唤醒所有等待的线程，
	7.hashCode()	返回对象的哈希码
	8.final wait()	暂停当前执行的线程，并且释放锁，而sleep是Thread的方法，
	9.final wait(long timeout)
	10.wait(long timeout,int naos)
	11.finalize()	垃圾回收器，回收该对象的时候，使用
	

hashcode() 返回对象在哈希表中索引位置，然后将对象的内存地址信息转换为int类型返回
HashSet中检查元素重复：对象加入HashSet时，首先会计算该对象的hashCode来判断对象加入的位置，同时也会和其他的hashcode作比较，对比hashcode
	如果没有相符合的hashcode，则会加入；如果发现有相同的hashcode值，则会使用equals对比相同hashcode位置的值是否相同，
	如果相同，则不进行任何处理；如果不相同的话，则散列到其他位置；

equals方法要被覆盖的话，则必须覆盖hashcode方法

悲观锁：总是假设最坏的情况，认为竞争总是存在，每次获取数据时都认为是被会
	修改的，因此每次都会先上锁，其他线程阻塞等待释放锁

乐观锁：总是假设最好的情况，认为竞争总是不存在，每次获取数据时都认为是不
	会被修改的，因此不会上锁，数据更新时比较数据有无更新，可以通过版本号
	或者CAS实现

使用场景： 悲观锁适用于写比较多的场景
	    乐观锁适用于读比较多的场景

乐观锁实现方式：
	方式1：版本号机制：
		通过在数据库中增加version字段来实现
	方式2：CAS (Compare and Swap)
		需要使用三个操作数：旧值(内存中的值)、新值、要写入内存的值
		java.util.Concurent包中有一个AutomaticInteger这些类都是通过
		CAS来实现的，CAS属于原子操作，已经是不可分割的操作了
		CAS是通过硬件实现的，是一个汇编指令
		
