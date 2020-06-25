# 基础问题

> [!Note|label:关于String类的常见问题]
> 1. 下面代码创建了几个对象?
    ```java
    String a = new String("ABC");
    ```
>   + 创建了2个对象；首先"ABC"是字符串常量，应该存储在JVM的方法区中的常量池中；new String("ABC")是String类的一个实例对象
，该实例对象存储在JVM的heap堆中，new String("ABC")实际上时拷贝了一个"ABC"在堆中.
>       
    ```java
    /**
     * Initializes a newly created {@code String} object so that it represents
     * the same sequence of characters as the argument; in other words, the
     * newly created string is a copy of the argument string. Unless an
     * explicit copy of {@code original} is needed, use of this constructor is
     * unnecessary since Strings are immutable.
     *
     * @param  original
     *         A {@code String}
     */
    public String(String original) {
        this.value = original.value;
        this.hash = original.hash;
    }
    ```

> [!Note|label:JVM运行时数据区]
![Run-Time Data Areas](/../Image/BasicProblem/JVM_Run-Time_Data_Areas.jpg)
> + 详细参考[JVM规范](https://docs.oracle.com/javase/specs/jvms/se8/html/index.html)
