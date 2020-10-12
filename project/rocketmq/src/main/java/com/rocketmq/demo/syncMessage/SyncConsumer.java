package com.rocketmq.demo.syncMessage;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class SyncConsumer {

	public static void main(String[] args) throws MQClientException {
		
		// 采用的push方式的消费者
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name");
		
		consumer.setNamesrvAddr("127.0.0.1:9876");
		// 设置 消费者，消费的message类型，
		// @param topic : 消费的topic
		// @param subExpression: 匹配topic的表达式
		consumer.subscribe("TopicTest", "*");
		
		// 注册一个Message的Listener 用于监听当有message到达broker时，触发回调函数
		consumer.setMessageListener(new MessageListenerConcurrently() {
			
			// 消费消息
			@Override
			public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
				System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), list);
				
				// 设置 message 消费成功
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
		});
		
		// consumer 参数设置完成，启动消费者实例
		consumer.start();
		System.out.printf("Consumer Stated.%n");
		
	}
}
