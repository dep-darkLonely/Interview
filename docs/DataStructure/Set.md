# Set
> [!Note|label:Set相关知识]  
> + HashSet是无序且不重复的，因为HashSet底层是采用HashMap实现，HashMap中key是唯一的，HashSet.add(Element)时，
将Element对象作为key，因此是不重复的
> + TreeSet是有序且不重复的，因为TreeSet底层是采用TreeMap实现，TreeMap底层采用红黑树实现的

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