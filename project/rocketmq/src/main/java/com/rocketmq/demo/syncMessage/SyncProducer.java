package com.rocketmq.demo.syncMessage;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
		import org.apache.rocketmq.client.producer.SendResult;
		import org.apache.rocketmq.common.message.Message;
		import org.apache.rocketmq.remoting.common.RemotingHelper;


public class SyncProducer {
	
	public static void main(String[] args) throws Exception{
		
		// 实例化一个producer,使用 producer group name
		DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
		// 设置name server地址
		producer.setNamesrvAddr("127.0.0.1:9876");
		// 启动 instance
		producer.start();
		
		// 发送 message
		for (int i = 0; i < 100; i++) {
			// 创建一个Message 实例，指定一个topic、tag、message body
			Message msg = new Message("TopicTest",
					"TagA",
					("Hello RocketMQ" + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
			
			SendResult sendResult = producer.send(msg);
			System.out.printf("%s%n", sendResult);
		}
		
		// 关闭生产者实例
		producer.shutdown();
	}
}
