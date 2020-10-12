package com.rocketmq.demo.asyncMessage;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsyncProducer {
	
	public static void main(String[] args) throws  Exception{
		
		DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
		producer.setNamesrvAddr("127.0.0.1:9876");
		producer.start();
		
		producer.setRetryTimesWhenSendAsyncFailed(0);
		
		int messageCount = 100;
		final CountDownLatch countDownLatch = new CountDownLatch(messageCount);
		for (int i = 0; i < messageCount; i++) {
			final int index = i;
			Message message = new Message("Jodie_topic_1023",
					"TagB",
					"Hello World".getBytes(RemotingHelper.DEFAULT_CHARSET));
			
			producer.send(message, new SendCallback() {
				@Override
				public void onSuccess(SendResult sendResult) {
					countDownLatch.countDown();
					System.out.printf("%-10d OK %s %n", index, sendResult.getMsgId());
				}
				
				@Override
				public void onException(Throwable e) {
					countDownLatch.countDown();
					System.out.printf("%-10d Exception %s %n", index, e);
					e.printStackTrace();
				}
			});
		}
		
		countDownLatch.await(5, TimeUnit.SECONDS);
		producer.shutdown();
	}
}
