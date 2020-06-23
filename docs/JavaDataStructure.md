# JAVA 数据结构

- **ArrayList**

    > [!NOTE|label:ArrayList底层实现及其扩容]
    > 1. ArrayList 底层实现采用的是Object[]数组
    > 2. ArrayList 添加元素到Object[]数组时，若数组长度为0，则初始化Object[]数组容量为10
    > 3. ArrayList 扩容实现步骤：
       - 扩容： 数组的默认大小为10，将数组的容量每次扩充为原来数组内容的1.5倍
       - 复制:  把原数组的内容复制到新数组中

    ```java
    public static void main(String[] args) {
        // 实例化一个为空的Object对象数组
        List<String> list = new ArrayList<>();

        list.add("1");
        ...
        list.add("11");
    }
    ```

    > [!WARNING|label:源码分析①|icon:null]
    ```java
    java.util.ArrayList#ArrayList()

    无参构造器:
    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    有参构造器:
    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }

    ```
    > [!WARNING|label:ArrayList增加元素②|icon:null]
    ```java
    /**
     * Appends the specified element to the end of this list.
     *
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        elementData[size++] = e;
        return true;
    }

    调用相关方法:
    /**
     * 默认实例化容量
     */
    private static final int DEFAULT_CAPACITY = 10;

    // 默认空Object[]
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    transient Object[] elementData; 

    // ArrayList 扩容
    private void ensureCapacityInternal(int minCapacity) {
        ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
    }

    // 计算容量大小
    private static int calculateCapacity(Object[] elementData, int minCapacity) {
        // 判断Object数组 是否为 空数组
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        return minCapacity;
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }

    /**
     * 扩容: 使新数组可以容纳指定容量的元素
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;

        // 新数组容量扩充为原数组的1.5倍
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);

        // 拷贝旧数组内容到新数组内容
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
    ```

    > [!WARNING|label:删除元素方法:|icon:null]
    ```java
    java.util.ArrayList#remove(java.lang.Object)

    public E remove(int index) {
        rangeCheck(index);

        modCount++;
        E oldValue = elementData(index);

        int numMoved = size - index - 1;
        if (numMoved > 0)

            // 数组元素拷贝
            System.arraycopy(elementData, index+1, elementData, index,numMoved);

        elementData[--size] = null; // clear to let GC do its work

        return oldValue;
    }
    ```
    
    > [!NOTE|label:List元素排序Sort源码分析:]
    > + List 排序实际使用的是Arrays.sort()方法排序
    > + List 列表排序使用Comparator实现排序

    ```java
    java.util.List#sort

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("11");

        /**
         * 返回值>0，降序排列，否则升序排列
         */
        list.sort((o1, o2) -> {
            Integer i1 = Integer.parseInt(o1);
            Integer i2 = Integer.parseInt(o2);
            return  i2.compareTo(i1);
        });
        System.out.println(list);
    }

    源码分析: 
    /**
     * List 排序实际使用的是Arrays.sort()方法排序
     * List 列表排序使用Comparator实现排序
     */
    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    /**
     * Arrays 排序
     */
    public static <T> void sort(T[] a, int fromIndex, int toIndex,
                                Comparator<? super T> c) {
        if (c == null) {
            // 不使用自定义Comparator进行排序
            sort(a, fromIndex, toIndex);
        } else {
            rangeCheck(a.length, fromIndex, toIndex);
            if (LegacyMergeSort.userRequested)

                // 归并排序
                legacyMergeSort(a, fromIndex, toIndex, c);
            else
                // 使用自定义Comparator进行比较排序
                TimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
        }
    }
    ```

- **Set**
  
  ```java
    /**
     * HashSet 底层采用HashMap实现；
     * 默认初始化HashMap容量为16，加载因子0.75
     */ 
    public static void main(String[] args) {
        Set<String> set = new HashSet <>();
        set.add("zhangsan");
        set.add("wangwu");
        System.out.println(set.size());
    }
  ```
  > [!WARNING|label:源码分析①|icon:null]
  ```java

    // Set中存储的值实际为HashMap中Key值
    private transient HashMap<E,Object> map;

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * default initial capacity (16) and load factor (0.75).
     */
    public HashSet() {
        // 初始化HashMap
        map = new HashMap<>();
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and default load factor (0.75).
     *
     * @param      initialCapacity   the initial capacity of the hash table
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero
     */
    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The <tt>HashMap</tt> is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }
  ```
  > [!WARNING|label:存储元素②|icon:null]
  ```java
    /**
     * 当元素不存在时，添加元素到HashMap中。
     * map.put(e, PRESENT) 返回值为旧值
     */
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }
  ```
- HashMap
  
    > [!NOTE|label:HashMap底层实现原理]
    > **HashMap底层实现原理**:
    > + 底层采用Node数组 + 链表 + 红黑树实现
    > + 存储元素： 根据元素的key计算其hash值，在进行 hash & (cap - 1) 计算其在Node数组中的索引下标值
    > + 链表： JDK1.8 链表采用的是**尾插法**，当其长度>= 8时，会将其转换为红黑树，而JDK1.7采用的是**头插法**
    > + 红黑树： JDK1.8中引入红黑树的作用就是为了减少搜索时间
    > + **扩容**： HashMap扩容采用的是**2的整数次幂的形式进行扩容**
    > + HashMap中加载因为什么是0.75?
    >   - 加载因子表示的是HashMap中元素的填充程度，0.75是在空间利用率和查找时间方面做得一个平衡
    >   - 理想状况下，使用随机hashCode，在Hash桶中节点出现的频率满足**泊松分布**，桶中元素个数与发生率对比
    >   加载因子为0.75时，桶中元素个数超过8个，发生的概率为0.00000006，每个碰撞位置的链表长度超过８个是几乎不可能的。
    >   Ideally, under random hashCodes, the frequency of   
    >   nodes in bins follows a Poisson distribution   
    >   (http://en.wikipedia.org/wiki/Poisson_distribution) with a   
    >   parameter of about 0.5 on average for the default resizing   
    >   threshold of 0.75, although with a large variance because of   
    >   resizing granularity. Ignoring variance, the expected   
    >   occurrences of list size k are (exp(-0.5) * pow(0.5, k) /   
    >   factorial(k)). The first values are:       
    >   
    >    0:    0.60653066   
    >    1:    0.30326533   
    >    2:    0.07581633   
    >    3:    0.01263606   
    >    4:    0.00157952   
    >    5:    0.00015795   
    >    6:    0.00001316   
    >    7:    0.00000094   
    >    8:    0.00000006   
    >    more: less than 1 in ten million   
    > + HashMap中默认容量为什么是16?
    >   -  HashMap的默认容量为16，应该是一个经验值，在效率和内容方面的一个平衡，初始化容量太小，会频繁发生扩容，扩容时会进行内容迁移
        计算Hash，比较耗时；初始化容量太大，会浪费空间
    > + HashMap中扩容时为什么是2的整数次幂的形式？
    >   - 主要原因是计算hash时，需要进行取模运算;
    >   计算元素在数组中的位置:i = (n - 1) & hash
    >   - HashMap的默认初始化容量大小为 1<<4

    ```java
    public static void main(String[] args) {

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        map.put("key1", "value1");

        map.get("key");
    }
    ```
    > [!WARNING|label:源码分析①|icon:null]
    > **※** tableSizeFor(initialCapacity)
    > - 计算**大于并且最接近 自定义HashMap容量的2的整数次幂的数**,该值将在第一次初始化HashMap容量时使用;
    > 如 自定义HashMap容量为10，则返回16

    ```java
    java.util.HashMap#HashMap(int, float)

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity 初始化容量
     * @param  loadFactor      加载因子
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);

        // 最大容量为 1<<30
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;

        /**
         * 计算大于并且最接近 自定义HashMap容量的2的整数次幂的数,该值
         * 将在第一次初始化HashMap容量时使用
         * 如 自定义HashMap容量为10，则返回16
         */
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public HashMap() {

        // 初始化默认加载因子0.75f
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }
    ```

    > [!NOTE|label:put方法实现]

    ```java
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return key 存在，则返回key对应的旧值，否则，返回NULL
     */
    // put 方法是有返回值的
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
    ```

    > [!NOTE|label:put方法实现]
    > - key.hashCode() 默认返回值为int类型,int类型4byte，32位
    > - h >>> 16 保留高位，将高位的变化影响到低位的变化
    > - (h = key.hashCode()) ^ (h >>> 16) 主要是为了是减少hash冲突，使其更加的散列
    > - HashMap中默认支持key为null的值，并且插入到HashMap的索引为0的位置
    
    ```java 
    static final int hash(Object key) {
        int h;
        // key 为 NULL时，默认hashcode 为0
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
    ```

    > [!NOTE|label:putVal方法]
    ```java
    /**
     * Implements Map.put and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {

        // JDK1.8 HashMap底层采用的是Node数组
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)

            // 数组为空，初始化数组的容量
            n = (tab = resize()).length;

        if ((p = tab[i = (n - 1) & hash]) == null)
            // Node数组指定索引下标中元素值不存在时，插入Node节点
            tab[i] = newNode(hash, key, value, null);
        else {
            // Hash冲突，指定索引下标中元素值存在
            Node<K,V> e; K k;

            // key相同并且key的hash相同的情况下，证明指定索引下标中已经存在值
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            else if (p instanceof TreeNode)
            // 红黑树处理
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
            // 链表处理
                for (int binCount = 0; ; ++binCount) {
                    // 判断是否是链表的最后一个元素
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        /**
                         * static final int TREEIFY_THRESHOLD = 8;
                         * 链表树化的阈值为8.即当链表的长度>= 8时，将链表转换为红黑树，
                         * 以减少搜索时间
                         */
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    // 链表中查找元素，若该元素存在，则直接结束循环
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    
                    p = e;
                }
            }

            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    // 替换旧值
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        ++modCount;

        // 插入元素成功之后，判断Node 数组存放的元素个数是否>阈值，进行扩容
        if (++size > threshold)
            // 扩容
            resize();

        // 插入成功之后的回调函数，HashMap中未实现
        afterNodeInsertion(evict);
        return null;
    }
    ```

    > [!NOTE|label:HashMap扩容]
    > - **扩容**:以2的整数次幂的形式进行扩容
    > - JDK1.8 扩容时数组中链表数据迁移采用的是**尾插法**,可以保证插入顺序；而JDK1.7扩容时数组中链表采用的是**头插法**
    > - JDK1.8 扩容时数组中链表数据迁移时，可以将一个链表分割为两个链表，使用更加散列，减少碰撞

    ```java
    /**
     * 初始化Table的大小，或者以2的整数次幂的形式进行扩容
     *
     * @return the table
     */
    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        // 旧数组的容量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // 旧数组的阈值
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            // 新数组的容量为旧数组容量的2倍，阈值也为原来的2倍
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            // 初始化HashMap使用，使用阈值代替容量
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            // 默认初始化设置
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        // 初始化指定大小的数组
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        // 扩容，数组内容从旧数组迁移到新数组
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        // 桶中只有一个元素时，重新计算索引位置，插入元素
                        // 低位元素位置保持不变
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        // 红黑树数组迁移
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order

                        // 链表数组迁移
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        // 将单个链接进行分割成2个连表，低位链表索引位置保持不变，
                        // 高位链表索引位置= 原索引值+ 旧数组的容量
                        do {
                            next = e.next;
                            // 低4位的值，数组迁移时，索引位置保持不变
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            // 高4位的值，数组迁移时，索引位置变为j + oldCap
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
    ```

- ConcurrentHashMap
