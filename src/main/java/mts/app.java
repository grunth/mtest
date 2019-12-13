package mts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class app {
	public static void main(String[] args) throws SQLException, IOException, InterruptedException {
		int cnt = 520;
		int sum = 0;
		JDBCPool pool = new JDBCPool();
		Connection con = pool.getConnection();
		for (int i = 0; i <= cnt; i++) {
			String sql = "select * from test where entry = " + i;
			Thread.currentThread().sleep(10);
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(sql);
			while (rs.next()) {
				sum = sum + rs.getInt("entry");
			}

		}

		System.out.println(sum);

	}
}
