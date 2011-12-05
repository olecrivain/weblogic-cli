package weblogiccli.cmd;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weblogiccli.conf.Environment;
import weblogiccli.jmx.Weblogic;

public class ListappsCmd extends Command {

    static Logger LOG = LoggerFactory.getLogger(ListappsCmd.class);

    public ListappsCmd(Map<String, Environment> environments) {
        super(environments);
    }
    
	public void printHelp() {
    	LOG.info("Utilisation : weblogic listapps <environnement>");
        LOG.info("");
        LOG.info("Liste les applications installées sur un environnement.");
        LOG.info("");
    	printEnvironments();
	}

	public void run(String[] args) throws Exception {
	    Environment environment = checkEnvironment(getByPosition(args, 0));
	    
        Weblogic weblogic = new Weblogic(environment);
		
		for (String app : weblogic.listapps()) {
			LOG.info(app);
		}
	}
	
	// Test
    public static void main(String[] args) throws Exception {
        Map<String, Environment> envs = new HashMap<String, Environment>();

        Environment localhost = new Environment();
        localhost.setHost("localhost");
        localhost.setPort(7001);
        localhost.setUsername("weblogic");
        localhost.setPassword("...");
        envs.put("localhost", localhost);
        
    	ListappsCmd listapps = new ListappsCmd(envs);
    	listapps.run(new String[] { "localhost" });
    }
}
