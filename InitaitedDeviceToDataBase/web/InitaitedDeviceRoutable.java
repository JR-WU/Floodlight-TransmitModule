package net.floodlightcontroller.InitaitedDeviceToDataBase.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import net.floodlightcontroller.restserver.RestletRoutable;


public class InitaitedDeviceRoutable implements RestletRoutable {
    @Override
    public String basePath() {
        return "/wm/getMerged";
    }
    
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/", InitaitedDeviceResource.class);
        return router;
    }

}
