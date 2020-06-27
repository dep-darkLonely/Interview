# ConcurrentHashMap

```java
    Demo:
    public static void main(String[] args) {
        ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put("key", "value");
        concurrentHashMap.put("key1", "value1");
        concurrentHashMap.get("key");
    }
```

> [!Note|label:源码分析_构造函数]
```java

    /**
     * Creates a new, empty map with the default initial table size (16).
     */
    public ConcurrentHashMap() {
    }

    /**
     * Creates a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     */
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();

        /**
         * 指定ConcurrentHashMap的容量>=最大容量的1/2时，则初始化ConcurrentHashMap的容量为最大容量1<<30
         * 否则，返回与指定容量1.5倍+1最近的2的整数次幂的容量值，将该容量值作为初始化
         * 如：指定容量为14，则tableSizeFor(14+7+1)=32
         */
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                   MAXIMUM_CAPACITY :
                   tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        
        /**
         * 用来控制初始化和扩容.
         */
        this.sizeCtl = cap;
    }

    /**
     * Creates a new map with the same mappings as the given map.
     *
     * @param m the map
     */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }
```

> [!Note|label:Put方法，ConcurrentHashMap中增加元素]
```java
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /** Implementation for put and putIfAbsent */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        // K 和 V 都不允许为空
        if (key == null || value == null) throw new NullPointerException();

        // 计算key的hash值
        int hash = spread(key.hashCode());
        // 链表的长度
        int binCount = 0;
        
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                // 初始化ConcurrentHashMap
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                /**
                 * 若该下标返回的节点为空，即该ConcurrentHashMap中不存在该键值对，则通过cas机制，判断内存中tab对象中下标为i
                 * 的位置上面的值是否为null，若为null，则将Node节点信息更新，否则将进入自旋
                 */
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {

                // 发生Hash碰撞的键值对
                V oldVal = null;

                // Synchronized给当前链表的首节点加锁
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        // 取出来的元素的hash值大于0，当转换为树之后，hash值为-2
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                // 判断该链表中是否存相同的key
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                    // 使用Value替换oldValue
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                // 判断是否是链表的尾节点
                                if ((e = e.next) == null) {
                                    // 将Node追加至链表的尾节点
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            // 红黑树
                            Node<K,V> p;
                            binCount = 2;
                            /**
                             * 判断红黑树中是否存在该键值对，若不存在，则添加该键值对，返回值为null
                             * 否则的话，返回该树节点
                             */
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                // 红黑树中存在已经存在该键值对
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    // 链表长度>=8时，链表转换为红黑树
                    if (binCount >= TREEIFY_THRESHOLD)
                        // 转换红黑树
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }

    /**
     * 与HashMap中hash不同
     * & HASH_BITS：为了保证得到的index的第一位为0，也就是为了得到一个正数。因为有符号数第一位0代表正数，1代表负数
     */
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }
```