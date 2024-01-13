package jihudus.demo.crptapi.localservice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;

@RestController
@RequestMapping("api")
public class RestApi {

    @PostMapping
    public void postApiPoint(HttpServletRequest request) throws IOException {
        System.out.println(Instant.now().getEpochSecond());
        System.out.println(request.getHeader("authorization"));
    }
}
