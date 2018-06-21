package it.gov.mef.cloudify.servlet;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import it.gov.mef.cloudify.ServiceController;
import it.gov.mef.cloudify.ServiceType;
import it.gov.mef.cloudify.kie.KieServiceManagerDelegate;
import it.gov.mef.cloudify.kie.bpm.BPMClient;
import it.gov.mef.cloudify.kie.ruleengine.RuleEngineClient;

@SpringBootApplication
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackageClasses = {ServiceController.class, BPMClient.class, RuleEngineClient.class})
public class NoiPAServletInitializer extends SpringBootServletInitializer {
 
	@Value("${spring.datasource.jndi-name}")
	private String primaryJndiName;
    
    @Value("${spring.datasource.driver-class}")
    private String driverClass;
	 
    @Bean
    public static PropertyPlaceholderConfigurer ppc() throws IOException {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocations(new ClassPathResource("application.properties"));
        ppc.setIgnoreUnresolvablePlaceholders(true);
        return ppc;
    }
    
    @Bean(name="kieServiceManager") 
    KieServiceManagerDelegate kieServiceManager() {
    	
    	KieServiceManagerDelegate serviceManager = new KieServiceManagerDelegate(ServiceType.HTTPS, "https", System.getProperty("HOSTNAME"), "8443");
        return serviceManager;
    }
    
    @Primary
    @Bean(destroyMethod = "", name="dataSource")
    public DataSource jndiDataSource() {
    	JndiDataSourceLookup lookup = new JndiDataSourceLookup();
    	return lookup.getDataSource(primaryJndiName);  	
    }
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, JpaVendorAdapter jpaVendorAdapter) {
        LocalContainerEntityManagerFactoryBean lef = new LocalContainerEntityManagerFactoryBean();
        lef.setDataSource(dataSource);
        lef.setJpaVendorAdapter(jpaVendorAdapter);
        lef.setPackagesToScan("it.gov.mef.cloudify.process.types");
        return lef;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setShowSql(false);
        hibernateJpaVendorAdapter.setGenerateDdl(true);
        hibernateJpaVendorAdapter.setDatabase(Database.MYSQL);
        return hibernateJpaVendorAdapter;
    }
   
	
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(NoiPAServletInitializer.class);
    }
 
    public static void main(String[] args) throws Exception {
        SpringApplication.run(NoiPAServletInitializer.class, args);
    }
 
}