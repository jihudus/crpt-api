package jihudus.demo.crptapi;

import jihudus.demo.crptapi.limiter.CrptApi;
import jihudus.demo.crptapi.limiter.CrptApiWithSlidingWindow;
import jihudus.demo.crptapi.localservice.Start;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class CrptApiApplication {

	@Bean
	CrptApi crptApi() {
		return new CrptApiWithSlidingWindow(TimeUnit.MINUTES, 15);
	}

	@Bean
	Start starter() {
		return new Start(crptApi());
	}

	@Bean
	CommandLineRunner commandLineRunner() {
		return x -> {

			Start starter = starter();
			ExecutorService executor = Executors.newFixedThreadPool(16);
			for (int i = 0; i < 16; i++) {
				int num = i;
				executor.execute(() -> starter.start("TOKEN__________" + num));
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(CrptApiApplication.class, args);
	}

}
