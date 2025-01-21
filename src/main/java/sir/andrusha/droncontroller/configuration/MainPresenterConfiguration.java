package sir.andrusha.droncontroller.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import se.bitcraze.crazyfliecontrol2.MainPresenter;

import java.io.File;

@Configuration
public class MainPresenterConfiguration {

    @Bean
    public MainPresenter mainPresenter() {
        File mCacheDir = new File("TOC_cache");
        mCacheDir.mkdirs();
        MainPresenter pr = new MainPresenter();
        pr.connect(mCacheDir);
        return pr;
    }
}
