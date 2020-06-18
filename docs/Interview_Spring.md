 Spring ����

1. Spring Boot �� Spring ������

	1. Spring ��һ����ԴӦ�ó����ܣ��򻯿������ֲ�ܹ����������
	2. Spring Boot ����Spring �Ļ����ϴ��һ����ܣ�����Spring��ܵ�һ����չ�����˴�ͳSpring��Ŀ������XML����ù���
	3. ����Tomcat��Jetty�ȷ���������ͨ��java -jar����ʽֱ������
	4. �ṩ��һЩstarters ��POM������

2. Spring AOP(����������) ʵ��ԭ��

	1. AOP ���������̣� AOP��OOP��һ�����䣬һ�֡����С����������ڽ���Щ�����ҵ�������޹ص�ͨ�ù��ܳ��������������װ����
	   ��һ�����������档
	2. AOP��ʵ��һ�㶼�� [����ģʽ]��һ���Ϊ [��̬����] �� [��̬����]��
		2.1 ��̬��������̬��������JVM�ж�̬���ɵģ�����ʱ��ǿ
			JDK ��̬������ �����ӿڣ�Ŀ������ʵ�������ʵ�ֽӿ�
			CGLIB ��̬������ �����࣬Ŀ������ʵ����û��ʵ�ֽӿ�
		2.2 ��̬��������̬�������ڱ����ڼ�����AOP�����࣬����ʱ��ǿ
			ASpectJ ��̬������֧�� ����ʱ������󡢼���ʱ֯�루Weaving������ʹ��ajc������
			֯�룺 ��ʾ����ͨ������ı�����ajc��Ƕ�����浽java����
	3. Spring AOP ��5��֪ͨ���ͣ�
		3.1 ǰ��֪ͨ�� �ڷ���(�е�)ִ��֮ǰ����
		3.2 ����֪ͨ�� �ڷ���(�е�)ִ��ǰ��ִ��
		3.3 ����֪ͨ�� �ڷ���(�е�)ִ��֮�󷵻�
		3.4 �쳣֪ͨ�� �ڷ���(�е�)�׳��쳣֮��ִ��
		3.5 ����֪ͨ�� �ڷ���(�е�)���ؽ��֮��֪ͨ
		
3. Spring IOC/DI ���Ʒ�ת������ע��

	1. ����JAVA �������ʵ��
	2. ��JAVA ���󽻸�Spring �������й���
	3. Spring IOC ��ʼ�����̣�
		XML -- ��ȡ --> Resource -- ���� --> BeanDefinition -- ע��Bean --> BeanFactory(�����͹���Bean)
		3.1 ��ȡXML��Bean ��������Ϣ
		3.2 ����Bean

4. Spring Bean ��������

	4.1 Singleton�� ������Spring IOC�����н�����һ��Beanʵ����Bean�Ե�����ʽ���ڣ�Ĭ��ֵ
	4.2 Prototype�� ԭ��, ÿ�λ�ȡBean���᷵��һ���µ�ʵ���� �൱��ִ��new Bean()
	4.3 Request�� ÿ��HTTP���󶼻ᴴ��һ��Beanʵ��
	4.4 Session�� ͬһ��HTTP Session�У�����һ��Bean
	4.5 GlobalSession��һ������PortalӦ�û�����Portal������Portal��������

5. Spring Transcation ����:
	5.1 ������Ĵ����ԣ� ԭ���ԣ��־��ԣ������ԣ�һ���ԣ�ACID��
		ԭ���ԣ� ԭ������С��λ��ָ���������а����Ĳ����ǲ��ɷָ�ģ�Ҫôȫ���ɹ�Ҫôȫ��ʧ��
		�־��ԣ� ָ��������һ�����ύ�ˣ������ݿ��е����ݵĸı��������Ե�
		�����ԣ� ������û������������ݿ�ʱ�����ݿ�Ϊÿһ���û��������������໥����ģ����಻���ŵ�
		һ���ԣ� ָ�����������ʹ���ݿ��һ��һ����״̬ת�䵽��һ��һ����״̬��Ҳ����˵

	5.2 transcationDefinition.isolation_default, Ĭ��ʹ�ú�����ݿ��Ĭ�ϸ��뼶��
			MYSQL Ĭ��ʹ�õ���REPEATABLE_READ���뼶��
			Oracle Ĭ��ʹ�õ���READ_COMMITED���뼶��
	  transcationDefinition.isolation_read_uncommited: ��͵ĸ��뼶��������ȡ��������δ�ύ�����ݱ�������ܻᵼ��������ö�
				�����ظ���
	  transcationDefinition.isolation_read_commited: ������ȡ�����������Ѿ��ύ�����ݣ�����ֹ��������ö��Ͳ����ض����п��ܷ���
	  transcationDefinition.isolation_repetable_read: �ظ���ȡ����ͬһ�ֶεĶ�ζ�ȡ�������һ�µģ����������Ǳ������������޸�
				����ֹ����������ظ�������������ֹ�ö� 
	  transcationDefinition.serializable: ��ߵĸ��뼶����ȫ����ACID�ĸ��뼶�����е�����������˳��ִ�У���������
				֮���ǲ����໥Ӱ��ġ����Է�ֹ������ö��������ظ����������ܽ����ܵ�����Ӱ�졣