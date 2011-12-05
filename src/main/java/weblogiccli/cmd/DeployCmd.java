package weblogiccli.cmd;


import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weblogic.management.configuration.AppDeploymentMBean;
import weblogiccli.conf.Environment;
import weblogiccli.jmx.Weblogic;
import weblogiccli.utils.Console;

import com.google.common.collect.Sets;

public class DeployCmd extends Command {

    static Logger LOG = LoggerFactory.getLogger(DeployCmd.class);
    
    public DeployCmd(Map<String, Environment> environments) {
        super(environments);
    }
    
	@Override
	public void printHelp() {
    	LOG.info("Utilisation : weblogic deploy <env> <cible> <war> [--nom <nom>] [--force] [--dryrun]");
        LOG.info("");
        printEnvironments();
    	LOG.info("");
    	LOG.info("Options:");
        LOG.info("  --nom     Nom de l'application sur weblogic. Le nom par défaut est le");
        LOG.info("            nom du fichier war sans le numéro de version et sans le");
        LOG.info("            \".war\".");
        LOG.info("  --force   Force le déploiement même s'il y a déjà 2 versions actives");
        LOG.info("            de l'application. Attention, ce type de déploiement supprime");
        LOG.info("            des sessions");
        LOG.info("  --dryrun  Affiche le plan de déploiement sans l'exécuter.");
	}

	@Override
	public void run(String[] args) throws Exception {
        Environment environment = checkEnvironment(getByPosition(args, 0));
        String target = getByPosition(args, 1);
        
        String warFilename = getByPosition(args, 2);
        File war = checkWar(warFilename);
        
        String appName = getOption(args, "--nom", applicationName(war));
        if (getOption(args, "--nom") == null) {
            LOG.info("Nom du déploiement calculé à partir du fichier war : \"{}\"", appName);
        }
        
        boolean force = getFlag(args, "--force");
        boolean dryrun = getFlag(args, "--dryrun");
        
        final Weblogic weblogic = new Weblogic(environment);
        String version = weblogic.retrieveWeblogicApplicationVersion(war);
        
        if (version != null) {
            LOG.info("Version : \"{}\"", version);
        } else {
            LOG.info("Aucune version détéctée");
        }
	    
        Console.printEmptyLine();
        weblogic.checkServersAreRunning();
		
		Set<AppDeploymentMBean> apps = weblogic.findApplications(appName);
		
		if (apps.size() > 0) {
		    Console.printEmptyLine();
		    LOG.info("Versions déjà deployées :");
    		for (AppDeploymentMBean app : apps) {
                LOG.info("  " + weblogic.appDeploymentMBeanToString(app));
            }
		}

		// Versions actives
		Set<AppDeploymentMBean> activeApps = new HashSet<AppDeploymentMBean>();
		for (AppDeploymentMBean a : apps) {
			if (weblogic.appState(a).equals("STATE_ACTIVE")) {
				activeApps.add(a);
			}
		}
        
        // Versions inactives
		Set<AppDeploymentMBean> inactiveApps = Sets.difference(apps, activeApps);
        
		// Liste des versions à désinstaller
		Set<String> appsToRemove = new HashSet<String>();
		for (AppDeploymentMBean app : inactiveApps) {
            appsToRemove.add(app.getName());
        }
		
		if (activeApps.size() >=2) {
		    if (force) {
		        // Ajout l'application active la plus ancienne à la liste des applications à supprimer
		        Set<String> activeAppNames = new HashSet<String>();
		        for (AppDeploymentMBean app : activeApps) {
		            activeAppNames.add(app.getName());
		        }
		        appsToRemove.add(Collections.min(activeAppNames));
		    } else {
                String msg = "Impossible de déployer l'application car il y a déjà 2 versions ";
                msg += "actives installées! Utilisez l'option --force pour forcer l'installation.";
                throw new IllegalStateException(msg);
		    }
		}
		
        // Désinstallations
        for (String atr : appsToRemove) {
            Console.printEmptyLine();
        	weblogic.undeploy(atr, dryrun);
        }
        
        // Déploiement
        Console.printEmptyLine();
        weblogic.deploy(appName, war, target, dryrun);
	}

	public String applicationName(File war) {
	    String basename = war.getName();
	    if (StringUtils.countMatches(basename, "_") >= 4) {
	        // Ex : it-wlst-test_INTE_INTR_20111028_1119.war -> it-wlst-test
	        String[] split = basename.split("_");
            return StringUtils.join(Arrays.copyOfRange(split, 0, split.length-4), "_");
	    } else {
	        // Ex : it-wlst-test-1.0-SNAPSHOT.war -> it-wlst-test
	        Pattern pattern = Pattern.compile("(.+)-[0-9]+((\\.[0-9]+)*)?(-SNAPSHOT)?(\\.war)?$");
	        Matcher matcher = pattern.matcher(basename);
	        matcher.find();
	        try {
	            String group1 = matcher.group(1);
	            return group1;
	        } catch (Exception e) {
	            // Si le fichier n'a pas de version, retourne le nom du fichier sans le ".war"
	            return basename.replace(".war", "");
	        }
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
        
        DeployCmd deploy = new DeployCmd(envs);
		try {
            deploy.run(new String[] { "localhost", "AdminServer", "src/test/resources/it-wlst-test.war" });
		} catch (Exception e) {
		    e.printStackTrace();
		}
    }
}
