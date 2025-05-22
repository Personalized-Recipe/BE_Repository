package ac.su.kdt.prompttest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.util.concurrent.CountDownLatch;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoadTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void userApiLoadTest() throws InterruptedException {
        int numberOfUsers = 100;
        int numberOfRequestsPerUser = 50;
        CountDownLatch latch = new CountDownLatch(numberOfUsers * numberOfRequestsPerUser);

        long startTime = System.currentTimeMillis();

        Flux.range(1, numberOfUsers)
            .flatMap(user -> Flux.range(1, numberOfRequestsPerUser)
                .flatMap(request -> webTestClient
                    .get().uri("/api/users/1")  // 테스트할 엔드포인트
                    .exchange()
                    .returnResult(String.class)
                    .getResponseBody()
                    .doOnNext(response -> {
                        latch.countDown();
                    })
                    .doOnError(error -> {
                        System.err.println("Error: " + error.getMessage());
                        latch.countDown();
                    })
                )
                .subscribeOn(Schedulers.boundedElastic())
            )
            .subscribe();

        latch.await();
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average response time: " + (totalTime / (numberOfUsers * numberOfRequestsPerUser)) + "ms");
    }
} 