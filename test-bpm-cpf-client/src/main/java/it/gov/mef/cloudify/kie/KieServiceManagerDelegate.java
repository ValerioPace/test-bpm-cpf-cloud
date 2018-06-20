package it.gov.mef.cloudify.kie;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.kie.api.KieServices;
import org.kie.api.command.KieCommands;
import org.kie.remote.common.rest.KieRemoteHttpRequest;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.gov.mef.cloudify.ServiceType;

@Component("kieServiceManager")
public class KieServiceManagerDelegate {

	private KieCommands commands;
	private MarshallingFormat marshallingFormat;
	private String protocol;
	private String host;
	private String port;
	private String username;
	private String password;
	private String qusername;
	private String qpassword;
	private KieServicesConfiguration kiecfg;
	private ServiceType serviceType;
	private KieServicesClient kieServicesClient;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public KieServiceManagerDelegate(String serviceType, String protocol, String host, String port, String username,
			String password) {
		this(ServiceType.valueOf(serviceType.toUpperCase()), protocol, host, port);
	}

	public KieServiceManagerDelegate(ServiceType serviceType, String protocol, String host, String port) {
		this.serviceType = serviceType;
		this.protocol = protocol;
		this.host = host != null ? host : System.getenv("HOSTNAME");
		this.port = port;
		this.username = StringUtils.trimToNull(System.getProperty("username", "kieserver"));
		this.password = StringUtils.trimToNull(System.getProperty("password", "kieserver1!"));

		logger.debug("---------> host: " + host);
		logger.debug("---------> username: " + username);
		logger.debug("---------> password: " + password);
	}

	public KieServiceManagerDelegate(ServiceType serviceType, String protocol, String host, String port,
			String username, String password) {
		this.serviceType = serviceType;
		this.protocol = protocol;
		this.host = host != null ? host : System.getenv("HOSTNAME");
		this.port = port;
		this.username = username;
		this.password = password;
		
		logger.debug("---------> host: " + host);
		logger.debug("---------> username: " + username);
		logger.debug("---------> password: " + password);
	}

	@PostConstruct
	public void initLibraryConfig() {

		commands = KieServices.Factory.get().getCommands();
		switch (serviceType) {
		case HTTP:
			HTTPS: this.kiecfg = KieServicesFactory.newRestConfiguration(getBaseUrl(protocol, host, port), username,
					password);

			if (ServiceType.HTTPS.equals(serviceType)) {
				kiecfg.setUseSsl(true);
				try {
					forgiveUnknownCert();
				} catch (Exception e) {
					logger.error("error during Kie Service Manager initialization: ", e);
					throw new RuntimeException(e);
				}
			}

			break;
		default:
			break;
		}
		
		this.kiecfg.setMarshallingFormat(getMarshallingFormat());	
		this.kieServicesClient = KieServicesFactory.newKieServicesClient(kiecfg);
		
	}
	
	@PreDestroy
	public void releaseKIEClient() {
		kieServicesClient.disposeContainer(kieServicesClient
				.listContainers().getResult()
				.getContainers().iterator().next().getContainerId());
	}

	private void forgiveUnknownCert() throws Exception {

		KieRemoteHttpRequest.ConnectionFactory connf = new KieRemoteHttpRequest.ConnectionFactory() {
			public HttpURLConnection create(URL u) throws IOException {
				return forgiveUnknownCert((HttpURLConnection) u.openConnection());
			}

			public HttpURLConnection create(URL u, Proxy p) throws IOException {
				return forgiveUnknownCert((HttpURLConnection) u.openConnection(p));
			}

			private HttpURLConnection forgiveUnknownCert(HttpURLConnection conn) throws IOException {
				if (conn instanceof HttpsURLConnection) {
					HttpsURLConnection sconn = HttpsURLConnection.class.cast(conn);
					sconn.setHostnameVerifier(new HostnameVerifier() {
						public boolean verify(String arg0, SSLSession arg1) {
							return true;
						}
					});
					try {
						SSLContext context = SSLContext.getInstance("TLS");
						context.init(null, new TrustManager[] { new X509TrustManager() {
							public void checkClientTrusted(X509Certificate[] chain, String authType)
									throws CertificateException {
							}

							public void checkServerTrusted(X509Certificate[] chain, String authType)
									throws CertificateException {
							}

							public X509Certificate[] getAcceptedIssuers() {
								return null;
							}
						} }, null);
						sconn.setSSLSocketFactory(context.getSocketFactory());
					} catch (Exception e) {
						throw new IOException(e);
					}
				}
				return conn;
			}
		};
		Field field = KieRemoteHttpRequest.class.getDeclaredField("CONNECTION_FACTORY");
		field.setAccessible(true);
		field.set(null, connf);

	}
	
	public KieServicesClient getKieServicesClient() {
		return kieServicesClient;
	}

	public void setKieServicesClient(KieServicesClient kieServicesClient) {
		this.kieServicesClient = kieServicesClient;
	}

	protected MarshallingFormat getMarshallingFormat() {
		if (marshallingFormat == null) {
            // can use xstream, xml (jaxb), or json
            String type = System.getProperty("MarshallingFormat", "xstream");
            if (type.trim().equalsIgnoreCase("jaxb")) {
                type = "xml";
            }
            marshallingFormat = MarshallingFormat.fromType(type);
        }
		
		return marshallingFormat;
	}

	protected void setMarshallingFormat(MarshallingFormat marshallingFormat) {
		this.marshallingFormat = marshallingFormat;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getQusername() {
		return qusername;
	}

	public void setQusername(String qusername) {
		this.qusername = qusername;
	}

	public String getQpassword() {
		return qpassword;
	}

	public void setQpassword(String qpassword) {
		this.qpassword = qpassword;
	}

	public KieCommands getCommands() {
		return commands;
	}

	public String getBaseUrl(String defaultProtocol, String defaultHost, String defaultPort) {
		String protocol = getProtocol();
		String host = getHost();
		String port = getPort();
		String baseurl = protocol + "://" + host + (port != null ? ":" + port : "");
		logger.info("---------> baseurl: " + baseurl);
		return baseurl;
	}
}
