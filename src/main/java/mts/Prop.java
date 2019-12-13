package mts;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Prop {

	String User = "";
	String Password = "";
	String Driver = "";
	String URL = "";
	int MaxOpenedConnections = 0;

	public Prop() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = getClass().getClassLoader().getResourceAsStream("conn.properties");
			prop.load(input);
			this.User = prop.getProperty("User");
			this.Password = prop.getProperty("Password");
			this.Driver = prop.getProperty("Driver");
			this.URL = prop.getProperty("URL");
			this.MaxOpenedConnections = Integer.valueOf(prop.getProperty("MaxOpenedConnections"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
