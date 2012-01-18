package info.fitnesse;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class FitnesseSpringContext {
    private static AbstractApplicationContext instance;

    public static AbstractApplicationContext getInstance() {
        if (instance != null) return instance;
        if (System.getProperty("spring.context") == null) {
            throw new Error("spring.context environment variable is not defined. please set it to your spring context path");
        }

        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(System.getProperty("spring.context"));
        xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(new ClassPathResource("embedded-rollback.xml"));
        ctx.refresh();
        ctx.registerShutdownHook();

        instance = ctx;

        return instance;
    }

    public static RollbackIntf getRollbackBean() {
        return (RollbackIntf) getInstance().getBean("rollbackBean");
    }

    public static void shutdown() {
        getInstance().close();
    }
}
