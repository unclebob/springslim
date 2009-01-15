package info.fitnesse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class JDBCTestRepository implements TestRepository{
	private JdbcTemplate template;
	public JDBCTestRepository(JdbcTemplate template){
		this.template=template;
	}
	@Transactional
	public void put(String value){
		template.update("insert into test(name) values (?)",new Object[]{value});
	}
	@Transactional
	public int check (String value){
		return template.queryForInt("select count(*) from test where name=?",new Object[]{value});
	}		

}
