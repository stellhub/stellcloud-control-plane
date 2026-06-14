package io.github.stellcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** StellCloud 统一控制面启动类。 */
@SpringBootApplication
public class StellCloudControlPlaneApplication {

    /**
     * 启动 StellCloud Control Plane。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        System.setProperty("log.stdout", "true");
        SpringApplication.run(StellCloudControlPlaneApplication.class, args);
    }
}
