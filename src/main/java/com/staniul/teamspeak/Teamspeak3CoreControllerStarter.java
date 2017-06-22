package com.staniul.teamspeak;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class Teamspeak3CoreControllerStarter
        implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private final Teamspeak3CoreController coreController;

    @Autowired
    public Teamspeak3CoreControllerStarter(Teamspeak3CoreController coreController) {
        this.coreController = coreController;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        coreController.setApplicationContext(applicationContext);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        coreController.init();
    }
}
