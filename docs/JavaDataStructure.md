# JAVA 数据结构

- ArrayList 源码分析

    ```java
    public static void main(String[] args) {
        // 实例化一个为空的Object对象数组
        List<String> list = new ArrayList<>();

        /* 1.  ArrayList 底层实现采用的是Object[]数组
        * 2.  ArrayList 添加元素到Object[]数组时，若数组长度为0，则初始化Object[]数组容量为10
        * 3.  ArrayList 扩容实现步骤：
        * 		-> 1. 扩容： 数组的默认大小为10，将数组的容量每次扩充为原来数组内容的1.5倍
        * 		-> 2. 复制:  把原数组的内容复制到新数组中
        */
        list.add("1");
        ...
        list.add("11");
    }
    ```

    源码分析①:
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
    ② ArrayList增加元素:
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

    删除元素方法:
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
    List元素排序Sort:

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

- Set
  
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
  源码分析① : 
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
  存储元素② :
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
- ConcurrentHashMap
