package searchengine.config;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public RussianLuceneMorphology luceneMorphology() throws Exception {
        return new RussianLuceneMorphology();
    }


}