# LinkedList 

> [!NOTE|label:源码分析]
```java
    public static void main(String[] args) {
        List<String> linkedList = new LinkedList<>();
        linkedList.add("a");
        linkedList.add("b");
    }
```
>[!NOTE|label:源码分析_构造函数]
```java

    /**
     * Constructs an empty list.
     */
    public LinkedList() {
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param  c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public LinkedList(Collection<? extends E> c) {
        this();
        // 初始化指定Collections
        addAll(c);
    }

```

> [!NOTE|label:add方法实现]
```java
    /**
     * 追加指定元素到list的尾部
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean add(E e) {
        // 在list的尾部追加元素
        linkLast(e);
        return true;
    }


    /**
     * Links e as last element.
     */
    void linkLast(E e) {
        // 链表Node中最后一个节点
        final Node<E> l = last;
        // 创建新的Node节点
        final Node<E> newNode = new Node<>(l, e, null);
        // 将新创建的Node节点当做最后一个节点
        last = newNode;
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        size++;
        modCount++;
    }

    // 链表Node<E> 实体类
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
```

> [!NOTE|label:addAll方法实现]
```java

    transient int size = 0;

    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

     /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *              from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        // 检查是否存在索引越界
        checkPositionIndex(index);

        // 将Collection集合内容转换Object数组
        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0)
            return false;

        Node<E> pred, succ;
        if (index == size) {
            succ = null;
            pred = last;
        } else {
            succ = node(index);
            pred = succ.prev;
        }

        for (Object o : a) {
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;
            pred = newNode;
        }

        if (succ == null) {
            last = pred;
        } else {
            pred.next = succ;
            succ.prev = pred;
        }

        size += numNew;
        modCount++;
        return true;
    }
```
> [!NOTE|label:常见问题|icon:null]
> + LinkedList的优点:
> + LinkedList的缺点:
> + LinkedList中是否可以存储NULL？