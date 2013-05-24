package weblogiccli.cmd;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weblogiccli.conf.Environment;
import weblogiccli.jmx.Weblogic;


public class UndeployCmd extends Command {

    static Logger LOG = LoggerFactory.getLogger(UndeployCmd.class);

    public UndeployCmd(Map<String, Environment> environments) {
        super(environments);
    }
    
	@Override
	public void printHelp() {
    	LOG.info("Usage : weblogic undeploy <environnement> <name> [--version <version>]");
    	LOG.info("");
        printEnvironments();
    	LOG.info("");
    	LOG.info("Options :");
    	LOG.info("  --version  Application version to undeploy.");
	}

	@Override
	public void run(String[] args) throws Exception {
	    Environment environment = checkEnvironment(getByPosition(args, 0));
	    String name = getByPosition(args, 1);
	    String version = getOption(args, "--version");
        
        Weblogic weblogic = new Weblogic(environment);
        weblogic.undeploy(name, version);
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
        
        UndeployCmd undeploy = new UndeployCmd(envs);
        undeploy.run(new String[] { "localhost", "wlst-test", "--version", "3" });
    }
}
