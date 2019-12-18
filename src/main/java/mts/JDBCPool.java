package mts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.TimerTask;
import java.util.Vector;

public class JDBCPool implements Runnable {

	private String driver, url, username, password;
	private Vector availableCnx, busyCnx;
	private boolean connectionPending = false;
	private int MaxOpenedConnections;
	private long timeWaitFreeConn;
	private long timeWaitDelayCloseConn;

	private boolean waitIfBusy = true;

	public JDBCPool() throws SQLException {
		Prop p = new Prop();
		this.driver = p.Driver;
		this.url = p.URL;
		this.username = p.User;
		this.password = p.Password;
		this.MaxOpenedConnections = p.MaxOpenedConnections;
		this.timeWaitFreeConn = p.timeWaitFreeConn;
		this.timeWaitDelayCloseConn = p.timeWaitDelayCloseConn;

		availableCnx = new Vector(MaxOpenedConnections);
		busyCnx = new Vector();

		for (int i = 0; i < MaxOpenedConnections; i++) {
			availableCnx.addElement(makeNewConnection());
		}
	}

	public synchronized Connection getConnection() throws SQLException {
		if (!availableCnx.isEmpty()) {
			Connection existingConnection = (Connection) availableCnx.lastElement();
			int lastIndex = availableCnx.size() - 1;
			availableCnx.removeElementAt(lastIndex);
			if (existingConnection.isClosed()) {
				notifyAll();
				System.out.println("Connection closed: " + this.toString());
				return getConnection();
			} else {
				busyCnx.addElement(existingConnection);
				System.out.println(this.toString());
				return existingConnection;
			}

		} else {
			if ((totalConnections() < MaxOpenedConnections) && !connectionPending) {
				makeBackgroundConnection();
			}

			else if (!waitIfBusy) {
				System.out.println("Исключительная ситуация: Превышено время в течение которого пул будет ожидать свободного соединения");
			}

			try {
				wait(timeWaitFreeConn);
				waitIfBusy = false;
			}

			catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println(this.toString());
			return getConnection();

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
		return availableCnx.size() + busyCnx.size();
	}

	public void release(Connection connection) {
		new java.util.Timer().schedule(new TimerTask() {
			public void run() {
				busyCnx.removeElement(connection);
				availableCnx.addElement(connection);
				System.out.println("Соединение было удалено");
			}
		}, timeWaitDelayCloseConn);
		System.out.println(this.toString());
	}

	private void closeConnections(Vector connections) {
		try {
			for (int i = 0; i < connections.size(); i++) {
				Connection connection = (Connection) connections.elementAt(i);
				if (!connection.isClosed()) {
					connection.close();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private Connection makeNewConnection() throws SQLException {
		try {
			Class.forName(driver);
			Connection connection = DriverManager.getConnection(url, username, password);
			return (connection);

		} catch (ClassNotFoundException e) {
			throw new SQLException("Can't find class for driver: " + driver);
		}
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

	public synchronized String toString() {
		return "available=" + availableCnx.size() + ", busy=" + busyCnx.size() + ", MaxOpenedConnections=" + MaxOpenedConnections;
		
	}

}
