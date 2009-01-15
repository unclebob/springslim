package info.fitnesse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public interface TestRepository {

		public void put(String value);;
		public int check (String value);
}
