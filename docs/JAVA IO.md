# JAVA I/O

### 1.  Java I/O 分类：

- BIO(同步阻塞IO)
- NIO(同步非阻塞IO)
- AIO(异步非阻塞IO)

---

### 2. BIO 同步阻塞IO:

**下图是BIO通信模型**(图源网络，原出处不明):

<img src="./Image/IO/BIO.jpg" alt="传统BIO通信模型图" />

BIO的通信模型：一个请求对应一个线程，通常都是由一个独立的Acceptor线程负责监听客户端的连接。一般，我们都是在while(true)循环中服务端调用accept()阻塞方法监听客户端的连接请求，收到客户端的连接请求之后，就可以创建一个socket，进行读写操作。

**BIO通信中的阻塞方法**：

- socket.accept()   **用来获取新的客户端连接；若没有客户端连接，则程序会阻塞**
- socket.read()
- socket.write()

**BIO 一请求一线程Demo**(来源他人博客)：

client：

```java
package com.guoguo.onlineshop;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class Client {
	
	public static void main(String[] args) {
		// TODO 创建多个线程，模拟多个客户端连接服务端
		new Thread(() -> {
			try {
				Socket socket = new Socket("127.0.0.1", 3333);
				while (true) {
					try {
						socket.getOutputStream().write((new Date() + " ----> Client").getBytes());
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				}
			} catch (IOException e) {
			}
		}).start();
		
	}
}
```

Server:

```java
package com.guoguo.onlineshop;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	
	public static void main(String[] args) throws IOException {
		// TODO 服务端处理客户端连接请求
		ServerSocket serverSocket = new ServerSocket(3333);
		
		// 接收到客户端连接请求之后为每个客户端创建一个新的线程进行链路处理
		new Thread(() -> {
			while (true) {
				try {
					// 阻塞方法   获取新的连接
					Socket socket = serverSocket.accept();
					
					// 每一个新的连接都创建一个线程，负责读取数据
					new Thread(() -> {
						try {
							int len;
							byte[] data = new byte[1024];
							InputStream inputStream = socket.getInputStream();
							// 按字节流方式读取数据,阻塞方法
							while ((len = inputStream.read(data)) != -1) {
								System.out.println(new String(data, 0, len));
							}
						} catch (IOException e) {
						}
					}).start();
					
				} catch (IOException e) {
				}
			}
		}).start();	
	}	
}
```

---

### 3. NIO 同步非阻塞IO:

NIO 同步非阻塞IO，NIO的核心组件：

- Channel 管道： NIO 通过channel管道进行读写，这个管道是双向的，既可以写，也可以读。
- Buffer 缓冲区 ： Buffer对象中包含的是一些要进行读或者写的数据;NIO中所有的数据都是通过Buffer缓存区进行处理的。读数据时，直接读到缓冲区中，写数据的时候，直接写到缓冲区。访问NIO的中数据，都是要通过Buffer对象
- Selector 选择器：选择器用来选择Channel通道，可以使用单个线程来处理多个channel。线程之间切换是非常消耗CPU资源，使用Selector可以提高系统的效率。



### 4.AIO 异步非阻塞IO:

AIO是NIO的改进版，AIO异步非阻塞IO，是基于事件和回调机制实现的，实际操作中

