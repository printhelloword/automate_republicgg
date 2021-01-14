package bot.republic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RepublicApplication {
//	public static Logger logger = LogManager.getRootLogger();
public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RepublicApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(RepublicApplication.class, args);
	}
}
