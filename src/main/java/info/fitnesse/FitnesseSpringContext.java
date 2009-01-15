package info.fitnesse;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class FitnesseSpringContext {
	private static ApplicationContext instance;
	public static ApplicationContext getInstance(){
		if (instance!=null) return instance;
		if (System.getProperty("spring.context")==null){
			throw new Error("spring.context environment variable is not defined. please set it to your spring context path");
		}
		 GenericApplicationContext ctx = new GenericApplicationContext();
		 XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
		 xmlReader.loadBeanDefinitions(System.getProperty("spring.context"));
		 xmlReader = new XmlBeanDefinitionReader(ctx);
		 xmlReader.loadBeanDefinitions(new ClassPathResource("embedded-rollback.xml"));
		 ctx.refresh();
		 instance=ctx;
		return instance;
	}
	public static RollbackIntf getRollbackBean() {
		return (RollbackIntf) getInstance().getBean("rollbackBean");
	}
}
