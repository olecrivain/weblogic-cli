package weblogiccli;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weblogic.logging.LoggingHelper;
import weblogiccli.cmd.Command;
import weblogiccli.cmd.DeployCmd;
import weblogiccli.cmd.ListappsCmd;
import weblogiccli.cmd.UndeployCmd;
import weblogiccli.cmd.exception.ArgsException;
import weblogiccli.conf.Environment;
import weblogiccli.utils.Console;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.ConfigImpl;

public class Main extends Command {

	private static Logger LOG = LoggerFactory.getLogger(Main.class);
    
    private Map<String, Command> commands = new HashMap<String, Command>();

    private static final int OK = 0;
    private static final int KO = 1;

    
    public void printHelp() {
    	LOG.info("Usage : weblogic <commande> [options]");
    	LOG.info("");
    	LOG.info("With, listapps     List applications");
    	LOG.info("      deploy       Deploy an application");
    	LOG.info("      undeploy     Undeploy an application");
    }
    
    public void run(String[] args) {
        
        if (getFlag(args, "-v")) {
            // Affiche la version
            try {
                URL url = Resources.getResource("VERSION");
                String version = Resources.toString(url, Charsets.UTF_8);
                System.out.println("version : \"" + version + "\"");
            } catch (IOException e) {
                LOG.error("Cannot retrieve version number.");
            }
            System.exit(OK);
        }
        
        boolean printStackTrace = getFlag(args, "-e");
        
        String cfg = null;
        try {
            cfg = getOption(args, "-cfg");
        } catch (ArgsException e1) {
        } finally {
            if (cfg == null) {
                LOG.error("You have to specify a properties file with the \"-cfg\" option.");
                System.exit(KO);
            }
        }
        
        Map<String, Environment> environments = loadEnvironments(cfg);
        
        commands.put("listapps", new ListappsCmd(environments));
        commands.put("deploy", new DeployCmd(environments));
        commands.put("undeploy", new UndeployCmd(environments));
        
        if (args.length < 1 || !commands.containsKey(args[0])) {
            printHelp();
            System.exit(1);
        } else {
            long startTime = new Date().getTime();
            
            int exitcode = OK;
            Command command = commands.get(args[0]);
            try {
            	
                // Make weblogic logger less verbose...
                LoggingHelper.getClientLogger().setLevel(Level.WARNING);
            	
                command.run(Arrays.copyOfRange(args, 1, args.length));
                
                printFooter(startTime, exitcode);
                
            } catch (ArgsException e) {
                command.printHelp();
                exitcode = 1;
                
            } catch (Exception e) {
                if (printStackTrace) {
                    LOG.error(e.getMessage(), e);
                } else {
                    LOG.error(e.getMessage());
                }
                exitcode = KO;
                printFooter(startTime, exitcode);
                
            } finally {
                System.exit(exitcode);
            }
        }
    }

    private void printFooter(long startTime, int exitcode) {
        Console.printEmptyLine();
        if (exitcode == OK) {
            Console.printSuccess();
        } else {
            Console.printFailure();
        }
        
        long endTime = new Date().getTime();
        LOG.info("Total time : {}s", (endTime-startTime)/1000.0);
    }

	private Map<String, Environment> loadEnvironments(String cfg) {
		Config conf = ConfigImpl.parseFileAnySyntax(new File(cfg), ConfigParseOptions.defaults()).toConfig();
		Map<String, Environment> environments = new HashMap<String, Environment>();
		for (String env : conf.root().keySet()) {
			Environment environment = new Environment();
			environment.setHost(conf.getString(env + ".host"));
			environment.setPort(conf.getInt(env + ".port"));
			environment.setUsername(conf.getString(env + ".username"));
			environment.setPassword(conf.getString(env + ".password"));
			environments.put(env, environment);
		}
		return environments;
	}
    
    public static void main(String[] args) {
        Main main = new Main();
        main.run(args);
    }
}
