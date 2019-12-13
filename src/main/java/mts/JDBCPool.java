package mts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Vector;

public class JDBCPool implements Runnable {

	private String driver, url, username, password;
	private Vector availableCnx, busyCnx;
	private boolean connectionPending = false;
	private int MaxOpenedConnections = 10;

	public JDBCPool() throws SQLException {
		Prop p = new Prop();
		this.driver = p.Driver;
		this.url = p.URL;
		this.username = p.User;
		this.password = p.Password;
		int MaxOpenedConnections = p.MaxOpenedConnections;

		availableCnx = new Vector(MaxOpenedConnections);
		busyCnx = new Vector();

		for (int i = 0; i < MaxOpenedConnections; i++) {
			availableCnx.addElement(makeNewConnection());
		}
	}

	private Connection makeNewConnection() throws SQLException {
		try {
			Class.forName(driver);
			Connection connection = DriverManager.getConnection(url, username, password);
			return (connection);

		} catch (ClassNotFoundException cnfe) {
			throw new SQLException("Can't find class for driver: " + driver);
		}
	}

	public synchronized Connection getConnection() throws SQLException {
		if (!availableCnx.isEmpty()) {
			Connection existingConnection = (Connection) availableCnx.lastElement();
			int lastIndex = availableCnx.size() - 1;
			availableCnx.removeElementAt(lastIndex);

			if (existingConnection.isClosed()) {
				notifyAll();
				return (getConnection());
			} else {
				busyCnx.addElement(existingConnection);
				return (existingConnection);
			}
		} else {

			if ((totalConnections() < MaxOpenedConnections) && !connectionPending) {
				makeBackgroundConnection();
			}
			try {
				wait();
			} catch (InterruptedException ie) {
			}
			return (getConnection());
		}
	}

	private void makeBackgroundConnection() {
		connectionPending = true;
		try {
			Thread connectThread = new Thread(this);
			connectThread.start();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
	}

	public synchronized int totalConnections() {
		return (availableCnx.size() + busyCnx.size());
	}

	@Override
	public void run() {
		try {
			Connection connection = makeNewConnection();
			synchronized (this) {
				availableCnx.addElement(connection);
				connectionPending = false;
				notifyAll();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
