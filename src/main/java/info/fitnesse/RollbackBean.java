package info.fitnesse;
import org.springframework.transaction.annotation.Transactional;

	 
public class RollbackBean implements RollbackIntf {
	@Transactional(rollbackFor=RollbackNow.class)
	public void process(Runnable r) {
		r.run();
		throw new RollbackNow();
	}
}
