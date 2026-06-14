package io.github.stellcloud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** StellCloud 控制面启动测试。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StellCloudControlPlaneApplicationTests {

    /** 验证 Spring Boot 上下文可以完成启动。 */
    @Test
    void contextLoads() {}
}
