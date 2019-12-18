package mts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TimerTask;

public class app {
	public static void main(String[] args) throws SQLException, IOException, InterruptedException {
		int cnt = 10;
		JDBCPool pool = new JDBCPool();
		for (int i = 1; i <= cnt; i++) {
			createLongTimeConnnections(pool, i);
		}
		System.out.println("Создаем новый запрос, который будет ожидать свободный con");
		Connection con = pool.getConnection();
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery("select * from test where entry = 1");
		while (rs.next()) {
			System.out.println("Выполнился запрос, который ждал, пока освободится con");
		}
		pool.release(con);
	}

	public static void createLongTimeConnnections(JDBCPool pool, int i) throws SQLException {
		//Проверяем что соединение повисает в ожидании
		Connection con = pool.getConnection();
		new java.util.Timer().schedule(new TimerTask() {
			public void run() {
				String sql = "select * from test where entry = " + i;
				try {
					Statement st = con.createStatement();
					ResultSet rs = st.executeQuery(sql);
					while (rs.next()) {
						System.out.println(rs.getInt("entry"));
					}
					pool.release(con);

				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}, 5000);
	}
}