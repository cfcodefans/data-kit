package cf.data.kit.hub

import cf.data.kit.hub.DataHubApp.Companion.log
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.*
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@ImportResource("classpath:spring-kts.xml")
@EnableJpaRepositories(entityManagerFactoryRef = "EMF",
        transactionManagerRef = "TM",
        basePackages = ["cf.data.kit.hub.components"])
@ComponentScan("cf.data.kit.hub.components")
@PropertySource("classpath:application.properties")
//@ConfigurationProperties(prefix = "persistence")
@EnableAsync
open class RepoCfg {
//    companion object {
//        val JPA_EM_NAME = "unit-name"
//    }
//
//    lateinit var cfgs: Map<String, String>
//
//    @Bean(name = ["EMF"])
//    open fun getEMF(): EntityManagerFactory = Persistence.createEntityManagerFactory(cfgs[JPA_EM_NAME], cfgs)
//
//    @Bean(name = ["TM"])
//    open fun getTM(): PlatformTransactionManager = JpaTransactionManager(getEMF())
//
//    //https://stackoverflow.com/questions/20848485/spring-boot-cannot-use-persistence
}

@SpringBootApplication
open class DataHubApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DataHubApp::class.java)
        lateinit var appCtx: ConfigurableApplicationContext
    }
}

@Configuration
@EnableSwagger2
open class Swagger2 {
    @Bean
    open fun createRestApi(): Docket {
        val apiInfo = ApiInfoBuilder()
                .title("MFM Service API")
                .description("Media File Service api service")
                .version("1.0")
                .build()

        return Docket(DocumentationType.SWAGGER_2)
                .forCodeGeneration(true)
                .protocols(setOf("http"))
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("cf.data.kit.hub.components.ctrls"))
                .paths(PathSelectors.any())
                .build()
    }
}

fun main(args: Array<String>) {
    log.info("\n\tDataHubApp Starting......")
    try {
        setIdeaIoUseFallback()
        DataHubApp.appCtx = SpringApplication.run(DataHubApp::class.java, *args)
        log.info("initiation finished")
    } catch (e: Exception) {
        log.error("failed to start", e)
    }
}