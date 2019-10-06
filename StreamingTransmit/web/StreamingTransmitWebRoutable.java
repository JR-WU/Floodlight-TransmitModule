
package net.floodlightcontroller.StreamingTransmit.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class StreamingTransmitWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		// TODO Auto-generated method stub
        Router router = new Router(context);
        router.attach("/Trans", StreamingTransmitResource.class);
        return router;
	}

	@Override
	public String basePath() {
		// TODO Auto-generated method stub
		return "/wm/streamingtransit";
	}

}
// curl http://localhost:8080/wm/streamingtransit/Trans -X POST -d '{"IPSrc":"10.0.0.1","IPDst":"10.0.0.2"}' | python -m json.tool

